
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
import com.example.vetman01.ui.home.VetHomeScreen
import com.example.vetman01.ui.home.OwnerHomeScreen
import com.example.vetman01.ui.patients.AddAppointmentScreen
import com.example.vetman01.ui.patients.AddBillScreen
import com.example.vetman01.ui.patients.AddMedicalRecordScreen
import com.example.vetman01.ui.patients.AddPatientScreen
import com.example.vetman01.ui.patients.AppointmentListScreen
import com.example.vetman01.ui.patients.BillListScreen
import com.example.vetman01.ui.patients.EditPatientScreen
import com.example.vetman01.ui.patients.PatientListScreen
import com.example.vetman01.ui.patients.PatientProfileScreen
import com.example.vetman01.ui.theme.Vetman01Theme
import com.google.firebase.auth.FirebaseAuth

/**
 * Main entry point for Vetman.
 *
 * Handles authentication, user roles, and navigation between
 * the veterinarian dashboard, patients, medical records,
 * appointments, billing and pet-owner screens.
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

                // User role and authentication errors.
                var role by remember {
                    mutableStateOf<String?>(null)
                }

                var roleError by remember {
                    mutableStateOf<String?>(null)
                }

                /**
                 * Dashboard navigation.
                 *
                 * Veterinarians now start on VetHomeScreen.
                 * The patient list opens only when selected.
                 */
                var showPatientList by remember {
                    mutableStateOf(false)
                }

                // Patient-related navigation.
                var showAddPatient by remember {
                    mutableStateOf(false)
                }

                var showEditPatient by remember {
                    mutableStateOf(false)
                }

                var showAddMedicalRecord by remember {
                    mutableStateOf(false)
                }

                // Appointment-related navigation.
                var showAddAppointment by remember {
                    mutableStateOf(false)
                }

                var showAppointments by remember {
                    mutableStateOf(false)
                }

                // Billing-related navigation.
                var showAddBill by remember {
                    mutableStateOf(false)
                }

                var showBills by remember {
                    mutableStateOf(false)
                }

                // Currently selected patient.
                var selectedPatient by remember {
                    mutableStateOf<Patient?>(null)
                }

                // Reload Firestore data after changes.
                var patientListVersion by remember {
                    mutableStateOf(0)
                }

                var patientProfileVersion by remember {
                    mutableStateOf(0)
                }

                /**
                 * Handle Android's Back button.
                 *
                 * Close forms first, then patient profiles,
                 * then return from the patient list to dashboard.
                 */
                BackHandler(
                    enabled = role == "veterinarian" && (
                            showAddPatient ||
                                    showEditPatient ||
                                    showAddMedicalRecord ||
                                    showAddAppointment ||
                                    showAppointments ||
                                    showAddBill ||
                                    showBills ||
                                    selectedPatient != null ||
                                    showPatientList
                            )
                ) {
                    when {
                        showAddBill -> showAddBill = false

                        showBills -> showBills = false

                        showAddAppointment -> {
                            showAddAppointment = false
                        }

                        showAppointments -> {
                            showAppointments = false
                        }

                        showAddMedicalRecord -> {
                            showAddMedicalRecord = false
                        }

                        showEditPatient -> {
                            showEditPatient = false
                        }

                        showAddPatient -> {
                            showAddPatient = false
                        }

                        selectedPatient != null -> {
                            selectedPatient = null
                        }

                        showPatientList -> {
                            showPatientList = false
                        }
                    }
                }

                /**
                 * Retrieve the user's role after authentication.
                 * Determines veterinarian or pet-owner access.
                 */
                LaunchedEffect(signedIn) {
                    if (signedIn) {
                        role = null
                        roleError = null

                        repository.getUserRole { result ->
                            result
                                .onSuccess {
                                    role = it
                                }
                                .onFailure {
                                    roleError = it.message
                                        ?: "Could not load your role"
                                }
                        }
                    }
                }

                /**
                 * Sign out and reset all navigation states.
                 */
                val signOut = {
                    repository.signOut()

                    signedIn = false
                    role = null
                    roleError = null

                    showPatientList = false
                    showAddPatient = false
                    showEditPatient = false
                    showAddMedicalRecord = false

                    showAddAppointment = false
                    showAppointments = false

                    showAddBill = false
                    showBills = false

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
                                        // New veterinarian sessions
                                        // begin on the dashboard.
                                        showPatientList = false
                                        selectedPatient = null
                                        signedIn = true
                                    }
                                )
                            }

                            // Display role retrieval errors.
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
                                                patientListVersion++

                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Patient saved",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )
                                    }

                                    // Create a bill for the selected patient.
                                    showAddBill && selectedPatient != null -> {
                                        AddBillScreen(
                                            patient = selectedPatient!!,

                                            onBack = {
                                                showAddBill = false
                                            },

                                            onSaved = {
                                                showAddBill = false

                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Bill created",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )
                                    }

                                    // Display bills and payment history.
                                    showBills && selectedPatient != null -> {
                                        BillListScreen(
                                            patient = selectedPatient!!,

                                            onBack = {
                                                showBills = false
                                            }
                                        )
                                    }

                                    // Schedule an appointment.
                                    showAddAppointment &&
                                            selectedPatient != null -> {

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

                                    // View and manage appointments.
                                    showAppointments &&
                                            selectedPatient != null -> {

                                        AppointmentListScreen(
                                            patient = selectedPatient!!,

                                            onBack = {
                                                showAppointments = false
                                            }
                                        )
                                    }

                                    // Add a medical consultation.
                                    showAddMedicalRecord &&
                                            selectedPatient != null -> {

                                        AddMedicalRecordScreen(
                                            patient = selectedPatient!!,

                                            onBack = {
                                                showAddMedicalRecord = false
                                            },

                                            onSaved = {
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
                                    showEditPatient &&
                                            selectedPatient != null -> {

                                        EditPatientScreen(
                                            patient = selectedPatient!!,

                                            onBack = {
                                                showEditPatient = false
                                            },

                                            onSaved = { updatedPatient ->
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

                                    /**
                                     * Patient profile.
                                     *
                                     * If opened from the dashboard,
                                     * Back returns to the dashboard.
                                     *
                                     * If opened from My Patients,
                                     * Back returns to the patient list.
                                     */
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

                                                onScheduleAppointment = {
                                                    showAddAppointment = true
                                                },

                                                onViewAppointments = {
                                                    showAppointments = true
                                                },

                                                onCreateBill = {
                                                    showAddBill = true
                                                },

                                                onViewBills = {
                                                    showBills = true
                                                }
                                            )
                                        }
                                    }

                                    /**
                                     * My Patients screen.
                                     *
                                     * Reuses the existing patient list,
                                     * registration and profile navigation.
                                     */
                                    showPatientList -> {
                                        key(patientListVersion) {
                                            PatientListScreen(
                                                onAddPatient = {
                                                    showAddPatient = true
                                                },

                                                onPatientClick = { patient ->
                                                    selectedPatient = patient
                                                },

                                                onSignOut = signOut,

                                                onBack = {
                                                    showPatientList = false
                                                }
                                            )
                                        }
                                    }

                                    /**
                                     * Default veterinarian destination.
                                     *
                                     * The Minimal Clinical dashboard loads
                                     * real data from PatientRepository and
                                     * AppointmentRepository.
                                     */
                                    else -> {
                                        VetHomeScreen(
                                            onAddPatient = {
                                                showAddPatient = true
                                            },
                                            onViewPatients = {
                                                showPatientList = true
                                            },
                                            onPatientClick = { patient ->
                                                showPatientList = false
                                                selectedPatient = patient
                                            },
                                            onSignOut = signOut
                                        )
                                    }
                                }
                            }

                            // Preserve the existing pet-owner module.
                            role == "pet_owner" -> {
                                OwnerHomeScreen(
                                    onSignOut = signOut
                                )
                            }

                            // Handle unknown user roles.
                            role != null -> {
                                Text("Unknown role: $role")
                            }

                            // Wait for Firestore to retrieve the role.
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
