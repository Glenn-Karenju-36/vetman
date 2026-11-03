
package com.example.vetman01.data

import com.example.vetman01.model.Patient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Handles patient records for veterinarians and pet owners.
 * All Firestore patient operations belong in this repository.
 */
class PatientRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Create a patient owned by the signed-in veterinarian.
     * A new patient initially has no linked owner account.
     */
    fun addPatient(
        patient: Patient,
        onResult: (Result<Unit>) -> Unit
    ) {
        val vetId = auth.currentUser?.uid

        if (vetId == null) {
            onResult(Result.failure(Exception("Not signed in")))
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
            .addOnFailureListener { exception ->
                onResult(Result.failure(exception))
            }
    }

    /**
     * Retrieve patients registered by the signed-in veterinarian.
     */
    fun getPatients(
        onResult: (Result<List<Patient>>) -> Unit
    ) {
        val vetId = auth.currentUser?.uid

        if (vetId == null) {
            onResult(Result.failure(Exception("Not signed in")))
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
            .addOnFailureListener { exception ->
                onResult(Result.failure(exception))
            }
    }

    /**
     * OWNER PATIENTS
     *
     * Retrieve only pets linked to the signed-in pet owner.
     *
     * Firestore security rules independently verify that
     * the requesting user is authorized to read these records.
     */
    fun getOwnerPatients(
        onResult: (Result<List<Patient>>) -> Unit
    ) {
        val ownerId = auth.currentUser?.uid

        if (ownerId == null) {
            onResult(Result.failure(Exception("Not signed in")))
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
            .addOnFailureListener { exception ->
                onResult(Result.failure(exception))
            }
    }

    /**
     * Update editable patient fields without changing
     * ownership, identity or creation information.
     */
    fun updatePatient(
        patient: Patient,
        onResult: (Result<Unit>) -> Unit
    ) {
        val vetId = auth.currentUser?.uid

        if (vetId == null) {
            onResult(Result.failure(Exception("Not signed in")))
            return
        }

        if (patient.id.isBlank()) {
            onResult(
                Result.failure(Exception("Patient ID is missing"))
            )
            return
        }

        if (patient.vetId != vetId) {
            onResult(
                Result.failure(Exception("You cannot edit this patient"))
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
            .addOnFailureListener { exception ->
                onResult(Result.failure(exception))
            }
    }

    /**
     * EXISTING OWNER LINKING METHOD
     *
     * Retained for future secure linking implementation.
     * Current Firestore rules prevent this client-side method
     * from reading another user's profile or changing ownerId.
     */
    fun linkPetOwner(
        patientId: String,
        ownerId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val vetId = auth.currentUser?.uid

        if (vetId == null) {
            onResult(Result.failure(Exception("Not signed in")))
            return
        }

        if (patientId.isBlank() || ownerId.isBlank()) {
            onResult(
                Result.failure(Exception("Patient or owner ID is missing"))
            )
            return
        }

        val patientRef = db.collection("pets").document(patientId)

        patientRef.get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {
                    onResult(
                        Result.failure(Exception("Patient not found"))
                    )
                    return@addOnSuccessListener
                }

                if (document.getString("vetId") != vetId) {
                    onResult(
                        Result.failure(
                            Exception("You cannot link this patient")
                        )
                    )
                    return@addOnSuccessListener
                }

                db.collection("users").document(ownerId).get()
                    .addOnSuccessListener { ownerDoc ->

                        if (!ownerDoc.exists() ||
                            ownerDoc.getString("role") != "pet_owner"
                        ) {
                            onResult(
                                Result.failure(
                                    Exception("Registered pet owner not found")
                                )
                            )
                            return@addOnSuccessListener
                        }

                        patientRef.update("ownerId", ownerId)
                            .addOnSuccessListener {
                                onResult(Result.success(Unit))
                            }
                            .addOnFailureListener { exception ->
                                onResult(Result.failure(exception))
                            }
                    }
                    .addOnFailureListener { exception ->
                        onResult(Result.failure(exception))
                    }
            }
            .addOnFailureListener { exception ->
                onResult(Result.failure(exception))
            }
    }
}
