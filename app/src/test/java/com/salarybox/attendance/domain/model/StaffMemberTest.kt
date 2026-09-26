package com.salarybox.attendance.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StaffMemberTest {

    @Test
    fun `staff member created with defaults`() {
        val staff = StaffMember(employeeId = "EMP001", name = "Test")
        assertEquals(0L, staff.id)
        assertEquals("EMP001", staff.employeeId)
        assertEquals("Test", staff.name)
        assertTrue(staff.isActive)
    }

    @Test
    fun `staff member equality`() {
        val staff1 = StaffMember(id = 1, employeeId = "EMP001", name = "Test", createdAt = 100, updatedAt = 100)
        val staff2 = StaffMember(id = 1, employeeId = "EMP001", name = "Test", createdAt = 100, updatedAt = 100)
        assertEquals(staff1, staff2)
    }
}
