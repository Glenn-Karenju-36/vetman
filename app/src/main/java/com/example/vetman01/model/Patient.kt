
package com.example.vetman01.model

/**
 * Represents an animal patient stored in Firestore.
 *
 * Each patient is saved under the "pets" collection.
 * Default values allow Firebase to reconstruct Patient objects
 * when retrieving records from Firestore.
 */
data class Patient(

    // Unique Firestore document ID.
    val id: String = "",

    // Firebase UID of the veterinarian who registered the patient.
    val vetId: String = "",

    // Basic animal information.
    val petName: String = "",
    val species: String = "",
    val breed: String = "",
    val age: String = "",
    val sex: String = "",

    // Pet owner's contact information.
    val ownerName: String = "",
    val ownerPhone: String = "",

    // Additional patient information.
    val notes: String = "",

    // Timestamp recorded when the patient was registered.
    val createdAt: Long = 0L
)
