
package com.example.vetman01.ui.patients

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vetman01.data.BillRepository
import com.example.vetman01.model.Bill
import com.example.vetman01.model.Patient
import java.math.BigDecimal

/**
 * Creates a bill for a registered veterinary patient.
 *
 * Bills start as unpaid. Payments are recorded separately
 * through the billing module.
 */
@Composable
fun AddBillScreen(
    patient: Patient,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    repository: BillRepository = remember { BillRepository() }
) {
    // Form fields.
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Save status and database error messages.
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Only allow valid positive amounts with at most two decimal places.
    val validAmount = remember(amount) {
        val formatValid = Regex("""\d+(\.\d{1,2})?""").matches(amount)

        if (formatValid) {
            val value = amount.toBigDecimalOrNull()
            value != null &&
                    value > BigDecimal.ZERO &&
                    value <= BigDecimal("1000000000")
        } else {
            false
        }
    }

    val canSave = description.isNotBlank() &&
            validAmount &&
            !saving

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TextButton(
            onClick = onBack,
            enabled = !saving
        ) {
            Text("< Back to profile")
        }

        Text(
            text = "Create Bill",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(8.dp))

        Text("Patient: ${patient.petName}")

        Spacer(Modifier.height(20.dp))

        // Describe the consultation, service or products being billed.
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Service description *") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // Monetary value in Kenyan Shillings.
        OutlinedTextField(
            value = amount,
            onValueChange = { input ->
                if (input.isEmpty() ||
                    Regex("""\d*(\.\d{0,2})?""").matches(input)
                ) {
                    amount = input
                }
            },
            label = { Text("Amount (KSh) *") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Additional notes") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Payment status: Unpaid",
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "Payments can be recorded after creating the bill.",
            style = MaterialTheme.typography.bodySmall
        )

        // Show errors without clearing the form.
        error?.let { message ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (!canSave) return@Button

                saving = true
                error = null

                val bill = Bill(
                    patientId = patient.id,
                    description = description.trim(),
                    amount = amount.toDouble(),
                    notes = notes.trim()
                )

                // Save the bill in Firestore.
                repository.addBill(bill) { result ->
                    saving = false

                    result
                        .onSuccess {
                            onSaved()
                        }
                        .onFailure {
                            error = it.message
                                ?: "Could not create bill"
                        }
                }
            },
            enabled = canSave,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (saving) "Saving..." else "Create Bill")
        }

        Spacer(Modifier.height(16.dp))
    }
}
