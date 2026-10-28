
package com.example.vetman01.ui.patients

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vetman01.data.MedicalRecordRepository
import com.example.vetman01.model.MedicalRecord
import com.example.vetman01.model.Patient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Displays patient information, medical history and appointments.
 * Provides navigation to editing, medical records and scheduling.
 */
@Composable
fun PatientProfileScreen(
    patient: Patient,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAddMedicalRecord: () -> Unit,
    onScheduleAppointment: () -> Unit,
    onViewAppointments: () -> Unit,
    repository: MedicalRecordRepository = remember {
        MedicalRecordRepository()
    }
) {
    // Medical records retrieved from Firestore.
    var medicalRecords by remember(patient.id) {
        mutableStateOf<List<MedicalRecord>>(emptyList())
    }

    var loading by remember(patient.id) {
        mutableStateOf(true)
    }

    var error by remember(patient.id) {
        mutableStateOf<String?>(null)
    }

    // Load this patient's medical history when the profile opens.
    LaunchedEffect(patient.id) {
        repository.getMedicalRecords(patient.id) { result ->
            loading = false

            result
                .onSuccess {
                    medicalRecords = it
                }
                .onFailure {
                    error = it.message ?: "Could not load medical history"
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        // Return to the veterinarian's patient list.
        TextButton(onClick = onBack) {
            Text("< Back to patients")
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = patient.petName,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(16.dp))

        // Placeholder for future patient photos.
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Pet photo will appear here")
            }
        }

        Spacer(Modifier.height(16.dp))

        // Open the existing patient editing form.
        Button(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Edit Patient")
        }

        Spacer(Modifier.height(24.dp))

        // Animal information.
        Text(
            "Patient Information",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(8.dp))

        PatientDetail("Name", patient.petName)
        PatientDetail("Species", patient.species)
        PatientDetail("Breed", patient.breed)
        PatientDetail("Age", patient.age)
        PatientDetail("Sex", patient.sex)

        Spacer(Modifier.height(24.dp))

        // Pet owner information.
        Text(
            "Owner Information",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(8.dp))

        PatientDetail("Owner", patient.ownerName)
        PatientDetail("Phone", patient.ownerPhone)

        Spacer(Modifier.height(24.dp))

        // Notes entered during registration or editing.
        Text(
            "Patient Notes",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(8.dp))

        Text(
            patient.notes.ifBlank { "No notes recorded." }
        )

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        // Appointment management for the selected patient.
        Text(
            "Appointments",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onScheduleAppointment,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Schedule Appointment")
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = onViewAppointments,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View Appointments")
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        // Medical history section.
        Text(
            "Medical History",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onAddMedicalRecord,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Medical Record")
        }

        Spacer(Modifier.height(16.dp))

        // Display medical records or their loading/error state.
        when {
            loading -> {
                CircularProgressIndicator()
            }

            error != null -> {
                Text(
                    text = error ?: "",
                    color = MaterialTheme.colorScheme.error
                )
            }

            medicalRecords.isEmpty() -> {
                Text("No medical records added yet.")
            }

            else -> {
                medicalRecords.forEach { record ->
                    MedicalRecordCard(record)
                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Displays a patient information field and its value.
 */
@Composable
private fun PatientDetail(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value.ifBlank { "Not provided" },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(2f)
        )
    }
}

/**
 * Displays a single consultation in the patient's medical history.
 */
@Composable
private fun MedicalRecordCard(record: MedicalRecord) {

    // Convert the saved timestamp into a readable visit date.
    val date = remember(record.visitDate) {
        SimpleDateFormat(
            "dd MMM yyyy, HH:mm",
            Locale.getDefault()
        ).format(Date(record.visitDate))
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = date,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(8.dp))

            MedicalDetail("Symptoms", record.symptoms)
            MedicalDetail("Diagnosis", record.diagnosis)
            MedicalDetail("Treatment", record.treatment)
            MedicalDetail("Medications", record.medications)
            MedicalDetail("Procedures", record.procedures)
            MedicalDetail("Notes", record.notes)
        }
    }
}

/**
 * Displays a clinical field only if information was recorded.
 */
@Composable
private fun MedicalDetail(
    label: String,
    value: String
) {
    if (value.isNotBlank()) {
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(6.dp))
    }
}
