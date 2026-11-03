package com.example.vetman01.ui.patients

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vetman01.data.AppointmentRepository
import com.example.vetman01.data.PatientRepository
import com.example.vetman01.model.Appointment
import com.example.vetman01.model.Patient
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Veterinarian-wide appointments screen.
 * Displays appointments across every registered patient.
 */
@Composable
fun AllAppointmentsScreen(
    onBack: () -> Unit,
    onPatientClick: (Patient) -> Unit,
    appointmentRepository: AppointmentRepository = remember {
        AppointmentRepository()
    },
    patientRepository: PatientRepository = remember {
        PatientRepository()
    }
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current

    var appointments by remember {
        mutableStateOf<List<Appointment>>(emptyList())
    }
    var patients by remember {
        mutableStateOf<List<Patient>>(emptyList())
    }
    var appointmentsLoading by remember {
        mutableStateOf(true)
    }
    var patientsLoading by remember {
        mutableStateOf(true)
    }
    var error by remember {
        mutableStateOf<String?>(null)
    }
    var updatingId by remember {
        mutableStateOf<String?>(null)
    }
    var cancelTarget by remember {
        mutableStateOf<Appointment?>(null)
    }
    var refreshVersion by remember {
        mutableIntStateOf(0)
    }
    var filter by remember {
        mutableStateOf("All")
    }

    // Reload appointments after a successful modification.
    LaunchedEffect(refreshVersion) {
        appointmentsLoading = true
        appointmentRepository.getAppointments { result ->
            result
                .onSuccess {
                    appointments = it
                    error = null
                }
                .onFailure {
                    error = it.message
                        ?: "Unable to load appointments"
                }
            appointmentsLoading = false
        }
    }

    // Fetch patient names for appointment cards.
    LaunchedEffect(Unit) {
        patientRepository.getPatients { result ->
            result
                .onSuccess { patients = it }
                .onFailure {
                    error = it.message
                        ?: "Unable to load patient names"
                }
            patientsLoading = false
        }
    }

    val byPatient = patients.associateBy { it.id }

    val visible = appointments
        .filter {
            filter == "All" ||
                    it.status.equals(filter, ignoreCase = true)
        }
        .sortedWith(
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

    // Shared status update action.
    fun updateStatus(appointment: Appointment, status: String) {
        if (updatingId != null) return

        updatingId = appointment.id
        error = null

        appointmentRepository.updateAppointmentStatus(
            appointment.id,
            status
        ) { result ->
            updatingId = null

            result
                .onSuccess { refreshVersion++ }
                .onFailure {
                    error = it.message
                        ?: "Failed to update appointment"
                }
        }
    }

    // Confirm cancellation before updating Firestore.
    cancelTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { cancelTarget = null },
            title = {
                Text("Cancel appointment?")
            },
            text = {
                Text(
                    "The appointment will remain in the patient's " +
                            "history as cancelled."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        cancelTarget = null
                        updateStatus(target, "cancelled")
                    }
                ) {
                    Text(
                        "Confirm cancellation",
                        color = colors.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { cancelTarget = null }
                ) {
                    Text("Keep appointment")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextButton(
            onClick = onBack,
            enabled = updatingId == null
        ) {
            Text("← Back to Dashboard")
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "All Appointments",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(4.dp))

        Text(
            "Appointments across your patients",
            color = colors.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        // Filter appointments without modifying Firestore data.
        Row(
            modifier = Modifier.horizontalScroll(
                rememberScrollState()
            ),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "All",
                "Scheduled",
                "Completed",
                "Cancelled"
            ).forEach { option ->
                FilterChip(
                    selected = filter == option,
                    onClick = { filter = option },
                    label = {
                        Text(option)
                    }
                )
            }
        }

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                error ?: "",
                color = colors.error
            )
        }

        Spacer(Modifier.height(10.dp))

        when {
            appointmentsLoading || patientsLoading -> {
                CircularProgressIndicator()
            }

            visible.isEmpty() -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = colors.primaryContainer
                    )
                ) {
                    Text(
                        "No matching appointments.",
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = visible,
                        key = { it.id }
                    ) { appointment ->
                        val patient = byPatient[appointment.patientId]

                        val formattedDate = remember(
                            appointment.scheduledAt
                        ) {
                            SimpleDateFormat(
                                "dd MMM yyyy, hh:mm a",
                                Locale.getDefault()
                            ).format(
                                Date(appointment.scheduledAt)
                            )
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
                                    Arrangement.spacedBy(9.dp)
                            ) {
                                Text(
                                    patient?.petName
                                        ?: "Patient unavailable",
                                    style =
                                        MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Row(
                                    verticalAlignment =
                                        Alignment.CenterVertically,
                                    horizontalArrangement =
                                        Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.DateRange,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(18.dp)
                                    )

                                    Text(formattedDate)
                                }

                                Text(
                                    "Status: ${
                                        appointment.status
                                            .replaceFirstChar {
                                                it.uppercase()
                                            }
                                    }",
                                    color = colors.primary
                                )

                                Text(
                                    "Reason: ${
                                        appointment.reason.ifBlank {
                                            "Not provided"
                                        }
                                    }"
                                )

                                if (appointment.notes.isNotBlank()) {
                                    Text("Notes: ${appointment.notes}")
                                }

                                if (patient != null) {
                                    OutlinedButton(
                                        onClick = {
                                            onPatientClick(patient)
                                        },
                                        enabled = updatingId == null,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("View Patient Profile")
                                    }
                                }

                                if (appointment.status == "scheduled") {
                                    if (updatingId == appointment.id) {
                                        LinearProgressIndicator(
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        Button(
                                            onClick = {
                                                val initial =
                                                    Calendar.getInstance()
                                                        .apply {
                                                            timeInMillis =
                                                                appointment.scheduledAt
                                                        }

                                                val picker =
                                                    DatePickerDialog(
                                                        context,
                                                        { _, year, month, day ->
                                                            val selected =
                                                                Calendar.getInstance()
                                                                    .apply {
                                                                        set(
                                                                            year,
                                                                            month,
                                                                            day,
                                                                            0,
                                                                            0,
                                                                            0
                                                                        )
                                                                        set(
                                                                            Calendar.MILLISECOND,
                                                                            0
                                                                        )
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

                                                                    if (
                                                                        selected.timeInMillis <=
                                                                        System.currentTimeMillis()
                                                                    ) {
                                                                        error =
                                                                            "Select a future date and time."
                                                                    } else if (
                                                                        updatingId == null
                                                                    ) {
                                                                        updatingId =
                                                                            appointment.id
                                                                        error = null

                                                                        appointmentRepository
                                                                            .rescheduleAppointment(
                                                                                appointment.id,
                                                                                selected.timeInMillis
                                                                            ) { result ->
                                                                                updatingId = null
                                                                                result
                                                                                    .onSuccess {
                                                                                        refreshVersion++
                                                                                    }
                                                                                    .onFailure {
                                                                                        error =
                                                                                            it.message
                                                                                                ?: "Rescheduling failed"
                                                                                    }
                                                                            }
                                                                    }
                                                                },
                                                                initial.get(
                                                                    Calendar.HOUR_OF_DAY
                                                                ),
                                                                initial.get(
                                                                    Calendar.MINUTE
                                                                ),
                                                                false
                                                            ).show()
                                                        },
                                                        initial.get(
                                                            Calendar.YEAR
                                                        ),
                                                        initial.get(
                                                            Calendar.MONTH
                                                        ),
                                                        initial.get(
                                                            Calendar.DAY_OF_MONTH
                                                        )
                                                    )

                                                picker.datePicker.minDate =
                                                    Calendar.getInstance()
                                                        .apply {
                                                            set(
                                                                Calendar.HOUR_OF_DAY,
                                                                0
                                                            )
                                                            set(
                                                                Calendar.MINUTE,
                                                                0
                                                            )
                                                            set(
                                                                Calendar.SECOND,
                                                                0
                                                            )
                                                            set(
                                                                Calendar.MILLISECOND,
                                                                0
                                                            )
                                                        }.timeInMillis

                                                picker.show()
                                            },
                                            enabled = updatingId == null,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Reschedule")
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                updateStatus(
                                                    appointment,
                                                    "completed"
                                                )
                                            },
                                            enabled = updatingId == null,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Mark Completed")
                                        }

                                        TextButton(
                                            onClick = {
                                                cancelTarget = appointment
                                            },
                                            enabled = updatingId == null,
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
                }
            }
        }
    }
}
