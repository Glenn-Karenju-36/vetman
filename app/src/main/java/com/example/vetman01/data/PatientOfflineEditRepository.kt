package com.example.vetman01.data

import com.example.vetman01.model.Patient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges

/**
 * Status of an offline-capable patient edit.
 *
 * QueuedLocally: Firestore has applied the edit locally.
 * Synced: The server has acknowledged the edit.
 * Failed: The write was rejected or couldn't be applied.
 */
sealed class PatientEditSaveStatus {
    data object QueuedLocally : PatientEditSaveStatus()
    data object Synced : PatientEditSaveStatus()
    data class Failed(val error: Exception) :
        PatientEditSaveStatus()
}

/**
 * Handles offline-aware editing of existing patients.
 *
 * It does not modify ownership, identity, or
 * creation-time fields.
 */
class PatientOfflineEditRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Save an existing patient's editable details.
     *
     * Returns a function for detaching the UI listener.
     * Detaching does not cancel a queued Firestore write.
     */
    fun updatePatientOfflineAware(
        patient: Patient,
        onStatus: (PatientEditSaveStatus) -> Unit
    ): () -> Unit {

        val vetId = auth.currentUser?.uid

        if (vetId == null) {
            onStatus(
                PatientEditSaveStatus.Failed(
                    IllegalStateException("Not signed in")
                )
            )
            return {}
        }

        if (patient.id.isBlank()) {
            onStatus(
                PatientEditSaveStatus.Failed(
                    IllegalArgumentException(
                        "Patient ID is missing"
                    )
                )
            )
            return {}
        }

        if (patient.vetId != vetId) {
            onStatus(
                PatientEditSaveStatus.Failed(
                    SecurityException(
                        "You cannot edit this patient"
                    )
                )
            )
            return {}
        }

        val document = db.collection("pets")
            .document(patient.id)

        // Only these existing fields are editable.
        // The security rules enforce this restriction too.
        val changes = mapOf(
            "petName" to patient.petName,
            "species" to patient.species,
            "breed" to patient.breed,
            "age" to patient.age,
            "sex" to patient.sex,
            "ownerName" to patient.ownerName,
            "ownerPhone" to patient.ownerPhone,
            "notes" to patient.notes
        )

        var listener: ListenerRegistration? = null
        var detached = false
        var queued = false
        var finished = false

        fun finish(status: PatientEditSaveStatus) {
            if (detached || finished) return

            finished = true
            listener?.remove()
            listener = null

            onStatus(status)
        }

        // Observe metadata to distinguish a locally applied
        // edit from a server-confirmed one.
        listener = document.addSnapshotListener(
            MetadataChanges.INCLUDE
        ) { snapshot, error ->

            if (detached || finished) {
                return@addSnapshotListener
            }

            if (error != null) {
                finish(PatientEditSaveStatus.Failed(error))
                return@addSnapshotListener
            }

            if (snapshot == null || !snapshot.exists()) {
                return@addSnapshotListener
            }

            // Only report our edit after its local values
            // actually appear in the document snapshot.
            val hasOurChanges = changes.all {
                    (key, value) ->
                snapshot.getString(key) == value
            }

            if (!hasOurChanges) {
                return@addSnapshotListener
            }

            if (snapshot.metadata.hasPendingWrites()) {
                if (!queued) {
                    queued = true
                    onStatus(
                        PatientEditSaveStatus.QueuedLocally
                    )
                }
            } else if (queued) {
                finish(PatientEditSaveStatus.Synced)
            }
        }

        // update() preserves all other document fields.
        // Its success callback waits for backend approval.
        document.update(changes)
            .addOnSuccessListener {
                finish(PatientEditSaveStatus.Synced)
            }
            .addOnFailureListener { error ->
                finish(PatientEditSaveStatus.Failed(error))
            }

        return {
            detached = true
            listener?.remove()
            listener = null
        }
    }
}
