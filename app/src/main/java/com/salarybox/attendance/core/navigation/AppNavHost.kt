package com.salarybox.attendance.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.salarybox.attendance.presentation.admin.addstaff.AddStaffScreen
import com.salarybox.attendance.presentation.admin.dashboard.AdminDashboardScreen
import com.salarybox.attendance.presentation.admin.enrollment.FaceEnrollmentScreen
import com.salarybox.attendance.presentation.admin.stafflist.StaffListScreen
import com.salarybox.attendance.presentation.admin.staffprofile.StaffProfileScreen
import com.salarybox.attendance.presentation.auth.LoginScreen
import com.salarybox.attendance.presentation.staff.AttendanceHistoryScreen
import com.salarybox.attendance.presentation.staff.AttendanceMarkScreen
import com.salarybox.attendance.presentation.staff.StaffDashboardScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = NavRoute.Login.route
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(NavRoute.Login.route) {
            LoginScreen(navController = navController)
        }
        composable(NavRoute.AdminDashboard.route) {
            AdminDashboardScreen(navController = navController)
        }
        composable(NavRoute.StaffList.route) {
            StaffListScreen(navController = navController)
        }
        composable(NavRoute.AddStaff.route) {
            AddStaffScreen(navController = navController)
        }
        composable(
            route = NavRoute.StaffProfile.route,
            arguments = listOf(navArgument("staffId") { type = NavType.LongType })
        ) { backStackEntry ->
            val staffId = backStackEntry.arguments?.getLong("staffId") ?: 0L
            StaffProfileScreen(navController = navController, staffId = staffId)
        }
        composable(
            route = NavRoute.FaceEnrollment.route,
            arguments = listOf(navArgument("staffId") { type = NavType.LongType })
        ) { backStackEntry ->
            val staffId = backStackEntry.arguments?.getLong("staffId") ?: 0L
            FaceEnrollmentScreen(navController = navController, staffId = staffId)
        }
        composable(NavRoute.StaffDashboard.route) {
            StaffDashboardScreen(navController = navController)
        }
        composable(NavRoute.AttendanceMark.route) {
            AttendanceMarkScreen(navController = navController)
        }
        composable(
            route = NavRoute.AttendanceHistory.route,
            arguments = listOf(navArgument("staffId") { type = NavType.LongType })
        ) { backStackEntry ->
            val staffId = backStackEntry.arguments?.getLong("staffId") ?: 0L
            AttendanceHistoryScreen(navController = navController, staffId = staffId)
        }
    }
}
