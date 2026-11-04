
package com.example.vetman01.data

import com.example.vetman01.model.Appointment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import java.util.UUID

/**
 * QueuedLocally means the operation is in Firestore's
 * local cache, but is not yet confirmed by the server.
 */
sealed class AppointmentSaveStatus {
    data object QueuedLocally : AppointmentSaveStatus()
    data object Synced : AppointmentSaveStatus()
    data class Failed(val error: Exception) :
        AppointmentSaveStatus()
}

/**
 * Offline-aware appointment creation and updates.
 *
 * No server read is made before creating an appointment,
 * avoiding permission errors on nonexistent documents.
 */
class AppointmentOfflineRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Create an appointment using one stable submission ID.
     *
     * Repeated writes with this ID cannot create additional
     * appointment documents. The screen also locks the
     * submission button to prevent duplicate attempts.
     */
    fun addAppointmentOfflineAware(
        appointment: Appointment,
        onStatus: (AppointmentSaveStatus) -> Unit,
        submissionId: String = appointment.id.ifBlank {
            UUID.randomUUID().toString()
        }
    ): () -> Unit {
        val vetId = auth.currentUser?.uid

        if (vetId == null) {
            onStatus(
                AppointmentSaveStatus.Failed(
                    IllegalStateException("Not signed in")
                )
            )
            return {}
        }

        if (appointment.patientId.isBlank()) {
            onStatus(
                AppointmentSaveStatus.Failed(
                    IllegalArgumentException(
                        "Patient ID is missing."
                    )
                )
            )
            return {}
        }

        if (appointment.scheduledAt <= System.currentTimeMillis()) {
            onStatus(
                AppointmentSaveStatus.Failed(
                    IllegalArgumentException(
                        "Select a future appointment date."
                    )
                )
            )
            return {}
        }

        if (
            submissionId.isBlank() ||
            submissionId.contains("/")
        ) {
            onStatus(
                AppointmentSaveStatus.Failed(
                    IllegalArgumentException(
                        "Invalid submission ID."
                    )
                )
            )
            return {}
        }

        val document = db.collection("appointments")
            .document(submissionId)

        val toSave = appointment.copy(
            id = submissionId,
            vetId = vetId,
            status = "scheduled",
            createdAt = System.currentTimeMillis()
        )

        // IMPORTANT:
        // Do not attach a server-backed snapshot listener
        // before the appointment exists. Its read can fail
        // even when the following write is allowed.
        return observeWrite(
            document = document,
            expectedValues = mapOf(
                "id" to submissionId,
                "patientId" to appointment.patientId,
                "vetId" to vetId,
                "scheduledAt" to appointment.scheduledAt,
                "status" to "scheduled"
            ),
            performWrite = { success, failure ->
                document.set(toSave)
                    .addOnSuccessListener { success() }
                    .addOnFailureListener { failure(it) }
            },
            onStatus = onStatus
        )
    }

    /**
     * Reschedule an existing appointment without
     * changing its patient, owner, or document identity.
     */
    fun rescheduleOfflineAware(
        appointmentId: String,
        scheduledAt: Long,
        onStatus: (AppointmentSaveStatus) -> Unit
    ): () -> Unit {
        if (scheduledAt <= System.currentTimeMillis()) {
            onStatus(
                AppointmentSaveStatus.Failed(
                    IllegalArgumentException(
                        "Select a future appointment date."
                    )
                )
            )
            return {}
        }

        return updateOfflineAware(
            appointmentId = appointmentId,
            changes = mapOf(
                "scheduledAt" to scheduledAt,
                "status" to "scheduled"
            ),
            onStatus = onStatus
        )
    }

    /**
     * Cancellation changes status; it does not delete
     * the appointment or its historical information.
     */
    fun updateStatusOfflineAware(
        appointmentId: String,
        status: String,
        onStatus: (AppointmentSaveStatus) -> Unit
    ): () -> Unit {
        if (
            status !in listOf(
                "scheduled", "completed", "cancelled"
            )
        ) {
            onStatus(
                AppointmentSaveStatus.Failed(
                    IllegalArgumentException(
                        "Invalid appointment status."
                    )
                )
            )
            return {}
        }

        return updateOfflineAware(
            appointmentId = appointmentId,
            changes = mapOf("status" to status),
            onStatus = onStatus
        )
    }

    /**
     * Update only fields permitted by the existing
     * Firestore appointment security rules.
     */
    private fun updateOfflineAware(
        appointmentId: String,
        changes: Map<String, Any>,
        onStatus: (AppointmentSaveStatus) -> Unit
    ): () -> Unit {
        if (auth.currentUser == null) {
            onStatus(
                AppointmentSaveStatus.Failed(
                    IllegalStateException("Not signed in")
                )
            )
            return {}
        }

        if (appointmentId.isBlank() || appointmentId.contains("/")) {
            onStatus(
                AppointmentSaveStatus.Failed(
                    IllegalArgumentException(
                        "Invalid appointment ID."
                    )
                )
            )
            return {}
        }

        val document = db.collection("appointments")
            .document(appointmentId)

        return observeWrite(
            document = document,
            expectedValues = changes,
            performWrite = { success, failure ->
                document.update(changes)
                    .addOnSuccessListener { success() }
                    .addOnFailureListener { failure(it) }
            },
            onStatus = onStatus
        )
    }

    /**
     * Shared local-first write observation.
     *
     * Instead of listening to the server document,
     * we read ONLY the local Firestore cache after
     * submitting the write.
     *
     * The original write task is the authority for
     * whether Firestore ultimately accepts the change.
     */
    private fun observeWrite(
        document: DocumentReference,
        expectedValues: Map<String, Any>,
        performWrite: (
            onSuccess: () -> Unit,
            onFailure: (Exception) -> Unit
        ) -> Unit,
        onStatus: (AppointmentSaveStatus) -> Unit
    ): () -> Unit {

        var detached = false
        var finished = false
        var queuedReported = false

        fun finish(status: AppointmentSaveStatus) {
            if (finished) return

            finished = true

            if (!detached) {
                onStatus(status)
            }
        }

        // Submit the write before attempting any read.
        performWrite(
            {
                finish(AppointmentSaveStatus.Synced)
            },
            { error ->
                finish(AppointmentSaveStatus.Failed(error))
            }
        )

        // A cache-only read cannot produce a server-side
        // permission error from a nonexistent document.
        document.get(Source.CACHE)
            .addOnSuccessListener { snapshot ->
                if (detached || finished || queuedReported) {
                    return@addOnSuccessListener
                }

                if (!snapshot.exists()) {
                    return@addOnSuccessListener
                }

                val matches = expectedValues.all { (field, value) ->
                    when (value) {
                        is Long ->
                            snapshot.getLong(field) == value

                        is String ->
                            snapshot.getString(field) == value

                        else -> false
                    }
                }

                if (
                    matches &&
                    snapshot.metadata.hasPendingWrites()
                ) {
                    queuedReported = true
                    onStatus(AppointmentSaveStatus.QueuedLocally)
                }
            }
            .addOnFailureListener {
                // Cache-only availability is best-effort.
                // Do not report this as a failed write:
                // the write task still determines its result.
            }

        // Detach UI callbacks, never the Firestore write.
        return {
            detached = true
        }
    }
}
