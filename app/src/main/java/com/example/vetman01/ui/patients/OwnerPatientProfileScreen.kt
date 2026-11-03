
package com.example.vetman01.ui.patients

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.example.vetman01.data.MedicalRecordRepository
import com.example.vetman01.data.OwnerRecordsRepository
import com.example.vetman01.model.Appointment
import com.example.vetman01.model.Bill
import com.example.vetman01.model.MedicalRecord
import com.example.vetman01.model.Patient
import com.example.vetman01.model.Payment

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Read-only patient profile for pet owners.
 * All data is loaded through the existing repositories.
 * No creation, editing or deletion is allowed here.
 */
@Composable
fun OwnerPatientProfileScreen(
    patient: Patient,
    onBack: () -> Unit,
    medicalRepository: MedicalRecordRepository = remember {
        MedicalRecordRepository()
    },
    ownerRepository: OwnerRecordsRepository = remember {
        OwnerRecordsRepository()
    }
) {
    val colors = MaterialTheme.colorScheme

    var medicalRecords by remember(patient.id) {
        mutableStateOf<List<MedicalRecord>>(emptyList())
    }

    var appointments by remember(patient.id) {
        mutableStateOf<List<Appointment>>(emptyList())
    }

    var bills by remember(patient.id) {
        mutableStateOf<List<Bill>>(emptyList())
    }

    var medicalLoading by remember(patient.id) {
        mutableStateOf(true)
    }

    var appointmentLoading by remember(patient.id) {
        mutableStateOf(true)
    }

    var billLoading by remember(patient.id) {
        mutableStateOf(true)
    }

    var medicalError by remember(patient.id) {
        mutableStateOf<String?>(null)
    }

    var appointmentError by remember(patient.id) {
        mutableStateOf<String?>(null)
    }

    var billError by remember(patient.id) {
        mutableStateOf<String?>(null)
    }

    var expandedBillId by remember(patient.id) {
        mutableStateOf<String?>(null)
    }

    var payments by remember(patient.id) {
        mutableStateOf<List<Payment>>(emptyList())
    }

    var paymentsLoading by remember(patient.id) {
        mutableStateOf(false)
    }

    var paymentsError by remember(patient.id) {
        mutableStateOf<String?>(null)
    }

    // Fetch existing medical and financial records.
    // These repositories must continue enforcing owner access.
    LaunchedEffect(patient.id) {
        medicalRepository.getMedicalRecords(patient.id) { result ->
            result
                .onSuccess {
                    medicalRecords = it
                    medicalError = null
                }
                .onFailure {
                    medicalError = it.message
                        ?: "Unable to load medical history."
                }

            medicalLoading = false
        }

        ownerRepository.getAppointments(patient.id) { result ->
            result
                .onSuccess {
                    appointments = it
                    appointmentError = null
                }
                .onFailure {
                    appointmentError = it.message
                        ?: "Unable to load appointments."
                }

            appointmentLoading = false
        }

        ownerRepository.getBills(patient.id) { result ->
            result
                .onSuccess {
                    bills = it
                    billError = null
                }
                .onFailure {
                    billError = it.message
                        ?: "Unable to load bills."
                }

            billLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        TextButton(onClick = onBack) {
            Text("← Back to My Pets")
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = patient.petName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Patient Profile",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))

        // Match the clinical patient profile photo area.
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = colors.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ClinicalOwnerPaw(
                    modifier = Modifier.size(62.dp),
                    color = colors.primary
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = patient.petName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onPrimaryContainer
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = listOf(
                        patient.species,
                        patient.breed
                    ).filter {
                        it.isNotBlank()
                    }.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Surface(
            color = colors.surfaceVariant,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Read-only patient information",
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 8.dp
                ),
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium
            )
        }

        Spacer(Modifier.height(24.dp))

        // PATIENT INFORMATION
        OwnerClinicalHeader("Patient Information")

        Spacer(Modifier.height(12.dp))

        OwnerClinicalCard {
            OwnerClinicalRow("Name", patient.petName)
            OwnerClinicalRow("Species", patient.species)
            OwnerClinicalRow("Breed", patient.breed)
            OwnerClinicalRow("Age", patient.age)
            OwnerClinicalRow("Sex", patient.sex)
        }

        Spacer(Modifier.height(24.dp))

        // OWNER INFORMATION
        OwnerClinicalHeader("Owner Information")

        Spacer(Modifier.height(12.dp))

        OwnerClinicalCard {
            OwnerClinicalRow("Owner Name", patient.ownerName)
            OwnerClinicalRow("Phone", patient.ownerPhone)
        }

        if (patient.notes.isNotBlank()) {
            Spacer(Modifier.height(24.dp))

            OwnerClinicalHeader("Patient Notes")

            Spacer(Modifier.height(12.dp))

            OwnerClinicalCard {
                Text(
                    text = patient.notes,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // APPOINTMENTS
        OwnerClinicalHeader("Appointments")

        Spacer(Modifier.height(12.dp))

        when {
            appointmentLoading -> OwnerClinicalLoading()

            appointmentError != null -> {
                OwnerClinicalMessage(
                    message = appointmentError
                        ?: "Unable to load appointments.",
                    isError = true
                )
            }

            appointments.isEmpty() -> {
                OwnerClinicalMessage(
                    "No appointments recorded."
                )
            }

            else -> {
                appointments
                    .sortedByDescending { it.scheduledAt }
                    .forEach { appointment ->

                        OwnerClinicalCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = clinicalOwnerDate(
                                        appointment.scheduledAt
                                    ),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )

                                OwnerClinicalStatus(
                                    appointment.status
                                )
                            }

                            OwnerClinicalField(
                                "Reason",
                                appointment.reason
                            )

                            OwnerClinicalField(
                                "Notes",
                                appointment.notes
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                    }
            }
        }

        Spacer(Modifier.height(12.dp))

        // BILLING
        OwnerClinicalHeader("Billing & Payments")

        Spacer(Modifier.height(12.dp))

        when {
            billLoading -> OwnerClinicalLoading()

            billError != null -> {
                OwnerClinicalMessage(
                    message = billError
                        ?: "Unable to load bills.",
                    isError = true
                )
            }

            bills.isEmpty() -> {
                OwnerClinicalMessage(
                    "No bills recorded."
                )
            }

            else -> {
                bills
                    .sortedByDescending { it.createdAt }
                    .forEach { bill ->

                        val totalCents =
                            (bill.amount * 100).roundToLong()

                        val paidCents =
                            (bill.amountPaid * 100).roundToLong()

                        val balanceCents =
                            (totalCents - paidCents)
                                .coerceAtLeast(0L)

                        OwnerClinicalCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = bill.description.ifBlank {
                                        "Veterinary Bill"
                                    },
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                OwnerClinicalStatus(
                                    bill.status
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            OwnerClinicalRow(
                                "Total",
                                "KSh ${clinicalOwnerMoney(bill.amount)}"
                            )

                            OwnerClinicalRow(
                                "Paid",
                                "KSh ${clinicalOwnerMoney(bill.amountPaid)}"
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(
                                    vertical = 8.dp
                                ),
                                color = colors.outlineVariant
                            )

                            OwnerClinicalRow(
                                "Balance",
                                "KSh ${
                                    clinicalOwnerMoney(
                                        balanceCents / 100.0
                                    )
                                }",
                                emphasize = true
                            )

                            OwnerClinicalRow(
                                "Created",
                                clinicalOwnerDate(bill.createdAt)
                            )

                            OwnerClinicalField(
                                "Notes",
                                bill.notes
                            )

                            Spacer(Modifier.height(10.dp))

                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                onClick = {
                                    if (expandedBillId == bill.id) {
                                        expandedBillId = null
                                        payments = emptyList()
                                        paymentsError = null
                                        paymentsLoading = false
                                    } else {
                                        expandedBillId = bill.id
                                        payments = emptyList()
                                        paymentsError = null
                                        paymentsLoading = true

                                        ownerRepository.getPayments(
                                            patientId = patient.id,
                                            billId = bill.id
                                        ) { result ->
                                            // Ignore responses from bills
                                            // that are no longer selected.
                                            if (expandedBillId == bill.id) {
                                                result
                                                    .onSuccess {
                                                        payments = it
                                                    }
                                                    .onFailure {
                                                        paymentsError =
                                                            it.message
                                                                ?: "Unable to load payments."
                                                    }

                                                paymentsLoading = false
                                            }
                                        }
                                    }
                                }
                            ) {
                                Text(
                                    if (expandedBillId == bill.id) {
                                        "Hide Payment History"
                                    } else {
                                        "View Payment History"
                                    }
                                )
                            }

                            if (expandedBillId == bill.id) {
                                Spacer(Modifier.height(12.dp))

                                HorizontalDivider(
                                    color = colors.outlineVariant
                                )

                                Spacer(Modifier.height(12.dp))

                                Text(
                                    text = "Payment History",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(Modifier.height(8.dp))

                                when {
                                    paymentsLoading -> {
                                        OwnerClinicalLoading()
                                    }

                                    paymentsError != null -> {
                                        OwnerClinicalMessage(
                                            message = paymentsError
                                                ?: "Unable to load payments.",
                                            isError = true
                                        )
                                    }

                                    payments.isEmpty() -> {
                                        Text(
                                            text = "No payments recorded.",
                                            color = colors.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }

                                    else -> {
                                        payments
                                            .sortedByDescending {
                                                it.createdAt
                                            }
                                            .forEachIndexed { index, payment ->

                                                if (index > 0) {
                                                    HorizontalDivider(
                                                        modifier = Modifier.padding(
                                                            vertical = 10.dp
                                                        ),
                                                        color = colors.outlineVariant
                                                    )
                                                }

                                                Text(
                                                    text = "KSh ${
                                                        clinicalOwnerMoney(
                                                            payment.amount
                                                        )
                                                    }",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.primary
                                                )

                                                OwnerClinicalRow(
                                                    "Method",
                                                    payment.method
                                                )

                                                if (payment.reference.isNotBlank()) {
                                                    OwnerClinicalRow(
                                                        "Reference",
                                                        payment.reference
                                                    )
                                                }

                                                OwnerClinicalRow(
                                                    "Date",
                                                    clinicalOwnerDate(
                                                        payment.createdAt
                                                    )
                                                )
                                            }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                    }
            }
        }

        Spacer(Modifier.height(12.dp))

        // MEDICAL HISTORY
        OwnerClinicalHeader("Medical History")

        Spacer(Modifier.height(12.dp))

        when {
            medicalLoading -> OwnerClinicalLoading()

            medicalError != null -> {
                OwnerClinicalMessage(
                    message = medicalError
                        ?: "Unable to load medical history.",
                    isError = true
                )
            }

            medicalRecords.isEmpty() -> {
                OwnerClinicalMessage(
                    "No medical records available."
                )
            }

            else -> {
                medicalRecords
                    .sortedByDescending { it.visitDate }
                    .forEach { record ->

                        OwnerClinicalCard {
                            Text(
                                text = clinicalOwnerDate(
                                    record.visitDate
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.primary
                            )

                            Spacer(Modifier.height(6.dp))

                            OwnerClinicalField(
                                "Symptoms",
                                record.symptoms
                            )

                            OwnerClinicalField(
                                "Diagnosis",
                                record.diagnosis
                            )

                            OwnerClinicalField(
                                "Treatment",
                                record.treatment
                            )

                            OwnerClinicalField(
                                "Medications",
                                record.medications
                            )

                            OwnerClinicalField(
                                "Procedures",
                                record.procedures
                            )

                            OwnerClinicalField(
                                "Notes",
                                record.notes
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                    }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Custom paw placeholder.
 *
 * Uses theme colour rather than platform-dependent
 * emoji artwork. No image dependency is required.
 */
@Composable
private fun ClinicalOwnerPaw(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(modifier = modifier) {
        drawClinicalPaw(color)
    }
}

private fun DrawScope.drawClinicalPaw(color: Color) {
    val w = size.width
    val h = size.height
    val radius = w * 0.095f

    // Four toe pads.
    drawCircle(
        color = color,
        radius = radius,
        center = Offset(w * 0.23f, h * 0.31f)
    )

    drawCircle(
        color = color,
        radius = radius,
        center = Offset(w * 0.42f, h * 0.18f)
    )

    drawCircle(
        color = color,
        radius = radius,
        center = Offset(w * 0.63f, h * 0.18f)
    )

    drawCircle(
        color = color,
        radius = radius,
        center = Offset(w * 0.81f, h * 0.33f)
    )

    // Main pad.
    drawOval(
        color = color,
        topLeft = Offset(w * 0.22f, h * 0.43f),
        size = androidx.compose.ui.geometry.Size(
            w * 0.57f,
            h * 0.43f
        )
    )
}

/**
 * Same heading treatment across clinical screens.
 */
@Composable
private fun OwnerClinicalHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}

/**
 * White clinical card with subtle outline.
 */
@Composable
private fun OwnerClinicalCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content
        )
    }
}

/**
 * Two-column read-only detail row.
 */
@Composable
private fun OwnerClinicalRow(
    label: String,
    value: String,
    emphasize: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value.ifBlank { "Not provided" },
            modifier = Modifier.weight(1.5f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) {
                FontWeight.Bold
            } else {
                FontWeight.Medium
            },
            color = if (emphasize) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

/**
 * Optional long-form text for clinical notes.
 */
@Composable
private fun OwnerClinicalField(
    label: String,
    value: String
) {
    if (value.isNotBlank()) {
        Column(
            modifier = Modifier.padding(vertical = 5.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Neutral clinical status label.
 */
@Composable
private fun OwnerClinicalStatus(status: String) {
    val colors = MaterialTheme.colorScheme

    Surface(
        color = colors.primaryContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = status.replaceFirstChar {
                it.uppercase()
            },
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 6.dp
            ),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onPrimaryContainer
        )
    }
}

@Composable
private fun OwnerClinicalLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(26.dp),
            strokeWidth = 2.dp
        )
    }
}

@Composable
private fun OwnerClinicalMessage(
    message: String,
    isError: Boolean = false
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceVariant
        )
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) {
                colors.error
            } else {
                colors.onSurfaceVariant
            }
        )
    }
}

private fun clinicalOwnerDate(timestamp: Long): String {
    return SimpleDateFormat(
        "dd MMM yyyy, HH:mm",
        Locale.getDefault()
    ).format(Date(timestamp))
}

private fun clinicalOwnerMoney(amount: Double): String {
    return String.format(
        Locale.US,
        "%,.2f",
        amount
    )
}
