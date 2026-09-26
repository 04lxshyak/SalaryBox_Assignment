package com.salarybox.attendance.face

/**
 * Centralized Configuration for On-Device Face Recognition.
 *
 * ============================================================================
 * SPECIFICATION & ARCHITECTURE DOCUMENTATION
 * ============================================================================
 *
 * 1. SELECTED MODEL:
 *    - Model: MobileFaceNet (TFLite)
 *    - Model File: mobilefacenet.tflite (in assets directory)
 *    - Architecture: Lightweight Convolutional Neural Network designed specifically
 *      for real-time high-accuracy mobile face verification.
 *
 * 2. EMBEDDING DIMENSION:
 *    - Output vector dimension: 192 (FloatArray of size 192)
 *
 * 3. PREPROCESSING PIPELINE:
 *    - Input Image: Front-camera bitmap with orientation corrected
 *    - Face Detection: Google ML Kit Face Detection (fast, high-accuracy landmarks)
 *    - Quality Checks:
 *        * Exactly 1 face (zero or multiple faces rejected immediately)
 *        * Euler Angles: Yaw within [-20°, +20°], Pitch within [-20°, +20°], Roll within [-20°, +20°]
 *        * Minimum Face Size: Bounding box width & height >= 100 pixels
 *        * Bounding Box: Must be within camera frame bounds
 *    - Face Alignment / Crop:
 *        * Bounding box expanded by 15% margin to preserve facial structure
 *        * Resized to model input dimensions: 112 x 112 pixels (ARGB_8888)
 *    - Pixel Normalization:
 *        * Formula: (pixel_channel_value - 127.5f) / 128.0f
 *        * Maps 8-bit [0, 255] RGB channels to floating point range [-1.0, 1.0]
 *
 * 4. EMBEDDING NORMALIZATION:
 *    - L2 Normalization applied to the raw 192-dimensional output vector:
 *        norm = sqrt(sum(v_i ^ 2))
 *        normalized_v = v / max(norm, 1e-10)
 *
 * 5. COMPARISON METRIC:
 *    - Cosine Similarity between two L2-normalized vectors:
 *        similarity = dot_product(v1, v2)
 *    - Range: [-1.0, 1.0], where 1.0 is identical identity
 *
 * 6. DECISION THRESHOLD:
 *    - SIMILARITY_THRESHOLD = 0.70f
 *    - If similarity >= 0.70f -> MATCH (Identity Verified)
 *    - If similarity < 0.70f  -> LOW_SIMILARITY (Rejected)
 *    - Multiple Template Support: Compares against all enrolled template embeddings
 *      and uses the maximum similarity score for decision.
 *
 * 7. LIMITATIONS & CALIBRATION:
 *    - Practically tuned for mobile demo environments with front-facing camera.
 *    - Extreme low lighting, extreme backlight, or heavy face occlusions (sunglasses, masks)
 *      will trigger POOR_QUALITY rejection before reaching embedding comparison.
 *    - Security principle: Fail-closed. Biometric data never leaves the device.
 * ============================================================================
 */
object FaceRecognitionConfig {

    // Model Architecture Specs
    const val MODEL_FILE_NAME = "mobilefacenet.tflite"
    const val MODEL_VERSION = "MobileFaceNet-192d-v1"
    const val INPUT_IMAGE_SIZE = 112 // 112x112 input
    const val EMBEDDING_DIM = 192    // 192-dimensional output vector

    // Pixel Normalization parameters
    const val PIXEL_MEAN = 127.5f
    const val PIXEL_STD = 128.0f

    // Face Pose Validation Limits (degrees)
    const val MAX_ABS_YAW_DEGREE = 20.0f   // Left/Right head turn
    const val MAX_ABS_PITCH_DEGREE = 20.0f // Up/Down head tilt
    const val MAX_ABS_ROLL_DEGREE = 20.0f  // Head tilt to shoulder

    // Face Size Validation (pixels)
    const val MIN_FACE_WIDTH_PX = 100
    const val MIN_FACE_HEIGHT_PX = 100
    const val FACE_CROP_MARGIN_RATIO = 0.15f // 15% bounding box padding

    // Enrollment Settings
    const val REQUIRED_ENROLLMENT_SAMPLES = 3

    // Identity Decision Threshold
    // Empirically tuned for MobileFaceNet L2-normalized cosine similarity
    const val SIMILARITY_THRESHOLD = 0.70f

    /**
     * Evaluates similarity against the centralized threshold.
     * Returns true if identity is verified (similarity >= threshold).
     */
    fun isMatch(similarity: Float): Boolean {
        return similarity >= SIMILARITY_THRESHOLD
    }
}
