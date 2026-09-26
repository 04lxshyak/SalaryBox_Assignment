package com.salarybox.attendance.presentation.staff

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.salarybox.attendance.camera.CameraPreviewView
import com.salarybox.attendance.camera.FaceOvalGuideOverlay
import com.salarybox.attendance.camera.rememberCameraCaptureController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceMarkScreen(
    navController: NavController,
    viewModel: AttendanceMarkViewModel = viewModel(factory = AttendanceMarkViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cameraCaptureController = rememberCameraCaptureController()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mark Attendance") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // SUCCESS STATE: Show Full Details Screen
                uiState.step == AttendanceProcessStep.SUCCESS && uiState.successRecord != null -> {
                    AttendanceSuccessView(
                        uiState = uiState,
                        onDone = { navController.navigateUp() }
                    )
                }

                // NOT ENROLLED ERROR STATE
                !uiState.isEnrolled -> {
                    NotEnrolledView(
                        onBack = { navController.navigateUp() }
                    )
                }

                // CAMERA & VERIFICATION FLOW
                else -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // 1. Camera Preview with front camera
                        CameraPreviewView(
                            modifier = Modifier.fillMaxSize(),
                            controller = cameraCaptureController,
                            overlayContent = {
                                val borderColor = when (uiState.step) {
                                    AttendanceProcessStep.ERROR -> MaterialTheme.colorScheme.error
                                    AttendanceProcessStep.SUCCESS -> Color(0xFF34A853)
                                    AttendanceProcessStep.DETECTING_FACE,
                                    AttendanceProcessStep.CHECKING_IMAGE,
                                    AttendanceProcessStep.GENERATING_EMBEDDING,
                                    AttendanceProcessStep.VERIFYING_IDENTITY -> MaterialTheme.colorScheme.primary
                                    else -> Color.White.copy(alpha = 0.8f)
                                }

                                FaceOvalGuideOverlay(
                                    borderColor = borderColor,
                                    strokeWidthDp = if (uiState.isProcessing) 4f else 2.5f
                                )
                            },
                            onCameraError = { errorMsg ->
                                // handled in overlay or UI state
                            }
                        )

                        // 2. Top Instructions Banner
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .align(Alignment.TopCenter),
                            colors = CardDefaults.cardColors(
                                containerColor = if (uiState.step == AttendanceProcessStep.ERROR) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
                                }
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = uiState.errorMessage ?: uiState.statusMessage,
                                color = if (uiState.step == AttendanceProcessStep.ERROR) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 16.dp)
                            )
                        }

                        // 3. Processing Overlay
                        AnimatedVisibility(
                            visible = uiState.isProcessing,
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                                    Text(
                                        text = uiState.statusMessage,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // 4. Bottom Controls
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (uiState.step == AttendanceProcessStep.ERROR) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { navController.navigateUp() },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Cancel")
                                        }
                                        Button(
                                            onClick = { viewModel.retry() },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Try Again")
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            cameraCaptureController.capturePhoto(
                                                onSuccess = { bitmap ->
                                                    viewModel.processAttendanceCapture(bitmap)
                                                },
                                                onError = {
                                                    // error handled by viewModel
                                                }
                                            )
                                        },
                                        enabled = !uiState.isProcessing && uiState.step == AttendanceProcessStep.READY,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (uiState.isProcessing) "Verifying..." else "Verify & Mark Attendance",
                                            style = MaterialTheme.typography.titleMedium
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

@Composable
private fun AttendanceSuccessView(
    uiState: AttendanceMarkUiState,
    onDone: () -> Unit
) {
    val record = uiState.successRecord ?: return
    val dateFormat = SimpleDateFormat("EEEE, dd MMM yyyy 'at' hh:mm:ss a", Locale.getDefault())
    val formattedTime = dateFormat.format(Date(record.timestamp))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFF34A853).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = Color(0xFF34A853),
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Attendance Marked!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your identity and location were verified successfully.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailRow(label = "Employee", value = "${record.staffName} (${record.employeeId})")
                DetailRow(label = "Time", value = formattedTime)
                DetailRow(
                    label = "Identity Verification",
                    value = "%s (Confidence: %.1f%%)".format(
                        record.verificationStatus,
                        record.matchScore * 100f
                    )
                )
                DetailRow(
                    label = "Location",
                    value = uiState.locationDescription ?: "Lat: %.4f, Lng: %.4f".format(record.latitude, record.longitude)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Dashboard")
        }
    }
}

@Composable
private fun NotEnrolledView(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Not Enrolled",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Face Not Enrolled",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Please ask an administrator to enroll your face.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        FilledTonalButton(onClick = onBack) {
            Text("Back to Dashboard")
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}
