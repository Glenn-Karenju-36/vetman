
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.vetman01.data.AuthRepository
import com.example.vetman01.ui.auth.AuthScreen
import com.example.vetman01.ui.home.OwnerHomeScreen
import com.example.vetman01.ui.patients.AddPatientScreen
import com.example.vetman01.ui.patients.PatientListScreen
import com.example.vetman01.ui.theme.Vetman01Theme
import com.google.firebase.auth.FirebaseAuth

/**
 * MainActivity
 *
 * Entry point of Vetman.
 * Handles authentication, user roles, and screen navigation.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Allow the app to draw behind system bars.
        enableEdgeToEdge()

        setContent {
            Vetman01Theme {

                val repository = remember { AuthRepository() }

                // Check whether Firebase remembers a signed-in user.
                var signedIn by remember {
                    mutableStateOf(
                        FirebaseAuth.getInstance().currentUser != null
                    )
                }

                // Store the user's role retrieved from Firestore.
                var role by remember { mutableStateOf<String?>(null) }
                var roleError by remember { mutableStateOf<String?>(null) }

                // Controls navigation between patient list and add form.
                var showAddPatient by remember {
                    mutableStateOf(false)
                }

                // Incrementing this forces the patient list to reload
                // after a new patient has been saved.
                var patientListVersion by remember {
                    mutableStateOf(0)
                }

                // Android Back button returns to the patient list.
                BackHandler(enabled = showAddPatient) {
                    showAddPatient = false
                }

                /**
                 * Retrieve the user's role when authentication changes.
                 * Veterinarians and pet owners have different screens.
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

                // Sign out and reset the current navigation state.
                val signOut = {
                    repository.signOut()
                    signedIn = false
                    role = null
                    roleError = null
                    showAddPatient = false
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

                        // Decide which screen to show based on login and role.
                        when {

                            // User is not authenticated.
                            !signedIn -> {
                                AuthScreen(
                                    onAuthSuccess = {
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

                                if (showAddPatient) {

                                    // Form for registering new patients.
                                    AddPatientScreen(
                                        onBack = {
                                            showAddPatient = false
                                        },

                                        onSaved = {

                                            // Return to the list and refresh it.
                                            showAddPatient = false
                                            patientListVersion++

                                            Toast.makeText(
                                                this@MainActivity,
                                                "Patient saved",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )

                                } else {

                                    // Display patients retrieved from Firestore.
                                    // The key changes after saving, reloading the list.
                                    androidx.compose.runtime.key(
                                        patientListVersion
                                    ) {
                                        PatientListScreen(
                                            onAddPatient = {
                                                showAddPatient = true
                                            },

                                            // Patient profile screen comes later.
                                            onPatientClick = { patient ->
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Selected: ${patient.petName}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )
                                    }
                                }
                            }

                            // Pet owners retain their existing home screen.
                            role == "pet_owner" -> {
                                OwnerHomeScreen(
                                    onSignOut = signOut
                                )
                            }

                            // Handle unknown user roles.
                            role != null -> {
                                Text("Unknown role: $role")
                            }

                            // Wait for Firestore to return the user role.
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
