
package com.example.vetman01.ui.patients

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vetman01.data.AppointmentRepository
import com.example.vetman01.model.Appointment
import com.example.vetman01.model.Patient
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Displays appointments for the selected patient.
 *
 * Maintains the existing Firestore operations:
 * rescheduling, completion and cancellation.
 */
@Composable
fun AppointmentListScreen(
    patient: Patient,
    onBack: () -> Unit,
    repository: AppointmentRepository = remember {
        AppointmentRepository()
    }
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    var appointments by remember(patient.id) {
        mutableStateOf<List<Appointment>>(emptyList())
    }

    var loading by remember(patient.id) {
        mutableStateOf(true)
    }

    var error by remember(patient.id) {
        mutableStateOf<String?>(null)
    }

    var updatingId by remember(patient.id) {
        mutableStateOf<String?>(null)
    }

    var refreshVersion by remember(patient.id) {
        mutableIntStateOf(0)
    }

    // Appointment awaiting cancellation confirmation.
    var appointmentToCancel by remember(patient.id) {
        mutableStateOf<Appointment?>(null)
    }

    // Reload after a successful Firestore update.
    LaunchedEffect(patient.id, refreshVersion, repository) {
        loading = true

        repository.getAppointments { result ->
            loading = false

            result
                .onSuccess { allAppointments ->
                    appointments = allAppointments.filter {
                        it.patientId == patient.id
                    }
                }
                .onFailure {
                    error = it.message
                        ?: "Could not load appointments"
                }
        }
    }

    // Active appointments first, then historical records.
    val sortedAppointments = remember(appointments) {
        appointments.sortedWith(
            compareBy<Appointment> {
                if (it.status == "scheduled") 0 else 1
            }.thenBy {
                if (it.status == "scheduled") {
                    it.scheduledAt
                } else {
                    -it.scheduledAt
                }
            }
        )
    }

    val scheduledCount = appointments.count {
        it.status == "scheduled"
    }

    val completedCount = appointments.count {
        it.status == "completed"
    }

    // Confirmation prevents accidental cancellation.
    appointmentToCancel?.let { appointment ->
        AlertDialog(
            onDismissRequest = {
                appointmentToCancel = null
            },
            title = {
                Text("Cancel Appointment?")
            },
            text = {
                Text(
                    "This appointment will be marked as cancelled. " +
                            "It will remain in the patient's appointment history."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        appointmentToCancel = null
                        updatingId = appointment.id
                        error = null

                        repository.updateAppointmentStatus(
                            appointment.id,
                            "cancelled"
                        ) { result ->
                            updatingId = null

                            result
                                .onSuccess {
                                    refreshVersion++
                                }
                                .onFailure {
                                    error = it.message
                                        ?: "Cancellation failed"
                                }
                        }
                    }
                ) {
                    Text(
                        "Cancel Appointment",
                        color = colors.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        appointmentToCancel = null
                    }
                ) {
                    Text("Keep Appointment")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TextButton(
            onClick = onBack,
            enabled = updatingId == null
        ) {
            Text("← Back to Profile")
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Appointments",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface
        )

        Spacer(Modifier.height(5.dp))

        Text(
            text = "Patient: ${patient.petName}",
            color = colors.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        // Appointment summary.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppointmentSummaryCard(
                title = "Scheduled",
                value = scheduledCount.toString(),
                modifier = Modifier.weight(1f)
            )

            AppointmentSummaryCard(
                title = "Completed",
                value = completedCount.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(28.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.DateRange,
                contentDescription = null,
                tint = colors.primary
            )

            Text(
                text = "Appointment History",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(16.dp))

        // Errors remain visible without hiding loaded records.
        error?.let { message ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = colors.errorContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = message,
                    color = colors.onErrorContainer,
                    modifier = Modifier.padding(14.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
        }

        when {
            loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            sortedAppointments.isEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.primaryContainer
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "No appointments found for this patient.",
                        modifier = Modifier.padding(20.dp),
                        color = colors.onPrimaryContainer
                    )
                }
            }

            else -> {
                sortedAppointments.forEach { appointment ->
                    ClinicalAppointmentCard(
                        appointment = appointment,
                        updating = updatingId == appointment.id,
                        actionsEnabled = updatingId == null,
                        onReschedule = {
                            error = null

                            // Begin with the appointment's current date.
                            val calendar = Calendar.getInstance().apply {
                                timeInMillis = appointment.scheduledAt
                            }

                            val datePicker = DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val selected = Calendar.getInstance().apply {
                                        set(year, month, day)
                                    }

                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            selected.set(
                                                Calendar.HOUR_OF_DAY,
                                                hour
                                            )
                                            selected.set(
                                                Calendar.MINUTE,
                                                minute
                                            )
                                            selected.set(
                                                Calendar.SECOND,
                                                0
                                            )
                                            selected.set(
                                                Calendar.MILLISECOND,
                                                0
                                            )

                                            if (
                                                selected.timeInMillis <=
                                                System.currentTimeMillis()
                                            ) {
                                                error = "Select a future date and time."
                                            } else {
                                                updatingId = appointment.id

                                                repository.rescheduleAppointment(
                                                    appointment.id,
                                                    selected.timeInMillis
                                                ) { result ->
                                                    updatingId = null

                                                    result
                                                        .onSuccess {
                                                            refreshVersion++
                                                        }
                                                        .onFailure {
                                                            error = it.message
                                                                ?: "Rescheduling failed"
                                                        }
                                                }
                                            }
                                        },
                                        calendar.get(
                                            Calendar.HOUR_OF_DAY
                                        ),
                                        calendar.get(
                                            Calendar.MINUTE
                                        ),
                                        false
                                    ).show()
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(
                                    Calendar.DAY_OF_MONTH
                                )
                            )

                            // Prevent choosing previous calendar dates.
                            datePicker.datePicker.minDate =
                                Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis

                            datePicker.show()
                        },
                        onComplete = {
                            updatingId = appointment.id
                            error = null

                            repository.updateAppointmentStatus(
                                appointment.id,
                                "completed"
                            ) { result ->
                                updatingId = null

                                result
                                    .onSuccess {
                                        refreshVersion++
                                    }
                                    .onFailure {
                                        error = it.message
                                            ?: "Update failed"
                                    }
                            }
                        },
                        onCancel = {
                            appointmentToCancel = appointment
                        }
                    )

                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Displays an appointment count.
 */
@Composable
private fun AppointmentSummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = colors.primaryContainer
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onPrimaryContainer
            )

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )
        }
    }
}

