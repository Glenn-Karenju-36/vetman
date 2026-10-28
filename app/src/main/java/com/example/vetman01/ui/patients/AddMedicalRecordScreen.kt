
package com.example.vetman01.ui.patients

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vetman01.data.MedicalRecordRepository
import com.example.vetman01.model.MedicalRecord
import com.example.vetman01.model.Patient

/**
 * Form for recording a patient's veterinary consultation.
 *
 * Records are associated with the selected patient.
 * The same MedicalRecord model will support OCR extraction later.
 */
@Composable
fun AddMedicalRecordScreen(
    patient: Patient,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    repository: MedicalRecordRepository = remember {
        MedicalRecordRepository()
    }
) {

    // Stores information entered by the veterinarian.
    var symptoms by remember { mutableStateOf("") }
    var diagnosis by remember { mutableStateOf("") }
    var treatment by remember { mutableStateOf("") }
    var medications by remember { mutableStateOf("") }
    var procedures by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Tracks saving progress and database errors.
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Require clinical information before allowing submission.
    val canSave = listOf(
        symptoms, diagnosis, treatment,
        medications, procedures, notes
    ).any { it.isNotBlank() } && !saving

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        TextButton(
            onClick = onBack,
            enabled = !saving
        ) {
            Text("< Back to profile")
        }

        Text(
            text = "New Medical Record",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Patient: ${patient.petName}",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(20.dp))

        // Clinical information for the consultation.
        OutlinedTextField(
            value = symptoms,
            onValueChange = { symptoms = it },
            label = { Text("Symptoms") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = diagnosis,
            onValueChange = { diagnosis = it },
            label = { Text("Diagnosis") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = treatment,
            onValueChange = { treatment = it },
            label = { Text("Treatment") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = medications,
            onValueChange = { medications = it },
            label = { Text("Medications") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = procedures,
            onValueChange = { procedures = it },
            label = { Text("Procedures") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Additional notes") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        // Show Firestore errors without losing entered data.
        error?.let { message ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {

                saving = true
                error = null

                // Convert the form into a MedicalRecord object.
                // The repository assigns record ID, vet ID and createdAt.
                val record = MedicalRecord(
                    patientId = patient.id,
                    visitDate = System.currentTimeMillis(),
                    symptoms = symptoms.trim(),
                    diagnosis = diagnosis.trim(),
                    treatment = treatment.trim(),
                    medications = medications.trim(),
                    procedures = procedures.trim(),
                    notes = notes.trim(),
                    source = "manual"
                )

                // Save under this patient's Firestore medicalRecords collection.
                repository.addMedicalRecord(
                    patientId = patient.id,
                    record = record
                ) { result ->
                    saving = false

                    result
                        .onSuccess {
                            onSaved()
                        }
                        .onFailure {
                            error = it.message
                                ?: "Could not save medical record"
                        }
                }
            },
            enabled = canSave,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (saving) "Saving..." else "Save Medical Record"
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}
