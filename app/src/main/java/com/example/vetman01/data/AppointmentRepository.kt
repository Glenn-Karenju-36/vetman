
package com.example.vetman01.data

import com.example.vetman01.model.Appointment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

/**
 * Manages veterinary appointments in Firestore.
 *
 * Collection: appointments/{appointmentId}
 * Each appointment is linked to a patient and veterinarian.
 */
class AppointmentRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * CREATE APPOINTMENT
     * Saves a new appointment for a registered patient.
     */
    fun addAppointment(
        appointment: Appointment,
        onResult: (Result<Unit>) -> Unit
    ) {
        val vetId = auth.currentUser?.uid

        if (vetId == null) {
            onResult(Result.failure(Exception("Not signed in")))
            return
        }

        if (appointment.patientId.isBlank() || appointment.scheduledAt <= 0L) {
            onResult(Result.failure(Exception("Invalid patient or appointment date")))
            return
        }

        // Verify the patient belongs to the signed-in veterinarian.
        db.collection("pets").document(appointment.patientId)
            .get()
            .addOnSuccessListener { patient ->
                if (!patient.exists() || patient.getString("vetId") != vetId) {
                    onResult(Result.failure(Exception("Patient not found or access denied")))
                    return@addOnSuccessListener
                }

                // Create a unique appointment document.
                val doc = db.collection("appointments").document()

                val toSave = appointment.copy(
                    id = doc.id,
                    vetId = vetId,
                    status = "scheduled",
                    createdAt = System.currentTimeMillis()
                )

                doc.set(toSave)
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
     * GET APPOINTMENTS
     * Retrieves all appointments assigned to the current veterinarian.
     */
    fun getAppointments(
        onResult: (Result<List<Appointment>>) -> Unit
    ) {
        val vetId = auth.currentUser?.uid

        if (vetId == null) {
            onResult(Result.failure(Exception("Not signed in")))
            return
        }

        db.collection("appointments")
            .whereEqualTo("vetId", vetId)
            .get()
            .addOnSuccessListener { snapshot ->

                // Sort locally to avoid requiring a composite Firestore index.
                val appointments = snapshot.documents
                    .mapNotNull { it.toObject(Appointment::class.java) }
                    .sortedBy { it.scheduledAt }

                onResult(Result.success(appointments))
            }
            .addOnFailureListener {
                onResult(Result.failure(it))
            }
    }

    /**
     * RESCHEDULE APPOINTMENT
     * Changes the scheduled date and time without creating another record.
     */
    fun rescheduleAppointment(
        appointmentId: String,
        newScheduledAt: Long,
        onResult: (Result<Unit>) -> Unit
    ) {
        if (newScheduledAt <= 0L) {
            onResult(Result.failure(Exception("Invalid appointment date")))
            return
        }

        updateOwnedAppointment(
            appointmentId,
            mapOf(
                "scheduledAt" to newScheduledAt,
                "status" to "scheduled"
            ),
            onResult
        )
    }

    /**
     * UPDATE APPOINTMENT STATUS
     * Supports marking appointments as completed or cancelled.
     */
    fun updateAppointmentStatus(
        appointmentId: String,
        status: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        if (status !in listOf("scheduled", "completed", "cancelled")) {
            onResult(Result.failure(Exception("Invalid appointment status")))
            return
        }

        updateOwnedAppointment(
            appointmentId,
            mapOf("status" to status),
            onResult
        )
    }

    /**
     * Verifies ownership before updating an appointment.
     *
     * Firestore security rules must also enforce ownership,
     * since client-side checks alone cannot protect the database.
     */
    private fun updateOwnedAppointment(
        appointmentId: String,
        changes: Map<String, Any>,
        onResult: (Result<Unit>) -> Unit
    ) {
        val vetId = auth.currentUser?.uid

        if (vetId == null) {
            onResult(Result.failure(Exception("Not signed in")))
            return
        }

        if (appointmentId.isBlank()) {
            onResult(Result.failure(Exception("Appointment ID is missing")))
            return
        }

        val doc = db.collection("appointments").document(appointmentId)

        // Ensure the appointment belongs to this veterinarian.
        doc.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists() || snapshot.getString("vetId") != vetId) {
                    onResult(Result.failure(Exception("Appointment not found or access denied")))
                    return@addOnSuccessListener
                }

                // Update the existing document, preserving its ID and owner.
                doc.update(changes)
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
}
