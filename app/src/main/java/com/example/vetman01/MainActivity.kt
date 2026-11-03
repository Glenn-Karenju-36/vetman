
package com.example.vetman01

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import com.example.vetman01.data.AuthRepository
import com.example.vetman01.model.Patient
import com.example.vetman01.ui.auth.AuthScreen
import com.example.vetman01.ui.home.VetHomeScreen
import com.example.vetman01.ui.home.OwnerDashboardScreen
import com.example.vetman01.ui.patients.AddAppointmentScreen
import com.example.vetman01.ui.patients.AddBillScreen
import com.example.vetman01.ui.patients.AddMedicalRecordScreen
import com.example.vetman01.ui.patients.AddPatientScreen
import com.example.vetman01.ui.patients.AllAppointmentsScreen
import com.example.vetman01.ui.patients.AllBillsScreen
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
 * Manages authentication, user roles and navigation.
 * Veterinarians have management screens.
 * Pet owners use the separate read-only dashboard.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            Vetman01Theme {

                val authRepository = remember {
                    AuthRepository()
                }

                var signedIn by remember {
                    mutableStateOf(
                        FirebaseAuth.getInstance().currentUser != null
                    )
                }

                var role by remember {
                    mutableStateOf<String?>(null)
                }

                var roleError by remember {
                    mutableStateOf<String?>(null)
                }

                // Veterinarian navigation state.
                var showPatientList by remember {
                    mutableStateOf(false)
                }

                var showAllAppointments by remember {
                    mutableStateOf(false)
                }

                var showAllBills by remember {
                    mutableStateOf(false)
                }

                var showAddPatient by remember {
                    mutableStateOf(false)
                }

                var showEditPatient by remember {
                    mutableStateOf(false)
                }

                var showAddMedicalRecord by remember {
                    mutableStateOf(false)
                }

                var showAddAppointment by remember {
                    mutableStateOf(false)
                }

                var showAppointments by remember {
                    mutableStateOf(false)
                }

                var showAddBill by remember {
                    mutableStateOf(false)
                }

                var showBills by remember {
                    mutableStateOf(false)
                }

                // Selected veterinarian patient.
                var selectedPatient by remember {
                    mutableStateOf<Patient?>(null)
                }

                // Patient selected from the All Bills screen.
                var billingPatient by remember {
                    mutableStateOf<Patient?>(null)
                }

                var patientListVersion by remember {
                    mutableIntStateOf(0)
                }

                var patientProfileVersion by remember {
                    mutableIntStateOf(0)
                }

                /**
                 * Handle Android Back in priority order:
                 * forms, patient subpages, profiles, global lists,
                 * and finally the patient list.
                 *
                 * The owner dashboard manages its own back navigation.
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
                                    showAllAppointments ||
                                    showAllBills ||
                                    selectedPatient != null ||
                                    billingPatient != null ||
                                    showPatientList
                            )
                ) {
                    when {
                        showAddBill -> {
                            showAddBill = false
                        }

                        showBills -> {
                            showBills = false
                            billingPatient = null
                        }

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

                        billingPatient != null -> {
                            billingPatient = null
                        }

                        showAllBills -> {
                            showAllBills = false
                        }

                        showAllAppointments -> {
                            showAllAppointments = false
                        }

                        showPatientList -> {
                            showPatientList = false
                        }
                    }
                }

                // Retrieve the user's role after login.
                LaunchedEffect(signedIn) {
                    if (signedIn) {
                        role = null
                        roleError = null

                        authRepository.getUserRole { result ->
                            result
                                .onSuccess {
                                    role = it
                                }
                                .onFailure {
                                    roleError = it.message
                                        ?: "Could not load your role"
                                }
                        }
                    } else {
                        role = null
                        roleError = null
                    }
                }

                /**
                 * Sign out and clear navigation so the next
                 * account starts in the correct dashboard.
                 */
                val signOut = {
                    authRepository.signOut()

                    signedIn = false
                    role = null
                    roleError = null

                    showPatientList = false
                    showAllAppointments = false
                    showAllBills = false

                    showAddPatient = false
                    showEditPatient = false
                    showAddMedicalRecord = false

                    showAddAppointment = false
                    showAppointments = false

                    showAddBill = false
                    showBills = false

                    selectedPatient = null
                    billingPatient = null

                    patientListVersion = 0
                    patientProfileVersion = 0
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {

                        when {

                            // LOGIN AND REGISTRATION
                            !signedIn -> {
                                AuthScreen(
                                    onAuthSuccess = {
                                        showPatientList = false
                                        selectedPatient = null
                                        billingPatient = null
                                        signedIn = true
                                    }
                                )
                            }

                            // AUTHENTICATION / ROLE ERROR
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

                                    Button(
                                        onClick = signOut
                                    ) {
                                        Text("Sign Out")
                                    }
                                }
                            }

                            // VETERINARIAN MODULE
                            role == "veterinarian" -> {

                                when {

                                    // ADD PATIENT
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

                                    // CREATE BILL
                                    showAddBill &&
                                            selectedPatient != null -> {

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

                                    // PATIENT BILLING AND PAYMENTS
                                    showBills &&
                                            (selectedPatient != null ||
                                                    billingPatient != null) -> {

                                        BillListScreen(
                                            patient = selectedPatient
                                                ?: billingPatient!!,
                                            onBack = {
                                                showBills = false
                                                billingPatient = null
                                            }
                                        )
                                    }

                                    // ADD APPOINTMENT
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

                                    // PATIENT APPOINTMENTS
                                    showAppointments &&
                                            selectedPatient != null -> {

                                        AppointmentListScreen(
                                            patient = selectedPatient!!,
                                            onBack = {
                                                showAppointments = false
                                            }
                                        )
                                    }

                                    // ADD MEDICAL RECORD
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

                                    // EDIT PATIENT
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

                                    // PATIENT PROFILE
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

                                    // ALL BILLS
                                    showAllBills -> {
                                        AllBillsScreen(
                                            onBack = {
                                                showAllBills = false
                                            },
                                            onManagePatientBills = { patient ->
                                                billingPatient = patient
                                                showBills = true
                                            }
                                        )
                                    }

                                    // ALL APPOINTMENTS
                                    showAllAppointments -> {
                                        AllAppointmentsScreen(
                                            onBack = {
                                                showAllAppointments = false
                                            },
                                            onPatientClick = { patient ->
                                                showAllAppointments = false
                                                selectedPatient = patient
                                            }
                                        )
                                    }

                                    // MY PATIENTS
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

                                    // VETERINARIAN DASHBOARD
                                    else -> {
                                        VetHomeScreen(
                                            onAddPatient = {
                                                showAddPatient = true
                                            },
                                            onViewPatients = {
                                                showPatientList = true
                                            },
                                            onViewAppointments = {
                                                showAllAppointments = true
                                            },
                                            onViewBills = {
                                                showAllBills = true
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

                            /**
                             * PET-OWNER DASHBOARD
                             *
                             * This is the updated destination.
                             * Replaces OwnerHomeScreen.
                             *
                             * OwnerDashboardScreen handles:
                             * - Linked pets
                             * - Search
                             * - Pet cards
                             * - Read-only profile navigation
                             */
                            role == "pet_owner" -> {
                                OwnerDashboardScreen(
                                    onSignOut = signOut
                                )
                            }

                            // UNRECOGNIZED ACCOUNT ROLE
                            role != null -> {
                                Text(
                                    text = "Unknown role: $role"
                                )
                            }

                            // LOADING USER ROLE
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
