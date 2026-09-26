package com.salarybox.attendance

import android.app.Application
import com.salarybox.attendance.di.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AttendanceApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)

        // Warm up on-device face recognition engine asynchronously
        appScope.launch {
            try {
                ServiceLocator.faceRecognitionEngine.initialize(applicationContext)
            } catch (_: Exception) {
                // If initialization fails at startup, engine will attempt on demand
            }
        }
    }
}
