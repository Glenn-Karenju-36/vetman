
package com.example.vetman01.ui.patients

import android.util.Patterns

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

import com.example.vetman01.data.PatientRepository
import com.example.vetman01.data.PetLinkRequestRepository
import com.example.vetman01.model.Patient

/**
 * My Patients screen for veterinarians.
 *
 * Maintains existing patient registration, search,
 * profile navigation and sign-out functionality.
 *
 * The linking interface is opened from this screen
 * without changing MainActivity navigation.
 */
@Composable
fun PatientListScreen(
    onAddPatient: () -> Unit,
    onPatientClick: (Patient) -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit,
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

    var showLinkScreen by remember {
        mutableStateOf(false)
    }

    var refreshVersion by remember {
        mutableIntStateOf(0)
    }

    /**
     * Load the veterinarian's patients.
     *
     * Refresh when returning from the linking screen
     * so newly linked pets can be excluded from the
     * next linking search.
     */
    LaunchedEffect(refreshVersion) {
        loading = true
        error = null

        repository.getPatients { result ->
            result
                .onSuccess {
                    patients = it
                }
                .onFailure {
                    error = it.message
                        ?: "Unable to load patients."
                }

            loading = false
        }
    }

    // Back returns from linking to the patient list.
    BackHandler(enabled = showLinkScreen) {
        showLinkScreen = false
        refreshVersion++
    }

    if (showLinkScreen) {
        LinkPetsOwnerScreen(
            patients = patients,
            onBack = {
                showLinkScreen = false
                refreshVersion++
            }
        )
    } else {

        // Search all registered patients, including linked pets.
        // Linking status must not restrict veterinary care.
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
                        patient.ownerName.contains(
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
                .padding(16.dp)
        ) {
            TextButton(onClick = onBack) {
                Text("← Back to Dashboard")
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "My Patients",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Manage your registered patients",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }

                Surface(
                    color = colors.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                },
                label = {
                    Text("Search Patients")
                },
                placeholder = {
                    Text("Pet name, owner or species")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(16.dp))

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

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    showLinkScreen = true
                },
                enabled = !loading && error == null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Link Pet Owner")
            }

            Spacer(Modifier.height(20.dp))

            if (!loading && error == null) {
                Text(
                    text = if (searchQuery.isBlank()) {
                        "${patients.size} Registered Patients"
                    } else {
                        "${filteredPatients.size} Search Results"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(12.dp))
            }

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
                        text = error ?: "Unable to load patients.",
                        color = colors.error
                    )
                }

                filteredPatients.isEmpty() -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = colors.primaryContainer
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
                            color = colors.onPrimaryContainer
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        items(
                            items = filteredPatients,
                            key = { it.id }
                        ) { patient ->
                            PatientListClinicalCard(
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

            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Sign Out")
            }
        }
    }
}

/**
 * Veterinarian-side owner-linking screen.
 *
 * Only unlinked pets are selectable.
 *
 * Request submission never directly changes ownerId.
 * The owner must approve the request in Vetman.
 */
