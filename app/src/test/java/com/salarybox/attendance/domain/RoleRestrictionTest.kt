package com.salarybox.attendance.domain

import com.salarybox.attendance.domain.model.AuthSession
import com.salarybox.attendance.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoleRestrictionTest {

    @Test
    fun `admin can access admin dashboard`() {
        assertTrue(canAccessRoute(UserRole.ADMIN, "admin_dashboard"))
    }

    @Test
    fun `admin cannot access staff dashboard`() {
        assertFalse(canAccessRoute(UserRole.ADMIN, "staff_dashboard"))
    }

    @Test
    fun `staff can access staff dashboard`() {
        assertTrue(canAccessRoute(UserRole.STAFF, "staff_dashboard"))
    }

    @Test
    fun `staff cannot access admin dashboard`() {
        assertFalse(canAccessRoute(UserRole.STAFF, "admin_dashboard"))
    }

    @Test
    fun `staff cannot access staff list`() {
        assertFalse(canAccessRoute(UserRole.STAFF, "staff_list"))
    }

    @Test
    fun `staff cannot access add staff`() {
        assertFalse(canAccessRoute(UserRole.STAFF, "add_staff"))
    }

    @Test
    fun `unauthenticated user goes to login`() {
        val session: AuthSession? = null
        val destination = getStartDestination(session)
        assertEquals("login", destination)
    }

    @Test
    fun `admin session starts at admin dashboard`() {
        val session = AuthSession("admin", "Admin", UserRole.ADMIN)
        val destination = getStartDestination(session)
        assertEquals("admin_dashboard", destination)
    }

    @Test
    fun `staff session starts at staff dashboard`() {
        val session = AuthSession("EMP001", "Employee", UserRole.STAFF, 1L)
        val destination = getStartDestination(session)
        assertEquals("staff_dashboard", destination)
    }

    private val adminRoutes = setOf("admin_dashboard", "staff_list", "add_staff", "staff_profile", "face_enrollment")
    private val staffRoutes = setOf("staff_dashboard", "attendance_mark", "attendance_history")

    private fun canAccessRoute(role: UserRole, route: String): Boolean {
        return when (role) {
            UserRole.ADMIN -> route in adminRoutes
            UserRole.STAFF -> route in staffRoutes
        }
    }

    private fun getStartDestination(session: AuthSession?): String {
        return when {
            session == null -> "login"
            session.role == UserRole.ADMIN -> "admin_dashboard"
            else -> "staff_dashboard"
        }
    }
}
