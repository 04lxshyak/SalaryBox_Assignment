package com.salarybox.attendance.core.navigation

sealed class NavRoute(val route: String) {
    object Login : NavRoute("login")
    object AdminDashboard : NavRoute("admin_dashboard")
    object StaffList : NavRoute("staff_list")
    object AddStaff : NavRoute("add_staff")
    object StaffProfile : NavRoute("staff_profile/{staffId}") {
        fun createRoute(staffId: Long) = "staff_profile/$staffId"
    }
    object FaceEnrollment : NavRoute("face_enrollment/{staffId}") {
        fun createRoute(staffId: Long) = "face_enrollment/$staffId"
    }
    object StaffDashboard : NavRoute("staff_dashboard")
    object AttendanceMark : NavRoute("attendance_mark")
    object AttendanceHistory : NavRoute("attendance_history/{staffId}") {
        fun createRoute(staffId: Long) = "attendance_history/$staffId"
    }
}
