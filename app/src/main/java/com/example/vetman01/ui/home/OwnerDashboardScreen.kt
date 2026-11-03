
package com.example.vetman01.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.example.vetman01.data.OwnerLinkApprovalRepository
import com.example.vetman01.data.OwnerLinkRequest
import com.example.vetman01.data.PatientRepository
import com.example.vetman01.model.Patient
import com.example.vetman01.ui.patients.OwnerPatientProfileScreen
import com.google.firebase.auth.FirebaseAuth

/**
 * Pet-owner dashboard.
 *
 * Displays:
 * - Pending invitations from veterinarians
 * - Pets already linked to this account
 * - Searchable patient cards
 *
 * Owners cannot edit clinical records here.
 */
@Composable
fun OwnerDashboardScreen(
    onSignOut: () -> Unit,
    patientRepository: PatientRepository = remember {
        PatientRepository()
    },
    linkRepository: OwnerLinkApprovalRepository = remember {
        OwnerLinkApprovalRepository()
    }
) {
    val colors = MaterialTheme.colorScheme

    var patients by remember {
        mutableStateOf<List<Patient>>(emptyList())
    }

    var requests by remember {
        mutableStateOf<List<OwnerLinkRequest>>(emptyList())
    }

    var patientsLoading by remember {
        mutableStateOf(true)
    }

    var requestsLoading by remember {
        mutableStateOf(false)
    }

    var patientsError by remember {
        mutableStateOf<String?>(null)
    }

    var requestsError by remember {
        mutableStateOf<String?>(null)
    }

    var message by remember {
        mutableStateOf<String?>(null)
    }

    var selectedPatient by remember {
        mutableStateOf<Patient?>(null)
    }

    var selectedRequest by remember {
        mutableStateOf<OwnerLinkRequest?>(null)
    }

    var pendingAction by remember {
        mutableStateOf<String?>(null)
    }

    var search by remember {
        mutableStateOf("")
    }

    var refreshVersion by remember {
        mutableIntStateOf(0)
    }

    var verified by remember {
        mutableStateOf(
            FirebaseAuth.getInstance()
                .currentUser?.isEmailVerified == true
        )
    }

    var verifying by remember {
        mutableStateOf(false)
    }

    /**
     * Reload linked patients after a request is accepted.
     *
     * The existing repository queries patients using the
     * authenticated owner's UID.
     */
    LaunchedEffect(refreshVersion) {
        patientsLoading = true
        patientsError = null

        patientRepository.getOwnerPatients { result ->
            result
                .onSuccess {
                    patients = it
                }
                .onFailure {
                    patientsError = it.message
                        ?: "Could not load linked pets."
                }

            patientsLoading = false
        }

        if (verified) {
            requestsLoading = true
            requestsError = null

            linkRepository.getPendingRequests { result ->
                result
                    .onSuccess {
                        requests = it
                    }
                    .onFailure {
                        requestsError = it.message
                            ?: "Could not load linking requests."
                    }

                requestsLoading = false
            }
        } else {
            requests = emptyList()
        }
    }

    BackHandler(enabled = selectedPatient != null) {
        selectedPatient = null
    }

    val activePatient = selectedPatient

    if (activePatient != null) {
        OwnerPatientProfileScreen(
            patient = activePatient,
            onBack = {
                selectedPatient = null
            }
        )
        return
    }

    /**
     * A final confirmation prevents accidental acceptance
     * or rejection of an invitation.
     */
    selectedRequest?.let { request ->
        AlertDialog(
            onDismissRequest = {
                if (pendingAction == null) {
                    selectedRequest = null
                }
            },
            title = {
                Text("Confirm Pet Linking")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Pet: ${request.petName.ifBlank { "Unknown pet" }}"
                    )

                    Text(
                        listOf(
                            request.species,
                            request.breed
                        ).filter {
                            it.isNotBlank()
                        }.joinToString(" · ")
                    )

                    Text(
                        "Accepting will allow this pet to appear " +
                                "in your Vetman account."
                    )

                    if (pendingAction != null) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = pendingAction == null,
                    onClick = {
                        pendingAction = request.id

                        // Update the pet and invitation atomically.
                        linkRepository.acceptRequest(request) { result ->
                            pendingAction = null
                            selectedRequest = null

                            result
                                .onSuccess {
                                    message = "Pet linked successfully."
                                    refreshVersion++
                                }
                                .onFailure {
                                    requestsError = it.message
                                        ?: "Could not accept request."
                                }
                        }
                    }
                ) {
                    Text("Accept & Link")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = pendingAction == null,
                    onClick = {
                        selectedRequest = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    val visiblePatients = remember(patients, search) {
        patients.filter {
            search.isBlank() ||
                    it.petName.contains(
                        search.trim(),
                        ignoreCase = true
                    ) ||
                    it.species.contains(
                        search.trim(),
                        ignoreCase = true
                    ) ||
                    it.breed.contains(
                        search.trim(),
                        ignoreCase = true
                    )
        }.sortedBy {
            it.petName.lowercase()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // HEADER
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "My Pets",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "Your veterinary dashboard",
                        color = colors.onSurfaceVariant
                    )
                }

                TextButton(onClick = onSignOut) {
                    Text("Sign Out")
                }
            }
        }

        // LINKED PETS SUMMARY
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = colors.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "🐾",
                        style = MaterialTheme.typography.headlineLarge
                    )

                    Column {
                        Text(
                            if (patientsLoading) {
                                "—"
                            } else {
                                patients.size.toString()
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onPrimaryContainer
                        )

                        Text(
                            "Linked Pets",
                            color = colors.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // EMAIL VERIFICATION
        if (!verified) {
            item {
                ClinicalOwnerDashboardCard {
                    Text(
                        "Verify Your Email",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        "Verify your registered email to receive " +
                                "pet-linking requests from veterinarians.",
                        color = colors.onSurfaceVariant
                    )

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !verifying,
                        onClick = {
                            verifying = true

                            linkRepository.sendVerificationEmail {
                                    result ->
                                verifying = false

                                result
                                    .onSuccess {
                                        message = "Verification email sent."
                                    }
                                    .onFailure {
                                        requestsError = it.message
                                            ?: "Could not send verification email."
                                    }
                            }
                        }
                    ) {
                        Text("Send Verification Email")
                    }

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !verifying,
                        onClick = {
                            verifying = true

                            linkRepository.refreshVerification {
                                    result ->
                                verifying = false

                                result
                                    .onSuccess { isVerified ->
                                        verified = isVerified

                                        if (isVerified) {
                                            message = "Email verified."
                                            refreshVersion++
                                        } else {
                                            message = "Email is not verified yet."
                                        }
                                    }
                                    .onFailure {
                                        requestsError = it.message
                                            ?: "Could not refresh verification."
                                    }
                            }
                        }
                    ) {
                        Text(
                            if (verifying) {
                                "Checking..."
                            } else {
                                "I've Verified My Email"
                            }
                        )
                    }
                }
            }
        }

        // LINKING REQUESTS
        if (verified) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Pending Linking Requests",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primary
                    )

                    TextButton(
                        onClick = {
                            refreshVersion++
                        }
                    ) {
                        Text("Refresh")
                    }
                }
            }

            if (requestsLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (requests.isEmpty() &&
                requestsError == null
            ) {
                item {
                    OwnerDashboardNotice(
                        "No pending linking requests."
                    )
                }
            }

            items(
                items = requests,
                key = { "request_${it.id}" }
            ) { request ->
                ClinicalOwnerDashboardCard {
                    Text(
                        request.petName.ifBlank {
                            "Pet awaiting approval"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        listOf(
                            request.species,
                            request.breed
                        ).filter {
                            it.isNotBlank()
                        }.joinToString(" · "),
                        color = colors.onSurfaceVariant
                    )

                    Text(
                        "A veterinarian has requested to " +
                                "link this pet to your account.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            enabled = pendingAction == null,
                            onClick = {
                                pendingAction = request.id

                                linkRepository.declineRequest(request) {
                                        result ->
                                    pendingAction = null

                                    result
                                        .onSuccess {
                                            message = "Request declined."
                                            refreshVersion++
                                        }
                                        .onFailure {
                                            requestsError = it.message
                                                ?: "Could not decline request."
                                        }
                                }
                            }
                        ) {
                            Text("Decline")
                        }

                        Button(
                            modifier = Modifier.weight(1f),
                            enabled = pendingAction == null,
                            onClick = {
                                selectedRequest = request
                            }
                        ) {
                            Text("Accept")
                        }
                    }
                }
            }
        }

        // FEEDBACK
        message?.let { value ->
            item {
                OwnerDashboardNotice(value)
            }
        }

        requestsError?.let { value ->
            item {
                Text(
                    value,
                    color = colors.error
                )
            }
        }

        patientsError?.let { value ->
            item {
                Text(
                    value,
                    color = colors.error
                )
            }
        }

        // LINKED PETS SEARCH
        item {
            Text(
                "Your Patients",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.primary
            )
        }

        item {
            OutlinedTextField(
                value = search,
                onValueChange = {
                    search = it
                },
                label = {
                    Text("Search Pets")
                },
                placeholder = {
                    Text("Name, species or breed")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        if (patientsLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (visiblePatients.isEmpty()) {
            item {
                OwnerDashboardNotice(
                    if (patients.isEmpty()) {
                        "No linked pets yet."
                    } else {
                        "No matching pets found."
                    }
                )
            }
        } else {
            items(
                items = visiblePatients,
                key = { "pet_${it.id}" }
            ) { patient ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedPatient = patient
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = colors.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        colors.outlineVariant
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(60.dp),
                            color = colors.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "🐾",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                patient.petName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                patient.species,
                                color = colors.onSurfaceVariant
                            )

                            Text(
                                patient.breed.ifBlank {
                                    "Breed not specified"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }

                        Text(
                            "›",
                            style = MaterialTheme.typography.headlineMedium,
                            color = colors.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reusable Minimal Clinical card.
 *
 * The same surface, border and shape are applied
 * to verification and invitation sections.
 */
@Composable
private fun ClinicalOwnerDashboardCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

/**
 * Non-interactive status/empty-state card.
 */
@Composable
private fun OwnerDashboardNotice(
    message: String
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colors.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = colors.onPrimaryContainer
        )
    }
}
