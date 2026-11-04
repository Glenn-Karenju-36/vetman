
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vetman01.data.AppointmentOfflineRepository
import com.example.vetman01.data.AppointmentRepository
import com.example.vetman01.data.AppointmentSaveStatus
import com.example.vetman01.model.Appointment
import com.example.vetman01.model.Patient
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Patient-specific appointment history.
 *
 * Rescheduling and cancellation use local-first writes.
 * Completion retains the existing online-confirmed flow.
 */
@Composable
fun AppointmentListScreen(
    patient: Patient,
    onBack: () -> Unit,
    repository: AppointmentRepository = remember {
        AppointmentRepository()
    },
    offlineRepository: AppointmentOfflineRepository = remember {
        AppointmentOfflineRepository()
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
    var appointmentToCancel by remember(patient.id) {
        mutableStateOf<Appointment?>(null)
    }

    // Each local change is provisional until server confirmation.
    var pendingId by remember(patient.id) {
        mutableStateOf<String?>(null)
    }
    var saveMessage by remember(patient.id) {
        mutableStateOf<String?>(null)
    }
    var stopObserver by remember(patient.id) {
        mutableStateOf<(() -> Unit)?>(null)
    }

    DisposableEffect(patient.id) {
        onDispose {
            stopObserver?.invoke()
        }
    }

    LaunchedEffect(patient.id, refreshVersion, repository) {
        loading = true
        repository.getAppointments { result ->
            loading = false
            result.onSuccess { all ->
                appointments = all.filter { it.patientId == patient.id }
            }.onFailure {
                error = it.message ?: "Could not load appointments"
            }
        }
    }

    // Start a local-first reschedule or cancellation operation.
    fun startOfflineUpdate(
        appointment: Appointment,
        newDate: Long? = null,
        cancel: Boolean = false
    ) {
        if (updatingId != null || pendingId != null) return

        updatingId = appointment.id
        error = null
        saveMessage = null

        val callback: (AppointmentSaveStatus) -> Unit = { status ->
            when (status) {
                AppointmentSaveStatus.QueuedLocally -> {
                    updatingId = null
                    pendingId = appointment.id

                    // Reflect locally queued changes immediately.
                    appointments = appointments.map { current ->
                        if (current.id != appointment.id) current
                        else if (cancel) {
                            current.copy(status = "cancelled")
                        } else {
                            current.copy(
                                scheduledAt = newDate
                                    ?: current.scheduledAt,
                                status = "scheduled"
                            )
                        }
                    }

                    saveMessage =
                        "Saved on this device. Awaiting synchronization."
                }

                AppointmentSaveStatus.Synced -> {
                    updatingId = null
                    pendingId = null
                    stopObserver = null
                    saveMessage = "Appointment synchronized."
                    refreshVersion++
                }

                is AppointmentSaveStatus.Failed -> {
                    updatingId = null
                    pendingId = null
                    stopObserver = null
                    saveMessage = null
                    error = status.error.message
                        ?: "Appointment update failed."
                    refreshVersion++
                }
            }
        }

        stopObserver = if (cancel) {
            offlineRepository.updateStatusOfflineAware(
                appointment.id,
                "cancelled",
                callback
            )
        } else {
            offlineRepository.rescheduleOfflineAware(
                appointment.id,
                newDate ?: appointment.scheduledAt,
                callback
            )
        }
    }

    fun completeAppointment(appointment: Appointment) {
        if (updatingId != null || pendingId != null) return

        updatingId = appointment.id
        error = null

        repository.updateAppointmentStatus(
            appointment.id,
            "completed"
        ) { result ->
            updatingId = null
            result.onSuccess {
                refreshVersion++
            }.onFailure {
                error = it.message ?: "Update failed"
            }
        }
    }

    val sortedAppointments = appointments.sortedWith(
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

    val scheduledCount = appointments.count {
        it.status == "scheduled"
    }
    val completedCount = appointments.count {
        it.status == "completed"
    }

    appointmentToCancel?.let { appointment ->
        AlertDialog(
            onDismissRequest = { appointmentToCancel = null },
            title = { Text("Cancel Appointment?") },
            text = {
                Text(
                    "This appointment will be marked as cancelled " +
                            "and retained in the patient's history."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        appointmentToCancel = null
                        startOfflineUpdate(
                            appointment = appointment,
                            cancel = true
                        )
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
                    onClick = { appointmentToCancel = null }
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
            "Appointments",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(5.dp))
        Text(
            "Patient: ${patient.petName}",
            color = colors.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            AppointmentCountCard(
                "Scheduled",
                scheduledCount.toString(),
                Modifier.weight(1f)
            )
            AppointmentCountCard(
                "Completed",
                completedCount.toString(),
                Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Filled.DateRange,
                contentDescription = null,
                tint = colors.primary
            )
            Text(
                "Appointment History",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(16.dp))

        saveMessage?.let { message ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = colors.primaryContainer
                )
            ) {
                Text(
                    message,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    color = colors.onPrimaryContainer
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        error?.let { message ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = colors.errorContainer
                )
            ) {
                Text(
                    message,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    color = colors.onErrorContainer
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        when {
            loading -> {
                Box(
                    Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            sortedAppointments.isEmpty() -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = colors.primaryContainer
                    )
                ) {
                    Text(
                        "No appointments found for this patient.",
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }

            else -> {
                sortedAppointments.forEach { appointment ->
                    PatientAppointmentOfflineCard(
                        appointment = appointment,
                        updating = updatingId == appointment.id,
                        pending = pendingId == appointment.id,
                        actionsEnabled =
                            updatingId == null && pendingId == null,
                        onReschedule = {
                            val initial =
                                Calendar.getInstance().apply {
                                    timeInMillis = appointment.scheduledAt
                                }

                            val picker = DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val selected =
                                        Calendar.getInstance().apply {
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
                                            selected.set(Calendar.SECOND, 0)
                                            selected.set(
                                                Calendar.MILLISECOND,
                                                0
                                            )

                                            if (
                                                selected.timeInMillis <=
                                                System.currentTimeMillis()
                                            ) {
                                                error =
                                                    "Select a future date and time."
                                            } else {
                                                startOfflineUpdate(
                                                    appointment,
                                                    newDate =
                                                        selected.timeInMillis
                                                )
                                            }
                                        },
                                        initial.get(
                                            Calendar.HOUR_OF_DAY
                                        ),
                                        initial.get(Calendar.MINUTE),
                                        false
                                    ).show()
                                },
                                initial.get(Calendar.YEAR),
                                initial.get(Calendar.MONTH),
                                initial.get(Calendar.DAY_OF_MONTH)
                            )

                            picker.datePicker.minDate =
                                Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis

                            picker.show()
                        },
                        onComplete = {
                            completeAppointment(appointment)
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

@Composable
private fun AppointmentCountCard(
    title: String,
    count: String,
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
                title,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onPrimaryContainer
            )
            Text(
                count,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )
        }
    }
}

@Composable
private fun PatientAppointmentOfflineCard(
    appointment: Appointment,
    updating: Boolean,
    pending: Boolean,
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
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Filled.DateRange,
                    contentDescription = null,
                    tint = colors.primary
                )
                Text(
                    date,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(12.dp))

            Surface(
                color = statusBackground,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    appointment.status.replaceFirstChar {
                        it.uppercase()
                    },
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 6.dp
                    ),
                    color = statusColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(14.dp))
            Text(
                "Reason",
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant
            )
            Text(
                appointment.reason.ifBlank { "Not provided" }
            )

            if (appointment.notes.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Notes",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant
                )
                Text(appointment.notes)
            }

            if (pending) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Pending synchronization",
                    color = colors.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (appointment.status == "scheduled") {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(14.dp))

                if (updating) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Saving appointment...")
                } else {
                    Button(
                        onClick = onReschedule,
                        enabled = actionsEnabled,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Reschedule")
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onComplete,
                        enabled = actionsEnabled,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
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
                            color = colors.error
                        )
                    }
                }
            }
        }
    }
}
