package com.salarybox.attendance.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.salarybox.attendance.data.database.entity.AttendanceEntity
import com.salarybox.attendance.domain.model.AttendanceRecord
import kotlinx.coroutines.flow.Flow

data class AttendanceWithStaff(
    val id: Long,
    val staffId: Long,
    val timestamp: Long,
    val selfiePath: String,
    val latitude: Double,
    val longitude: Double,
    val locationAccuracy: Float,
    val matchScore: Float,
    val verificationStatus: String,
    val empId: String,
    val staffName: String
) {
    fun toAttendanceRecord(): AttendanceRecord {
        return AttendanceRecord(
            id = id,
            staffId = staffId,
            employeeId = empId,
            staffName = staffName,
            timestamp = timestamp,
            selfiePath = selfiePath,
            latitude = latitude,
            longitude = longitude,
            locationAccuracy = locationAccuracy,
            matchScore = matchScore,
            verificationStatus = verificationStatus
        )
    }
}

@Dao
interface AttendanceDao {
    @Insert
    suspend fun insertAttendance(attendance: AttendanceEntity): Long

    @Query("SELECT a.*, s.employeeId as empId, s.name as staffName FROM attendance a INNER JOIN staff s ON a.staffId = s.id WHERE a.staffId = :staffId ORDER BY a.timestamp DESC")
    fun getAttendanceByStaffId(staffId: Long): Flow<List<AttendanceWithStaff>>

    @Query("SELECT a.*, s.employeeId as empId, s.name as staffName FROM attendance a INNER JOIN staff s ON a.staffId = s.id WHERE a.timestamp >= :startOfDay ORDER BY a.timestamp DESC")
    fun getTodayAttendance(startOfDay: Long): Flow<List<AttendanceWithStaff>>

    @Query("SELECT COUNT(*) FROM attendance WHERE timestamp >= :startOfDay")
    suspend fun getTodayAttendanceCount(startOfDay: Long): Int

    @Query("SELECT a.*, s.employeeId as empId, s.name as staffName FROM attendance a INNER JOIN staff s ON a.staffId = s.id WHERE a.staffId = :staffId ORDER BY a.timestamp DESC LIMIT 1")
    suspend fun getLatestAttendance(staffId: Long): AttendanceWithStaff?

    @Query("SELECT a.*, s.employeeId as empId, s.name as staffName FROM attendance a INNER JOIN staff s ON a.staffId = s.id ORDER BY a.timestamp DESC")
    fun getAllAttendance(): Flow<List<AttendanceWithStaff>>
}
