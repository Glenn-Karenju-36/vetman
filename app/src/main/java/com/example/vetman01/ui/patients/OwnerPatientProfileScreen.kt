
package com.example.vetman01.ui.patients

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
 * Complete read-only pet profile for owners.
 *
 * Includes medical history, appointments, bills
 * and payment history.
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

    // Payment history is loaded only when a bill is expanded.
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

    // Load all three record categories when a pet is selected.
    LaunchedEffect(patient.id) {
        medicalRepository.getMedicalRecords(patient.id) { result ->
            result.onSuccess {
                medicalRecords = it
            }.onFailure {
                medicalError = it.message ?: "Could not load medical history"
            }
            medicalLoading = false
        }

        ownerRepository.getAppointments(patient.id) { result ->
            result.onSuccess {
                appointments = it
            }.onFailure {
                appointmentError = it.message ?: "Could not load appointments"
            }
            appointmentLoading = false
        }

        ownerRepository.getBills(patient.id) { result ->
            result.onSuccess {
                bills = it
            }.onFailure {
                billError = it.message ?: "Could not load bills"
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
            Text("‹ Back to My Pets")
        }

        Spacer(Modifier.height(12.dp))

        Text(
            patient.petName,
            style = MaterialTheme.typography.headlineMedium
        )
        Text("Pet Profile")

        Spacer(Modifier.height(24.dp))

        // PET DETAILS
        Text(
            "Pet Information",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(8.dp))

        OwnerInfoRow("Name", patient.petName)
        OwnerInfoRow("Species", patient.species)
        OwnerInfoRow("Breed", patient.breed)
        OwnerInfoRow("Age", patient.age)
        OwnerInfoRow("Sex", patient.sex)

        Spacer(Modifier.height(20.dp))

        Text(
            "Owner Information",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(8.dp))

        OwnerInfoRow("Owner", patient.ownerName)
        OwnerInfoRow("Phone", patient.ownerPhone)

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        // APPOINTMENTS
        Text(
            "Appointments",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(12.dp))

        when {
            appointmentLoading -> CircularProgressIndicator()

            appointmentError != null -> Text(
                appointmentError ?: "",
                color = MaterialTheme.colorScheme.error
            )

            appointments.isEmpty() -> Text(
                "No appointments recorded."
            )

            else -> appointments.forEach { appointment ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            ownerDate(appointment.scheduledAt),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text("Status: ${appointment.status}")
                        OwnerOptionalText("Reason", appointment.reason)
                        OwnerOptionalText("Notes", appointment.notes)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        // BILLS
        Text(
            "Bills and Payments",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(12.dp))

        when {
            billLoading -> CircularProgressIndicator()

            billError != null -> Text(
                billError ?: "",
                color = MaterialTheme.colorScheme.error
            )

            bills.isEmpty() -> Text(
                "No bills recorded."
            )

            else -> bills.forEach { bill ->
                val totalCents = (bill.amount * 100).roundToLong()
                val paidCents = (bill.amountPaid * 100).roundToLong()
                val balanceCents =
                    (totalCents - paidCents).coerceAtLeast(0L)

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            bill.description,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text("Total: KSh ${ownerMoney(bill.amount)}")
                        Text("Paid: KSh ${ownerMoney(bill.amountPaid)}")
                        Text(
                            "Balance: KSh ${
                                ownerMoney(balanceCents / 100.0)
                            }"
                        )
                        Text("Status: ${bill.status}")
                        Text(
                            ownerDate(bill.createdAt),
                            style = MaterialTheme.typography.bodySmall
                        )
                        OwnerOptionalText("Notes", bill.notes)

                        Spacer(Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                if (expandedBillId == bill.id) {
                                    expandedBillId = null
                                    payments = emptyList()
                                } else {
                                    expandedBillId = bill.id
                                    payments = emptyList()
                                    paymentsError = null
                                    paymentsLoading = true

                                    ownerRepository.getPayments(
                                        patientId = patient.id,
                                        billId = bill.id
                                    ) { result ->
                                        // Ignore an older request if
                                        // another bill is now selected.
                                        if (expandedBillId == bill.id) {
                                            result.onSuccess {
                                                payments = it
                                            }.onFailure {
                                                paymentsError = it.message
                                                    ?: "Could not load payments"
                                            }
                                            paymentsLoading = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (expandedBillId == bill.id)
                                    "Hide Payment History"
                                else
                                    "View Payment History"
                            )
                        }

                        if (expandedBillId == bill.id) {
                            Spacer(Modifier.height(8.dp))

                            when {
                                paymentsLoading -> CircularProgressIndicator()

                                paymentsError != null -> Text(
                                    paymentsError ?: "",
                                    color = MaterialTheme.colorScheme.error
                                )

                                payments.isEmpty() -> Text(
                                    "No payments recorded."
                                )

                                else -> payments.forEach { payment ->
                                    HorizontalDivider()
                                    Spacer(Modifier.height(8.dp))

                                    Text(
                                        "KSh ${ownerMoney(payment.amount)}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text("Method: ${payment.method}")
                                    OwnerOptionalText(
                                        "Reference",
                                        payment.reference
                                    )
                                    Text(
                                        ownerDate(payment.createdAt),
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

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        // MEDICAL HISTORY
        Text(
            "Medical History",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(12.dp))

        when {
            medicalLoading -> CircularProgressIndicator()

            medicalError != null -> Text(
                medicalError ?: "",
                color = MaterialTheme.colorScheme.error
            )

            medicalRecords.isEmpty() -> Text(
                "No medical records available."
            )

            else -> medicalRecords.forEach { record ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            ownerDate(record.visitDate),
                            style = MaterialTheme.typography.titleMedium
                        )

                        OwnerOptionalText("Symptoms", record.symptoms)
                        OwnerOptionalText("Diagnosis", record.diagnosis)
                        OwnerOptionalText("Treatment", record.treatment)
                        OwnerOptionalText("Medications", record.medications)
                        OwnerOptionalText("Procedures", record.procedures)
                        OwnerOptionalText("Notes", record.notes)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

/** Display an individual pet or owner detail. */
@Composable
private fun OwnerInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f)
        )
        Text(
            value.ifBlank { "Not provided" },
            modifier = Modifier.weight(2f)
        )
    }
}

/** Show optional clinical or billing information. */
@Composable
private fun OwnerOptionalText(
    label: String,
    value: String
) {
    if (value.isNotBlank()) {
        Text("$label: $value")
    }
}

/** Format Firestore timestamps. */
private fun ownerDate(timestamp: Long): String {
    return SimpleDateFormat(
        "dd MMM yyyy, HH:mm",
        Locale.getDefault()
    ).format(Date(timestamp))
}

/** Format Kenyan Shilling amounts to two decimal places. */
private fun ownerMoney(amount: Double): String {
    return String.format(Locale.US, "%,.2f", amount)
}
