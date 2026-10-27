
package com.example.vetman01.data

import com.example.vetman01.model.Patient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * PatientRepository
 *
 * Handles communication with Firebase Firestore for patient records.
 * Screens use this repository instead of accessing Firestore directly.
 */
class PatientRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * ADD PATIENT
     *
     * Saves a new patient in the Firestore "pets" collection.
     * Associates the patient with the veterinarian who registered it.
     */
    fun addPatient(
        patient: Patient,
        onResult: (Result<Unit>) -> Unit
    ) {

        // Get the unique ID of the currently logged-in veterinarian.
        val vetId = auth.currentUser?.uid

        // Prevent saving if no veterinarian is authenticated.
        if (vetId == null) {
            onResult(Result.failure(Exception("Not signed in")))
            return
        }

        // Generate a unique Firestore document ID for the patient.
        val doc = db.collection("pets").document()

        // Add the document ID, veterinarian ID and creation timestamp.
        val toSave = patient.copy(
            id = doc.id,
            vetId = vetId,
            createdAt = System.currentTimeMillis()
        )

        // Save the patient and notify the screen of the result.
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
     * Returns a list of Patient objects or an error.
     */
    fun getPatients(
        onResult: (Result<List<Patient>>) -> Unit
    ) {

        // Identify the currently logged-in veterinarian.
        val vetId = auth.currentUser?.uid

        // Stop retrieval if the user is not authenticated.
        if (vetId == null) {
            onResult(Result.failure(Exception("Not signed in")))
            return
        }

        // Query the "pets" collection for this veterinarian's patients.
        db.collection("pets")
            .whereEqualTo("vetId", vetId)
            .get()

            // Convert the retrieved documents into Patient objects.
            .addOnSuccessListener { snapshot ->

                val patients = snapshot.documents.mapNotNull { document ->
                    document.toObject(Patient::class.java)
                }

                // Return the retrieved patients to the calling screen.
                onResult(Result.success(patients))
            }

            // Return an error if Firestore cannot retrieve the records.
            .addOnFailureListener { exception ->
                onResult(Result.failure(exception))
            }
    }
}
