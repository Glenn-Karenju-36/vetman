
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
import com.example.vetman01.ui.patients.AddMedicalRecordScreen
import com.example.vetman01.ui.patients.AddPatientScreen
import com.example.vetman01.ui.patients.EditPatientScreen
import com.example.vetman01.ui.patients.PatientListScreen
import com.example.vetman01.ui.patients.PatientProfileScreen
import com.example.vetman01.ui.theme.Vetman01Theme
import com.google.firebase.auth.FirebaseAuth

/**
 * MainActivity
 *
 * Handles authentication, user roles, and navigation between
 * patient registration, profiles, editing, and medical records.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            Vetman01Theme {

                val repository = remember { AuthRepository() }

                // Restore the existing Firebase authentication session.
                var signedIn by remember {
                    mutableStateOf(
                        FirebaseAuth.getInstance().currentUser != null
                    )
                }

                // Determine which interface the signed-in user can access.
                var role by remember { mutableStateOf<String?>(null) }
                var roleError by remember { mutableStateOf<String?>(null) }

                // Control navigation between veterinarian screens.
                var showAddPatient by remember { mutableStateOf(false) }
                var showEditPatient by remember { mutableStateOf(false) }
                var showAddMedicalRecord by remember { mutableStateOf(false) }

                // Store the currently selected patient's information.
                var selectedPatient by remember {
                    mutableStateOf<Patient?>(null)
                }

                // Increment these to reload Firestore data after saving.
                var patientListVersion by remember { mutableStateOf(0) }
                var patientProfileVersion by remember { mutableStateOf(0) }

                // Handle the Android system Back button.
                BackHandler(
                    enabled = showAddPatient ||
                            showEditPatient ||
                            showAddMedicalRecord ||
                            selectedPatient != null
                ) {
                    when {
                        showAddMedicalRecord -> showAddMedicalRecord = false
                        showEditPatient -> showEditPatient = false
                        showAddPatient -> showAddPatient = false
                        selectedPatient != null -> selectedPatient = null
                    }
                }

                /**
                 * Retrieve the user's role from Firestore after login.
                 * Veterinarians and pet owners use different screens.
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

                // Sign out and reset all navigation state.
                val signOut = {
                    repository.signOut()
                    signedIn = false
                    role = null
                    roleError = null
                    showAddPatient = false
                    showEditPatient = false
                    showAddMedicalRecord = false
                    selectedPatient = null
                    patientListVersion = 0
                    patientProfileVersion = 0
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

                            // Display the login screen.
                            !signedIn -> {
                                AuthScreen(
                                    onAuthSuccess = {
                                        signedIn = true
                                    }
                                )
                            }

                            // Display errors while retrieving the user's role.
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

                                                // Refresh the patient list.
                                                patientListVersion++

                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Patient saved",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )
                                    }

                                    // Add a consultation to the selected patient.
                                    showAddMedicalRecord && selectedPatient != null -> {
                                        AddMedicalRecordScreen(
                                            patient = selectedPatient!!,

                                            onBack = {
                                                showAddMedicalRecord = false
                                            },

                                            onSaved = {
                                                // Return to the profile and reload history.
                                                showAddMedicalRecord = false
                                                patientProfileVersion++

                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Medical record saved",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )
                                    }

                                    // Edit an existing patient.
                                    showEditPatient && selectedPatient != null -> {
                                        EditPatientScreen(
                                            patient = selectedPatient!!,

                                            onBack = {
                                                showEditPatient = false
                                            },

                                            onSaved = { updatedPatient ->

                                                // Replace the old patient data.
                                                selectedPatient = updatedPatient
                                                showEditPatient = false

                                                // Refresh both list and profile.
                                                patientListVersion++
                                                patientProfileVersion++

                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Patient updated",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )
                                    }

                                    // Display patient information and medical history.
                                    selectedPatient != null -> {
                                        key(patientProfileVersion) {
                                            PatientProfileScreen(
                                                patient = selectedPatient!!,

                                                onBack = {
                                                    selectedPatient = null
                                                },

                                                onEdit = {
                                                    showEditPatient = true
                                                },

                                                // Open the medical record entry form.
                                                onAddMedicalRecord = {
                                                    showAddMedicalRecord = true
                                                }
                                            )
                                        }
                                    }

                                    // Display registered patients.
                                    else -> {
                                        key(patientListVersion) {
                                            PatientListScreen(
                                                onAddPatient = {
                                                    showAddPatient = true
                                                },

                                                onPatientClick = { patient ->
                                                    selectedPatient = patient
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Preserve the existing pet-owner screen.
                            role == "pet_owner" -> {
                                OwnerHomeScreen(
                                    onSignOut = signOut
                                )
                            }

                            // Handle an unknown account role.
                            role != null -> {
                                Text("Unknown role: $role")
                            }

                            // Wait while Firebase retrieves the user's role.
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
