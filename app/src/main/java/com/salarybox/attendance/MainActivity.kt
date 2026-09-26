package com.salarybox.attendance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.salarybox.attendance.core.navigation.AppNavHost
import com.salarybox.attendance.core.navigation.NavRoute
import com.salarybox.attendance.core.theme.AttendanceTheme
import com.salarybox.attendance.di.ServiceLocator
import com.salarybox.attendance.domain.model.UserRole
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AttendanceTheme {
                val navController = rememberNavController()
                var isSessionLoaded by remember { mutableStateOf(false) }
                var startDestination by remember { mutableStateOf(NavRoute.Login.route) }

                LaunchedEffect(Unit) {
                    ServiceLocator.authRepository.getCurrentSession().first().let { session ->
                        startDestination = when {
                            session == null -> NavRoute.Login.route
                            session.role == UserRole.ADMIN -> NavRoute.AdminDashboard.route
                            else -> NavRoute.StaffDashboard.route
                        }
                        isSessionLoaded = true
                    }
                }

                if (isSessionLoaded) {
                    AppNavHost(
                        navController = navController,
                        startDestination = startDestination
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
