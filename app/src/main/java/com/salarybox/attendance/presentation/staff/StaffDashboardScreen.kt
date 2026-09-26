package com.salarybox.attendance.presentation.staff

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.salarybox.attendance.core.navigation.NavRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffDashboardScreen(
    navController: NavController,
    viewModel: StaffDashboardViewModel = viewModel(factory = StaffDashboardViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance") },
                actions = {
                    IconButton(onClick = {
                        viewModel.logout()
                        navController.navigate(NavRoute.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                Text("Welcome, ${uiState.staff?.name ?: "User"}", style = MaterialTheme.typography.headlineMedium)

                if (!uiState.isEnrolled) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(
                            text = "Please ask admin to enroll your face for attendance.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                Button(
                    onClick = { navController.navigate(NavRoute.AttendanceMark.route) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.isEnrolled
                ) {
                    Text("Mark Attendance")
                }

                FilledTonalButton(
                    onClick = { uiState.staff?.id?.let { navController.navigate(NavRoute.AttendanceHistory.createRoute(it)) } },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View History")
                }
                
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Today's Attendance", style = MaterialTheme.typography.titleMedium)
                        if (uiState.todayAttendance.isEmpty()) {
                            Text("No attendance marked today.")
                        } else {
                            uiState.todayAttendance.forEach { record ->
                                Text("Status: ${record.verificationStatus} at ${java.util.Date(record.timestamp)}")
                            }
                        }
                    }
                }
            }
        }
    }
}
