
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
import com.example.vetman01.model.Payment
import com.example.vetman01.model.Patient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Displays a patient's bills and payment history.
 *
 * Supports manual recording of M-Pesa, Cash and Airtel Money
 * payments without processing transactions through an API.
 */
@Composable
fun BillListScreen(
    patient: Patient,
    onBack: () -> Unit,
    repository: BillRepository = remember { BillRepository() }
) {
    var bills by remember { mutableStateOf<List<Bill>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshVersion by remember { mutableIntStateOf(0) }

    // Controls the payment entry dialog.
    var selectedBill by remember { mutableStateOf<Bill?>(null) }
    var paymentAmount by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("M-Pesa") }
    var reference by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    // Stores the payment history of the selected bill.
    var historyBillId by remember { mutableStateOf<String?>(null) }
    var payments by remember { mutableStateOf<List<Payment>>(emptyList()) }
    var historyLoading by remember { mutableStateOf(false) }
    var historyError by remember { mutableStateOf<String?>(null) }

    val dateFormat = remember {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    }

    /**
     * Retrieve the veterinarian's bills and show only
     * those belonging to the selected patient.
     */
    LaunchedEffect(patient.id, refreshVersion) {
        loading = true
        error = null

        repository.getBills { result ->
            loading = false

            result
                .onSuccess { allBills ->
                    bills = allBills.filter {
                        it.patientId == patient.id
                    }
                }
                .onFailure {
                    error = it.message ?: "Could not load bills"
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("< Back to profile")
        }

        Text(
            "Billing History",
            style = MaterialTheme.typography.headlineMedium
        )

        Text("Patient: ${patient.petName}")

        Spacer(Modifier.height(20.dp))

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
        }

        when {
            loading -> CircularProgressIndicator()

            bills.isEmpty() && error == null -> {
                Text("No bills recorded for this patient.")
            }

            else -> bills.forEach { bill ->

                // Calculate the remaining balance in cents.
                val totalCents = (bill.amount * 100).roundToLong()
                val paidCents = (bill.amountPaid * 100).roundToLong()
                val balanceCents = (totalCents - paidCents).coerceAtLeast(0)

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {

                        Text(
                            bill.description,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(Modifier.height(8.dp))

                        Text("Total: KSh ${money(bill.amount)}")
                        Text("Paid: KSh ${money(bill.amountPaid)}")
                        Text("Balance: KSh ${money(balanceCents / 100.0)}")
                        Text("Status: ${bill.status.replaceFirstChar { it.uppercase() }}")

                        Text(
                            dateFormat.format(Date(bill.createdAt)),
                            style = MaterialTheme.typography.bodySmall
                        )

                        if (bill.notes.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text("Notes: ${bill.notes}")
                        }

                        Spacer(Modifier.height(12.dp))

                        // Fully paid bills no longer accept payments.
                        if (balanceCents > 0) {
                            Button(
                                onClick = {
                                    selectedBill = bill
                                    paymentAmount = ""
                                    paymentMethod = "M-Pesa"
                                    reference = ""
                                    error = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Record Payment")
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                if (historyBillId == bill.id) {
                                    historyBillId = null
                                } else {
                                    historyBillId = bill.id
                                    payments = emptyList()
                                    historyLoading = true
                                    historyError = null

                                    // Retrieve individual payments for this bill.
                                    repository.getPayments(bill.id) { result ->
                                        historyLoading = false

                                        result
                                            .onSuccess { payments = it }
                                            .onFailure {
                                                historyError = it.message
                                                    ?: "Could not load payments"
                                            }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (historyBillId == bill.id)
                                    "Hide Payment History"
                                else
                                    "View Payment History"
                            )
                        }

                        // Display individual payments when expanded.
                        if (historyBillId == bill.id) {
                            Spacer(Modifier.height(12.dp))

                            when {
                                historyLoading -> CircularProgressIndicator()

                                historyError != null -> Text(
                                    historyError ?: "",
                                    color = MaterialTheme.colorScheme.error
                                )

                                payments.isEmpty() -> {
                                    Text("No payments recorded.")
                                }

                                else -> payments.forEach { payment ->
                                    HorizontalDivider()
                                    Spacer(Modifier.height(8.dp))

                                    Text("KSh ${money(payment.amount)}")
                                    Text("Method: ${payment.method}")

                                    if (payment.reference.isNotBlank()) {
                                        Text("Reference: ${payment.reference}")
                                    }

                                    Text(
                                        dateFormat.format(Date(payment.createdAt)),
                                        style = MaterialTheme.typography.bodySmall
                                    )

                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }

    // Dialog for recording payments against the selected bill.
    selectedBill?.let { bill ->

        val balanceCents = (
                (bill.amount * 100).roundToLong() -
                        (bill.amountPaid * 100).roundToLong()
                ).coerceAtLeast(0)

        val amountCents = paymentAmount.toDoubleOrNull()
            ?.takeIf { it.isFinite() }
            ?.let { (it * 100).roundToLong() }

        val validPayment = amountCents != null &&
                amountCents > 0 &&
                amountCents <= balanceCents

        AlertDialog(
            onDismissRequest = {
                if (!saving) selectedBill = null
            },
            title = { Text("Record Payment") },
            text = {
                Column {
                    Text("Balance: KSh ${money(balanceCents / 100.0)}")

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = paymentAmount,
                        onValueChange = { input ->
                            if (input.isEmpty() ||
                                Regex("""\d*(\.\d{0,2})?""").matches(input)
                            ) {
                                paymentAmount = input
                            }
                        },
                        label = { Text("Amount received (KSh)") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        singleLine = true
                    )

                    Spacer(Modifier.height(12.dp))

                    Text("Payment Method")

                    // Supported payment methods in Kenya.
                    listOf("M-Pesa", "Cash", "Airtel Money").forEach { method ->
                        Row {
                            RadioButton(
                                selected = paymentMethod == method,
                                onClick = { paymentMethod = method },
                                enabled = !saving
                            )

                            Text(
                                text = method,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    }

                    // Transaction references apply to mobile money.
                    if (paymentMethod != "Cash") {
                        OutlinedTextField(
                            value = reference,
                            onValueChange = { reference = it },
                            label = { Text("Transaction reference (optional)") },
                            singleLine = true
                        )
                    }
                }
            },

            confirmButton = {
                Button(
                    onClick = {
                        saving = true

                        /**
                         * Save the payment and update the parent bill
                         * together using the repository transaction.
                         */
                        repository.recordPayment(
                            billId = bill.id,
                            paymentAmount = amountCents!! / 100.0,
                            paymentMethod = paymentMethod,
                            reference = if (paymentMethod == "Cash") {
                                ""
                            } else {
                                reference.trim()
                            }
                        ) { result ->
                            saving = false

                            result
                                .onSuccess {
                                    selectedBill = null

                                    // Reload bill balances and payment history.
                                    historyBillId = null
                                    refreshVersion++
                                }
                                .onFailure {
                                    error = it.message ?: "Payment failed"
                                    selectedBill = null
                                }
                        }
                    },
                    enabled = validPayment && !saving
                ) {
                    Text(if (saving) "Saving..." else "Save Payment")
                }
            },

            dismissButton = {
                TextButton(
                    onClick = { selectedBill = null },
                    enabled = !saving
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Formats monetary values to two decimal places.
 */
private fun money(amount: Double): String {
    return String.format(Locale.US, "%,.2f", amount)
}
