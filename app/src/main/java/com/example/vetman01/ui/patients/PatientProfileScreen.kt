
package com.example.vetman01.ui.patients

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vetman01.data.MedicalRecordRepository
import com.example.vetman01.model.MedicalRecord
import com.example.vetman01.model.Patient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Veterinarian patient profile.
 *
 * Displays patient details, owner information,
 * medical history, appointments, and billing actions.
 */
@Composable
fun PatientProfileScreen(
    patient: Patient,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAddMedicalRecord: () -> Unit,
    onScheduleAppointment: () -> Unit,
    onViewAppointments: () -> Unit,
    onCreateBill: () -> Unit,
    onViewBills: () -> Unit,
    repository: MedicalRecordRepository = remember {
        MedicalRecordRepository()
    }
) {
    val colors = MaterialTheme.colorScheme

    var medicalRecords by remember(patient.id) {
        mutableStateOf<List<MedicalRecord>>(emptyList())
    }

    var loading by remember(patient.id) {
        mutableStateOf(true)
    }

    var error by remember(patient.id) {
        mutableStateOf<String?>(null)
    }

    // Retrieve this patient's medical history from Firestore.
    LaunchedEffect(patient.id, repository) {
        repository.getMedicalRecords(patient.id) { result ->
            result
                .onSuccess {
                    medicalRecords = it
                    error = null
                }
                .onFailure {
                    error = it.message
                        ?: "Could not load medical history"
                }

            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← Back to Patients")
        }

        Spacer(Modifier.height(12.dp))

        // PATIENT HEADER
        Text(
            text = patient.petName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = listOf(patient.species, patient.breed)
                .filter { it.isNotBlank() }
                .joinToString(" • "),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))

        // PET PHOTO PLACEHOLDER
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(155.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = colors.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(44.dp)
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Pet photo coming soon",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = null
            )

            Spacer(Modifier.width(8.dp))

            Text("Edit Patient")
        }

        Spacer(Modifier.height(28.dp))

        // PATIENT INFORMATION
        ClinicalSectionHeader(
            title = "Patient Information",
            icon = Icons.Filled.Person
        )

        Spacer(Modifier.height(12.dp))

        ClinicalInformationCard {
            ClinicalDetailRow("Name", patient.petName)
            ClinicalDetailRow("Species", patient.species)
            ClinicalDetailRow("Breed", patient.breed)
            ClinicalDetailRow("Age", patient.age)
            ClinicalDetailRow("Sex", patient.sex)
        }

        Spacer(Modifier.height(24.dp))

        // OWNER INFORMATION
        ClinicalSectionHeader(
            title = "Owner Information",
            icon = Icons.Filled.Person
        )

        Spacer(Modifier.height(12.dp))

        ClinicalInformationCard {
            ClinicalDetailRow("Owner", patient.ownerName)
            ClinicalDetailRow("Phone", patient.ownerPhone)
        }

        Spacer(Modifier.height(24.dp))

        // PATIENT NOTES
        ClinicalSectionHeader(
            title = "Patient Notes",
            icon = Icons.Filled.List
        )

        Spacer(Modifier.height(12.dp))

        ClinicalInformationCard {
            Text(
                text = patient.notes.ifBlank {
                    "No notes recorded."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (patient.notes.isBlank()) {
                    colors.onSurfaceVariant
                } else {
                    colors.onSurface
                }
            )
        }

        Spacer(Modifier.height(28.dp))

        // APPOINTMENTS
        ClinicalSectionHeader(
            title = "Appointments",
            icon = Icons.Filled.DateRange
        )

        Spacer(Modifier.height(12.dp))

        ClinicalActionCard(
            description = "Schedule and manage visits for ${patient.petName}."
        ) {
            Button(
                onClick = onScheduleAppointment,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null
                )

                Spacer(Modifier.width(8.dp))

                Text("Schedule Appointment")
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onViewAppointments,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("View Appointments")
            }
        }

        Spacer(Modifier.height(24.dp))

        // BILLING
        ClinicalSectionHeader(
            title = "Billing",
            icon = Icons.Filled.List
        )

        Spacer(Modifier.height(12.dp))

        ClinicalActionCard(
            description = "Manage invoices, balances and payments."
        ) {
            Button(
                onClick = onCreateBill,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null
                )

                Spacer(Modifier.width(8.dp))

                Text("Create Bill")
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onViewBills,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("View Bills")
            }
        }

        Spacer(Modifier.height(24.dp))

        // MEDICAL HISTORY
        ClinicalSectionHeader(
            title = "Medical History",
            icon = Icons.Filled.List
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onAddMedicalRecord,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null
            )

            Spacer(Modifier.width(8.dp))

            Text("Add Medical Record")
        }

        Spacer(Modifier.height(16.dp))

        when {
            loading -> {
                CircularProgressIndicator()
            }

            error != null -> {
                Text(
                    text = error ?: "",
                    color = colors.error
                )
            }

            medicalRecords.isEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.primaryContainer
                    )
                ) {
                    Text(
                        text = "No medical records added yet.",
                        modifier = Modifier.padding(20.dp),
                        color = colors.onPrimaryContainer
                    )
                }
            }

            else -> {
                // Most recent consultations first.
                medicalRecords
                    .sortedByDescending { it.visitDate }
                    .forEach { record ->
                        ClinicalMedicalRecordCard(record)

                        Spacer(Modifier.height(12.dp))
                    }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Consistent section heading with a Material icon.
 */
@Composable
private fun ClinicalSectionHeader(
    title: String,
    icon: ImageVector
) {
    val colors = MaterialTheme.colorScheme

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            color = colors.primaryContainer,
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.padding(9.dp)
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface
        )
    }
}

/**
 * Reusable white card for information sections.
 */
@Composable
private fun ClinicalInformationCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

/**
 * Displays a label and its corresponding patient value.
 */
@Composable
private fun ClinicalDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value.ifBlank { "Not provided" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1.6f)
        )
    }
}

/**
 * Action panel for appointments and billing.
 */
@Composable
private fun ClinicalActionCard(
    description: String,
    content: @Composable ColumnScope.() -> Unit
) {
    ClinicalInformationCard {
        Text(
            text = description,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(14.dp))

        content()
    }
}

/**
 * Read-only medical consultation card.
 * Existing medical data fields are preserved.
 */
@Composable
private fun ClinicalMedicalRecordCard(
    record: MedicalRecord
) {
    val colors = MaterialTheme.colorScheme

    val date = remember(record.visitDate) {
        SimpleDateFormat(
            "dd MMM yyyy, HH:mm",
            Locale.getDefault()
        ).format(Date(record.visitDate))
    }

    ClinicalInformationCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.DateRange,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = date,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )
        }

        Spacer(Modifier.height(12.dp))

        HorizontalDivider(
            color = colors.outlineVariant
        )

        Spacer(Modifier.height(12.dp))

        ClinicalMedicalDetail(
            "Symptoms",
            record.symptoms
        )

        ClinicalMedicalDetail(
            "Diagnosis",
            record.diagnosis
        )

        ClinicalMedicalDetail(
            "Treatment",
            record.treatment
        )

        ClinicalMedicalDetail(
            "Medications",
            record.medications
        )

        ClinicalMedicalDetail(
            "Procedures",
            record.procedures
        )

        ClinicalMedicalDetail(
            "Notes",
            record.notes
        )
    }
}

/**
 * Hide medical record fields that have no values.
 */
@Composable
private fun ClinicalMedicalDetail(
    label: String,
    value: String
) {
    if (value.isNotBlank()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(3.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(12.dp))
    }
}
