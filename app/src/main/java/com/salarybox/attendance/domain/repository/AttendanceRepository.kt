package com.salarybox.attendance.domain.repository

import com.salarybox.attendance.domain.model.AttendanceRecord
import kotlinx.coroutines.flow.Flow

interface AttendanceRepository {
    suspend fun recordAttendance(record: AttendanceRecord): Long
    fun getAttendanceByStaffId(staffId: Long): Flow<List<AttendanceRecord>>
    fun getTodayAttendance(): Flow<List<AttendanceRecord>>
    suspend fun getTodayAttendanceCount(): Int
    suspend fun getLatestAttendance(staffId: Long): AttendanceRecord?
    fun getAllAttendance(): Flow<List<AttendanceRecord>>
}
