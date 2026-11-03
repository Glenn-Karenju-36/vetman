
package com.example.vetman01.ui.patients

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vetman01.data.BillRepository
import com.example.vetman01.data.PatientRepository
import com.example.vetman01.model.Bill
import com.example.vetman01.model.Patient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Veterinarian-wide billing dashboard.
 *
 * Displays all bills and financial totals.
 * Payment recording remains in the existing BillListScreen,
 * preserving its tested Firestore transaction logic.
 */
@Composable
fun AllBillsScreen(
    onBack: () -> Unit,
    onManagePatientBills: (Patient) -> Unit,
    billRepository: BillRepository = remember {
        BillRepository()
    },
    patientRepository: PatientRepository = remember {
        PatientRepository()
    }
) {
    val colors = MaterialTheme.colorScheme

    var bills by remember {
        mutableStateOf<List<Bill>>(emptyList())
    }

    var patients by remember {
        mutableStateOf<List<Patient>>(emptyList())
    }

    var billsLoading by remember {
        mutableStateOf(true)
    }

    var patientsLoading by remember {
        mutableStateOf(true)
    }

    var billsError by remember {
        mutableStateOf<String?>(null)
    }

    var patientsError by remember {
        mutableStateOf<String?>(null)
    }

    var selectedFilter by remember {
        mutableStateOf("All")
    }

    var searchQuery by remember {
        mutableStateOf("")
    }

    var refreshVersion by remember {
        mutableIntStateOf(0)
    }

    // Reload bills when this screen is opened or refreshed.
    LaunchedEffect(refreshVersion, billRepository) {
        billsLoading = true
        billsError = null

        billRepository.getBills { result ->
            result
                .onSuccess {
                    bills = it
                }
                .onFailure {
                    billsError = it.message
                        ?: "Could not load bills"
                }

            billsLoading = false
        }
    }

    // Retrieve patient names for the bill cards.
    LaunchedEffect(patientRepository) {
        patientsLoading = true
        patientsError = null

        patientRepository.getPatients { result ->
            result
                .onSuccess {
                    patients = it
                }
                .onFailure {
                    patientsError = it.message
                        ?: "Could not load patient information"
                }

            patientsLoading = false
        }
    }

    val patientsById = patients.associateBy { it.id }

    // Calculate financial totals in cents to reduce rounding errors.
    val totalBilledCents = bills.sumOf {
        (it.amount * 100).roundToLong()
    }

    val totalPaidCents = bills.sumOf {
        (it.amountPaid * 100).roundToLong()
    }

    val outstandingCents = bills.sumOf { bill ->
        (
                (bill.amount * 100).roundToLong() -
                        (bill.amountPaid * 100).roundToLong()
                ).coerceAtLeast(0)
    }

    val visibleBills = bills
        .filter { bill ->
            val status = billingStatus(bill)

            selectedFilter == "All" ||
                    status == selectedFilter
        }
        .filter { bill ->
            val patient = patientsById[bill.patientId]
            val query = searchQuery.trim()

            query.isBlank() ||
                    bill.description.contains(
                        query,
                        ignoreCase = true
                    ) ||
                    patient?.petName?.contains(
                        query,
                        ignoreCase = true
                    ) == true ||
                    patient?.ownerName?.contains(
                        query,
                        ignoreCase = true
                    ) == true
        }
        .sortedByDescending { it.createdAt }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← Back to Dashboard")
        }

        Spacer(Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.List,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(30.dp)
            )

            Text(
                text = "Billing",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(5.dp))

        Text(
            text = "Manage invoices and payments across your patients.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))

        when {
            billsLoading -> {
                CircularProgressIndicator()
            }

            else -> {
                // FINANCIAL SUMMARY
                BillingSummaryCard(
                    label = "Total Billed",
                    amountCents = totalBilledCents
                )

                Spacer(Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        BillingSummaryCard(
                            label = "Received",
                            amountCents = totalPaidCents
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        BillingSummaryCard(
                            label = "Outstanding",
                            amountCents = outstandingCents
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "All Bills",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search bills") },
            placeholder = {
                Text("Patient, owner or description")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(12.dp))

        // Filters use the calculated balance, not a potentially
        // outdated stored status.
        Row(
            modifier = Modifier.horizontalScroll(
                rememberScrollState()
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "All",
                "Unpaid",
                "Partially Paid",
                "Paid"
            ).forEach { option ->
                FilterChip(
                    selected = selectedFilter == option,
                    onClick = {
                        selectedFilter = option
                    },
                    label = { Text(option) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        billsError?.let {
            Text(
                text = it,
                color = colors.error
            )

            Spacer(Modifier.height(10.dp))
        }

        patientsError?.let {
            Text(
                text = it,
                color = colors.error
            )

            Spacer(Modifier.height(10.dp))
        }

        when {
            billsLoading || patientsLoading -> {
                CircularProgressIndicator()
            }

            visibleBills.isEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.primaryContainer
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "No bills match your selection.",
                        modifier = Modifier.padding(20.dp),
                        color = colors.onPrimaryContainer
                    )
                }
            }

            else -> {
                Text(
                    text = "${visibleBills.size} bills",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )

                Spacer(Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = visibleBills,
                        key = { it.id }
                    ) { bill ->
                        val patient = patientsById[bill.patientId]

                        AllBillsCard(
                            bill = bill,
                            patient = patient,
                            onManage = {
                                if (patient != null) {
                                    onManagePatientBills(patient)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Summary card displaying a financial amount in KSh.
 */
@Composable
private fun BillingSummaryCard(
    label: String,
    amountCents: Long
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colors.primaryContainer
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onPrimaryContainer
            )

            Text(
                text = "KSh ${formatBillMoney(amountCents / 100.0)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )
        }
    }
}

/**
 * Bill card showing patient, amounts and payment status.
 */
@Composable
private fun AllBillsCard(
    bill: Bill,
    patient: Patient?,
    onManage: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    val totalCents = (bill.amount * 100).roundToLong()
    val paidCents = (bill.amountPaid * 100).roundToLong()
    val balanceCents = (totalCents - paidCents).coerceAtLeast(0)

    val status = billingStatus(bill)

    val formattedDate = remember(bill.createdAt) {
        SimpleDateFormat(
            "dd MMM yyyy, HH:mm",
            Locale.getDefault()
        ).format(Date(bill.createdAt))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        ),
        border = BorderStroke(
            1.dp,
            colors.outlineVariant
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = colors.primary
                )

                Text(
                    text = patient?.petName
                        ?: "Patient unavailable",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = bill.description.ifBlank {
                    "Veterinary bill"
                },
                style = MaterialTheme.typography.bodyMedium
            )

            Surface(
                color = when (status) {
                    "Paid" -> colors.primaryContainer
                    "Partially Paid" -> colors.secondaryContainer
                    else -> colors.errorContainer
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = status,
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 6.dp
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = when (status) {
                        "Paid" -> colors.onPrimaryContainer
                        "Partially Paid" -> colors.onSecondaryContainer
                        else -> colors.onErrorContainer
                    }
                )
            }

            HorizontalDivider(
                color = colors.outlineVariant
            )

            BillingAmountRow(
                "Total",
                "KSh ${formatBillMoney(bill.amount)}"
            )

            BillingAmountRow(
                "Paid",
                "KSh ${formatBillMoney(bill.amountPaid)}"
            )

            BillingAmountRow(
                "Balance",
                "KSh ${formatBillMoney(balanceCents / 100.0)}"
            )

            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )

            if (bill.notes.isNotBlank()) {
                Text(
                    text = bill.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            // Reuse the existing secure payment workflow.
            OutlinedButton(
                onClick = onManage,
                enabled = patient != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Manage Bills & Payments")
            }
        }
    }
}

@Composable
private fun BillingAmountRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            value,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Calculate payment status from amounts.
 */
private fun billingStatus(bill: Bill): String {
    val total = (bill.amount * 100).roundToLong()
    val paid = (bill.amountPaid * 100).roundToLong()

    return when {
        total <= paid -> "Paid"
        paid > 0 -> "Partially Paid"
        else -> "Unpaid"
    }
}

private fun formatBillMoney(amount: Double): String {
    return String.format(Locale.US, "%,.2f", amount)
}
