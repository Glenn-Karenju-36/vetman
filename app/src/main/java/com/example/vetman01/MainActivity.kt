
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
import com.example.vetman01.ui.patients.PatientListScreen
import com.example.vetman01.ui.patients.PatientProfileScreen
import com.example.vetman01.ui.theme.Vetman01Theme
import com.google.firebase.auth.FirebaseAuth

/**
 * MainActivity
 *
 * Entry point of Vetman.
 * Handles authentication, user roles and screen navigation.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Allow content to draw behind system bars.
        enableEdgeToEdge()

        setContent {
            Vetman01Theme {

                val repository = remember { AuthRepository() }

                // Restore the existing Firebase login session.
                var signedIn by remember {
                    mutableStateOf(
                        FirebaseAuth.getInstance().currentUser != null
                    )
                }

                // User role determines which interface is displayed.
                var role by remember {
                    mutableStateOf<String?>(null)
                }

                var roleError by remember {
                    mutableStateOf<String?>(null)
                }

                // Controls navigation to the Add Patient form.
                var showAddPatient by remember {
                    mutableStateOf(false)
                }

                // Stores the patient selected from the patient list.
                // Null means no patient profile is currently open.
                var selectedPatient by remember {
                    mutableStateOf<Patient?>(null)
                }

                // Changes after saving to reload the patient list.
                var patientListVersion by remember {
                    mutableStateOf(0)
                }

                // Android Back button returns to the patient list.
                BackHandler(
                    enabled = showAddPatient || selectedPatient != null
                ) {
                    when {
                        showAddPatient -> showAddPatient = false
                        selectedPatient != null -> selectedPatient = null
                    }
                }

                /**
                 * Retrieve the user's role whenever login status changes.
                 * Firestore stores whether the account is a veterinarian
                 * or a pet owner.
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

                // Sign out and clear the current navigation state.
                val signOut = {
                    repository.signOut()
                    signedIn = false
                    role = null
                    roleError = null
                    showAddPatient = false
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

                        // Display the appropriate screen for the user.
                        when {

                            // Login and registration screen.
                            !signedIn -> {
                                AuthScreen(
                                    onAuthSuccess = {
                                        signedIn = true
                                    }
                                )
                            }

                            // Display authentication or role errors.
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
                                                // Return to the list and reload records.
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

                                    // Open the selected patient's profile.
                                    selectedPatient != null -> {

                                        PatientProfileScreen(
                                            patient = selectedPatient!!,

                                            onBack = {
                                                selectedPatient = null
                                            }
                                        )
                                    }

                                    // Display patients belonging to this vet.
                                    else -> {

                                        key(patientListVersion) {
                                            PatientListScreen(
                                                onAddPatient = {
                                                    showAddPatient = true
                                                },

                                                // Open the selected patient's profile.
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

                            // Handle an unrecognized role.
                            role != null -> {
                                Text("Unknown role: $role")
                            }

                            // Show a loading indicator while retrieving the role.
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
