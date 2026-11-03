
package com.example.vetman01.ui.auth

import android.util.Patterns

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

import com.example.vetman01.data.AuthRepository

/**
 * Vetman authentication screen.
 *
 * Handles both login and registration using the existing
 * Firebase AuthRepository.
 *
 * The UI follows Vetman's Minimal Clinical design.
 * Account permissions and roles remain managed by Firebase.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    repository: AuthRepository = remember {
        AuthRepository()
    }
) {
    val colors = MaterialTheme.colorScheme

    // true = sign in, false = register a new account.
    var isLogin by remember {
        mutableStateOf(true)
    }

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    // Registration requires matching passwords.
    var confirmPassword by remember {
        mutableStateOf("")
    }

    // Role is saved by AuthRepository during registration.
    var role by remember {
        mutableStateOf("veterinarian")
    }

    var showPassword by remember {
        mutableStateOf(false)
    }

    var showConfirmPassword by remember {
        mutableStateOf(false)
    }

    var loading by remember {
        mutableStateOf(false)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    // Validate locally before contacting Firebase.
    val trimmedEmail = email.trim()

    val validEmail = Patterns.EMAIL_ADDRESS
        .matcher(trimmedEmail)
        .matches()

    val validPassword = password.length >= 6

    val passwordsMatch =
        confirmPassword == password

    val canSubmit =
        validEmail &&
                validPassword &&
                (isLogin || (
                        confirmPassword.isNotBlank() &&
                                passwordsMatch
                        )) &&
                !loading

    /**
     * Firebase authentication runs asynchronously.
     *
     * Keep the loading indicator visible until Firebase
     * returns a result, then navigate or display an error.
     */
    val onResult: (Result<Unit>) -> Unit = { result ->
        loading = false

        result
            .onSuccess {
                onAuthSuccess()
            }
            .onFailure { exception ->
                error = exception.localizedMessage
                    ?: "Authentication failed. Please try again."
            }
    }

    /**
     * Switching modes clears passwords and errors.
     *
     * Keeping the email makes it easier to move between
     * registration and sign-in without retyping it.
     */
    fun switchMode() {
        isLogin = !isLogin
        password = ""
        confirmPassword = ""
        showPassword = false
        showConfirmPassword = false
        error = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // BRANDING
        Surface(
            modifier = Modifier.size(76.dp),
            shape = RoundedCornerShape(22.dp),
            color = colors.primaryContainer
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                AuthPawIcon(
                    modifier = Modifier.size(48.dp),
                    color = colors.primary
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Vetman",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = colors.primary
        )

        Spacer(Modifier.height(5.dp))

        Text(
            text = "Mobile Veterinary Care",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant
        )

        Spacer(Modifier.height(28.dp))

        // AUTHENTICATION FORM
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = colors.surface
            ),
            border = BorderStroke(
                1.dp,
                colors.outlineVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Text(
                    text = if (isLogin) {
                        "Welcome Back"
                    } else {
                        "Create Account"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = if (isLogin) {
                        "Sign in to access your dashboard."
                    } else {
                        "Register to get started with Vetman."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )

                Spacer(Modifier.height(24.dp))

                // EMAIL
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        error = null
                    },
                    label = {
                        Text("Email Address")
                    },
                    placeholder = {
                        Text("you@example.com")
                    },
                    singleLine = true,
                    enabled = !loading,
                    isError = email.isNotBlank() && !validEmail,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (email.isNotBlank() && !validEmail) {
                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Enter a valid email address.",
                        color = colors.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(14.dp))

                // PASSWORD
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        error = null
                    },
                    label = {
                        Text("Password")
                    },
                    singleLine = true,
                    enabled = !loading,
                    visualTransformation = if (showPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        TextButton(
                            onClick = {
                                showPassword = !showPassword
                            },
                            enabled = !loading
                        ) {
                            Text(
                                text = if (showPassword) {
                                    "Hide"
                                } else {
                                    "Show"
                                }
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (isLogin) {
                            ImeAction.Done
                        } else {
                            ImeAction.Next
                        }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (!isLogin) {
                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "Use at least 6 characters.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }

                // REGISTRATION-ONLY FIELDS
                if (!isLogin) {

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            error = null
                        },
                        label = {
                            Text("Confirm Password")
                        },
                        singleLine = true,
                        enabled = !loading,
                        isError = confirmPassword.isNotEmpty() &&
                                !passwordsMatch,
                        visualTransformation = if (
                            showConfirmPassword
                        ) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            TextButton(
                                onClick = {
                                    showConfirmPassword =
                                        !showConfirmPassword
                                },
                                enabled = !loading
                            ) {
                                Text(
                                    text = if (
                                        showConfirmPassword
                                    ) {
                                        "Hide"
                                    } else {
                                        "Show"
                                    }
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (
                        confirmPassword.isNotEmpty() &&
                        !passwordsMatch
                    ) {
                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = "Passwords do not match.",
                            color = colors.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // ROLE SELECTION
                    Text(
                        text = "Account Type",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(5.dp))

                    Text(
                        text = "Choose how you'll use Vetman.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = role == "veterinarian",
                            onClick = {
                                role = "veterinarian"
                            },
                            enabled = !loading,
                            label = {
                                Text("Veterinarian")
                            }
                        )

                        FilterChip(
                            selected = role == "pet_owner",
                            onClick = {
                                role = "pet_owner"
                            },
                            enabled = !loading,
                            label = {
                                Text("Pet Owner")
                            }
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = colors.primaryContainer
                    ) {
                        Text(
                            text = if (role == "veterinarian") {
                                "Manage patients, appointments, medical records and billing."
                            } else {
                                "View your linked pets and their veterinary records."
                            },
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onPrimaryContainer
                        )
                    }
                }

                // FIREBASE ERROR MESSAGE
                error?.let { message ->
                    Spacer(Modifier.height(16.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = colors.errorContainer
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(12.dp),
                            color = colors.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                /**
                 * Sign in and registration use the original
                 * AuthRepository methods.
                 *
                 * No Firestore schema or role identifiers change.
                 */
                Button(
                    onClick = {
                        if (canSubmit) {
                            loading = true
                            error = null

                            if (isLogin) {
                                repository.signIn(
                                    trimmedEmail,
                                    password,
                                    onResult
                                )
                            } else {
                                repository.register(
                                    trimmedEmail,
                                    password,
                                    role,
                                    onResult
                                )
                            }
                        }
                    },
                    enabled = canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = colors.onPrimary,
                            strokeWidth = 2.dp
                        )

                        Spacer(Modifier.width(10.dp))

                        Text("Please wait...")
                    } else {
                        Text(
                            text = if (isLogin) {
                                "Sign In"
                            } else {
                                "Create Account"
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                TextButton(
                    onClick = {
                        switchMode()
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isLogin) {
                            "New to Vetman? Create an account"
                        } else {
                            "Already have an account? Sign in"
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Vetman • Veterinary Practice Management",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))
    }
}

/**
 * Custom Vetman paw logo.
 *
 * Matches the paw design used in the veterinarian
 * dashboard without adding a new icon dependency.
 *
 * Once UI cleanup is complete, we can move the shared
 * icon into a reusable UI component.
 */
@Composable
private fun AuthPawIcon(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(modifier = modifier) {
        drawAuthPaw(color)
    }
}

private fun DrawScope.drawAuthPaw(
    color: Color
) {
    val w = size.width
    val h = size.height

    // Four individual toe pads.
    drawCircle(
        color = color,
        radius = w * 0.105f,
        center = Offset(w * 0.20f, h * 0.30f)
    )

    drawCircle(
        color = color,
        radius = w * 0.115f,
        center = Offset(w * 0.40f, h * 0.17f)
    )

    drawCircle(
        color = color,
        radius = w * 0.115f,
        center = Offset(w * 0.63f, h * 0.17f)
    )

    drawCircle(
        color = color,
        radius = w * 0.105f,
        center = Offset(w * 0.83f, h * 0.30f)
    )

    // Main central paw pad.
    drawOval(
        color = color,
        topLeft = Offset(
            w * 0.23f,
            h * 0.43f
        ),
        size = Size(
            w * 0.55f,
            h * 0.42f
        )
    )
}
