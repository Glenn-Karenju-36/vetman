
package com.example.vetman01.data

import com.example.vetman01.model.Bill
import com.example.vetman01.model.Payment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.roundToLong

/**
 * Manages bills and payment records in Firestore.
 *
 * bills/{billId}
 * bills/{billId}/payments/{paymentId}
 */
class BillRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * CREATE BILL
     * Creates a bill for a patient registered by the veterinarian.
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

        if (bill.patientId.isBlank() ||
            !bill.amount.isFinite() ||
            bill.amount <= 0.0
        ) {
            onResult(Result.failure(Exception("Invalid bill details")))
            return
        }

        // Confirm that the patient belongs to this veterinarian.
        db.collection("pets")
            .document(bill.patientId)
            .get()
            .addOnSuccessListener { patient ->

                if (!patient.exists() ||
                    patient.getString("vetId") != vetId
                ) {
                    onResult(
                        Result.failure(
                            Exception("Patient not found or access denied")
                        )
                    )
                    return@addOnSuccessListener
                }

                // Create a unique bill with an unpaid starting balance.
                val doc = db.collection("bills").document()
                val now = System.currentTimeMillis()

                val newBill = bill.copy(
                    id = doc.id,
                    vetId = vetId,
                    amountPaid = 0.0,
                    status = "unpaid",
                    paymentMethod = "",
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
     * Retrieves the current veterinarian's bills, newest first.
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
                    .mapNotNull { it.toObject(Bill::class.java) }
                    .sortedByDescending { it.createdAt }

                onResult(Result.success(bills))
            }
            .addOnFailureListener {
                onResult(Result.failure(it))
            }
    }

    /**
     * RECORD PAYMENT
     *
     * Records a payment and updates its bill in one transaction.
     * Both operations succeed together or fail together.
     *
     * The reference is optional for cash payments.
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

        if (billId.isBlank() ||
            !paymentAmount.isFinite() ||
            paymentAmount <= 0.0 ||
            paymentMethod !in allowedMethods
        ) {
            onResult(Result.failure(Exception("Invalid payment details")))
            return
        }

        // Convert payments to cents to compare balances reliably.
        val paymentCents = (paymentAmount * 100).roundToLong()

        if (paymentCents <= 0L) {
            onResult(Result.failure(Exception("Invalid payment amount")))
            return
        }

        val billRef = db.collection("bills").document(billId)

        // Generate the payment ID before starting the transaction.
        val paymentRef = billRef
            .collection("payments")
            .document()

        db.runTransaction { transaction ->

            // Read the bill and verify veterinarian ownership.
            val snapshot = transaction.get(billRef)

            if (!snapshot.exists() ||
                snapshot.getString("vetId") != vetId
            ) {
                throw Exception("Bill not found or access denied")
            }

            val totalAmount = snapshot.getDouble("amount")
                ?: throw Exception("Bill amount missing")

            val alreadyPaid = snapshot.getDouble("amountPaid") ?: 0.0

            val totalCents = (totalAmount * 100).roundToLong()
            val paidCents = (alreadyPaid * 100).roundToLong()

            // Prevent overpayments or updates to invalid balances.
            if (paymentCents > totalCents - paidCents) {
                throw Exception("Payment exceeds outstanding balance")
            }

            val newPaidCents = paidCents + paymentCents
            val newAmountPaid = newPaidCents / 100.0

            // Determine the new payment status.
            val newStatus = when {
                newPaidCents >= totalCents -> "paid"
                newPaidCents > 0L -> "partial"
                else -> "unpaid"
            }

            val now = System.currentTimeMillis()

            // Create a separate history entry for this payment.
            val payment = Payment(
                id = paymentRef.id,
                billId = billId,
                amount = paymentCents / 100.0,
                method = paymentMethod,
                reference = reference.trim(),
                createdAt = now,
                vetId = vetId
            )

            transaction.set(paymentRef, payment)

            // Update the parent bill's balance and status.
            transaction.update(
                billRef,
                mapOf(
                    "amountPaid" to newAmountPaid,
                    "status" to newStatus,
                    "paymentMethod" to paymentMethod,
                    "updatedAt" to now
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
     * Retrieves individual payments for a bill after
     * confirming veterinarian ownership.
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

        // Verify ownership before requesting payment history.
        billRef.get()
            .addOnSuccessListener { billSnapshot ->

                if (!billSnapshot.exists() ||
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

                        // Display the most recent payments first.
                        val payments = snapshot.documents
                            .mapNotNull {
                                it.toObject(Payment::class.java)
                            }
                            .sortedByDescending { it.createdAt }

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
