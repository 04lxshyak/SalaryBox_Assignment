package com.salarybox.attendance.domain.model

data class StaffMember(
    val id: Long = 0,
    val employeeId: String,
    val name: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
