
package com.example.vetman01.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vetman01.data.AppointmentRepository
import com.example.vetman01.data.PatientRepository
import com.example.vetman01.model.Appointment
import com.example.vetman01.model.Patient
import com.example.vetman01.ui.patients.OwnerPatientProfileScreen
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Minimal Clinical palette.
private val ClinicalGreen = Color(0xFF176B54)
private val ClinicalLight = Color(0xFFEAF6F0)
private val ClinicalText = Color(0xFF182720)
private val ClinicalMuted = Color(0xFF64736B)
private val ClinicalBorder = Color(0xFFE2EAE5)

/**
 * Custom Vetman paw icon.
 *
 * Drawn as a vector using Compose Canvas.
 * No external images or icon dependencies required.
 */
@Composable
private fun PawIcon(
    modifier: Modifier = Modifier,
    color: Color = ClinicalGreen
) {
    Canvas(modifier = modifier.size(26.dp)) {
        drawPaw(color)
    }
}

private fun DrawScope.drawPaw(color: Color) {
    val w = size.width
    val h = size.height

    // Four paw toes.
    drawCircle(
        color = color,
        radius = w * 0.105f,
        center = androidx.compose.ui.geometry.Offset(w * 0.20f, h * 0.30f)
    )

    drawCircle(
        color = color,
        radius = w * 0.115f,
        center = androidx.compose.ui.geometry.Offset(w * 0.40f, h * 0.17f)
    )

    drawCircle(
        color = color,
        radius = w * 0.115f,
        center = androidx.compose.ui.geometry.Offset(w * 0.63f, h * 0.17f)
    )

    drawCircle(
        color = color,
        radius = w * 0.105f,
        center = androidx.compose.ui.geometry.Offset(w * 0.83f, h * 0.30f)
    )

    // Main paw pad.
    drawOval(
        color = color,
        topLeft = androidx.compose.ui.geometry.Offset(
            w * 0.23f,
            h * 0.43f
        ),
        size = androidx.compose.ui.geometry.Size(
            w * 0.55f,
            h * 0.42f
        )
    )
}

/**
 * Veterinarian dashboard.
 *
 * Displays real patient and appointment data.
 * Uses Material icons and Vetman's custom paw icon.
 */
