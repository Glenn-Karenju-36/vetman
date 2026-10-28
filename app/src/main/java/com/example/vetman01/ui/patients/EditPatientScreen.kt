
package com.example.vetman01.ui.patients

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vetman01.data.PatientRepository
import com.example.vetman01.model.Patient

/**
 * Allows a veterinarian to edit an existing patient's details.
 * Fields are prefilled using the selected patient's saved information.
 */
@Composable
fun EditPatientScreen(
    patient: Patient,
    onBack: () -> Unit,
    onSaved: (Patient) -> Unit,
    repository: PatientRepository = remember { PatientRepository() }
) {
    // Initialize editable fields from the existing patient record.
    var petName by remember(patient.id) { mutableStateOf(patient.petName) }
    var species by remember(patient.id) { mutableStateOf(patient.species) }
    var breed by remember(patient.id) { mutableStateOf(patient.breed) }
    var age by remember(patient.id) { mutableStateOf(patient.age) }
    var sex by remember(patient.id) { mutableStateOf(patient.sex) }
    var ownerName by remember(patient.id) { mutableStateOf(patient.ownerName) }
    var ownerPhone by remember(patient.id) { mutableStateOf(patient.ownerPhone) }
    var notes by remember(patient.id) { mutableStateOf(patient.notes) }

    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Prevent saving incomplete patient records.
    val canSave = petName.isNotBlank() &&
            species.isNotBlank() &&
            sex.isNotBlank() &&
            ownerName.isNotBlank() &&
            !saving

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
            "Edit Patient",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = petName,
            onValueChange = { petName = it },
            label = { Text("Pet name *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = species,
            onValueChange = { species = it },
            label = { Text("Species *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = breed,
            onValueChange = { breed = it },
            label = { Text("Breed") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = age,
            onValueChange = { age = it },
            label = { Text("Age") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = sex,
            onValueChange = { sex = it },
            label = { Text("Sex *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = ownerName,
            onValueChange = { ownerName = it },
            label = { Text("Owner name *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = ownerPhone,
            onValueChange = { ownerPhone = it },
            label = { Text("Owner phone") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        error?.let { message ->
            Spacer(Modifier.height(12.dp))
            Text(
                message,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {

                // Copy the existing record to preserve its ID,
                // veterinarian ownership and creation timestamp.
                val updatedPatient = patient.copy(
                    petName = petName.trim(),
                    species = species.trim(),
                    breed = breed.trim(),
                    age = age.trim(),
                    sex = sex.trim(),
                    ownerName = ownerName.trim(),
                    ownerPhone = ownerPhone.trim(),
                    notes = notes.trim()
                )

                saving = true
                error = null

                // Update the existing Firestore document.
                repository.updatePatient(updatedPatient) { result ->
                    saving = false

                    result
                        .onSuccess {
                            // Return the updated record to MainActivity.
                            onSaved(updatedPatient)
                        }
                        .onFailure {
                            error = it.message
                                ?: "Could not update patient"
                        }
                }
            },
            enabled = canSave,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (saving) "Saving..." else "Save Changes")
        }
    }
}
