package com.example.vetman01.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Home for veterinarians. The patient list will appear here later.
@Composable
fun VetHomeScreen(onAddPatient: () -> Unit, onSignOut: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Veterinarian home", style = MaterialTheme.typography.headlineMedium)
        Text("Patients will appear here", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAddPatient) { Text("Add patient") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onSignOut) { Text("Sign out") }
    }
}

// Placeholder home for pet owners. Later this shows their pet's
// information with the "Download PDF card" button.
@Composable
fun OwnerHomeScreen(onSignOut: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Pet owner home", style = MaterialTheme.typography.headlineMedium)
        Text("Your pet's information will appear here", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onSignOut) { Text("Sign out") }
    }
}