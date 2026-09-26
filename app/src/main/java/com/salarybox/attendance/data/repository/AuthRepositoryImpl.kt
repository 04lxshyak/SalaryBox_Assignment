package com.salarybox.attendance.data.repository

import com.salarybox.attendance.core.util.Resource
import com.salarybox.attendance.data.database.dao.StaffDao
import com.salarybox.attendance.data.session.SessionManager
import com.salarybox.attendance.domain.model.AuthSession
import com.salarybox.attendance.domain.model.UserRole
import com.salarybox.attendance.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AuthRepositoryImpl(
    private val staffDao: StaffDao,
    private val sessionManager: SessionManager
) : AuthRepository {

    override suspend fun loginAdmin(username: String, password: String): Resource<AuthSession> = withContext(Dispatchers.IO) {
        if (username == "admin" && password == "admin123") {
            val session = AuthSession(
                userId = "admin",
                userName = "Administrator",
                role = UserRole.ADMIN,
                staffId = null
            )
            sessionManager.saveSession(session)
            Resource.Success(session)
        } else {
            Resource.Error("Invalid admin credentials")
        }
    }

    override suspend fun loginStaff(employeeId: String, password: String): Resource<AuthSession> = withContext(Dispatchers.IO) {
        val staffEntity = staffDao.authenticateStaff(employeeId, password)
        if (staffEntity != null) {
            if (!staffEntity.isActive) {
                return@withContext Resource.Error("Staff account is disabled")
            }
            val session = AuthSession(
                userId = staffEntity.employeeId,
                userName = staffEntity.name,
                role = UserRole.STAFF,
                staffId = staffEntity.id
            )
            sessionManager.saveSession(session)
            Resource.Success(session)
        } else {
            Resource.Error("Invalid employee credentials")
        }
    }

    override fun getCurrentSession(): Flow<AuthSession?> {
        return sessionManager.getSession()
    }

    override suspend fun logout() = withContext(Dispatchers.IO) {
        sessionManager.clearSession()
    }

    override fun isLoggedIn(): Flow<Boolean> {
        return sessionManager.isLoggedIn()
    }
}
