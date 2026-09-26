package com.salarybox.attendance.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.salarybox.attendance.data.database.dao.AttendanceDao
import com.salarybox.attendance.data.database.dao.FaceEnrollmentDao
import com.salarybox.attendance.data.database.dao.StaffDao
import com.salarybox.attendance.data.database.entity.AttendanceEntity
import com.salarybox.attendance.data.database.entity.FaceEnrollmentEntity
import com.salarybox.attendance.data.database.entity.StaffEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [StaffEntity::class, FaceEnrollmentEntity::class, AttendanceEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun staffDao(): StaffDao
    abstract fun faceEnrollmentDao(): FaceEnrollmentDao
    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "salarybox_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    val staffDao = database.staffDao()
                    staffDao.insertStaff(
                        StaffEntity(
                            employeeId = "EMP001",
                            name = "Demo Employee",
                            password = "1234",
                            isActive = true,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }
}
