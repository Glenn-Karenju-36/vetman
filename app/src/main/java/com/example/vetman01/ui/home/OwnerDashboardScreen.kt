
package com.example.vetman01.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.example.vetman01.data.PatientRepository
import com.example.vetman01.model.Patient
import com.example.vetman01.ui.patients.OwnerPatientProfileScreen

/**
 * Read-only dashboard for a signed-in pet owner.
 *
 * Uses the existing PatientRepository so the owner
 * sees only pets linked to their Firebase account.
 *
 * No CRUD operations are exposed here.
 */
@Composable
fun OwnerDashboardScreen(
    onSignOut: () -> Unit,
    repository: PatientRepository = remember {
        PatientRepository()
    }
) {
    val colors = MaterialTheme.colorScheme

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

    var selectedPatient by remember {
        mutableStateOf<Patient?>(null)
    }

    var refreshVersion by remember {
        mutableIntStateOf(0)
    }

    // Only retrieve patients belonging to this owner.
    LaunchedEffect(refreshVersion) {
        loading = true
        error = null

        repository.getOwnerPatients { result ->
            result
                .onSuccess { loadedPatients ->
                    patients = loadedPatients
                }
                .onFailure { exception ->
                    error = exception.message
                        ?: "Unable to load your pets."
                }

            loading = false
        }
    }

    BackHandler(enabled = selectedPatient != null) {
        selectedPatient = null
    }

    // Preserve existing read-only profile navigation.
    val activePatient = selectedPatient

    if (activePatient != null) {
        OwnerPatientProfileScreen(
            patient = activePatient,
            onBack = {
                selectedPatient = null
            }
        )
    } else {

        // Local filtering: no Firestore requests per keystroke.
        val filteredPatients = remember(
            patients,
            searchQuery
        ) {
            val query = searchQuery.trim()

            patients.filter { patient ->
                query.isEmpty() ||
                        patient.petName.contains(
                            query,
                            ignoreCase = true
                        ) ||
                        patient.species.contains(
                            query,
                            ignoreCase = true
                        ) ||
                        patient.breed.contains(
                            query,
                            ignoreCase = true
                        )
            }.sortedBy {
                it.petName.lowercase()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {

            Spacer(Modifier.height(16.dp))

            // DASHBOARD HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "My Pets",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Your veterinary dashboard",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }

                TextButton(onClick = onSignOut) {
                    Text("Sign Out")
                }
            }

            Spacer(Modifier.height(20.dp))

            // DASHBOARD SUMMARY
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colors.surface
                    ) {
                        Box(
                            modifier = Modifier.size(52.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Placeholder until actual pet images exist.
                            Text(
                                text = "🐾",
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }

                    Column {
                        Text(
                            text = if (loading) {
                                "—"
                            } else {
                                patients.size.toString()
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onPrimaryContainer
                        )

                        Text(
                            text = if (patients.size == 1) {
                                "Linked Pet"
                            } else {
                                "Linked Pets"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            // SEARCH
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                },
                label = {
                    Text("Search Pets")
                },
                placeholder = {
                    Text("Name, species or breed")
                },
                leadingIcon = {
                    Text(
                        text = "⌕",
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                searchQuery = ""
                            }
                        ) {
                            Text(
                                text = "×",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(20.dp))

            // PET LIST HEADING
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Patients",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.primary,
                    fontWeight = FontWeight.SemiBold
                )

                if (!loading && error == null) {
                    Text(
                        text = "${filteredPatients.size} shown",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // LOADING, ERROR, EMPTY AND CONTENT STATES
            when {
                loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = error ?: "Unable to load pets.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.error
                        )

                        Spacer(Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = {
                                refreshVersion++
                            }
                        ) {
                            Text("Try Again")
                        }
                    }
                }

                patients.isEmpty() -> {
                    OwnerEmptyState(
                        title = "No Linked Pets Yet",
                        description = "Your veterinarian can link your pets to this account.",
                        modifier = Modifier.weight(1f)
                    )
                }

                filteredPatients.isEmpty() -> {
                    OwnerEmptyState(
                        title = "No Matching Pets",
                        description = "Try a different name, species or breed.",
                        modifier = Modifier.weight(1f)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            bottom = 24.dp
                        )
                    ) {
                        items(
                            items = filteredPatients,
                            key = { patient ->
                                patient.id
                            }
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
        }
    }
}

/**
 * Read-only pet card.
 *
 * Uses the patient ID for list identity because
 * multiple animals may share a name.
 */
@Composable
private fun OwnerPetCard(
    patient: Patient,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            colors.outlineVariant
        ),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // PET PHOTO PLACEHOLDER
            Surface(
                modifier = Modifier.size(64.dp),
                shape = RoundedCornerShape(12.dp),
                color = colors.primaryContainer
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🐾",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = patient.petName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    text = patient.species.ifBlank {
                        "Species not specified"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )

                Spacer(Modifier.height(3.dp))

                Text(
                    text = patient.breed.ifBlank {
                        "Breed not specified"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            Text(
                text = "›",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.primary
            )
        }
    }
}

/**
 * Shared empty-state presentation.
 */
@Composable
private fun OwnerEmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🐾",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
        }
    }
}
