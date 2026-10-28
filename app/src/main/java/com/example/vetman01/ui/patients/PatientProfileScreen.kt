
package com.example.vetman01.ui.patients

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
 * Displays a patient's profile and medical history.
 * Provides navigation to patient editing and medical record creation.
 */
@Composable
fun PatientProfileScreen(
    patient: Patient,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAddMedicalRecord: () -> Unit,
    repository: MedicalRecordRepository = remember {
        MedicalRecordRepository()
    }
) {
    // Medical records retrieved from Firestore.
    var medicalRecords by remember(patient.id) {
        mutableStateOf<List<MedicalRecord>>(emptyList())
    }

    var loading by remember(patient.id) { mutableStateOf(true) }
    var error by remember(patient.id) { mutableStateOf<String?>(null) }

    // Retrieve this patient's medical history when the profile opens.
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

        // Return to the patient list.
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

        // Open the existing patient editing screen.
        Button(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Edit Patient")
        }

        Spacer(Modifier.height(24.dp))

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

        Text(
            "Owner Information",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(8.dp))

        PatientDetail("Owner", patient.ownerName)
        PatientDetail("Phone", patient.ownerPhone)

        Spacer(Modifier.height(24.dp))

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

        // Medical history for this patient.
        Text(
            "Medical History",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(12.dp))

        // Open the manual medical record form.
        Button(
            onClick = onAddMedicalRecord,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Medical Record")
        }

        Spacer(Modifier.height(16.dp))

        // Display loading, error, empty, or populated states.
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

                    // Each consultation appears as a separate card.
                    MedicalRecordCard(record)

                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Displays a patient's field label and value.
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
 * Displays one consultation from the patient's medical history.
 * Empty clinical fields are hidden.
 */
@Composable
private fun MedicalRecordCard(record: MedicalRecord) {

    // Format the saved visit timestamp for display.
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
 * Shows a clinical field only when it contains information.
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
