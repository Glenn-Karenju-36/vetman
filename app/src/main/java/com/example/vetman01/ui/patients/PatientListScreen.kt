package com.example.vetman01.ui.patients

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vetman01.data.PatientRepository
import com.example.vetman01.model.Patient

/**
 * Displays patients registered by the current veterinarian.
 * Retrieves patient records through PatientRepository.
 */
@Composable
fun PatientListScreen(
    onAddPatient: () -> Unit,
    onPatientClick: (Patient) -> Unit,
    onSignOut: () -> Unit,
    repository: PatientRepository = remember { PatientRepository() }
) {
    var patients by remember {
        mutableStateOf<List<Patient>>(emptyList())
    }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Load patients when the screen first appears.
    LaunchedEffect(Unit) {
        repository.getPatients { result ->
            loading = false

            result
                .onSuccess { patients = it }
                .onFailure {
                    error = it.message ?: "Failed to load patients"
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "My Patients",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onAddPatient,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Patient")
        }

        Spacer(Modifier.height(8.dp))

        // Use MainActivity's existing sign-out callback.
        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign Out")
        }

        Spacer(Modifier.height(16.dp))

        // Display loading, errors, or patient records.
        when {
            loading -> CircularProgressIndicator()

            error != null -> Text(
                text = error ?: "",
                color = MaterialTheme.colorScheme.error
            )

            patients.isEmpty() -> Text("No patients registered yet.")

            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(patients, key = { it.id }) { patient ->
                    // Select a patient to open their profile.
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPatientClick(patient) }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                patient.petName,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text("Species: ${patient.species}")
                            Text("Owner: ${patient.ownerName}")
                        }
                    }
                }
            }
        }
    }
}
