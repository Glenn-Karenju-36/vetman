
package com.example.vetman01.ui.patients

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.vetman01.data.BillRepository
import com.example.vetman01.model.Bill
import com.example.vetman01.model.Payment
import com.example.vetman01.model.Patient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Patient billing and manually recorded payments.
 *
 * Uses the existing BillRepository for Firestore operations.
 * Payment entry is handled by an interactive custom dialog.
 */
@Composable
fun BillListScreen(
    patient: Patient,
    onBack: () -> Unit,
    repository: BillRepository = remember {
        BillRepository()
    }
) {
    val colors = MaterialTheme.colorScheme

    var bills by remember(patient.id) {
        mutableStateOf<List<Bill>>(emptyList())
    }
    var loading by remember(patient.id) {
        mutableStateOf(true)
    }
    var error by remember(patient.id) {
        mutableStateOf<String?>(null)
    }
    var refreshVersion by remember(patient.id) {
        mutableIntStateOf(0)
    }

    var selectedBill by remember(patient.id) {
        mutableStateOf<Bill?>(null)
    }
    var saving by remember {
        mutableStateOf(false)
    }
    var paymentError by remember {
        mutableStateOf<String?>(null)
    }

    var historyBillId by remember(patient.id) {
        mutableStateOf<String?>(null)
    }
    var payments by remember {
        mutableStateOf<List<Payment>>(emptyList())
    }
    var historyLoading by remember {
        mutableStateOf(false)
    }
    var historyError by remember {
        mutableStateOf<String?>(null)
    }

    // Reload bills after a successful payment.
    LaunchedEffect(patient.id, refreshVersion, repository) {
        loading = true
        error = null

        repository.getBills { result ->
            result
                .onSuccess { allBills ->
                    bills = allBills
                        .filter { it.patientId == patient.id }
                        .sortedByDescending { it.createdAt }
                }
                .onFailure {
                    error = it.message
                        ?: "Could not load bills"
                }

            loading = false
        }
    }

    val totalCents = bills.sumOf {
        (it.amount * 100).roundToLong()
    }
    val paidCents = bills.sumOf {
        (it.amountPaid * 100).roundToLong()
    }
    val balanceCents = bills.sumOf {
        (
                (it.amount * 100).roundToLong() -
                        (it.amountPaid * 100).roundToLong()
                ).coerceAtLeast(0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextButton(
            onClick = onBack,
            enabled = !saving
        ) {
            Text("← Back")
        }

        Spacer(Modifier.height(10.dp))

        Text(
            "Billing History",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            patient.petName,
            color = colors.onSurfaceVariant
        )

        Spacer(Modifier.height(18.dp))

        if (!loading) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = colors.primaryContainer
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(9.dp)
                ) {
                    Text(
                        "Billing Overview",
                        fontWeight = FontWeight.SemiBold
                    )

                    BillAmountRow(
                        "Total Billed",
                        moneyText(totalCents)
                    )
                    BillAmountRow(
                        "Received",
                        moneyText(paidCents)
                    )
                    BillAmountRow(
                        "Outstanding",
                        moneyText(balanceCents)
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Patient Bills",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(12.dp))

        error?.let {
            Text(it, color = colors.error)
            Spacer(Modifier.height(10.dp))
        }

        when {
            loading -> {
                CircularProgressIndicator()
            }

            bills.isEmpty() -> {
                Text("No bills recorded for this patient.")
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = bills,
                        key = { it.id }
                    ) { bill ->
                        val billBalance = (
                                (bill.amount * 100).roundToLong() -
                                        (bill.amountPaid * 100).roundToLong()
                                ).coerceAtLeast(0)

                        val status = when {
                            billBalance == 0L -> "Paid"
                            bill.amountPaid > 0 -> "Partially Paid"
                            else -> "Unpaid"
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = colors.surface
                            ),
                            border = BorderStroke(
                                1.dp,
                                colors.outlineVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement =
                                    Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment =
                                        Alignment.CenterVertically,
                                    horizontalArrangement =
                                        Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.List,
                                        contentDescription = null,
                                        tint = colors.primary
                                    )

                                    Text(
                                        bill.description.ifBlank {
                                            "Veterinary Bill"
                                        },
                                        fontWeight =
                                            FontWeight.SemiBold,
                                        style = MaterialTheme
                                            .typography.titleMedium
                                    )
                                }

                                Surface(
                                    color = when (status) {
                                        "Paid" -> colors.primaryContainer
                                        "Partially Paid" ->
                                            colors.secondaryContainer
                                        else -> colors.errorContainer
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        status,
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 6.dp
                                        ),
                                        style = MaterialTheme
                                            .typography.labelMedium
                                    )
                                }

                                HorizontalDivider()

                                BillAmountRow(
                                    "Total",
                                    moneyText(
                                        (bill.amount * 100).roundToLong()
                                    )
                                )

                                BillAmountRow(
                                    "Paid",
                                    moneyText(
                                        (bill.amountPaid * 100).roundToLong()
                                    )
                                )

                                BillAmountRow(
                                    "Balance",
                                    moneyText(billBalance)
                                )

                                Text(
                                    formatBillingDate(bill.createdAt),
                                    color = colors.onSurfaceVariant,
                                    style = MaterialTheme
                                        .typography.bodySmall
                                )

                                if (bill.notes.isNotBlank()) {
                                    Text("Notes: ${bill.notes}")
                                }

                                if (billBalance > 0L) {
                                    Button(
                                        onClick = {
                                            paymentError = null
                                            selectedBill = bill
                                        },
                                        enabled = !saving,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            Icons.Filled.CheckCircle,
                                            contentDescription = null
                                        )

                                        Spacer(Modifier.width(8.dp))

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
                                            historyError = null
                                            historyLoading = true

                                            repository.getPayments(
                                                bill.id
                                            ) { result ->
                                                if (
                                                    historyBillId == bill.id
                                                ) {
                                                    historyLoading = false

                                                    result
                                                        .onSuccess {
                                                            payments = it
                                                                .sortedByDescending {
                                                                        payment ->
                                                                    payment.createdAt
                                                                }
                                                        }
                                                        .onFailure {
                                                            historyError =
                                                                it.message
                                                                    ?: "Could not load payments"
                                                        }
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        if (historyBillId == bill.id) {
                                            "Hide Payment History"
                                        } else {
                                            "View Payment History"
                                        }
                                    )
                                }

                                if (historyBillId == bill.id) {
                                    HorizontalDivider()

                                    when {
                                        historyLoading -> {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        historyError != null -> {
                                            Text(
                                                historyError ?: "",
                                                color = colors.error
                                            )
                                        }

                                        payments.isEmpty() -> {
                                            Text("No payments recorded.")
                                        }

                                        else -> {
                                            payments.forEach { payment ->
                                                HorizontalDivider()

                                                Text(
                                                    "KSh ${
                                                        String.format(
                                                            Locale.US,
                                                            "%,.2f",
                                                            payment.amount
                                                        )
                                                    }",
                                                    fontWeight =
                                                        FontWeight.SemiBold,
                                                    color = colors.primary
                                                )

                                                Text(
                                                    "Method: ${payment.method}"
                                                )

                                                if (
                                                    payment.reference.isNotBlank()
                                                ) {
                                                    Text(
                                                        "Reference: ${payment.reference}"
                                                    )
                                                }

                                                Text(
                                                    formatBillingDate(
                                                        payment.createdAt
                                                    ),
                                                    style = MaterialTheme
                                                        .typography.bodySmall,
                                                    color = colors.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // The dialog has its own independently editable form state.
    selectedBill?.let { bill ->
        key(bill.id) {
            RecordPaymentDialog(
                bill = bill,
                saving = saving,
                error = paymentError,
                onDismiss = {
                    if (!saving) {
                        selectedBill = null
                        paymentError = null
                    }
                },
                onSave = { amount, method, reference ->
                    if (!saving) {
                        saving = true
                        paymentError = null

                        // Preserve the existing secure transaction.
                        repository.recordPayment(
                            billId = bill.id,
                            paymentAmount = amount,
                            paymentMethod = method,
                            reference = reference
                        ) { result ->
                            saving = false

                            result
                                .onSuccess {
                                    selectedBill = null
                                    historyBillId = null
                                    refreshVersion++
                                }
                                .onFailure {
                                    paymentError = it.message
                                        ?: "Payment could not be saved"
                                }
                        }
                    }
                }
            )
        }
    }
}

/**
 * Interactive payment form.
 *
 * The content scrolls independently of the footer, so the
 * text field and method choices remain accessible on small screens.
 */
@Composable
private fun RecordPaymentDialog(
    bill: Bill,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (Double, String, String) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    var amountText by remember {
        mutableStateOf("")
    }
    var method by remember {
        mutableStateOf("M-Pesa")
    }
    var reference by remember {
        mutableStateOf("")
    }

    val balanceCents = (
            (bill.amount * 100).roundToLong() -
                    (bill.amountPaid * 100).roundToLong()
            ).coerceAtLeast(0)

    val amountCents = amountText.toDoubleOrNull()
        ?.takeIf { it.isFinite() && it > 0 }
        ?.let { (it * 100).roundToLong() }

    val valid = amountCents != null &&
            amountCents > 0 &&
            amountCents <= balanceCents

    Dialog(
        onDismissRequest = {
            if (!saving) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !saving,
            dismissOnClickOutside = !saving,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.84f),
            shape = RoundedCornerShape(20.dp),
            color = colors.surface,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                Text(
                    "Record Payment",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(14.dp))

                // Only this section scrolls; the action buttons
                // stay reachable at the bottom of the dialog.
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Surface(
                        color = colors.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Text(
                                "Outstanding Balance",
                                style = MaterialTheme
                                    .typography.bodySmall,
                                color = colors.onPrimaryContainer
                            )

                            Spacer(Modifier.height(5.dp))

                            Text(
                                moneyText(balanceCents),
                                style = MaterialTheme
                                    .typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.primary
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // Editable amount field.
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { input ->
                            if (
                                input.isEmpty() ||
                                Regex("""\d*(\.\d{0,2})?""")
                                    .matches(input)
                            ) {
                                amountText = input
                            }
                        },
                        label = {
                            Text("Amount Received (KSh)")
                        },
                        placeholder = {
                            Text("Enter amount")
                        },
                        enabled = !saving,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (
                        amountText.isNotBlank() &&
                        !valid
                    ) {
                        Spacer(Modifier.height(6.dp))

                        Text(
                            "Enter a valid amount within the remaining balance.",
                            color = colors.error,
                            style = MaterialTheme
                                .typography.bodySmall
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        "Payment Method",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(8.dp))

                    // Full clickable rows for easier selection.
                    listOf(
                        "M-Pesa",
                        "Cash",
                        "Airtel Money"
                    ).forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !saving) {
                                    method = option
                                }
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = method == option,
                                onClick = {
                                    method = option
                                },
                                enabled = !saving
                            )

                            Spacer(Modifier.width(8.dp))

                            Text(option)
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    if (method != "Cash") {
                        OutlinedTextField(
                            value = reference,
                            onValueChange = {
                                reference = it
                            },
                            label = {
                                Text("Transaction Reference")
                            },
                            placeholder = {
                                Text("Optional")
                            },
                            enabled = !saving,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))
                    }

                    Text(
                        "Record a payment already received. Vetman does not process transfers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )

                    if (error != null) {
                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = error,
                            color = colors.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                HorizontalDivider()

                Spacer(Modifier.height(12.dp))

                // Persistent dialog actions.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !saving,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (valid && !saving) {
                                onSave(
                                    amountCents!! / 100.0,
                                    method,
                                    if (method == "Cash") {
                                        ""
                                    } else {
                                        reference.trim()
                                    }
                                )
                            }
                        },
                        enabled = valid && !saving,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = colors.onPrimary
                            )
                        } else {
                            Text("Save Payment")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BillAmountRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            value,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun moneyText(cents: Long): String {
    return "KSh " + String.format(
        Locale.US,
        "%,.2f",
        cents / 100.0
    )
}

private fun formatBillingDate(timestamp: Long): String {
    return SimpleDateFormat(
        "dd MMM yyyy, HH:mm",
        Locale.getDefault()
    ).format(Date(timestamp))
}
