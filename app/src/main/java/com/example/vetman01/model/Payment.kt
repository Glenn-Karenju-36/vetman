
package com.example.vetman01.model

/**
 * Represents one payment made toward a veterinary bill.
 *
 * Stored under:
 * bills/{billId}/payments/{paymentId}
 *
 * A bill can have multiple payments using different methods.
 */
data class Payment(

    // Unique Firestore document ID.
    val id: String = "",

    // Bill associated with this payment.
    val billId: String = "",

    // Amount received in Kenyan Shillings.
    val amount: Double = 0.0,

    // Accepted methods: M-Pesa, Cash and Airtel Money.
    val method: String = "",

    // Optional mobile-money transaction reference.
    val reference: String = "",

    // When the payment was recorded.
    val createdAt: Long = 0L,

    // Veterinarian who recorded the payment.
    val vetId: String = ""
)
