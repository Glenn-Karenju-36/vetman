
package com.example.vetman01.model

/**
 * Represents one veterinary consultation or medical visit.
 *
 * Each patient can have multiple medical records.
 * Records can originate from manual entry or OCR extraction.
 *
 * Stored in Firestore under:
 * pets/{patientId}/medicalRecords/{recordId}
 */
data class MedicalRecord(

    // Unique Firestore document ID.
    val id: String = "",

    // Patient associated with this medical record.
    val patientId: String = "",

    // Veterinarian who created the record.
    val vetId: String = "",

    // Date of the consultation in milliseconds.
    val visitDate: Long = 0L,

    // Clinical information recorded during the visit.
    val symptoms: String = "",
    val diagnosis: String = "",
    val treatment: String = "",
    val medications: String = "",
    val procedures: String = "",

    // Additional observations or follow-up instructions.
    val notes: String = "",

    // Identifies how the information was entered.
    // "manual" = entered by vet, "ocr" = extracted from a document.
    val source: String = "manual",

    // Time the record was first saved.
    val createdAt: Long = 0L
)
