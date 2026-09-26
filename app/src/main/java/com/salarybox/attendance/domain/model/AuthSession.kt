package com.salarybox.attendance.domain.model

data class AuthSession(
    val userId: String,
    val userName: String,
    val role: UserRole,
    val staffId: Long? = null
)
