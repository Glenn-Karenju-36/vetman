
package com.example.vetman01.model

/**
 * Represents a veterinary appointment.
 *
 * Each appointment belongs to a patient and veterinarian.
 * Stored in the Firestore "appointments" collection.
 *
 * Default values allow Firestore to deserialize documents.
 */
data class Appointment(

    // Unique Firestore document ID.
    val id: String = "",

    // Patient and veterinarian associated with the appointment.
    val patientId: String = "",
    val vetId: String = "",

    // Scheduled appointment date and time in milliseconds.
    val scheduledAt: Long = 0L,

    // Appointment status: scheduled, completed or cancelled.
    val status: String = "scheduled",

    // Reason for the visit and additional instructions.
    val reason: String = "",
    val notes: String = "",

    // Timestamp when the appointment was created.
    val createdAt: Long = 0L
)
