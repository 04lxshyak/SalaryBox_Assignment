package com.salarybox.attendance.domain.repository

import com.salarybox.attendance.core.util.Resource
import com.salarybox.attendance.domain.model.AuthSession
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun loginAdmin(username: String, password: String): Resource<AuthSession>
    suspend fun loginStaff(employeeId: String, password: String): Resource<AuthSession>
    fun getCurrentSession(): Flow<AuthSession?>
    suspend fun logout()
    fun isLoggedIn(): Flow<Boolean>
}
