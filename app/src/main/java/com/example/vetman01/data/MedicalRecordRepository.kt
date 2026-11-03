
package com.example.vetman01.data

import com.example.vetman01.model.MedicalRecord
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

/**
 * Handles patient medical records.
 *
 * Firestore structure:
 * pets/{patientId}/medicalRecords/{recordId}
 *
 * Veterinarians can create and read medical records.
 * Linked pet owners can only read them.
 *
 * Firestore security rules enforce these permissions.
 */
class MedicalRecordRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * ADD MEDICAL RECORD
     *
     * Only the veterinarian who registered the patient
     * can create medical records.
     */
    fun addMedicalRecord(
        patientId: String,
        record: MedicalRecord,
        onResult: (Result<Unit>) -> Unit
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

        val patientRef = db.collection("pets").document(patientId)

        // Verify veterinarian ownership before writing.
        patientRef.get()
            .addOnSuccessListener { patientSnapshot ->

                if (!patientSnapshot.exists() ||
                    patientSnapshot.getString("vetId") != vetId
                ) {
                    onResult(
                        Result.failure(
                            Exception("Patient not found or access denied")
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
                    .addOnFailureListener { exception ->
                        onResult(Result.failure(exception))
                    }
            }
            .addOnFailureListener { exception ->
                onResult(Result.failure(exception))
            }
    }

    /**
     * GET MEDICAL RECORDS
     *
     * Allows the assigned veterinarian or linked pet owner
     * to retrieve medical history.
     *
     * Firestore rules independently verify access.
     */
    fun getMedicalRecords(
        patientId: String,
        onResult: (Result<List<MedicalRecord>>) -> Unit
    ) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            onResult(Result.failure(Exception("Not signed in")))
            return
        }

        if (patientId.isBlank()) {
            onResult(Result.failure(Exception("Patient ID is missing")))
            return
        }

        val patientRef = db.collection("pets").document(patientId)

        // Verify that the signed-in user is either the
        // veterinarian or the pet's linked owner.
        patientRef.get()
            .addOnSuccessListener { patientSnapshot ->

                if (!patientSnapshot.exists()) {
                    onResult(
                        Result.failure(Exception("Patient not found"))
                    )
                    return@addOnSuccessListener
                }

                val vetId = patientSnapshot.getString("vetId")
                val ownerId = patientSnapshot.getString("ownerId")

                if (vetId != userId && ownerId != userId) {
                    onResult(
                        Result.failure(
                            Exception("You cannot access this patient's records")
                        )
                    )
                    return@addOnSuccessListener
                }

                // Both authorized roles can retrieve records.
                // Firestore security rules protect the actual data.
                patientRef.collection("medicalRecords")
                    .orderBy(
                        "visitDate",
                        Query.Direction.DESCENDING
                    )
                    .get()
                    .addOnSuccessListener { snapshot ->

                        val records = snapshot.documents.mapNotNull {
                            it.toObject(MedicalRecord::class.java)
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
