package com.salarybox.attendance.presentation.admin.staffprofile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.salarybox.attendance.core.navigation.NavRoute
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffProfileScreen(
    navController: NavController,
    staffId: Long,
    viewModel: StaffProfileViewModel = viewModel(factory = StaffProfileViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.staff?.name ?: "Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else if (uiState.staff != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Name: ${uiState.staff!!.name}", style = MaterialTheme.typography.titleLarge)
                            Text("ID: ${uiState.staff!!.employeeId}", style = MaterialTheme.typography.bodyLarge)
                            Text("Created: ${dateFormat.format(Date(uiState.staff!!.createdAt))}")
                        }
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Enrollment Status", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            if (uiState.enrollment != null) {
                                Text("Status: Enrolled", color = MaterialTheme.colorScheme.primary)
                                Text("Samples: ${uiState.enrollment!!.sampleCount}")
                                Text("Date: ${dateFormat.format(Date(uiState.enrollment!!.enrolledAt))}")
                                Button(
                                    onClick = { navController.navigate(NavRoute.FaceEnrollment.createRoute(staffId)) },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) {
                                    Text("Re-enroll Face")
                                }
                            } else {
                                Text("Status: Not Enrolled", color = MaterialTheme.colorScheme.error)
                                Button(
                                    onClick = { navController.navigate(NavRoute.FaceEnrollment.createRoute(staffId)) },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) {
                                    Text("Enroll Face")
                                }
                            }
                        }
                    }
                }

                item {
                    Text("Recent Attendance", style = MaterialTheme.typography.titleLarge)
                }

                val recent = uiState.attendanceHistory.sortedByDescending { it.timestamp }.take(5)
                if (recent.isEmpty()) {
                    item { Text("No attendance records.") }
                } else {
                    recent.forEach { record ->
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Date: ${dateFormat.format(Date(record.timestamp))}")
                                    Text("Status: ${record.verificationStatus}")
                                    Text("Score: ${record.matchScore}")
                                }
                            }
                        }
                    }
                    item {
                        TextButton(
                            onClick = { navController.navigate(NavRoute.AttendanceHistory.createRoute(staffId)) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("View All History")
                        }
                    }
                }
            }
        }
    }
}
