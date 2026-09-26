package com.salarybox.attendance.face

import android.content.Context
import android.graphics.Bitmap

interface FaceRecognitionEngine {
    suspend fun initialize(context: Context)
    fun isInitialized(): Boolean
    suspend fun detectAndValidateFace(bitmap: Bitmap): FaceDetectionResult
    suspend fun generateEmbedding(faceBitmap: Bitmap): FloatArray?
    fun compareFaces(embedding1: FloatArray, embedding2: FloatArray): Float
    fun release()

    /**
     * Verifies a candidate embedding against a list of enrolled template embeddings.
     * Compares using cosine similarity against all templates and checks against the
     * centralized threshold.
     */
    fun verifyIdentity(
        candidateEmbedding: FloatArray,
        enrolledEmbeddings: List<FloatArray>
    ): FaceVerificationResult {
        if (enrolledEmbeddings.isEmpty()) {
            return FaceVerificationResult.NoEnrollment
        }
        var maxSimilarity = -1.0f
        for (template in enrolledEmbeddings) {
            val sim = compareFaces(candidateEmbedding, template)
            if (sim > maxSimilarity) {
                maxSimilarity = sim
            }
        }
        return if (FaceRecognitionConfig.isMatch(maxSimilarity)) {
            FaceVerificationResult.Match(maxSimilarity)
        } else {
            FaceVerificationResult.LowSimilarity(maxSimilarity.coerceAtLeast(0f))
        }
    }
}
