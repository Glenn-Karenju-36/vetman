
package com.example.vetman01.model

/**
 * Represents an animal patient stored in Firestore.
 *
 * Collection: pets/{patientId}
 *
 * Default values allow Firestore to reconstruct Patient objects
 * when retrieving saved records.
 */
data class Patient(

    // Unique Firestore document ID.
    val id: String = "",

    // Veterinarian who registered the patient.
    val vetId: String = "",

    // Firebase UID of the linked pet-owner account.
    // Empty until the veterinarian links an owner.
    val ownerId: String = "",

    // Basic patient information.
    val petName: String = "",
    val species: String = "",
    val breed: String = "",
    val age: String = "",
    val sex: String = "",

    // Pet owner's contact information.
    val ownerName: String = "",
    val ownerPhone: String = "",

    // Additional patient notes.
    val notes: String = "",

    // Timestamp recorded during patient registration.
    val createdAt: Long = 0L
)
