package com.example.vetman01.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.vetman01.data.AuthRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    repository: AuthRepository = remember { AuthRepository() }
) {
    // State: values the screen remembers and redraws when they change
    var isLogin by remember { mutableStateOf(true) }          // true = sign in, false = register
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("veterinarian") }   // or "pet_owner"
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Runs when Firebase replies: stop loading, then continue or show the error
    val onResult: (Result<Unit>) -> Unit = { result ->
        loading = false
        result
            .onSuccess { onAuthSuccess() }
            .onFailure { error = it.message ?: "Something went wrong" }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Vetman", style = MaterialTheme.typography.headlineLarge)
        Text(
            if (isLogin) "Sign in to continue" else "Create your account",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        // Role choice is only needed when creating an account
        if (!isLogin) {
            Spacer(Modifier.height(16.dp))
            Text("I am a:", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = role == "veterinarian",
                    onClick = { role = "veterinarian" },
                    label = { Text("Veterinarian") }
                )
                FilterChip(
                    selected = role == "pet_owner",
                    onClick = { role = "pet_owner" },
                    label = { Text("Pet owner") }
                )
            }
        }

        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                error = null
                loading = true
                if (isLogin) repository.signIn(email.trim(), password, onResult)
                else repository.register(email.trim(), password, role, onResult)
            },
            enabled = !loading && email.isNotBlank() && password.length >= 6,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (loading) "Please wait..." else if (isLogin) "Sign in" else "Create account")
        }

        // Switch between sign in and register
        TextButton(onClick = { isLogin = !isLogin; error = null }) {
            Text(if (isLogin) "New here? Create an account" else "Already have an account? Sign in")
        }
    }
}