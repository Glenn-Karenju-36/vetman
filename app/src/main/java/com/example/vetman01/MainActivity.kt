
package com.example.vetman01

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.vetman01.data.AuthRepository
import com.example.vetman01.model.Patient
import com.example.vetman01.ui.auth.AuthScreen
import com.example.vetman01.ui.home.OwnerHomeScreen
import com.example.vetman01.ui.patients.AddPatientScreen
import com.example.vetman01.ui.patients.EditPatientScreen
import com.example.vetman01.ui.patients.PatientListScreen
import com.example.vetman01.ui.patients.PatientProfileScreen
import com.example.vetman01.ui.theme.Vetman01Theme
import com.google.firebase.auth.FirebaseAuth

/**
 * MainActivity
 *
 * Handles user authentication, role-based navigation,
 * patient registration, viewing and editing.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            Vetman01Theme {

                val repository = remember { AuthRepository() }

                // Restore Firebase's existing authentication session.
                var signedIn by remember {
                    mutableStateOf(
                        FirebaseAuth.getInstance().currentUser != null
                    )
                }

                // User role determines access to veterinarian or owner screens.
                var role by remember { mutableStateOf<String?>(null) }
                var roleError by remember { mutableStateOf<String?>(null) }

                // Navigation state for patient-related screens.
                var showAddPatient by remember { mutableStateOf(false) }
                var showEditPatient by remember { mutableStateOf(false) }

                // Holds the selected patient's current information.
                var selectedPatient by remember {
                    mutableStateOf<Patient?>(null)
                }

                // Changing this key reloads the list from Firestore.
                var patientListVersion by remember { mutableStateOf(0) }

                // Handle the Android system Back button.
                BackHandler(
                    enabled = showAddPatient ||
                            showEditPatient ||
                            selectedPatient != null
                ) {
                    when {
                        showEditPatient -> showEditPatient = false
                        showAddPatient -> showAddPatient = false
                        selectedPatient != null -> selectedPatient = null
                    }
                }

                /**
                 * Retrieve the authenticated user's role from Firestore.
                 */
                LaunchedEffect(signedIn) {
                    if (signedIn) {
                        role = null
                        roleError = null

                        repository.getUserRole { result ->
                            result
                                .onSuccess { role = it }
                                .onFailure {
                                    roleError = it.message
                                        ?: "Could not load your role"
                                }
                        }
                    }
                }

                // Clear authentication and navigation state on sign-out.
                val signOut = {
                    repository.signOut()
                    signedIn = false
                    role = null
                    roleError = null
                    showAddPatient = false
                    showEditPatient = false
                    selectedPatient = null
                    patientListVersion = 0
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->

                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    ) {

                        when {

                            // Show login when no user is authenticated.
                            !signedIn -> {
                                AuthScreen(
                                    onAuthSuccess = {
                                        signedIn = true
                                    }
                                )
                            }

                            // Display errors loading the user's role.
                            roleError != null -> {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = roleError ?: "",
                                        color = MaterialTheme.colorScheme.error
                                    )

                                    Button(onClick = signOut) {
                                        Text("Sign out")
                                    }
                                }
                            }

                            // Veterinarian navigation.
                            role == "veterinarian" -> {

                                when {

                                    // Register a new patient.
                                    showAddPatient -> {
                                        AddPatientScreen(
                                            onBack = {
                                                showAddPatient = false
                                            },
                                            onSaved = {
                                                showAddPatient = false

                                                // Reload the list after registration.
                                                patientListVersion++

                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Patient saved",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )
                                    }

                                    // Edit the currently selected patient.
                                    showEditPatient && selectedPatient != null -> {
                                        EditPatientScreen(
                                            patient = selectedPatient!!,

                                            onBack = {
                                                showEditPatient = false
                                            },

                                            onSaved = { updatedPatient ->

                                                // Replace old patient data with updated values.
                                                selectedPatient = updatedPatient

                                                // Return to the updated patient profile.
                                                showEditPatient = false

                                                // Refresh the list for the next visit.
                                                patientListVersion++

                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Patient updated",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )
                                    }

                                    // Display the selected patient's profile.
                                    selectedPatient != null -> {
                                        PatientProfileScreen(
                                            patient = selectedPatient!!,

                                            onBack = {
                                                selectedPatient = null
                                            },

                                            // Navigate to the prefilled edit form.
                                            onEdit = {
                                                showEditPatient = true
                                            }
                                        )
                                    }

                                    // Display registered patients.
                                    else -> {
                                        key(patientListVersion) {
                                            PatientListScreen(
                                                onAddPatient = {
                                                    showAddPatient = true
                                                },

                                                // Open the tapped patient's profile.
                                                onPatientClick = { patient ->
                                                    selectedPatient = patient
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Preserve the pet-owner home screen.
                            role == "pet_owner" -> {
                                OwnerHomeScreen(
                                    onSignOut = signOut
                                )
                            }

                            // Unrecognized user role.
                            role != null -> {
                                Text("Unknown role: $role")
                            }

                            // Wait for Firebase to return the user's role.
                            else -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
