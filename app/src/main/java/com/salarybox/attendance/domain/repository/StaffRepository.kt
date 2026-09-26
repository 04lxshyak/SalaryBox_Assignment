package com.salarybox.attendance.domain.repository

import com.salarybox.attendance.domain.model.FaceEnrollment
import com.salarybox.attendance.domain.model.StaffMember
import kotlinx.coroutines.flow.Flow

interface StaffRepository {
    fun getAllStaff(): Flow<List<StaffMember>>
    suspend fun getStaffById(id: Long): StaffMember?
    suspend fun getStaffByEmployeeId(employeeId: String): StaffMember?
    suspend fun addStaff(staff: StaffMember): Long
    suspend fun updateStaff(staff: StaffMember)
    suspend fun deleteStaff(id: Long)
    
    suspend fun getEnrollment(staffId: Long): FaceEnrollment?
    suspend fun saveEnrollment(enrollment: FaceEnrollment)
    suspend fun deleteEnrollment(staffId: Long)
    suspend fun isEnrolled(staffId: Long): Boolean
    
    suspend fun getEnrolledCount(): Int
    suspend fun getStaffCount(): Int
}
