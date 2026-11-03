
package com.example.vetman01.ui.patients

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.vetman01.data.MedicalRecordRepository
import com.example.vetman01.model.MedicalRecord
import com.example.vetman01.model.Patient
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Medical record entry modes.
 * Both manual and OCR modes use the same MedicalRecord model.
 */
private enum class RecordEntryMode {
    SELECT, MANUAL, SCAN
}

@Composable
fun AddMedicalRecordScreen(
    patient: Patient,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    repository: MedicalRecordRepository = remember {
        MedicalRecordRepository()
    }
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current

    var entryMode by remember(patient.id) {
        mutableStateOf(RecordEntryMode.SELECT)
    }

    // Manual medical record fields.
    var symptoms by remember(patient.id) { mutableStateOf("") }
    var diagnosis by remember(patient.id) { mutableStateOf("") }
    var treatment by remember(patient.id) { mutableStateOf("") }
    var medications by remember(patient.id) { mutableStateOf("") }
    var procedures by remember(patient.id) { mutableStateOf("") }
    var notes by remember(patient.id) { mutableStateOf("") }

    var saving by remember(patient.id) { mutableStateOf(false) }
    var error by remember(patient.id) {
        mutableStateOf<String?>(null)
    }

    // Selected image from the camera or photo picker.
    var selectedImageUri by remember(patient.id) {
        mutableStateOf<Uri?>(null)
    }

    // Pending camera output. Kept until the camera returns.
    var pendingCameraUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var scanError by remember {
        mutableStateOf<String?>(null)
    }

    // Gallery selection.
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            scanError = null
        }
    }

    // Full-resolution camera capture to a FileProvider URI.
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedImageUri = pendingCameraUri
            scanError = null
        } else {
            scanError = "Photograph was not captured."
        }

        pendingCameraUri = null
    }

    val canSave = listOf(
        symptoms, diagnosis, treatment,
        medications, procedures, notes
    ).any { it.isNotBlank() } && !saving

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TextButton(
            onClick = {
                if (entryMode == RecordEntryMode.SELECT) {
                    onBack()
                } else {
                    entryMode = RecordEntryMode.SELECT
                    error = null
                }
            },
            enabled = !saving
        ) {
            Text(
                if (entryMode == RecordEntryMode.SELECT)
                    "← Back to Profile"
                else
                    "← Entry Methods"
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "New Medical Record",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(6.dp))

        Text(
            "Patient: ${patient.petName}",
            color = colors.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        when (entryMode) {
            RecordEntryMode.SELECT -> {
                Text(
                    "Choose Entry Method",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "How would you like to record this visit?",
                    color = colors.onSurfaceVariant
                )

                Spacer(Modifier.height(20.dp))

                RecordEntryCard(
                    title = "Manual Entry",
                    description = "Type clinical details directly.",
                    icon = Icons.Filled.Edit,
                    onClick = {
                        entryMode = RecordEntryMode.MANUAL
                    }
                )

                Spacer(Modifier.height(14.dp))

                RecordEntryCard(
                    title = "Scan Document",
                    description = "Capture or choose a photograph of an existing medical record.",
                    icon = Icons.Filled.DateRange,
                    onClick = {
                        entryMode = RecordEntryMode.SCAN
                    }
                )
            }

            RecordEntryMode.SCAN -> {
                Text(
                    "Scan Medical Document",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Capture a new photograph or select an existing one.",
                    color = colors.onSurfaceVariant
                )

                Spacer(Modifier.height(20.dp))

                // Both image sources feed into the same preview.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ScanSourceCard(
                        title = "Capture Image",
                        description = "Open camera",
                        icon = Icons.Filled.Add,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            try {
                                val directory = File(
                                    context.cacheDir,
                                    "medical_scans"
                                )
                                directory.mkdirs()

                                val imageFile = File.createTempFile(
                                    "vetman_scan_",
                                    ".jpg",
                                    directory
                                )

                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    imageFile
                                )

                                pendingCameraUri = uri
                                scanError = null
                                cameraLauncher.launch(uri)
                            } catch (exception: Exception) {
                                scanError = exception.message
                                    ?: "Unable to open camera."
                            }
                        }
                    )

                    ScanSourceCard(
                        title = "Choose Photograph",
                        description = "Open gallery",
                        icon = Icons.Filled.List,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            scanError = null
                            galleryLauncher.launch("image/*")
                        }
                    )
                }

                Spacer(Modifier.height(20.dp))

                ClinicalScanCard {
                    Text(
                        "Document Preview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(14.dp))

                    if (selectedImageUri == null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            color = colors.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                horizontalAlignment =
                                    Alignment.CenterHorizontally,
                                verticalArrangement =
                                    Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Filled.DateRange,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(48.dp)
                                )

                                Spacer(Modifier.height(10.dp))

                                Text(
                                    "No document selected",
                                    color = colors.onPrimaryContainer
                                )
                            }
                        }
                    } else {
                        val imageUri = selectedImageUri!!

                        // Decode a smaller preview so large camera
                        // photographs do not consume excessive memory.
                        val preview by produceState<
                                androidx.compose.ui.graphics.ImageBitmap?
                                >(
                            initialValue = null,
                            key1 = imageUri
                        ) {
                            value = withContext(Dispatchers.IO) {
                                try {
                                    val resolver = context.contentResolver

                                    val bounds = BitmapFactory.Options().apply {
                                        inJustDecodeBounds = true
                                    }

                                    resolver.openInputStream(imageUri)?.use {
                                        BitmapFactory.decodeStream(
                                            it, null, bounds
                                        )
                                    }

                                    var sampleSize = 1
                                    while (
                                        bounds.outWidth / sampleSize > 1200 ||
                                        bounds.outHeight / sampleSize > 1200
                                    ) {
                                        sampleSize *= 2
                                    }

                                    val options = BitmapFactory.Options().apply {
                                        inSampleSize = sampleSize
                                    }

                                    resolver.openInputStream(imageUri)?.use {
                                        BitmapFactory.decodeStream(
                                            it, null, options
                                        )?.asImageBitmap()
                                    }
                                } catch (_: Exception) {
                                    null
                                }
                            }
                        }

                        if (preview != null) {
                            Image(
                                bitmap = preview!!,
                                contentDescription = "Selected medical document",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text(
                                "Loading document preview...",
                                color = colors.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "Document selected",
                            color = colors.primary,
                            fontWeight = FontWeight.Medium
                        )

                        TextButton(
                            onClick = {
                                selectedImageUri = null
                            }
                        ) {
                            Text("Remove Photograph")
                        }
                    }
                }

                if (scanError != null) {
                    Spacer(Modifier.height(12.dp))

                    Text(
                        scanError ?: "",
                        color = colors.error
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Enabled when ML Kit OCR processing is implemented.
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Extract Text")
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    "OCR extraction and review will be connected next. No image or extracted information is saved yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            RecordEntryMode.MANUAL -> {
                Text(
                    "Manual Entry",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(18.dp))

                RecordFormHeading("Clinical Assessment")
                Spacer(Modifier.height(12.dp))

                ClinicalScanCard {
                    RecordTextInput(
                        "Symptoms", symptoms,
                        { symptoms = it }, !saving
                    )

                    Spacer(Modifier.height(14.dp))

                    RecordTextInput(
                        "Diagnosis", diagnosis,
                        { diagnosis = it }, !saving
                    )
                }

                Spacer(Modifier.height(24.dp))

                RecordFormHeading("Treatment & Care")
                Spacer(Modifier.height(12.dp))

                ClinicalScanCard {
                    RecordTextInput(
                        "Treatment", treatment,
                        { treatment = it }, !saving
                    )

                    Spacer(Modifier.height(14.dp))

                    RecordTextInput(
                        "Medications", medications,
                        { medications = it }, !saving
                    )

                    Spacer(Modifier.height(14.dp))

                    RecordTextInput(
                        "Procedures", procedures,
                        { procedures = it }, !saving
                    )
                }

                Spacer(Modifier.height(24.dp))

                RecordFormHeading("Additional Notes")
                Spacer(Modifier.height(12.dp))

                ClinicalScanCard {
                    RecordTextInput(
                        "Consultation Notes", notes,
                        { notes = it }, !saving, 3
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "Complete at least one clinical field to save.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )

                error?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = colors.error)
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (canSave) {
                            saving = true
                            error = null

                            // Existing manual Firestore workflow.
                            val record = MedicalRecord(
                                patientId = patient.id,
                                visitDate = System.currentTimeMillis(),
                                symptoms = symptoms.trim(),
                                diagnosis = diagnosis.trim(),
                                treatment = treatment.trim(),
                                medications = medications.trim(),
                                procedures = procedures.trim(),
                                notes = notes.trim(),
                                source = "manual"
                            )

                            repository.addMedicalRecord(
                                patientId = patient.id,
                                record = record
                            ) { result ->
                                saving = false
                                result
                                    .onSuccess { onSaved() }
                                    .onFailure {
                                        error = it.message
                                            ?: "Could not save medical record"
                                    }
                            }
                        }
                    },
                    enabled = canSave,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = colors.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Saving...")
                    } else {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Save Medical Record")
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun RecordEntryCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        ),
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = colors.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.padding(14.dp)
                )
            }

            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ScanSourceCard(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Card(
        onClick = onClick,
        modifier = modifier.height(142.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        ),
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(30.dp)
            )

            Spacer(Modifier.height(10.dp))

            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(4.dp))

            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecordFormHeading(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun ClinicalScanCard(
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
            modifier = Modifier.padding(14.dp),
            content = content
        )
    }
}

@Composable
private fun RecordTextInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    minLines: Int = 2
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        minLines = minLines,
        maxLines = 7,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    )
}
