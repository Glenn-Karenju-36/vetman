
package com.example.vetman01.ui.patients

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vetman01.data.PatientRepository
import com.example.vetman01.model.Patient

/**
 * Veterinarian patient list.
 *
 * Uses the Minimal Clinical design while preserving
 * registration, profile navigation and sign-out.
 */
@Composable
fun PatientListScreen(
    onAddPatient: () -> Unit,
    onPatientClick: (Patient) -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit,
    repository: PatientRepository = remember { PatientRepository() }
) {
    val green = MaterialTheme.colorScheme.primary
    val softGreen = MaterialTheme.colorScheme.primaryContainer
    val textColor = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val border = MaterialTheme.colorScheme.outlineVariant

    var patients by remember {
        mutableStateOf<List<Patient>>(emptyList())
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    var searchQuery by remember {
        mutableStateOf("")
    }

    // Retrieve patients belonging to the signed-in veterinarian.
    LaunchedEffect(Unit) {
        repository.getPatients { result ->
            result
                .onSuccess {
                    patients = it
                    error = null
                }
                .onFailure {
                    error = it.message ?: "Failed to load patients"
                }

            loading = false
        }
    }

    // Search locally by pet name, species, breed or owner.
    // No additional Firestore queries are required.
    val filteredPatients = remember(patients, searchQuery) {
        val query = searchQuery.trim()

        patients.filter { patient ->
            query.isBlank() ||
                    patient.petName.contains(query, ignoreCase = true) ||
                    patient.ownerName.contains(query, ignoreCase = true) ||
                    patient.species.contains(query, ignoreCase = true) ||
                    patient.breed.contains(query, ignoreCase = true)
        }.sortedBy { it.petName.lowercase() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // Return to the veterinarian dashboard.
        TextButton(onClick = onBack) {
            Text("← Back to Dashboard")
        }

        Spacer(Modifier.height(12.dp))

        // SCREEN HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "My Patients",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Manage your registered patients",
                    style = MaterialTheme.typography.bodyMedium,
                    color = muted
                )
            }

            Surface(
                color = softGreen,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = green,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // SEARCH
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = {
                Text("Search patients")
            },
            placeholder = {
                Text("Pet name, owner or species")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(16.dp))

        // ADD PATIENT
        Button(
            onClick = onAddPatient,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null
            )

            Spacer(Modifier.width(8.dp))

            Text("Add Patient")
        }

        Spacer(Modifier.height(20.dp))

        // PATIENT COUNT
        if (!loading && error == null) {
            Text(
                text = if (searchQuery.isBlank()) {
                    "${patients.size} Registered Patients"
                } else {
                    "${filteredPatients.size} Search Results"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )

            Spacer(Modifier.height(12.dp))
        }

        // PATIENT LIST
        when {
            loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Text(
                    text = error ?: "Unable to load patients",
                    color = MaterialTheme.colorScheme.error
                )
            }

            filteredPatients.isEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = softGreen
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) {
                            "No patients registered yet."
                        } else {
                            "No patients match your search."
                        },
                        modifier = Modifier.padding(20.dp),
                        color = muted
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = filteredPatients,
                        key = { it.id }
                    ) { patient ->
                        ClinicalPatientCard(
                            patient = patient,
                            onClick = {
                                onPatientClick(patient)
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Preserve the existing sign-out action.
        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Sign Out")
        }
    }
}

/**
 * Reusable patient card.
 *
 * Each card opens the selected patient's existing profile.
 */
@Composable
private fun ClinicalPatientCard(
    patient: Patient,
    onClick: () -> Unit
) {
    val green = MaterialTheme.colorScheme.primary
    val softGreen = MaterialTheme.colorScheme.primaryContainer
    val textColor = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // Pet icon.
            Surface(
                color = softGreen,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = green,
                    modifier = Modifier
                        .size(52.dp)
                        .padding(12.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = patient.petName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )

                Text(
                    text = listOf(
                        patient.species,
                        patient.breed
                    ).filter { it.isNotBlank() }
                        .joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = muted
                )

                Text(
                    text = "Owner: ${patient.ownerName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = muted
                )
            }

            Text(
                text = "›",
                style = MaterialTheme.typography.headlineSmall,
                color = green
            )
        }
    }
}