@Composable
private fun LinkPetsOwnerScreen(
    patients: List<Patient>,
    onBack: () -> Unit,
    linkRepository: PetLinkRequestRepository = remember {
        PetLinkRequestRepository()
    }
) {
    val colors = MaterialTheme.colorScheme

    var petSearch by remember {
        mutableStateOf("")
    }

    var selectedIds by remember {
        mutableStateOf<Set<String>>(emptySet())
    }

    var ownerEmail by remember {
        mutableStateOf("")
    }

    var showConfirmation by remember {
        mutableStateOf(false)
    }

    var submitting by remember {
        mutableStateOf(false)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    var successMessage by remember {
        mutableStateOf<String?>(null)
    }

    /**
     * Only patients without an ownerId may receive
     * a new linking request.
     *
     * Existing owner-linked patients remain accessible
     * in the normal veterinarian patient list.
     */
    val unlinkedPatients = remember(patients) {
        patients.filter {
            it.ownerId.isBlank()
        }
    }

    // Identify selected pets by Firestore document ID.
    val selectedPets = remember(
        unlinkedPatients,
        selectedIds
    ) {
        unlinkedPatients.filter {
            it.id in selectedIds
        }
    }

    // Filter locally to avoid Firestore reads per keystroke.
    val matchingPets = remember(
        unlinkedPatients,
        selectedIds,
        petSearch
    ) {
        val query = petSearch.trim()

        unlinkedPatients.filter { patient ->
            patient.id !in selectedIds &&
                    (
                            query.isBlank() ||
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
                            )
        }.sortedBy {
            it.petName.lowercase()
        }
    }

    val emailIsValid = Patterns.EMAIL_ADDRESS
        .matcher(ownerEmail.trim())
        .matches()

    // The repository currently supports five pets per batch.
    val canSubmit = selectedPets.isNotEmpty() &&
            selectedPets.size <= 5 &&
            emailIsValid &&
            !submitting

    /**
     * Final confirmation dialog.
     *
     * The user sees exactly which pets and email
     * will be included in the pending request.
     */
    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = {
                if (!submitting) {
                    showConfirmation = false
                }
            },
            title = {
                Text("Confirm Linking Request")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Owner email: ${ownerEmail.trim()}"
                    )

                    Text(
                        text = "${selectedPets.size} selected " +
                                if (selectedPets.size == 1) {
                                    "pet"
                                } else {
                                    "pets"
                                },
                        fontWeight = FontWeight.SemiBold
                    )

                    selectedPets.forEach { patient ->
                        Text(
                            text = "• ${patient.petName} — " +
                                    listOf(
                                        patient.species,
                                        patient.breed
                                    ).filter {
                                        it.isNotBlank()
                                    }.joinToString(" · ")
                        )
                    }

                    HorizontalDivider()

                    Text(
                        text = "The owner must accept the request " +
                                "before access to these pets is granted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )

                    if (submitting) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )

                            Text("Sending request...")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (canSubmit) {
                            submitting = true
                            error = null

                            /**
                             * Submit all selected pets through
                             * the repository's atomic batch.
                             *
                             * A successful callback means the
                             * requests were submitted, not that
                             * the owner accepted them.
                             */
                            linkRepository.createLinkRequest(
                                patientIds = selectedPets.map {
                                    it.id
                                },
                                ownerEmail = ownerEmail
                            ) { result ->
                                submitting = false
                                showConfirmation = false

                                result
                                    .onSuccess { count ->
                                        successMessage =
                                            "$count linking " +
                                                    if (count == 1) {
                                                        "request sent. "
                                                    } else {
                                                        "requests sent. "
                                                    } +
                                                    "Awaiting owner approval."

                                        selectedIds = emptySet()
                                        petSearch = ""
                                        ownerEmail = ""
                                    }
                                    .onFailure { exception ->
                                        error = exception.message
                                            ?: "Could not send linking request."
                                    }
                            }
                        }
                    },
                    enabled = canSubmit
                ) {
                    Text(
                        if (submitting) {
                            "Sending..."
                        } else {
                            "Send Request"
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmation = false
                    },
                    enabled = !submitting
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TextButton(
            onClick = onBack,
            enabled = !submitting
        ) {
            Text("← Back to My Patients")
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Link Pet Owner",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Select pets and send an owner-linking request.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))

        successMessage?.let { message ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = colors.primaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Request Submitted",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimaryContainer
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = message,
                        color = colors.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        error?.let { message ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = colors.errorContainer
                )
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    color = colors.onErrorContainer
                )
            }

            Spacer(Modifier.height(16.dp))
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.primaryContainer,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Available for Linking",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onPrimaryContainer
                    )

                    Text(
                        text = "Already-linked pets are hidden",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onPrimaryContainer
                    )
                }

                Text(
                    text = unlinkedPatients.size.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        Text(
            text = "Search Pets",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.primary
        )

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = petSearch,
            onValueChange = {
                petSearch = it
                error = null
            },
            label = {
                Text("Pet name, species or breed")
            },
            singleLine = true,
            enabled = !submitting,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(12.dp))

        // Bounded results list prevents a long patient list
        // from pushing the email form off the screen.
        if (unlinkedPatients.isEmpty()) {
            OwnerLinkMessage(
                "All registered patients are already linked."
            )
        } else if (matchingPets.isEmpty()) {
            OwnerLinkMessage(
                if (petSearch.isBlank()) {
                    "All available pets have been selected."
                } else {
                    "No unlinked pets match your search."
                }
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                matchingPets.take(12).forEach { patient ->
                    LinkPetChoiceCard(
                        patient = patient,
                        onClick = {
                            selectedIds = selectedIds + patient.id
                            petSearch = ""
                            error = null
                            successMessage = null
                        },
                        enabled = !submitting &&
                                selectedIds.size < 5
                    )
                }

                if (matchingPets.size > 12) {
                    Text(
                        text = "Type a name to narrow the results.",
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        Text(
            text = "Selected Pets (${selectedPets.size}/5)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.primary
        )

        Spacer(Modifier.height(10.dp))

        if (selectedPets.isEmpty()) {
            Text(
                text = "Search above to select pets.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        } else {
            selectedPets.forEach { patient ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = patient.petName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = listOf(
                                patient.species,
                                patient.breed
                            ).filter {
                                it.isNotBlank()
                            }.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }

                    TextButton(
                        onClick = {
                            selectedIds = selectedIds - patient.id
                        },
                        enabled = !submitting
                    ) {
                        Text("Remove")
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        HorizontalDivider(
            color = colors.outlineVariant
        )

        Spacer(Modifier.height(18.dp))

        Text(
            text = "Pet Owner",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.primary
        )

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = ownerEmail,
            onValueChange = {
                ownerEmail = it
                error = null
            },
            label = {
                Text("Registered Email Address")
            },
            placeholder = {
                Text("owner@example.com")
            },
            singleLine = true,
            enabled = !submitting,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "The owner must have a Vetman account " +
                    "and approve the request before their pets " +
                    "appear in My Pets.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                showConfirmation = true
                error = null
                successMessage = null
            },
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (selectedPets.size == 1) {
                    "Review 1 Pet"
                } else {
                    "Review ${selectedPets.size} Pets"
                }
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Read-only message card for empty search results.
 */
@Composable
private fun OwnerLinkMessage(message: String) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = colors.onSurfaceVariant
        )
    }
}

/**
 * A selectable pet card.
 *
 * Species and breed help distinguish animals
 * that happen to share the same name.
 */
@Composable
private fun LinkPetChoiceCard(
    patient: Patient,
    onClick: () -> Unit,
    enabled: Boolean
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = onClick
            ),
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
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = colors.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Pet photo placeholder",
                    tint = colors.primary,
                    modifier = Modifier
                        .size(52.dp)
                        .padding(12.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = patient.petName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = listOf(
                        patient.species,
                        patient.breed
                    ).filter {
                        it.isNotBlank()
                    }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            Text(
                text = "Add +",
                style = MaterialTheme.typography.labelLarge,
                color = colors.primary
            )
        }
    }
}

/**
 * Normal veterinarian patient card.
 *
 * Linked pets remain visible here because the
 * veterinarian must continue managing their care.
 */
@Composable
private fun PatientListClinicalCard(
    patient: Patient,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        ),
        border = BorderStroke(
            1.dp,
            colors.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = colors.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier
                        .size(52.dp)
                        .padding(12.dp)
                )
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
                    text = listOf(
                        patient.species,
                        patient.breed
                    ).filter {
                        it.isNotBlank()
                    }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Owner: ${patient.ownerName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            Text(
                text = "›",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.primary
            )
        }
    }
}
