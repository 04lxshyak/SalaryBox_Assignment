package com.salarybox.attendance.presentation.admin.stafflist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.salarybox.attendance.core.navigation.NavRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffListScreen(
    navController: NavController,
    viewModel: StaffListViewModel = viewModel(factory = StaffListViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Staff Members") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(NavRoute.AddStaff.route) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Staff")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search by name or ID") },
                singleLine = true
            )

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.staffList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No staff members found.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.staffList) { staff ->
                        StaffCard(
                            staff = staff,
                            onClick = {
                                navController.navigate(NavRoute.StaffProfile.createRoute(staff.staffId))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StaffCard(staff: StaffListItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = staff.name, style = MaterialTheme.typography.titleMedium)
                Text(text = "ID: ${staff.employeeId}", style = MaterialTheme.typography.bodyMedium)
                if (staff.latestAttendance != null) {
                    Text(
                        text = "Last: ${staff.latestAttendance}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (staff.isEnrolled) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Enrolled", tint = Color(0xFF34A853))
            } else {
                Icon(Icons.Default.Clear, contentDescription = "Not Enrolled", tint = Color(0xFFEA4335))
            }
        }
    }
}
