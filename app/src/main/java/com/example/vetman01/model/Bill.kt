
package com.example.vetman01.model

/**
 * Represents a veterinary service bill.
 *
 * Each bill belongs to a patient and veterinarian.
 * Stored in Firestore under bills/{billId}.
 *
 * Default values allow Firestore to reconstruct Bill objects.
 */
data class Bill(

    // Unique Firestore document ID.
    val id: String = "",

    // Patient and veterinarian associated with the bill.
    val patientId: String = "",
    val vetId: String = "",

    // Description of veterinary services or products billed.
    val description: String = "",

    // Amount charged in Kenyan Shillings (KSh).
    val amount: Double = 0.0,

    // Payment status: unpaid, partial or paid.
    val status: String = "unpaid",

    // Total amount received so far.
    val amountPaid: Double = 0.0,

    // Payment method, e.g. cash, M-Pesa or bank transfer.
    val paymentMethod: String = "",

    // Additional billing notes.
    val notes: String = "",

    // Creation and last-updated timestamps.
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
