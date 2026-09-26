package com.salarybox.attendance.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.salarybox.attendance.data.database.entity.StaffEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff ORDER BY name ASC")
    fun getAllStaff(): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staff WHERE id = :id")
    suspend fun getStaffById(id: Long): StaffEntity?

    @Query("SELECT * FROM staff WHERE employeeId = :employeeId")
    suspend fun getStaffByEmployeeId(employeeId: String): StaffEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStaff(staff: StaffEntity): Long

    @Update
    suspend fun updateStaff(staff: StaffEntity)

    @Query("DELETE FROM staff WHERE id = :id")
    suspend fun deleteStaff(id: Long)

    @Query("SELECT COUNT(*) FROM staff")
    suspend fun getStaffCount(): Int

    @Query("SELECT * FROM staff WHERE employeeId = :employeeId AND password = :password")
    suspend fun authenticateStaff(employeeId: String, password: String): StaffEntity?
}
