package com.salarybox.attendance.data.repository

import com.salarybox.attendance.data.database.dao.AttendanceDao
import com.salarybox.attendance.data.database.entity.toEntity
import com.salarybox.attendance.domain.model.AttendanceRecord
import com.salarybox.attendance.domain.repository.AttendanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar

class AttendanceRepositoryImpl(
    private val attendanceDao: AttendanceDao
) : AttendanceRepository {

    override suspend fun recordAttendance(record: AttendanceRecord): Long = withContext(Dispatchers.IO) {
        attendanceDao.insertAttendance(record.toEntity())
    }

    override fun getAttendanceByStaffId(staffId: Long): Flow<List<AttendanceRecord>> {
        return attendanceDao.getAttendanceByStaffId(staffId).map { list ->
            list.map { it.toAttendanceRecord() }
        }
    }

    override fun getTodayAttendance(): Flow<List<AttendanceRecord>> {
        val startOfDay = getStartOfToday()
        return attendanceDao.getTodayAttendance(startOfDay).map { list ->
            list.map { it.toAttendanceRecord() }
        }
    }

    override suspend fun getTodayAttendanceCount(): Int = withContext(Dispatchers.IO) {
        val startOfDay = getStartOfToday()
        attendanceDao.getTodayAttendanceCount(startOfDay)
    }

    override suspend fun getLatestAttendance(staffId: Long): AttendanceRecord? = withContext(Dispatchers.IO) {
        attendanceDao.getLatestAttendance(staffId)?.toAttendanceRecord()
    }

    override fun getAllAttendance(): Flow<List<AttendanceRecord>> {
        return attendanceDao.getAllAttendance().map { list ->
            list.map { it.toAttendanceRecord() }
        }
    }

    private fun getStartOfToday(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}
