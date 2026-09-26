package com.salarybox.attendance.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.salarybox.attendance.data.database.entity.FaceEnrollmentEntity

@Dao
interface FaceEnrollmentDao {
    @Query("SELECT * FROM face_enrollment WHERE staffId = :staffId")
    suspend fun getEnrollment(staffId: Long): FaceEnrollmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnrollment(enrollment: FaceEnrollmentEntity)

    @Query("DELETE FROM face_enrollment WHERE staffId = :staffId")
    suspend fun deleteEnrollment(staffId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM face_enrollment WHERE staffId = :staffId)")
    suspend fun isEnrolled(staffId: Long): Boolean

    @Query("SELECT COUNT(*) FROM face_enrollment")
    suspend fun getEnrolledCount(): Int
}
