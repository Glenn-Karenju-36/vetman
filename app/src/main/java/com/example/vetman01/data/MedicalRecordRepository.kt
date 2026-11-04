
package com.example.vetman01.data

import com.example.vetman01.model.MedicalRecord
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query

/**
 * Tracks whether a medical record is queued locally,
 * confirmed by Firestore, or rejected.
 */
sealed class MedicalRecordSaveStatus {
    data object QueuedLocally : MedicalRecordSaveStatus()
    data object Synced : MedicalRecordSaveStatus()
    data class Failed(val error: Exception) :
        MedicalRecordSaveStatus()
}

/**
 * Manages medical records stored under:
 * pets/{patientId}/medicalRecords/{recordId}
 */
class MedicalRecordRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Original server-confirmed save.
     * Preserved for compatibility with existing callers.
     */
    fun addMedicalRecord(
        patientId: String,
        record: MedicalRecord,
        onResult: (Result<Unit>) -> Unit
    ) {
        val vetId = auth.currentUser?.uid

        if (vetId == null) {
            onResult(
                Result.failure(
                    IllegalStateException("Not signed in")
                )
            )
            return
        }

        if (patientId.isBlank()) {
            onResult(
                Result.failure(
                    IllegalArgumentException("Patient ID is missing")
                )
            )
            return
        }

        val patientRef = db.collection("pets")
            .document(patientId)

        patientRef.get()
            .addOnSuccessListener { patientSnapshot ->
                if (
                    !patientSnapshot.exists() ||
                    patientSnapshot.getString("vetId") != vetId
                ) {
                    onResult(
                        Result.failure(
                            IllegalStateException(
                                "Patient not found or access denied"
                            )
                        )
                    )
                    return@addOnSuccessListener
                }

                val doc = patientRef
                    .collection("medicalRecords")
                    .document()

                val toSave = record.copy(
                    id = doc.id,
                    patientId = patientId,
                    vetId = vetId,
                    createdAt = System.currentTimeMillis()
                )

                doc.set(toSave)
                    .addOnSuccessListener {
                        onResult(Result.success(Unit))
                    }
                    .addOnFailureListener { error ->
                        onResult(Result.failure(error))
                    }
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    /**
     * OFFLINE-AWARE SAVE
     *
     * Writes directly to Firestore's local cache.
     * The Firestore security rules validate ownership
     * when the record synchronizes with the server.
     *
     * Returns a function that removes the listener.
     * Removing it does not cancel the pending write.
     */
    fun addMedicalRecordOfflineAware(
        patientId: String,
        record: MedicalRecord,
        onStatus: (MedicalRecordSaveStatus) -> Unit
    ): () -> Unit {
        val vetId = auth.currentUser?.uid

        if (vetId == null) {
            onStatus(
                MedicalRecordSaveStatus.Failed(
                    IllegalStateException("Not signed in")
                )
            )
            return {}
        }

        if (patientId.isBlank()) {
            onStatus(
                MedicalRecordSaveStatus.Failed(
                    IllegalArgumentException(
                        "Patient ID is missing"
                    )
                )
            )
            return {}
        }

        // Generate the document ID once.
        // Offline synchronization reuses this ID.
        val doc = db.collection("pets")
            .document(patientId)
            .collection("medicalRecords")
            .document()

        val toSave = record.copy(
            id = doc.id,
            patientId = patientId,
            vetId = vetId,
            createdAt = System.currentTimeMillis()
        )

        var registration: ListenerRegistration? = null
        var detached = false
        var queued = false
        var finished = false

        fun reportFinal(status: MedicalRecordSaveStatus) {
            if (detached || finished) return

            finished = true
            registration?.remove()
            registration = null
            onStatus(status)
        }

        // Metadata changes reveal when a local write
        // is still waiting for server acknowledgment.
        registration = doc.addSnapshotListener(
            MetadataChanges.INCLUDE
        ) { snapshot, error ->

            if (detached || finished) {
                return@addSnapshotListener
            }

            if (error != null) {
                reportFinal(
                    MedicalRecordSaveStatus.Failed(error)
                )
                return@addSnapshotListener
            }

            if (snapshot == null || !snapshot.exists()) {
                return@addSnapshotListener
            }

            if (snapshot.metadata.hasPendingWrites()) {
                if (!queued) {
                    queued = true
                    onStatus(
                        MedicalRecordSaveStatus.QueuedLocally
                    )
                }
            } else if (queued) {
                reportFinal(MedicalRecordSaveStatus.Synced)
            }
        }

        // Firestore applies this locally first.
        // Success normally follows server acknowledgment.
        doc.set(toSave)
            .addOnSuccessListener {
                reportFinal(MedicalRecordSaveStatus.Synced)
            }
            .addOnFailureListener { error ->
                reportFinal(
                    MedicalRecordSaveStatus.Failed(error)
                )
            }

        return {
            detached = true
            registration?.remove()
            registration = null
        }
    }

    /**
     * Returns the medical history for an authorized
     * veterinarian or linked pet owner.
     */
    fun getMedicalRecords(
        patientId: String,
        onResult: (Result<List<MedicalRecord>>) -> Unit
    ) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            onResult(
                Result.failure(
                    IllegalStateException("Not signed in")
                )
            )
            return
        }

        if (patientId.isBlank()) {
            onResult(
                Result.failure(
                    IllegalArgumentException(
                        "Patient ID is missing"
                    )
                )
            )
            return
        }

        val patientRef = db.collection("pets")
            .document(patientId)

        patientRef.get()
            .addOnSuccessListener { patientSnapshot ->

                if (!patientSnapshot.exists()) {
                    onResult(
                        Result.failure(
                            IllegalStateException(
                                "Patient not found"
                            )
                        )
                    )
                    return@addOnSuccessListener
                }

                val vetId =
                    patientSnapshot.getString("vetId")

                val ownerId =
                    patientSnapshot.getString("ownerId")

                if (vetId != userId && ownerId != userId) {
                    onResult(
                        Result.failure(
                            SecurityException(
                                "You cannot access this patient's records"
                            )
                        )
                    )
                    return@addOnSuccessListener
                }

                patientRef.collection("medicalRecords")
                    .orderBy(
                        "visitDate",
                        Query.Direction.DESCENDING
                    )
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val records =
                            snapshot.documents.mapNotNull {
                                it.toObject(
                                    MedicalRecord::class.java
                                )
                            }

                        onResult(Result.success(records))
                    }
                    .addOnFailureListener { error ->
                        onResult(Result.failure(error))
                    }
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }
}
