package com.salarybox.attendance.face

/**
 * Structured result of an identity verification operation.
 */
sealed class FaceVerificationResult {

    /**
     * Identity successfully verified.
     * @param matchScore Cosine similarity score [0.0, 1.0]
     */
    data class Match(val matchScore: Float) : FaceVerificationResult()

    /**
     * Face detected and embedding generated, but identity does not match enrolled profile.
     * @param matchScore Cosine similarity score [0.0, 1.0]
     */
    data class LowSimilarity(val matchScore: Float) : FaceVerificationResult()

    /**
     * No face was detected in the frame.
     */
    data object NoFace : FaceVerificationResult()

    /**
     * More than one face was visible in the frame.
     */
    data class MultipleFaces(val count: Int) : FaceVerificationResult()

    /**
     * Face detected but rejected due to poor quality (head turned, too far, bad angle).
     */
    data class PoorQuality(val reason: String) : FaceVerificationResult()

    /**
     * The staff member does not have an enrolled face profile.
     */
    data object NoEnrollment : FaceVerificationResult()

    /**
     * Face recognition model is still initializing or could not be loaded.
     */
    data object ModelNotReady : FaceVerificationResult()

    /**
     * Failed during neural network inference or image preprocessing.
     */
    data class EmbeddingFailed(val reason: String) : FaceVerificationResult()

    /**
     * User-friendly message explaining the status or why verification failed.
     */
    val userMessage: String
        get() = when (this) {
            is Match -> "Identity verified successfully"
            is LowSimilarity -> "Face does not match your enrolled profile."
            is NoFace -> "No face detected. Please position your face in the guide."
            is MultipleFaces -> "Only one face should be visible. Found $count faces."
            is PoorQuality -> reason
            is NoEnrollment -> "Please ask an administrator to enroll your face."
            is ModelNotReady -> "Face recognition system is preparing. Please try again."
            is EmbeddingFailed -> "Could not process face. Please try again."
        }
}
