
package com.example.vetman01.data

import com.example.vetman01.model.Patient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * PatientRepository
 *
 * Handles saving, retrieving and updating patient records.
 * Screens communicate with Firestore through this repository.
 */
class PatientRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * ADD PATIENT
     *
     * Creates a new patient document in Firestore.
     * Associates the patient with the logged-in veterinarian.
     */
    fun addPatient(
        patient: Patient,
        onResult: (Result<Unit>) -> Unit
    ) {
        // Identify the veterinarian registering the patient.
        val vetId = auth.currentUser?.uid

        if (vetId == null) {
            onResult(Result.failure(Exception("Not signed in")))
            return
        }

        // Generate a unique document ID for the new patient.
        val doc = db.collection("pets").document()

        // Attach the patient ID, veterinarian ID and creation time.
        val toSave = patient.copy(
            id = doc.id,
            vetId = vetId,
            createdAt = System.currentTimeMillis()
        )

        // Save the patient and report the result.
        doc.set(toSave)
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { exception ->
                onResult(Result.failure(exception))
            }
    }

    /**
     * GET PATIENTS
     *
     * Retrieves patients registered by the logged-in veterinarian.
     */
    fun getPatients(
        onResult: (Result<List<Patient>>) -> Unit
    ) {
        val vetId = auth.currentUser?.uid

        // Prevent retrieval without an authenticated user.
        if (vetId == null) {
            onResult(Result.failure(Exception("Not signed in")))
            return
        }

        // Retrieve only patients associated with this veterinarian.
        db.collection("pets")
            .whereEqualTo("vetId", vetId)
            .get()
            .addOnSuccessListener { snapshot ->

                // Convert Firestore documents into Patient objects.
                val patients = snapshot.documents.mapNotNull { document ->
                    document.toObject(Patient::class.java)
                }

                onResult(Result.success(patients))
            }
            .addOnFailureListener { exception ->
                onResult(Result.failure(exception))
            }
    }

    /**
     * UPDATE PATIENT
     *
     * Modifies an existing patient's information in Firestore.
     * Preserves the original ID, veterinarian ID and creation date.
     */
    fun updatePatient(
        patient: Patient,
        onResult: (Result<Unit>) -> Unit
    ) {
        // Ensure a veterinarian is logged in.
        val vetId = auth.currentUser?.uid

        if (vetId == null) {
            onResult(Result.failure(Exception("Not signed in")))
            return
        }

        // An existing patient must have a Firestore document ID.
        if (patient.id.isBlank()) {
            onResult(
                Result.failure(Exception("Patient ID is missing"))
            )
            return
        }

        // Prevent updating a patient registered by another vet.
        if (patient.vetId != vetId) {
            onResult(
                Result.failure(Exception("You cannot edit this patient"))
            )
            return
        }

        // Update only editable fields in the existing document.
        // This preserves the original ID, vetId and createdAt.
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
}
