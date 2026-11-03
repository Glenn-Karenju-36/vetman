
package com.example.vetman01.data

import com.example.vetman01.model.Appointment
import com.example.vetman01.model.Bill
import com.example.vetman01.model.Payment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Read-only access to veterinary records for linked pet owners.
 *
 * No create, update or delete operations are provided.
 * Firestore security rules enforce access independently.
 */
class OwnerRecordsRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Verify that the selected pet belongs to the signed-in owner.
     */
    private fun verifyOwner(
        patientId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val ownerId = auth.currentUser?.uid

        if (ownerId == null) {
            onResult(Result.failure(Exception("Not signed in")))
            return
        }

        if (patientId.isBlank()) {
            onResult(Result.failure(Exception("Patient ID is missing")))
            return
        }

        db.collection("pets")
            .document(patientId)
            .get()
            .addOnSuccessListener { pet ->
                if (pet.exists() &&
                    pet.getString("ownerId") == ownerId
                ) {
                    onResult(Result.success(Unit))
                } else {
                    onResult(
                        Result.failure(
                            Exception("Pet not linked to your account")
                        )
                    )
                }
            }
            .addOnFailureListener {
                onResult(Result.failure(it))
            }
    }

    /**
     * Get appointments for one pet, ordered by scheduled date.
     */
    fun getAppointments(
        patientId: String,
        onResult: (Result<List<Appointment>>) -> Unit
    ) {
        verifyOwner(patientId) { verification ->
            if (verification.isFailure) {
                onResult(
                    Result.failure(
                        verification.exceptionOrNull()
                            ?: Exception("Access denied")
                    )
                )
            } else {
                db.collection("appointments")
                    .whereEqualTo("patientId", patientId)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val appointments = snapshot.documents
                            .mapNotNull {
                                it.toObject(Appointment::class.java)
                            }
                            .sortedBy { it.scheduledAt }

                        onResult(Result.success(appointments))
                    }
                    .addOnFailureListener {
                        onResult(Result.failure(it))
                    }
            }
        }
    }

    /**
     * Get bills for one pet, newest first.
     */
    fun getBills(
        patientId: String,
        onResult: (Result<List<Bill>>) -> Unit
    ) {
        verifyOwner(patientId) { verification ->
            if (verification.isFailure) {
                onResult(
                    Result.failure(
                        verification.exceptionOrNull()
                            ?: Exception("Access denied")
                    )
                )
            } else {
                db.collection("bills")
                    .whereEqualTo("patientId", patientId)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val bills = snapshot.documents
                            .mapNotNull {
                                it.toObject(Bill::class.java)
                            }
                            .sortedByDescending { it.createdAt }

                        onResult(Result.success(bills))
                    }
                    .addOnFailureListener {
                        onResult(Result.failure(it))
                    }
            }
        }
    }

    /**
     * Get payment history for a selected bill.
     *
     * Verify both pet ownership and the bill's patient ID
     * before requesting payment documents.
     */
    fun getPayments(
        patientId: String,
        billId: String,
        onResult: (Result<List<Payment>>) -> Unit
    ) {
        if (billId.isBlank()) {
            onResult(Result.failure(Exception("Bill ID is missing")))
            return
        }

        verifyOwner(patientId) { verification ->
            if (verification.isFailure) {
                onResult(
                    Result.failure(
                        verification.exceptionOrNull()
                            ?: Exception("Access denied")
                    )
                )
            } else {
                val billRef = db.collection("bills").document(billId)

                billRef.get()
                    .addOnSuccessListener { bill ->
                        if (!bill.exists() ||
                            bill.getString("patientId") != patientId
                        ) {
                            onResult(
                                Result.failure(
                                    Exception("Bill not found or access denied")
                                )
                            )
                        } else {
                            billRef.collection("payments")
                                .get()
                                .addOnSuccessListener { snapshot ->
                                    val payments = snapshot.documents
                                        .mapNotNull {
                                            it.toObject(Payment::class.java)
                                        }
                                        .sortedByDescending {
                                            it.createdAt
                                        }

                                    onResult(Result.success(payments))
                                }
                                .addOnFailureListener {
                                    onResult(Result.failure(it))
                                }
                        }
                    }
                    .addOnFailureListener {
                        onResult(Result.failure(it))
                    }
            }
        }
    }
}
