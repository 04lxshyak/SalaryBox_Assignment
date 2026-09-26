package com.salarybox.attendance.data.database.entity

import com.salarybox.attendance.domain.model.StaffMember
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntityMappingTest {

    @Test
    fun `staff entity to domain model`() {
        val entity = StaffEntity(
            id = 1,
            employeeId = "EMP001",
            name = "Test Staff",
            password = "1234",
            isActive = true,
            createdAt = 1000L,
            updatedAt = 2000L
        )
        val model = entity.toStaffMember()
        assertEquals(1L, model.id)
        assertEquals("EMP001", model.employeeId)
        assertEquals("Test Staff", model.name)
        assertTrue(model.isActive)
        assertEquals(1000L, model.createdAt)
    }

    @Test
    fun `domain model to staff entity`() {
        val model = StaffMember(
            id = 1,
            employeeId = "EMP002",
            name = "Another Staff",
            isActive = false,
            createdAt = 1000L,
            updatedAt = 2000L
        )
        val entity = model.toEntity()
        assertEquals(1L, entity.id)
        assertEquals("EMP002", entity.employeeId)
        assertEquals("Another Staff", entity.name)
        assertFalse(entity.isActive)
    }

    @Test
    fun `face enrollment entity embedding parsing`() {
        val embedding = floatArrayOf(0.1f, 0.2f, 0.3f)
        val embeddingStr = embedding.joinToString(",")
        val parsed = embeddingStr.split(",").map { it.toFloat() }.toFloatArray()
        assertArrayEquals(embedding, parsed, 0.001f)
    }

    @Test
    fun `attendance entity mapping`() {
        val entity = AttendanceEntity(
            id = 1,
            staffId = 1L,
            timestamp = 1000L,
            selfiePath = "/path/to/selfie.jpg",
            latitude = 28.6139,
            longitude = 77.2090,
            locationAccuracy = 10.5f,
            matchScore = 0.95f,
            verificationStatus = "VERIFIED"
        )
        // Test basic field access
        assertEquals(1L, entity.staffId)
        assertEquals("/path/to/selfie.jpg", entity.selfiePath)
        assertEquals(28.6139, entity.latitude, 0.0001)
        assertEquals(0.95f, entity.matchScore, 0.001f)
    }
}
