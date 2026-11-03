
package com.example.vetman01.data

import com.example.vetman01.model.Bill
import com.example.vetman01.model.Payment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.roundToLong

/**
 * Manages veterinary bills and payment records in Firestore.
 *
 * bills/{billId}
 * bills/{billId}/payments/{paymentId}
 *
 * Screens interact with this repository rather than
 * accessing Firestore directly.
 */
class BillRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * CREATE BILL
     *
     * Creates a new bill for a patient registered
     * by the currently authenticated veterinarian.
     */
    fun addBill(
        bill: Bill,
        onResult: (Result<Unit>) -> Unit
    ) {
        val vetId = auth.currentUser?.uid

        if (vetId == null) {
            onResult(Result.failure(Exception("Not signed in")))
            return
        }

        if (
            bill.patientId.isBlank() ||
            !bill.amount.isFinite() ||
            bill.amount <= 0.0
        ) {
            onResult(Result.failure(Exception("Invalid bill details")))
            return
        }

        // Confirm that the selected patient belongs to this vet.
        db.collection("pets")
            .document(bill.patientId)
            .get()
            .addOnSuccessListener { patient ->

                if (
                    !patient.exists() ||
                    patient.getString("vetId") != vetId
                ) {
                    onResult(
                        Result.failure(
                            Exception("Patient not found or access denied")
                        )
                    )
                    return@addOnSuccessListener
                }

                val doc = db.collection("bills").document()
                val now = System.currentTimeMillis()

                // New bills begin unpaid and have no payment history.
                val newBill = bill.copy(
                    id = doc.id,
                    vetId = vetId,
                    amountPaid = 0.0,
                    status = "unpaid",
                    paymentMethod = "",
                    lastPaymentId = "",
                    createdAt = now,
                    updatedAt = now
                )

                doc.set(newBill)
                    .addOnSuccessListener {
                        onResult(Result.success(Unit))
                    }
                    .addOnFailureListener {
                        onResult(Result.failure(it))
                    }
            }
            .addOnFailureListener {
                onResult(Result.failure(it))
            }
    }

    /**
     * GET BILLS
     *
     * Retrieves the current veterinarian's bills,
     * ordered from newest to oldest.
     */
    fun getBills(
        onResult: (Result<List<Bill>>) -> Unit
    ) {
        val vetId = auth.currentUser?.uid

        if (vetId == null) {
            onResult(Result.failure(Exception("Not signed in")))
            return
        }

        db.collection("bills")
            .whereEqualTo("vetId", vetId)
            .get()
            .addOnSuccessListener { snapshot ->

                val bills = snapshot.documents
                    .mapNotNull {
                        it.toObject(Bill::class.java)
                    }
                    .sortedByDescending {
                        it.createdAt
                    }

                onResult(Result.success(bills))
            }
            .addOnFailureListener {
                onResult(Result.failure(it))
            }
    }

    /**
     * RECORD PAYMENT
     *
     * Creates a payment record and updates its parent bill
     * inside one Firestore transaction.
     *
     * The bill's lastPaymentId points to the payment document
     * associated with the latest balance update.
     */
    fun recordPayment(
        billId: String,
        paymentAmount: Double,
        paymentMethod: String,
        reference: String = "",
        onResult: (Result<Unit>) -> Unit
    ) {
        val vetId = auth.currentUser?.uid

        if (vetId == null) {
            onResult(Result.failure(Exception("Not signed in")))
            return
        }

        val allowedMethods = listOf(
            "M-Pesa",
            "Cash",
            "Airtel Money"
        )

        if (
            billId.isBlank() ||
            !paymentAmount.isFinite() ||
            paymentAmount <= 0.0 ||
            paymentMethod !in allowedMethods
        ) {
            onResult(Result.failure(Exception("Invalid payment details")))
            return
        }

        // Convert KSh values to cents for balance comparisons.
        val paymentCents = (paymentAmount * 100).roundToLong()

        if (paymentCents <= 0L) {
            onResult(Result.failure(Exception("Invalid payment amount")))
            return
        }

        val billRef = db.collection("bills").document(billId)

        // Generate the payment ID outside the transaction.
        // Firestore may retry the transaction automatically.
        val paymentRef = billRef
            .collection("payments")
            .document()

        db.runTransaction { transaction ->

            // Read and verify the bill before performing writes.
            val snapshot = transaction.get(billRef)

            if (
                !snapshot.exists() ||
                snapshot.getString("vetId") != vetId
            ) {
                throw Exception("Bill not found or access denied")
            }

            val totalAmount = snapshot.getDouble("amount")
                ?: throw Exception("Bill amount missing")

            val alreadyPaid = snapshot.getDouble("amountPaid") ?: 0.0

            val totalCents = (totalAmount * 100).roundToLong()
            val paidCents = (alreadyPaid * 100).roundToLong()

            // Prevent overpayments.
            if (
                paymentCents > totalCents - paidCents
            ) {
                throw Exception("Payment exceeds outstanding balance")
            }

            val newPaidCents = paidCents + paymentCents
            val newAmountPaid = newPaidCents / 100.0

            val newStatus = when {
                newPaidCents >= totalCents -> "paid"
                newPaidCents > 0L -> "partial"
                else -> "unpaid"
            }

            val now = System.currentTimeMillis()

            // Prepare the individual payment record.
            val payment = Payment(
                id = paymentRef.id,
                billId = billId,
                amount = paymentCents / 100.0,
                method = paymentMethod,
                reference = reference.trim(),
                createdAt = now,
                vetId = vetId
            )

            // Write the payment history entry.
            transaction.set(paymentRef, payment)

            /**
             * Update the bill in the same transaction.
             *
             * lastPaymentId connects this balance change to
             * the newly created payment, allowing security
             * rules to validate the relationship.
             */
            transaction.update(
                billRef,
                mapOf(
                    "amountPaid" to newAmountPaid,
                    "status" to newStatus,
                    "paymentMethod" to paymentMethod,
                    "updatedAt" to now,
                    "lastPaymentId" to paymentRef.id
                )
            )

            Unit
        }
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener {
                onResult(Result.failure(it))
            }
    }

    /**
     * GET PAYMENT HISTORY
     *
     * Retrieves payments for a bill after verifying
     * that it belongs to the logged-in veterinarian.
     */
    fun getPayments(
        billId: String,
        onResult: (Result<List<Payment>>) -> Unit
    ) {
        val vetId = auth.currentUser?.uid

        if (vetId == null) {
            onResult(Result.failure(Exception("Not signed in")))
            return
        }

        if (billId.isBlank()) {
            onResult(Result.failure(Exception("Bill ID is missing")))
            return
        }

        val billRef = db.collection("bills").document(billId)

        // Verify ownership before retrieving payment history.
        billRef.get()
            .addOnSuccessListener { billSnapshot ->

                if (
                    !billSnapshot.exists() ||
                    billSnapshot.getString("vetId") != vetId
                ) {
                    onResult(
                        Result.failure(
                            Exception("Bill not found or access denied")
                        )
                    )
                    return@addOnSuccessListener
                }

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
            .addOnFailureListener {
                onResult(Result.failure(it))
            }
    }
}
