package com.salarybox.attendance.face

import android.content.Context
import android.graphics.Bitmap

class StubFaceRecognitionEngine : FaceRecognitionEngine {
    
    override suspend fun initialize(context: Context) {
        // Stub: do nothing
    }

    override fun isInitialized(): Boolean {
        return false
    }

    override suspend fun detectAndValidateFace(bitmap: Bitmap): FaceDetectionResult {
        return FaceDetectionResult(
            isValid = false,
            message = "Face recognition engine not initialized. Will be available in next update."
        )
    }

    override suspend fun generateEmbedding(faceBitmap: Bitmap): FloatArray? {
        return null
    }

    override fun compareFaces(embedding1: FloatArray, embedding2: FloatArray): Float {
        return 0f
    }

    override fun release() {
        // Stub: do nothing
    }
}
