
package com.example.vetman01.ui.patients

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vetman01.model.Patient

/**
 * Displays the selected patient's information.
 * Provides navigation back to the list or to the edit form.
 */
@Composable
fun PatientProfileScreen(
    patient: Patient,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
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

        // Placeholder for a future patient profile photo.
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

        // Open the editing form for the selected patient.
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

        // Notes recorded during patient registration or editing.
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

        // Medical history will be implemented later.
        Text(
            "Medical History",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(8.dp))

        Text("No medical records added yet.")
    }
}

/**
 * Displays a patient field alongside its value.
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
