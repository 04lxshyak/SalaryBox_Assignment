package com.salarybox.attendance.domain.model

data class AttendanceRecord(
    val id: Long = 0,
    val staffId: Long,
    val employeeId: String = "",
    val staffName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val selfiePath: String,
    val latitude: Double,
    val longitude: Double,
    val locationAccuracy: Float = 0f,
    val matchScore: Float = 0f,
    val verificationStatus: String = "VERIFIED"
)
