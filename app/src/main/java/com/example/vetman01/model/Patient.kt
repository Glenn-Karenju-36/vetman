package com.example.vetman01.model

/**
 * One animal patient, stored in Firestore under pets/{id}.
 * Every field has a default value because Firestore needs an
 * empty constructor to turn a document back into a Patient.
 */
data class Patient(
    val id: String = "",          // document ID, filled in when saved
    val vetId: String = "",       // uid of the vet who added the patient
    val petName: String = "",
    val species: String = "",     // dog, cat, cattle, goat or other
    val breed: String = "",
    val age: String = "",         // free text, e.g. "3 years" or "6 months"
    val sex: String = "",         // male or female
    val ownerName: String = "",
    val ownerPhone: String = "",
    val notes: String = "",
    val createdAt: Long = 0L      // time saved, in milliseconds
)