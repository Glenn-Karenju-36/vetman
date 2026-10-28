
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
import com.example.vetman01.ui.patients.AddAppointmentScreen
import com.example.vetman01.ui.patients.AddMedicalRecordScreen
import com.example.vetman01.ui.patients.AddPatientScreen
import com.example.vetman01.ui.patients.AppointmentListScreen
import com.example.vetman01.ui.patients.EditPatientScreen
import com.example.vetman01.ui.patients.PatientListScreen
import com.example.vetman01.ui.patients.PatientProfileScreen
import com.example.vetman01.ui.theme.Vetman01Theme
import com.google.firebase.auth.FirebaseAuth

/**
 * Main entry point for Vetman.
 *
 * Handles authentication, user roles, and navigation between
 * patient management, medical records and appointments.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            Vetman01Theme {

                val repository = remember { AuthRepository() }

                // Restore an existing Firebase login session.
                var signedIn by remember {
                    mutableStateOf(
                        FirebaseAuth.getInstance().currentUser != null
                    )
                }

                // Store the user's role and any loading errors.
                var role by remember { mutableStateOf<String?>(null) }
                var roleError by remember { mutableStateOf<String?>(null) }

                // Patient-related navigation states.
                var showAddPatient by remember { mutableStateOf(false) }
                var showEditPatient by remember { mutableStateOf(false) }
                var showAddMedicalRecord by remember { mutableStateOf(false) }

                // Appointment-related navigation states.
                var showAddAppointment by remember { mutableStateOf(false) }
                var showAppointments by remember { mutableStateOf(false) }

                // Currently selected patient.
                var selectedPatient by remember {
                    mutableStateOf<Patient?>(null)
                }

                // Used to reload Firestore data after changes.
                var patientListVersion by remember { mutableStateOf(0) }
                var patientProfileVersion by remember { mutableStateOf(0) }

                /**
                 * Handle Android's Back button.
                 * Close the active form before returning to the patient list.
                 */
                BackHandler(
                    enabled = showAddPatient ||
                            showEditPatient ||
                            showAddMedicalRecord ||
                            showAddAppointment ||
                            showAppointments ||
                            selectedPatient != null
                ) {
                    when {
                        showAddAppointment -> showAddAppointment = false
                        showAppointments -> showAppointments = false
                        showAddMedicalRecord -> showAddMedicalRecord = false
                        showEditPatient -> showEditPatient = false
                        showAddPatient -> showAddPatient = false
                        selectedPatient != null -> selectedPatient = null
                    }
                }

                /**
                 * Retrieve the user's role after authentication.
                 * Determines whether to display veterinarian or owner screens.
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

                // Sign out and reset all navigation states.
                val signOut = {
                    repository.signOut()
                    signedIn = false
                    role = null
                    roleError = null

                    showAddPatient = false
                    showEditPatient = false
                    showAddMedicalRecord = false
                    showAddAppointment = false
                    showAppointments = false

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

                            // Authentication screen.
                            !signedIn -> {
                                AuthScreen(
                                    onAuthSuccess = {
                                        signedIn = true
                                    }
                                )
                            }

                            // Display errors retrieving the user's role.
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

                            // Veterinarian screens.
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

                                                // Reload the patient list.
                                                patientListVersion++

                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Patient saved",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )
                                    }

                                    // Schedule an appointment.
                                    showAddAppointment && selectedPatient != null -> {
                                        AddAppointmentScreen(
                                            patient = selectedPatient!!,

                                            onBack = {
                                                showAddAppointment = false
                                            },

                                            onSaved = {
                                                showAddAppointment = false

                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Appointment scheduled",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )
                                    }

                                    // View, reschedule, complete or cancel appointments.
                                    showAppointments && selectedPatient != null -> {
                                        AppointmentListScreen(
                                            patient = selectedPatient!!,

                                            onBack = {
                                                showAppointments = false
                                            }
                                        )
                                    }

                                    // Add a consultation to the patient's history.
                                    showAddMedicalRecord && selectedPatient != null -> {
                                        AddMedicalRecordScreen(
                                            patient = selectedPatient!!,

                                            onBack = {
                                                showAddMedicalRecord = false
                                            },

                                            onSaved = {
                                                showAddMedicalRecord = false

                                                // Reload medical history.
                                                patientProfileVersion++

                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Medical record saved",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )
                                    }

                                    // Edit an existing patient's information.
                                    showEditPatient && selectedPatient != null -> {
                                        EditPatientScreen(
                                            patient = selectedPatient!!,

                                            onBack = {
                                                showEditPatient = false
                                            },

                                            onSaved = { updatedPatient ->

                                                // Replace old patient data with saved values.
                                                selectedPatient = updatedPatient
                                                showEditPatient = false

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

                                    // Display patient profile and medical history.
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

                                                onAddMedicalRecord = {
                                                    showAddMedicalRecord = true
                                                },

                                                // Navigate to appointment creation.
                                                onScheduleAppointment = {
                                                    showAddAppointment = true
                                                },

                                                // Navigate to appointment management.
                                                onViewAppointments = {
                                                    showAppointments = true
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

                            // Preserve the existing pet-owner home screen.
                            role == "pet_owner" -> {
                                OwnerHomeScreen(
                                    onSignOut = signOut
                                )
                            }

                            // Unrecognized user role.
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
