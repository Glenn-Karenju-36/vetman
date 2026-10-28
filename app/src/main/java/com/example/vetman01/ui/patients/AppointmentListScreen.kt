
package com.example.vetman01.ui.patients

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.vetman01.data.AppointmentRepository
import com.example.vetman01.model.Appointment
import com.example.vetman01.model.Patient
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Displays appointments belonging to a selected patient.
 * Allows the veterinarian to reschedule, complete or cancel them.
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

    var appointments by remember(patient.id) {
        mutableStateOf<List<Appointment>>(emptyList())
    }
    var loading by remember(patient.id) { mutableStateOf(true) }
    var error by remember(patient.id) { mutableStateOf<String?>(null) }
    var updatingId by remember { mutableStateOf<String?>(null) }
    var refreshVersion by remember { mutableIntStateOf(0) }

    /**
     * Reload appointments after a successful update.
     * Only display appointments belonging to this patient.
     */
    LaunchedEffect(patient.id, refreshVersion) {
        loading = true
        error = null

        repository.getAppointments { result ->
            loading = false
            result
                .onSuccess { allAppointments ->
                    appointments = allAppointments.filter {
                        it.patientId == patient.id
                    }
                }
                .onFailure {
                    error = it.message ?: "Could not load appointments"
                }
        }
    }

    val dateFormat = remember {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack, enabled = updatingId == null) {
            Text("< Back to profile")
        }

        Text(
            "Appointments",
            style = MaterialTheme.typography.headlineMedium
        )
        Text("Patient: ${patient.petName}")

        Spacer(Modifier.height(20.dp))

        when {
            loading -> CircularProgressIndicator()

            error != null -> Text(
                text = error ?: "",
                color = MaterialTheme.colorScheme.error
            )

            appointments.isEmpty() -> {
                Text("No appointments scheduled for this patient.")
            }

            else -> appointments.forEach { appointment ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            dateFormat.format(Date(appointment.scheduledAt)),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text("Status: ${appointment.status}")
                        Text("Reason: ${appointment.reason.ifBlank { "Not provided" }}")

                        if (appointment.notes.isNotBlank()) {
                            Text("Notes: ${appointment.notes}")
                        }

                        if (appointment.status == "scheduled") {
                            Spacer(Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    // Select a new date and time.
                                    val calendar = Calendar.getInstance()

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
                                                    selected.set(Calendar.MINUTE, minute)
                                                    selected.set(Calendar.SECOND, 0)
                                                    selected.set(Calendar.MILLISECOND, 0)

                                                    if (selected.timeInMillis >
                                                        System.currentTimeMillis()
                                                    ) {
                                                        updatingId = appointment.id

                                                        repository.rescheduleAppointment(
                                                            appointment.id,
                                                            selected.timeInMillis
                                                        ) { result ->
                                                            updatingId = null
                                                            result
                                                                .onSuccess { refreshVersion++ }
                                                                .onFailure {
                                                                    error = it.message
                                                                        ?: "Rescheduling failed"
                                                                }
                                                        }
                                                    } else {
                                                        error = "Select a future time"
                                                    }
                                                },
                                                calendar.get(Calendar.HOUR_OF_DAY),
                                                calendar.get(Calendar.MINUTE),
                                                false
                                            ).show()
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                enabled = updatingId == null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Reschedule")
                            }

                            OutlinedButton(
                                onClick = {
                                    updatingId = appointment.id
                                    repository.updateAppointmentStatus(
                                        appointment.id,
                                        "completed"
                                    ) { result ->
                                        updatingId = null
                                        result
                                            .onSuccess { refreshVersion++ }
                                            .onFailure {
                                                error = it.message ?: "Update failed"
                                            }
                                    }
                                },
                                enabled = updatingId == null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Mark Completed")
                            }

                            OutlinedButton(
                                onClick = {
                                    updatingId = appointment.id
                                    repository.updateAppointmentStatus(
                                        appointment.id,
                                        "cancelled"
                                    ) { result ->
                                        updatingId = null
                                        result
                                            .onSuccess { refreshVersion++ }
                                            .onFailure {
                                                error = it.message ?: "Cancellation failed"
                                            }
                                    }
                                },
                                enabled = updatingId == null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Cancel Appointment")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
