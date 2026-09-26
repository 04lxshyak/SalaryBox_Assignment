package com.salarybox.attendance.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StaffValidationTest {

    @Test
    fun `empty name is invalid`() {
        assertFalse(isValidName(""))
    }

    @Test
    fun `whitespace-only name is invalid`() {
        assertFalse(isValidName("   "))
    }

    @Test
    fun `valid name passes`() {
        assertTrue(isValidName("John Doe"))
    }

    @Test
    fun `empty employee ID is invalid`() {
        assertFalse(isValidEmployeeId(""))
    }

    @Test
    fun `whitespace-only employee ID is invalid`() {
        assertFalse(isValidEmployeeId("   "))
    }

    @Test
    fun `valid employee ID passes`() {
        assertTrue(isValidEmployeeId("EMP001"))
    }

    @Test
    fun `employee ID with spaces is trimmed and valid`() {
        assertTrue(isValidEmployeeId(" EMP002 "))
    }

    private fun isValidName(name: String): Boolean = name.trim().isNotEmpty()
    private fun isValidEmployeeId(id: String): Boolean = id.trim().isNotEmpty()
}
