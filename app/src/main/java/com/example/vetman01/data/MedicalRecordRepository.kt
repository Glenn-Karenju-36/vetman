
package com.example.vetman01.data

import com.example.vetman01.model.MedicalRecord
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Handles saving and retrieving patient medical records.
 *
 * Firestore structure:
 * pets/{patientId}/medicalRecords/{recordId}
 */
class MedicalRecordRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Saves a new medical record under an existing patient.
     */
    fun addMedicalRecord(
        patientId: String,
        record: MedicalRecord,
        onResult: (Result<Unit>) -> Unit
    ) {
        // Identify the veterinarian creating the record.
        val vetId = auth.currentUser?.uid

        if (vetId == null) {
            onResult(Result.failure(Exception("Not signed in")))
            return
        }

        if (patientId.isBlank()) {
            onResult(Result.failure(Exception("Patient ID is missing")))
            return
        }

        // Confirm the patient belongs to this veterinarian.
        val patientRef = db.collection("pets").document(patientId)

        patientRef.get()
            .addOnSuccessListener { patientSnapshot ->

                if (!patientSnapshot.exists() ||
                    patientSnapshot.getString("vetId") != vetId
                ) {
                    onResult(
                        Result.failure(Exception("Patient not found or access denied"))
                    )
                    return@addOnSuccessListener
                }

                // Create a unique record inside the patient's medical history.
                val doc = patientRef
                    .collection("medicalRecords")
                    .document()

                val toSave = record.copy(
                    id = doc.id,
                    patientId = patientId,
                    vetId = vetId,
                    createdAt = System.currentTimeMillis()
                )

                // Save the record and return the operation result.
                doc.set(toSave)
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

    /**
     * Retrieves a patient's medical history, newest visit first.
     */
    fun getMedicalRecords(
        patientId: String,
        onResult: (Result<List<MedicalRecord>>) -> Unit
    ) {
        val vetId = auth.currentUser?.uid

        if (vetId == null) {
            onResult(Result.failure(Exception("Not signed in")))
            return
        }

        if (patientId.isBlank()) {
            onResult(Result.failure(Exception("Patient ID is missing")))
            return
        }

        // Verify ownership before retrieving clinical information.
        val patientRef = db.collection("pets").document(patientId)

        patientRef.get()
            .addOnSuccessListener { patientSnapshot ->

                if (!patientSnapshot.exists() ||
                    patientSnapshot.getString("vetId") != vetId
                ) {
                    onResult(
                        Result.failure(Exception("Patient not found or access denied"))
                    )
                    return@addOnSuccessListener
                }

                // Load medical records ordered by consultation date.
                patientRef.collection("medicalRecords")
                    .orderBy(
                        "visitDate",
                        com.google.firebase.firestore.Query.Direction.DESCENDING
                    )
                    .get()
                    .addOnSuccessListener { snapshot ->

                        val records = snapshot.documents.mapNotNull { document ->
                            document.toObject(MedicalRecord::class.java)
                        }

                        onResult(Result.success(records))
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
