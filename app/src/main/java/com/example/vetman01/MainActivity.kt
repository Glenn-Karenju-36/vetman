
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
import com.example.vetman01.ui.home.OwnerHomeScreen
import com.example.vetman01.ui.home.VetHomeScreen
import com.example.vetman01.ui.patients.*
import com.example.vetman01.ui.theme.Vetman01Theme
import com.google.firebase.auth.FirebaseAuth

/**
 * Main application entry point.
 *
 * Manages authentication, user roles and navigation
 * between veterinarian and pet-owner screens.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Vetman01Theme {
                val repository = remember { AuthRepository() }

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

                // Main dashboard destinations.
                var showPatientList by remember {
                    mutableStateOf(false)
                }

                var showAllAppointments by remember {
                    mutableStateOf(false)
                }

                var showAllBills by remember {
                    mutableStateOf(false)
                }

                // Patient forms and related screens.
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

                // Used by patient profiles and patient-specific actions.
                var selectedPatient by remember {
                    mutableStateOf<Patient?>(null)
                }

                // Separate patient selection for the All Bills workflow.
                // This keeps dashboard billing navigation independent
                // from the patient-profile navigation.
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
                 * Android Back closes the most deeply nested screen.
                 *
                 * In particular:
                 * All Bills -> patient bills -> All Bills -> dashboard.
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
                                    showPatientList ||
                                    showAllAppointments ||
                                    showAllBills
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

                        showAllBills -> {
                            showAllBills = false
                            billingPatient = null
                        }

                        showAllAppointments -> {
                            showAllAppointments = false
                        }

                        showPatientList -> {
                            showPatientList = false
                        }
                    }
                }

                // Retrieve the current account's role from Firestore.
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
                 * Sign out and reset all navigation state.
                 */
                val signOut = {
                    repository.signOut()

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
                            .padding(innerPadding)
                            .fillMaxSize()
                    ) {
                        when {
                            // LOGIN / REGISTRATION
                            !signedIn -> {
                                AuthScreen(
                                    onAuthSuccess = {
                                        showPatientList = false
                                        showAllAppointments = false
                                        showAllBills = false

                                        selectedPatient = null
                                        billingPatient = null

                                        signedIn = true
                                    }
                                )
                            }

                            // AUTHENTICATION ERROR
                            roleError != null -> {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement =
                                        Arrangement.Center,
                                    horizontalAlignment =
                                        Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = roleError ?: "",
                                        color =
                                            MaterialTheme.colorScheme.error
                                    )

                                    Button(onClick = signOut) {
                                        Text("Sign out")
                                    }
                                }
                            }

                            // VETERINARIAN
                            role == "veterinarian" -> {
                                when {

                                    // REGISTER PATIENT
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

                                    /**
                                     * PATIENT BILLING / PAYMENTS
                                     *
                                     * Opens from either:
                                     * - An individual patient profile.
                                     * - The All Bills dashboard.
                                     */
                                    showBills &&
                                            (
                                                    billingPatient != null ||
                                                            selectedPatient != null
                                                    ) -> {

                                        BillListScreen(
                                            patient = billingPatient
                                                ?: selectedPatient!!,
                                            onBack = {
                                                showBills = false
                                                billingPatient = null
                                            }
                                        )
                                    }

                                    // SCHEDULE APPOINTMENT
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

                                    // PATIENT-SPECIFIC APPOINTMENTS
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
                                                selectedPatient =
                                                    updatedPatient

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
                                     * PATIENT PROFILE
                                     *
                                     * If opened through All Appointments,
                                     * Back returns to All Appointments.
                                     *
                                     * If opened through My Patients,
                                     * Back returns to My Patients.
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
                                     * ALL BILLS
                                     *
                                     * This is the dashboard-wide billing
                                     * screen, independent of My Patients.
                                     */
                                    showAllBills -> {
                                        AllBillsScreen(
                                            onBack = {
                                                showAllBills = false
                                                billingPatient = null
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

                                    /**
                                     * MAIN DASHBOARD
                                     *
                                     * Each main action now has its own
                                     * destination.
                                     */
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
                                                selectedPatient = patient
                                            },

                                            onSignOut = signOut
                                        )
                                    }
                                }
                            }

                            // PET OWNER
                            role == "pet_owner" -> {
                                OwnerHomeScreen(
                                    onSignOut = signOut
                                )
                            }

                            // UNKNOWN ROLE
                            role != null -> {
                                Text("Unknown role: $role")
                            }

                            // WAIT FOR ROLE LOOKUP
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
