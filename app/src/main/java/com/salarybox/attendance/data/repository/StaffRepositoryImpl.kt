package com.salarybox.attendance.data.repository

import com.salarybox.attendance.data.database.dao.FaceEnrollmentDao
import com.salarybox.attendance.data.database.dao.StaffDao
import com.salarybox.attendance.data.database.entity.toEntity
import com.salarybox.attendance.data.database.entity.toFaceEnrollment
import com.salarybox.attendance.data.database.entity.toStaffMember
import com.salarybox.attendance.domain.model.FaceEnrollment
import com.salarybox.attendance.domain.model.StaffMember
import com.salarybox.attendance.domain.repository.StaffRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class StaffRepositoryImpl(
    private val staffDao: StaffDao,
    private val faceEnrollmentDao: FaceEnrollmentDao
) : StaffRepository {

    override fun getAllStaff(): Flow<List<StaffMember>> {
        return staffDao.getAllStaff().map { list ->
            list.map { it.toStaffMember() }
        }
    }

    override suspend fun getStaffById(id: Long): StaffMember? = withContext(Dispatchers.IO) {
        staffDao.getStaffById(id)?.toStaffMember()
    }

    override suspend fun getStaffByEmployeeId(employeeId: String): StaffMember? = withContext(Dispatchers.IO) {
        staffDao.getStaffByEmployeeId(employeeId)?.toStaffMember()
    }

    override suspend fun addStaff(staff: StaffMember): Long = withContext(Dispatchers.IO) {
        staffDao.insertStaff(staff.toEntity())
    }

    override suspend fun updateStaff(staff: StaffMember) = withContext(Dispatchers.IO) {
        staffDao.updateStaff(staff.toEntity())
    }

    override suspend fun deleteStaff(id: Long) = withContext(Dispatchers.IO) {
        staffDao.deleteStaff(id)
    }

    override suspend fun getEnrollment(staffId: Long): FaceEnrollment? = withContext(Dispatchers.IO) {
        faceEnrollmentDao.getEnrollment(staffId)?.toFaceEnrollment()
    }

    override suspend fun saveEnrollment(enrollment: FaceEnrollment) = withContext(Dispatchers.IO) {
        faceEnrollmentDao.insertEnrollment(enrollment.toEntity())
    }

    override suspend fun deleteEnrollment(staffId: Long) = withContext(Dispatchers.IO) {
        faceEnrollmentDao.deleteEnrollment(staffId)
    }

    override suspend fun isEnrolled(staffId: Long): Boolean = withContext(Dispatchers.IO) {
        faceEnrollmentDao.isEnrolled(staffId)
    }

    override suspend fun getEnrolledCount(): Int = withContext(Dispatchers.IO) {
        faceEnrollmentDao.getEnrolledCount()
    }

    override suspend fun getStaffCount(): Int = withContext(Dispatchers.IO) {
        staffDao.getStaffCount()
    }
}
