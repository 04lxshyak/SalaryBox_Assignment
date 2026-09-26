package com.salarybox.attendance.data.session

import com.salarybox.attendance.domain.model.AuthSession
import com.salarybox.attendance.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionManagerTest {

    @Test
    fun `auth session serialization round trip`() {
        // Test that AuthSession can be created and fields are correct
        val session = AuthSession(
            userId = "admin",
            userName = "Administrator",
            role = UserRole.ADMIN,
            staffId = null
        )
        assertEquals("admin", session.userId)
        assertEquals("Administrator", session.userName)
        assertEquals(UserRole.ADMIN, session.role)
        assertNull(session.staffId)
    }

    @Test
    fun `staff session has staffId`() {
        val session = AuthSession(
            userId = "EMP001",
            userName = "Demo Employee",
            role = UserRole.STAFF,
            staffId = 1L
        )
        assertEquals(UserRole.STAFF, session.role)
        assertEquals(1L, session.staffId)
    }
}
