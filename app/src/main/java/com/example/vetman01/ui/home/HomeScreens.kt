
package com.example.vetman01.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vetman01.data.PatientRepository
import com.example.vetman01.model.Patient
import com.example.vetman01.ui.patients.OwnerPatientProfileScreen
import com.google.firebase.auth.FirebaseAuth

/**
 * Existing veterinarian home screen.
 * Preserved without changing its functionality.
 */
@Composable
fun VetHomeScreen(
    onAddPatient: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Veterinarian home",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            "Patients will appear here",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(24.dp))

        Button(onClick = onAddPatient) {
            Text("Add patient")
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(onClick = onSignOut) {
            Text("Sign out")
        }
    }
}

/**
 * Pet-owner dashboard.
 *
 * Shows pets linked to the logged-in owner.
 * Selecting a pet opens its read-only profile.
 */
@Composable
fun OwnerHomeScreen(
    onSignOut: () -> Unit
) {
    val repository = remember { PatientRepository() }
    val ownerId = FirebaseAuth.getInstance().currentUser?.uid

    var patients by remember(ownerId) {
        mutableStateOf<List<Patient>>(emptyList())
    }

    var selectedPatient by remember(ownerId) {
        mutableStateOf<Patient?>(null)
    }

    var isLoading by remember(ownerId) {
        mutableStateOf(true)
    }

    var errorMessage by remember(ownerId) {
        mutableStateOf<String?>(null)
    }

    // Retrieve only pets linked to this owner.
    LaunchedEffect(ownerId) {
        isLoading = true
        errorMessage = null

        repository.getOwnerPatients { result ->
            result
                .onSuccess { loadedPatients ->
                    patients = loadedPatients
                    errorMessage = null
                }
                .onFailure { exception ->
                    errorMessage = exception.localizedMessage
                        ?: "Unable to load your pets"
                }

            isLoading = false
        }
    }

    // Android's Back button returns to the owner's pet list.
    BackHandler(enabled = selectedPatient != null) {
        selectedPatient = null
    }

    // Navigate to the selected pet's read-only profile.
    if (selectedPatient != null) {
        OwnerPatientProfileScreen(
            patient = selectedPatient!!,
            onBack = {
                selectedPatient = null
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                "My Pets",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "View your registered pets",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(24.dp))

            when {
                isLoading -> {
                    CircularProgressIndicator()
                }

                errorMessage != null -> {
                    Text(
                        text = errorMessage ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                patients.isEmpty() -> {
                    Text(
                        "No pets linked to your account yet.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = patients,
                            key = { it.id }
                        ) { patient ->
                            OwnerPetCard(
                                patient = patient,
                                onClick = {
                                    selectedPatient = patient
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign out")
            }
        }
    }
}

/**
 * Clickable pet card.
 * Supports owners with multiple registered pets.
 */
@Composable
private fun OwnerPetCard(
    patient: Patient,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = patient.petName,
                style = MaterialTheme.typography.titleLarge
            )

            Text("Species: ${patient.species}")

            if (patient.breed.isNotBlank()) {
                Text("Breed: ${patient.breed}")
            }

            if (patient.age.isNotBlank()) {
                Text("Age: ${patient.age}")
            }

            Text(
                "Tap to view profile",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
