
package com.example.vetman01.ui.patients

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.*
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
 * Allows a veterinarian to schedule an appointment
 * for an existing patient.
 */
@Composable
fun AddAppointmentScreen(
    patient: Patient,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    repository: AppointmentRepository = remember {
        AppointmentRepository()
    }
) {
    val context = LocalContext.current

    // Holds the selected appointment date and time.
    var scheduledAt by remember { mutableLongStateOf(0L) }

    var reason by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Display the selected date in a readable format.
    val dateFormat = remember {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    }

    val formattedDate = if (scheduledAt > 0L) {
        dateFormat.format(Date(scheduledAt))
    } else {
        "No date selected"
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
            Text("< Back to profile")
        }

        Text(
            "Schedule Appointment",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(8.dp))

        Text("Patient: ${patient.petName}")

        Spacer(Modifier.height(20.dp))

        // Open the date picker, followed by the time picker.
        Button(
            onClick = {
                val calendar = Calendar.getInstance()

                DatePickerDialog(
                    context,
                    { _, year, month, day ->

                        val selected = Calendar.getInstance()
                        selected.set(year, month, day)

                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                selected.set(Calendar.HOUR_OF_DAY, hour)
                                selected.set(Calendar.MINUTE, minute)
                                selected.set(Calendar.SECOND, 0)
                                selected.set(Calendar.MILLISECOND, 0)

                                scheduledAt = selected.timeInMillis
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            false
                        ).show()
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).apply {
                    // Prevent selecting dates before today.
                    datePicker.minDate =
                        Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                }.show()
            },
            enabled = !saving,
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
            label = { Text("Reason for appointment") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Additional notes") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        error?.let { message ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(20.dp))

        // Only enable saving for a valid future appointment.
        Button(
            onClick = {
                if (scheduledAt <= System.currentTimeMillis()) {
                    error = "Select a future date and time"
                    return@Button
                }

                saving = true
                error = null

                val appointment = Appointment(
                    patientId = patient.id,
                    scheduledAt = scheduledAt,
                    reason = reason.trim(),
                    notes = notes.trim()
                )

                // Save the appointment through the repository.
                repository.addAppointment(appointment) { result ->
                    saving = false

                    result
                        .onSuccess {
                            onSaved()
                        }
                        .onFailure {
                            error = it.message
                                ?: "Could not save appointment"
                        }
                }
            },
            enabled = scheduledAt > System.currentTimeMillis() && !saving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (saving) "Saving..." else "Save Appointment"
            )
        }
    }
}