/**
 * Appointment information and veterinarian actions.
 */
@Composable
private fun ClinicalAppointmentCard(
    appointment: Appointment,
    updating: Boolean,
    actionsEnabled: Boolean,
    onReschedule: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    val date = remember(appointment.scheduledAt) {
        SimpleDateFormat(
            "dd MMM yyyy, hh:mm a",
            Locale.getDefault()
        ).format(Date(appointment.scheduledAt))
    }

    val statusColor = when (appointment.status) {
        "scheduled" -> colors.primary
        "completed" -> Color(0xFF526A5C)
        "cancelled" -> colors.error
        else -> colors.onSurfaceVariant
    }

    val statusBackground = when (appointment.status) {
        "scheduled" -> colors.primaryContainer
        "completed" -> Color(0xFFECEFEB)
        "cancelled" -> colors.errorContainer
        else -> colors.surfaceVariant
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
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(21.dp)
                    )

                    Text(
                        text = date,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Surface(
                color = statusBackground,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = appointment.status
                        .replaceFirstChar { it.uppercase() },
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 6.dp
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Reason",
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant
            )

            Spacer(Modifier.height(3.dp))

            Text(
                text = appointment.reason.ifBlank {
                    "Not provided"
                },
                style = MaterialTheme.typography.bodyMedium
            )

            if (appointment.notes.isNotBlank()) {
                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Notes",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant
                )

                Spacer(Modifier.height(3.dp))

                Text(
                    text = appointment.notes,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (appointment.status == "scheduled") {
                Spacer(Modifier.height(16.dp))

                HorizontalDivider(
                    color = colors.outlineVariant
                )

                Spacer(Modifier.height(14.dp))

                if (updating) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )

                        Text(
                            "Updating appointment...",
                            color = colors.onSurfaceVariant
                        )
                    }
                } else {
                    Button(
                        onClick = onReschedule,
                        enabled = actionsEnabled,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null
                        )

                        Spacer(Modifier.width(8.dp))

                        Text("Reschedule")
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onComplete,
                        enabled = actionsEnabled,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null
                        )

                        Spacer(Modifier.width(8.dp))

                        Text("Mark Completed")
                    }

                    Spacer(Modifier.height(8.dp))

                    TextButton(
                        onClick = onCancel,
                        enabled = actionsEnabled,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Cancel Appointment",
                            color = if (actionsEnabled) {
                                colors.error
                            } else {
                                colors.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}
