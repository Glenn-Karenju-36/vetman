
package com.example.vetman01.model

/**
 * Represents an animal patient stored in Firestore.
 *
 * Default values allow Firebase to reconstruct Patient objects
 * when retrieving records from the database.
 */
data class Patient(

    // Unique Firestore document ID.
    val id: String = "",

    // Veterinarian who registered the patient.
    val vetId: String = "",

    // Basic patient information.
    val petName: String = "",
    val species: String = "",
    val breed: String = "",
    val age: String = "",
    val sex: String = "",

    // Pet owner information.
    val ownerName: String = "",
    val ownerPhone: String = "",

    // Additional patient notes.
    val notes: String = "",

    // Firebase Storage path for the patient's profile photo.
    // Empty when no photo has been uploaded.
//    val photoPath: String = "",

    // Timestamp when the patient was registered.
    val createdAt: Long = 0L
)
