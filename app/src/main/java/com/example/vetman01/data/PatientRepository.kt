
package com.example.vetman01.data

import com.example.vetman01.model.Patient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges

/**
 * Tracks the progress of saving a new patient.
 * A local save is not the same as server confirmation.
 */
sealed class PatientSaveStatus {
    data object QueuedLocally : PatientSaveStatus()
    data object Synced : PatientSaveStatus()
    data class Failed(val error: Exception) : PatientSaveStatus()
}

/**
 * Handles patient records for veterinarians and pet owners.
 * Firestore operations remain centralized here.
 */
class PatientRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Original patient creation method.
     * Preserved for compatibility with existing callers.
     */
    fun addPatient(
        patient: Patient,
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

        val doc = db.collection("pets").document()

        val toSave = patient.copy(
            id = doc.id,
            vetId = vetId,
            ownerId = "",
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

    /**
     * OFFLINE-AWARE PATIENT CREATION
     *
     * Firestore applies the write locally before
     * synchronizing it with the server.
     *
     * The generated document ID remains the same,
     * preventing duplicate patient creation.
     *
     * Returns a function that removes the observer.
     * Removing the observer does not cancel the write.
     */
    fun addPatientOfflineAware(
        patient: Patient,
        onStatus: (PatientSaveStatus) -> Unit
    ): () -> Unit {
        val vetId = auth.currentUser?.uid

        if (vetId == null) {
            onStatus(
                PatientSaveStatus.Failed(
                    IllegalStateException("Not signed in")
                )
            )
            return {}
        }

        val doc = db.collection("pets").document()

        val toSave = patient.copy(
            id = doc.id,
            vetId = vetId,
            ownerId = "",
            createdAt = System.currentTimeMillis()
        )

        var listener: ListenerRegistration? = null
        var detached = false
        var queued = false
        var finished = false

        fun finish(status: PatientSaveStatus) {
            if (detached || finished) return

            finished = true
            listener?.remove()
            listener = null

            onStatus(status)
        }

        // Observe local writes and server synchronization.
        listener = doc.addSnapshotListener(
            MetadataChanges.INCLUDE
        ) { snapshot, error ->

            if (detached || finished) {
                return@addSnapshotListener
            }

            if (error != null) {
                finish(PatientSaveStatus.Failed(error))
                return@addSnapshotListener
            }

            if (snapshot == null || !snapshot.exists()) {
                return@addSnapshotListener
            }

            if (snapshot.metadata.hasPendingWrites()) {
                if (!queued) {
                    queued = true
                    onStatus(PatientSaveStatus.QueuedLocally)
                }
            } else if (queued) {
                finish(PatientSaveStatus.Synced)
            }
        }

        doc.set(toSave)
            .addOnSuccessListener {
                finish(PatientSaveStatus.Synced)
            }
            .addOnFailureListener { error ->
                finish(PatientSaveStatus.Failed(error))
            }

        return {
            detached = true
            listener?.remove()
            listener = null
        }
    }

    /**
     * Retrieve patients registered by the veterinarian.
     */
    fun getPatients(
        onResult: (Result<List<Patient>>) -> Unit
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

        db.collection("pets")
            .whereEqualTo("vetId", vetId)
            .get()
            .addOnSuccessListener { snapshot ->
                val patients = snapshot.documents.mapNotNull {
                    it.toObject(Patient::class.java)
                }

                onResult(Result.success(patients))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    /**
     * Retrieve pets linked to the signed-in owner.
     * Security rules enforce ownerId access.
     */
    fun getOwnerPatients(
        onResult: (Result<List<Patient>>) -> Unit
    ) {
        val ownerId = auth.currentUser?.uid

        if (ownerId == null) {
            onResult(
                Result.failure(
                    IllegalStateException("Not signed in")
                )
            )
            return
        }

        db.collection("pets")
            .whereEqualTo("ownerId", ownerId)
            .get()
            .addOnSuccessListener { snapshot ->
                val patients = snapshot.documents.mapNotNull {
                    it.toObject(Patient::class.java)
                }

                onResult(Result.success(patients))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    /**
     * Update patient details without modifying
     * identity, ownership, or creation information.
     */
    fun updatePatient(
        patient: Patient,
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

        if (patient.id.isBlank()) {
            onResult(
                Result.failure(
                    IllegalArgumentException(
                        "Patient ID is missing"
                    )
                )
            )
            return
        }

        if (patient.vetId != vetId) {
            onResult(
                Result.failure(
                    SecurityException(
                        "You cannot edit this patient"
                    )
                )
            )
            return
        }

        db.collection("pets")
            .document(patient.id)
            .update(
                mapOf(
                    "petName" to patient.petName,
                    "species" to patient.species,
                    "breed" to patient.breed,
                    "age" to patient.age,
                    "sex" to patient.sex,
                    "ownerName" to patient.ownerName,
                    "ownerPhone" to patient.ownerPhone,
                    "notes" to patient.notes
                )
            )
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    /**
     * LEGACY OWNER LINKING METHOD
     *
     * Retained for compatibility only.
     * Secure pet-owner linking now uses invitations
     * and owner approval.
     *
     * Current Firestore rules reject direct ownerId
     * changes made through this method.
     */
    fun linkPetOwner(
        patientId: String,
        ownerId: String,
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

        if (patientId.isBlank() || ownerId.isBlank()) {
            onResult(
                Result.failure(
                    IllegalArgumentException(
                        "Patient or owner ID is missing"
                    )
                )
            )
            return
        }

        val patientRef = db.collection("pets")
            .document(patientId)

        patientRef.get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {
                    onResult(
                        Result.failure(
                            IllegalStateException(
                                "Patient not found"
                            )
                        )
                    )
                    return@addOnSuccessListener
                }

                if (document.getString("vetId") != vetId) {
                    onResult(
                        Result.failure(
                            SecurityException(
                                "You cannot link this patient"
                            )
                        )
                    )
                    return@addOnSuccessListener
                }

                db.collection("users")
                    .document(ownerId)
                    .get()
                    .addOnSuccessListener { ownerDoc ->

                        if (
                            !ownerDoc.exists() ||
                            ownerDoc.getString("role") != "pet_owner"
                        ) {
                            onResult(
                                Result.failure(
                                    IllegalStateException(
                                        "Registered pet owner not found"
                                    )
                                )
                            )
                            return@addOnSuccessListener
                        }

                        patientRef.update("ownerId", ownerId)
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
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }
}
