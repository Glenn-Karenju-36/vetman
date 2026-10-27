package com.example.vetman01.ui.patients

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vetman01.data.PatientRepository
import com.example.vetman01.model.Patient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPatientScreen(
    onBack: () -> Unit,    // called when the vet taps Back
    onSaved: () -> Unit,   // called after the patient is saved successfully
    repository: PatientRepository = remember { PatientRepository() }
) {
    // One state variable per form field. The screen redraws when they change.
    var petName by remember { mutableStateOf("") }
    var species by remember { mutableStateOf("") }
    var otherSpecies by remember { mutableStateOf("") } // typed in when "Other" is chosen
    var breed by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var ownerPhone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Fields marked * are required before the Save button turns on
    val canSave = petName.isNotBlank() &&
            species.isNotBlank() && (species != "Other" || otherSpecies.isNotBlank()) &&
            sex.isNotBlank() && ownerName.isNotBlank() && !saving

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // the form scrolls on small screens
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack) { Text("< Back") }
        Text("Add patient", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = petName, onValueChange = { petName = it },
            label = { Text("Pet name *") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        Text("Species *", style = MaterialTheme.typography.labelLarge)
        ChipRow(
            options = listOf(
                "Dog", "Cat", "Cattle", "Goat", "Sheep", "Pig",
                "Poultry", "Donkey", "Camel", "Horse", "Rabbit", "Other"
            ),
            selected = species,
            onSelect = { species = it }
        )

        // Only shown when "Other" is selected, so the vet can type any species
        if (species == "Other") {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = otherSpecies, onValueChange = { otherSpecies = it },
                label = { Text("Specify species *") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = breed, onValueChange = { breed = it },
            label = { Text("Breed") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = age, onValueChange = { age = it },
            label = { Text("Age (e.g. 3 years, 6 months)") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        Text("Sex *", style = MaterialTheme.typography.labelLarge)
        ChipRow(
            options = listOf("Male", "Female"),
            selected = sex,
            onSelect = { sex = it }
        )

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = ownerName, onValueChange = { ownerName = it },
            label = { Text("Owner name *") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = ownerPhone, onValueChange = { ownerPhone = it },
            label = { Text("Owner phone") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = notes, onValueChange = { notes = it },
            label = { Text("Notes") }, minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                error = null
                saving = true
                val patient = Patient(
                    petName = petName.trim(),
                    // If "Other" was chosen, save what the vet typed instead
                    species = if (species == "Other") otherSpecies.trim() else species,
                    breed = breed.trim(),
                    age = age.trim(),
                    sex = sex,
                    ownerName = ownerName.trim(),
                    ownerPhone = ownerPhone.trim(),
                    notes = notes.trim()
                )
                // Firestore replies later; this block runs when it does
                repository.addPatient(patient) { result ->
                    saving = false
                    result
                        .onSuccess { onSaved() }
                        .onFailure { error = it.message ?: "Could not save patient" }
                }
            },
            enabled = canSave,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (saving) "Saving..." else "Save patient")
        }
    }
}

/**
 * Chips that wrap onto new lines when they don't fit (one choice at a time).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(option) }
            )
        }
    }
}