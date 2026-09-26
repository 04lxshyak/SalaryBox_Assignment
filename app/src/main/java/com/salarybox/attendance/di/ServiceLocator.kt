package com.salarybox.attendance.di

import android.content.Context
import com.salarybox.attendance.data.database.AppDatabase
import com.salarybox.attendance.data.repository.AttendanceRepositoryImpl
import com.salarybox.attendance.data.repository.AuthRepositoryImpl
import com.salarybox.attendance.data.repository.StaffRepositoryImpl
import com.salarybox.attendance.data.session.SessionManager
import com.salarybox.attendance.data.storage.InternalFileStorage
import com.salarybox.attendance.domain.repository.AttendanceRepository
import com.salarybox.attendance.domain.repository.AuthRepository
import com.salarybox.attendance.domain.repository.StaffRepository
import com.salarybox.attendance.face.FaceRecognitionEngine
import com.salarybox.attendance.face.RealFaceRecognitionEngine
import com.salarybox.attendance.location.LocationService

object ServiceLocator {
    private lateinit var applicationContext: Context
    private var _faceRecognitionEngine: FaceRecognitionEngine? = null

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(applicationContext)
    }

    val sessionManager: SessionManager by lazy {
        SessionManager(applicationContext)
    }

    val fileStorage: InternalFileStorage by lazy {
        InternalFileStorage(applicationContext)
    }

    val locationService: LocationService by lazy {
        LocationService(applicationContext)
    }

    val staffRepository: StaffRepository by lazy {
        StaffRepositoryImpl(database.staffDao(), database.faceEnrollmentDao())
    }

    val attendanceRepository: AttendanceRepository by lazy {
        AttendanceRepositoryImpl(database.attendanceDao())
    }

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(database.staffDao(), sessionManager)
    }

    val faceRecognitionEngine: FaceRecognitionEngine
        get() {
            if (_faceRecognitionEngine == null) {
                _faceRecognitionEngine = RealFaceRecognitionEngine()
            }
            return _faceRecognitionEngine!!
        }

    fun setFaceRecognitionEngine(engine: FaceRecognitionEngine) {
        _faceRecognitionEngine = engine
    }
}
