package com.example.vetman01.ui.patients

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vetman01.data.AppointmentOfflineRepository
import com.example.vetman01.data.AppointmentSaveStatus
import com.example.vetman01.model.Appointment
import com.example.vetman01.model.Patient
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Creates a single veterinary appointment, online or offline.
 *
 * Duplicate protection:
 * 1. AtomicBoolean blocks repeated taps immediately.
 * 2. rememberSaveable preserves one submission ID.
 * 3. Firestore writes use that ID as the document path.
 */
@Composable
fun AddAppointmentScreen(
    patient: Patient,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    repository: AppointmentOfflineRepository = remember {
        AppointmentOfflineRepository()
    }
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    // One ID per form, rather than a new ID on every tap.
    val submissionId = rememberSaveable(patient.id) {
        UUID.randomUUID().toString()
    }

    // Unlike a visual disabled state, this changes
    // synchronously when the first tap is processed.
    val submissionLocked = remember(patient.id, submissionId) {
        AtomicBoolean(false)
    }

    var scheduledAt by rememberSaveable(patient.id) {
        mutableLongStateOf(0L)
    }

    var reason by rememberSaveable(patient.id) {
        mutableStateOf("")
    }

    var notes by rememberSaveable(patient.id) {
        mutableStateOf("")
    }

    var saving by remember(patient.id) {
        mutableStateOf(false)
    }

    var submitted by remember(patient.id) {
        mutableStateOf(false)
    }

    var queuedLocally by remember(patient.id) {
        mutableStateOf(false)
    }

    var error by remember(patient.id) {
        mutableStateOf<String?>(null)
    }

    var stopObserver by remember(patient.id) {
        mutableStateOf<(() -> Unit)?>(null)
    }

    // A detached observer does not cancel the pending write.
    DisposableEffect(patient.id) {
        onDispose {
            stopObserver?.invoke()
        }
    }

    val dateFormat = remember {
        SimpleDateFormat(
            "dd MMM yyyy, HH:mm",
            Locale.getDefault()
        )
    }

    val formattedDate =
        if (scheduledAt > 0L) {
            dateFormat.format(Date(scheduledAt))
        } else {
            "No date selected"
        }

    val canSave =
        scheduledAt > System.currentTimeMillis() &&
                !saving &&
                !submitted &&
                !submissionLocked.get()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TextButton(
            onClick = onBack,
            enabled = !saving && !submitted
        ) {
            Text("← Back to Profile")
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Schedule Appointment",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Patient: ${patient.petName}",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                val today = Calendar.getInstance()

                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        val selected = Calendar.getInstance()

                        selected.set(year, month, day)

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

                                scheduledAt =
                                    selected.timeInMillis

                                error = null
                            },
                            today.get(Calendar.HOUR_OF_DAY),
                            today.get(Calendar.MINUTE),
                            false
                        ).show()
                    },
                    today.get(Calendar.YEAR),
                    today.get(Calendar.MONTH),
                    today.get(Calendar.DAY_OF_MONTH)
                ).apply {
                    datePicker.minDate =
                        Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                }.show()
            },
            enabled = !saving && !submitted,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Select Date and Time")
        }

        Spacer(Modifier.height(8.dp))

        Text("Scheduled for: $formattedDate")

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            label = {
                Text("Reason for appointment")
            },
            minLines = 2,
            enabled = !saving && !submitted,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = {
                Text("Additional notes")
            },
            minLines = 3,
            enabled = !saving && !submitted,
            modifier = Modifier.fillMaxWidth()
        )

        // Queued is not the same as server-confirmed.
        if (queuedLocally) {
            Spacer(Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = colors.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Saved on this device",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onPrimaryContainer
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "This appointment is queued for " +
                                "synchronization. Reconnect to the " +
                                "internet to complete the save.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onPrimaryContainer
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            stopObserver?.invoke()
                            stopObserver = null
                            onSaved()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Return to Patient Profile")
                    }
                }
            }
        }

        error?.let { message ->
            Spacer(Modifier.height(12.dp))

            Text(
                text = message,
                color = colors.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                // Atomic compare-and-set ensures that only
                // the first tap starts a Firestore operation.
                if (
                    !submitted &&
                    scheduledAt > System.currentTimeMillis() &&
                    submissionLocked.compareAndSet(false, true)
                ) {
                    saving = true
                    submitted = true
                    queuedLocally = false
                    error = null

                    val appointment = Appointment(
                        id = submissionId,
                        patientId = patient.id,
                        scheduledAt = scheduledAt,
                        reason = reason.trim(),
                        notes = notes.trim()
                    )

                    stopObserver =
                        repository.addAppointmentOfflineAware(
                            appointment = appointment,
                            submissionId = submissionId,
                            onStatus = { status ->
                                when (status) {
                                    AppointmentSaveStatus.QueuedLocally -> {
                                        saving = false
                                        queuedLocally = true
                                    }

                                    AppointmentSaveStatus.Synced -> {
                                        saving = false
                                        queuedLocally = false
                                        stopObserver = null
                                        onSaved()
                                    }

                                    is AppointmentSaveStatus.Failed -> {
                                        saving = false
                                        submitted = false
                                        queuedLocally = false
                                        stopObserver = null

                                        error = status.error.message
                                            ?: "Could not save appointment."

                                        // Permit a genuine retry, but keep
                                        // the same document ID.
                                        submissionLocked.set(false)
                                    }
                                }
                            }
                        )
                }
            },
            enabled = canSave,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                when {
                    saving -> "Saving..."
                    queuedLocally -> "Queued for Sync"
                    else -> "Save Appointment"
                }
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}