@Composable
fun VetHomeScreen(
    onAddPatient: () -> Unit,
    onViewPatients: () -> Unit,
    onPatientClick: (Patient) -> Unit,
    onSignOut: () -> Unit
) {
    val patientRepository = remember { PatientRepository() }
    val appointmentRepository = remember { AppointmentRepository() }

    var patients by remember {
        mutableStateOf<List<Patient>>(emptyList())
    }

    var appointments by remember {
        mutableStateOf<List<Appointment>>(emptyList())
    }

    var patientsLoading by remember { mutableStateOf(true) }
    var appointmentsLoading by remember { mutableStateOf(true) }

    var patientsError by remember {
        mutableStateOf<String?>(null)
    }

    var appointmentsError by remember {
        mutableStateOf<String?>(null)
    }

    // Retrieve dashboard data from existing repositories.
    LaunchedEffect(Unit) {
        patientRepository.getPatients { result ->
            result.onSuccess {
                patients = it
                patientsError = null
            }.onFailure {
                patientsError = it.message ?: "Could not load patients"
            }

            patientsLoading = false
        }

        appointmentRepository.getAppointments { result ->
            result.onSuccess {
                appointments = it
                appointmentsError = null
            }.onFailure {
                appointmentsError =
                    it.message ?: "Could not load appointments"
            }

            appointmentsLoading = false
        }
    }

    val now = System.currentTimeMillis()

    val todayStart = remember(now) {
        Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val tomorrowStart = remember(todayStart) {
        Calendar.getInstance().apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
    }

    val upcoming = appointments
        .filter {
            it.status == "scheduled" &&
                    it.scheduledAt >= now
        }
        .sortedBy { it.scheduledAt }

    val todayCount = appointments.count {
        it.status == "scheduled" &&
                it.scheduledAt >= todayStart &&
                it.scheduledAt < tomorrowStart
    }

    val patientsById = patients.associateBy { it.id }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        // APP HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PawIcon(
                    modifier = Modifier.size(30.dp)
                )

                Column {
                    Text(
                        text = "Vetman",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = ClinicalGreen
                    )

                    Text(
                        text = "Veterinarian Dashboard",
                        style = MaterialTheme.typography.bodySmall,
                        color = ClinicalMuted
                    )
                }
            }

            TextButton(onClick = onSignOut) {
                Text("Sign out")
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = "Welcome back",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = ClinicalText
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Here's your clinic overview.",
            color = ClinicalMuted,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(24.dp))

        // DASHBOARD METRICS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DashboardMetric(
                value = if (patientsLoading) "—"
                else patients.size.toString(),
                label = "Patients",
                icon = Icons.Filled.Person,
                modifier = Modifier.weight(1f)
            )

            DashboardMetric(
                value = if (appointmentsLoading) "—"
                else todayCount.toString(),
                label = "Today",
                icon = Icons.Filled.DateRange,
                modifier = Modifier.weight(1f)
            )

            DashboardMetric(
                value = if (appointmentsLoading) "—"
                else upcoming.size.toString(),
                label = "Upcoming",
                icon = Icons.Filled.CheckCircle,
                modifier = Modifier.weight(1f)
            )
        }

        if (patientsError != null) {
            Spacer(Modifier.height(8.dp))

            Text(
                text = patientsError ?: "",
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Upcoming Appointments",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = ClinicalText
        )

        Spacer(Modifier.height(12.dp))

        when {
            appointmentsLoading -> {
                CircularProgressIndicator(color = ClinicalGreen)
            }

            appointmentsError != null -> {
                Text(
                    text = appointmentsError ?: "",
                    color = MaterialTheme.colorScheme.error
                )
            }

            upcoming.isEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = ClinicalLight
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "No upcoming appointments.",
                        modifier = Modifier.padding(20.dp),
                        color = ClinicalMuted
                    )
                }
            }

            else -> {
                upcoming.take(5).forEach { appointment ->
                    val patient =
                        patientsById[appointment.patientId]

                    DashboardAppointmentCard(
                        appointment = appointment,
                        patient = patient,
                        onClick = {
                            if (patient != null) {
                                onPatientClick(patient)
                            }
                        }
                    )

                    Spacer(Modifier.height(10.dp))
                }

                if (upcoming.size > 5) {
                    Text(
                        text = "${upcoming.size - 5} more upcoming appointments",
                        color = ClinicalMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = ClinicalText
        )

        Spacer(Modifier.height(12.dp))

        // Actions reuse existing navigation callbacks.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DashboardAction(
                title = "Add Patient",
                icon = Icons.Filled.Add,
                onClick = onAddPatient,
                modifier = Modifier.weight(1f)
            )

            DashboardAction(
                title = "My Patients",
                icon = Icons.Filled.Person,
                onClick = onViewPatients,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DashboardAction(
                title = "Appointments",
                icon = Icons.Filled.DateRange,
                onClick = onViewPatients,
                modifier = Modifier.weight(1f)
            )

            DashboardAction(
                title = "Billing",
                icon = Icons.Filled.List,
                onClick = onViewPatients,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Select a patient to manage appointments or billing.",
            color = ClinicalMuted,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * A statistic card with a real vector icon.
 */
@Composable
private fun DashboardMetric(
    value: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = ClinicalLight
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ClinicalGreen,
                modifier = Modifier.size(23.dp)
            )

            Text(
                text = value,
                color = ClinicalText,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = label,
                color = ClinicalMuted,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/**
 * Upcoming appointment card.
 * Opens the associated patient profile when selected.
 */
@Composable
private fun DashboardAppointmentCard(
    appointment: Appointment,
    patient: Patient?,
    onClick: () -> Unit
) {
    val date = remember(appointment.scheduledAt) {
        SimpleDateFormat(
            "dd MMM, hh:mm a",
            Locale.getDefault()
        ).format(Date(appointment.scheduledAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (patient != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, ClinicalBorder),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = ClinicalLight
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PawIcon(
                        modifier = Modifier.size(27.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = patient?.petName ?: "Patient unavailable",
                    fontWeight = FontWeight.SemiBold,
                    color = ClinicalText
                )

                Text(
                    text = appointment.reason.ifBlank {
                        "Veterinary appointment"
                    },
                    color = ClinicalMuted,
                    style = MaterialTheme.typography.bodySmall
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = null,
                        tint = ClinicalGreen,
                        modifier = Modifier.size(15.dp)
                    )

                    Text(
                        text = date,
                        color = ClinicalGreen,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

/**
 * Quick action with a Material vector icon.
 */
@Composable
private fun DashboardAction(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, ClinicalBorder),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ClinicalGreen,
                modifier = Modifier.size(29.dp)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = ClinicalText
            )
        }
    }
}

/**
 * Existing pet-owner dashboard.
 * Preserved without changing navigation or record access.
 */
@Composable
fun OwnerHomeScreen(
    onSignOut: () -> Unit
) {
    val repository = remember { PatientRepository() }
    val ownerId = FirebaseAuth.getInstance().currentUser?.uid

    var patients by remember(ownerId) {
        mutableStateOf<List<Patient>>(emptyList())
    }

    var selectedPatient by remember(ownerId) {
        mutableStateOf<Patient?>(null)
    }

    var isLoading by remember(ownerId) {
        mutableStateOf(true)
    }

    var errorMessage by remember(ownerId) {
        mutableStateOf<String?>(null)
    }

    // Load pets linked to the signed-in owner.
    LaunchedEffect(ownerId) {
        isLoading = true
        errorMessage = null

        repository.getOwnerPatients { result ->
            result
                .onSuccess { loadedPatients ->
                    patients = loadedPatients
                    errorMessage = null
                }
                .onFailure { exception ->
                    errorMessage = exception.localizedMessage
                        ?: "Unable to load your pets"
                }

            isLoading = false
        }
    }

    // Android Back returns from pet profile to pet list.
    BackHandler(enabled = selectedPatient != null) {
        selectedPatient = null
    }

    if (selectedPatient != null) {
        OwnerPatientProfileScreen(
            patient = selectedPatient!!,
            onBack = {
                selectedPatient = null
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                "My Pets",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "View your registered pets",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(24.dp))

            when {
                isLoading -> CircularProgressIndicator()

                errorMessage != null -> {
                    Text(
                        text = errorMessage ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                patients.isEmpty() -> {
                    Text(
                        "No pets linked to your account yet.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = patients,
                            key = { it.id }
                        ) { patient ->
                            OwnerPetCard(
                                patient = patient,
                                onClick = {
                                    selectedPatient = patient
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign out")
            }
        }
    }
}

/**
 * Clickable pet card for owners with one or more pets.
 */
@Composable
private fun OwnerPetCard(
    patient: Patient,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = patient.petName,
                style = MaterialTheme.typography.titleLarge
            )

            Text("Species: ${patient.species}")

            if (patient.breed.isNotBlank()) {
                Text("Breed: ${patient.breed}")
            }

            if (patient.age.isNotBlank()) {
                Text("Age: ${patient.age}")
            }

            Text(
                text = "Tap to view profile",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
