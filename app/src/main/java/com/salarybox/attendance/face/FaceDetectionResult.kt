package com.salarybox.attendance.face

import android.graphics.Bitmap

data class FaceDetectionResult(
    val isValid: Boolean,
    val faceBitmap: Bitmap? = null,
    val message: String = "",
    val confidence: Float = 0f
)
