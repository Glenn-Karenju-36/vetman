
package com.example.vetman01.ui.patients

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vetman01.data.PatientOfflineEditRepository
import com.example.vetman01.data.PatientEditSaveStatus
import com.example.vetman01.model.Patient

/**
 * Edit an existing veterinary patient online or offline.
 *
 * Preserves patient identity, veterinarian ownership,
 * pet-owner linking, and creation timestamp.
 */
@Composable
fun EditPatientScreen(
    patient: Patient,
    onBack: () -> Unit,
    onSaved: (Patient) -> Unit,
    repository: PatientOfflineEditRepository = remember {
        PatientOfflineEditRepository()
    }
) {
    val colors = MaterialTheme.colorScheme

    val speciesOptions = listOf(
        "Dog", "Cat", "Cattle", "Goat", "Sheep", "Pig",
        "Poultry", "Donkey", "Camel", "Horse", "Rabbit"
    )

    val initialCustomSpecies =
        patient.species.isNotBlank() &&
                speciesOptions.none {
                    it.equals(patient.species, ignoreCase = true)
                }

    var petName by remember(patient.id) {
        mutableStateOf(patient.petName)
    }

    var species by remember(patient.id) {
        mutableStateOf(
            speciesOptions.firstOrNull {
                it.equals(patient.species, ignoreCase = true)
            } ?: "Other"
        )
    }

    var otherSpecies by remember(patient.id) {
        mutableStateOf(
            if (initialCustomSpecies) patient.species else ""
        )
    }

    var breed by remember(patient.id) {
        mutableStateOf(patient.breed)
    }

    var age by remember(patient.id) {
        mutableStateOf(patient.age)
    }

    var sex by remember(patient.id) {
        mutableStateOf(patient.sex)
    }

    var ownerName by remember(patient.id) {
        mutableStateOf(patient.ownerName)
    }

    var ownerPhone by remember(patient.id) {
        mutableStateOf(patient.ownerPhone)
    }

    var notes by remember(patient.id) {
        mutableStateOf(patient.notes)
    }

    var saving by remember(patient.id) {
        mutableStateOf(false)
    }

    var submitted by remember(patient.id) {
        mutableStateOf(false)
    }

    var queuedLocally by remember(patient.id) {
        mutableStateOf(false)
    }

    var error by remember(patient.id) {
        mutableStateOf<String?>(null)
    }

    var stopSaveObserver by remember(patient.id) {
        mutableStateOf<(() -> Unit)?>(null)
    }

    var pendingEditedPatient by remember(patient.id) {
        mutableStateOf<Patient?>(null)
    }

    // Stop watching when the screen leaves composition.
    // Firestore retains the queued edit for synchronization.
    DisposableEffect(patient.id) {
        onDispose {
            stopSaveObserver?.invoke()
        }
    }

    fun finishAndReturn() {
        stopSaveObserver?.invoke()
        stopSaveObserver = null

        pendingEditedPatient?.let { updated ->
            onSaved(updated)
        }
    }

    val canSave =
        petName.isNotBlank() &&
                species.isNotBlank() &&
                (species != "Other" || otherSpecies.isNotBlank()) &&
                sex.isNotBlank() &&
                ownerName.isNotBlank() &&
                !saving &&
                !submitted

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TextButton(
            onClick = onBack,
            enabled = !saving && !submitted
        ) {
            Text("← Back to Profile")
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Edit Patient",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(5.dp))

        Text(
            text = "Update ${patient.petName}'s information.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        // PHOTO PLACEHOLDER
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(155.dp),
            colors = CardDefaults.cardColors(
                containerColor = colors.primaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(42.dp)
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Pet Photo",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onPrimaryContainer
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Photo uploads coming soon",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(26.dp))

        // PATIENT INFORMATION
        OfflineEditSection("Patient Information")

        Spacer(Modifier.height(12.dp))

        OfflineEditFormCard {
            OfflineEditField(
                value = petName,
                onValueChange = { petName = it },
                label = "Pet Name *",
                enabled = !submitted && !saving
            )

            Spacer(Modifier.height(18.dp))

            Text(
                text = "Species *",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(8.dp))

            OfflineEditChoiceChips(
                options = speciesOptions + "Other",
                selected = species,
                enabled = !submitted && !saving,
                onSelect = { species = it }
            )

            if (species == "Other") {
                Spacer(Modifier.height(12.dp))

                OfflineEditField(
                    value = otherSpecies,
                    onValueChange = { otherSpecies = it },
                    label = "Specify Species *",
                    enabled = !submitted && !saving
                )
            }

            Spacer(Modifier.height(18.dp))

            OfflineEditField(
                value = breed,
                onValueChange = { breed = it },
                label = "Breed",
                enabled = !submitted && !saving
            )

            Spacer(Modifier.height(14.dp))

            OfflineEditField(
                value = age,
                onValueChange = { age = it },
                label = "Age (e.g. 3 years, 6 months)",
                enabled = !submitted && !saving
            )

            Spacer(Modifier.height(18.dp))

            Text(
                text = "Sex *",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(8.dp))

            OfflineEditChoiceChips(
                options = listOf("Male", "Female"),
                selected = sex,
                enabled = !submitted && !saving,
                onSelect = { sex = it }
            )
        }

        Spacer(Modifier.height(24.dp))

        // OWNER INFORMATION
        OfflineEditSection("Owner Information")

        Spacer(Modifier.height(12.dp))

        OfflineEditFormCard {
            OfflineEditField(
                value = ownerName,
                onValueChange = { ownerName = it },
                label = "Owner Name *",
                enabled = !submitted && !saving
            )

            Spacer(Modifier.height(14.dp))

            OfflineEditField(
                value = ownerPhone,
                onValueChange = { ownerPhone = it },
                label = "Owner Phone",
                enabled = !submitted && !saving,
                keyboardType = KeyboardType.Phone
            )
        }

        Spacer(Modifier.height(24.dp))

        // ADDITIONAL NOTES
        OfflineEditSection("Additional Notes")

        Spacer(Modifier.height(12.dp))

        OfflineEditFormCard {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Patient Notes") },
                placeholder = {
                    Text("Important information about this patient")
                },
                minLines = 3,
                maxLines = 7,
                enabled = !submitted && !saving,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Fields marked * are required.",
            color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )

        // Offline save confirmation.
        if (queuedLocally) {
            Spacer(Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = colors.primaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Saved on this device",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onPrimaryContainer
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "Your changes are saved locally " +
                                "and awaiting synchronization. " +
                                "Reconnect to the internet to " +
                                "complete the update.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onPrimaryContainer
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { finishAndReturn() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Return to Patient Profile")
                    }
                }
            }
        }

        error?.let { message ->
            Spacer(Modifier.height(12.dp))

            Text(
                text = message,
                color = colors.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(22.dp))

        Button(
            onClick = {
                if (canSave) {
                    // Copying keeps the existing patient ID,
                    // linked owner, vet ID, and creation date.
                    val updatedPatient = patient.copy(
                        petName = petName.trim(),
                        species = if (species == "Other") {
                            otherSpecies.trim()
                        } else {
                            species
                        },
                        breed = breed.trim(),
                        age = age.trim(),
                        sex = sex.trim(),
                        ownerName = ownerName.trim(),
                        ownerPhone = ownerPhone.trim(),
                        notes = notes.trim()
                    )

                    pendingEditedPatient = updatedPatient
                    saving = true
                    submitted = true
                    queuedLocally = false
                    error = null

                    stopSaveObserver =
                        repository.updatePatientOfflineAware(
                            updatedPatient
                        ) { status ->

                            when (status) {
                                PatientEditSaveStatus.QueuedLocally -> {
                                    saving = false
                                    queuedLocally = true
                                }

                                PatientEditSaveStatus.Synced -> {
                                    saving = false
                                    queuedLocally = false
                                    finishAndReturn()
                                }

                                is PatientEditSaveStatus.Failed -> {
                                    saving = false
                                    submitted = false
                                    queuedLocally = false
                                    pendingEditedPatient = null
                                    stopSaveObserver = null

                                    error = status.error.message
                                        ?: "Could not update patient"
                                }
                            }
                        }
                }
            },
            enabled = canSave,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (saving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = colors.onPrimary
                )

                Spacer(Modifier.width(8.dp))
                Text("Saving...")
            } else if (queuedLocally) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null
                )

                Spacer(Modifier.width(8.dp))
                Text("Queued for Sync")
            } else {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null
                )

                Spacer(Modifier.width(8.dp))
                Text("Save Changes")
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Minimal Clinical section heading.
 */
@Composable
private fun OfflineEditSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}

/**
 * Reusable form container.
 */
@Composable
private fun OfflineEditFormCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            content = content
        )
    }
}

/**
 * Consistent patient text input.
 */
@Composable
private fun OfflineEditField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    )
}

/**
 * Wrapping single-choice chips for species and sex.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OfflineEditChoiceChips(
    options: List<String>,
    selected: String,
    enabled: Boolean,
    onSelect: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                enabled = enabled,
                label = { Text(option) }
            )
        }
    }
}
