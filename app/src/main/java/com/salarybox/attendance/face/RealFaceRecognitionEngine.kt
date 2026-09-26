package com.salarybox.attendance.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Production implementation of FaceRecognitionEngine using:
 * 1. Google ML Kit for on-device face detection, bounding box, and pose validation.
 * 2. MobileFaceNet (TFLite) for 192-dimensional face embedding generation.
 * 3. L2 Normalization and Cosine Similarity for identity verification.
 */
class RealFaceRecognitionEngine : FaceRecognitionEngine {

    @Volatile
    private var interpreter: Interpreter? = null

    @Volatile
    private var detector: FaceDetector? = null

    @Volatile
    private var initialized = false

    override suspend fun initialize(context: Context): Unit = withContext(Dispatchers.IO) {
        if (initialized) return@withContext

        try {
            // 1. Initialize ML Kit Face Detector with accurate mode for validation
            val faceDetectorOptions = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .setMinFaceSize(0.15f)
                .build()
            detector = FaceDetection.getClient(faceDetectorOptions)

            // 2. Initialize TFLite Interpreter for MobileFaceNet
            val modelBuffer = loadModelFile(context, FaceRecognitionConfig.MODEL_FILE_NAME)
            val interpreterOptions = Interpreter.Options().apply {
                setNumThreads(4)
            }
            interpreter = Interpreter(modelBuffer, interpreterOptions)
            initialized = true
        } catch (e: Exception) {
            initialized = false
            release()
            throw IllegalStateException("Failed to initialize FaceRecognitionEngine: ${e.message}", e)
        }
    }

    override fun isInitialized(): Boolean = initialized && interpreter != null && detector != null

    override suspend fun detectAndValidateFace(bitmap: Bitmap): FaceDetectionResult = withContext(Dispatchers.Default) {
        val currentDetector = detector ?: return@withContext FaceDetectionResult(
            isValid = false,
            message = "Face detector not initialized"
        )

        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val faces: List<Face> = Tasks.await(currentDetector.process(inputImage))

            if (faces.isEmpty()) {
                return@withContext FaceDetectionResult(
                    isValid = false,
                    message = "No face detected. Please look directly at the camera."
                )
            }

            if (faces.size > 1) {
                return@withContext FaceDetectionResult(
                    isValid = false,
                    message = "Only one face should be visible. Detected ${faces.size} faces."
                )
            }

            val face = faces[0]

            // Validate Face Pose (Yaw, Pitch, Roll)
            val yaw = face.headEulerAngleY
            val pitch = face.headEulerAngleX
            val roll = face.headEulerAngleZ

            if (abs(yaw) > FaceRecognitionConfig.MAX_ABS_YAW_DEGREE) {
                return@withContext FaceDetectionResult(
                    isValid = false,
                    message = "Please turn your face directly toward the camera."
                )
            }

            if (abs(pitch) > FaceRecognitionConfig.MAX_ABS_PITCH_DEGREE) {
                return@withContext FaceDetectionResult(
                    isValid = false,
                    message = "Please keep your head level (tilt up/down too high)."
                )
            }

            if (abs(roll) > FaceRecognitionConfig.MAX_ABS_ROLL_DEGREE) {
                return@withContext FaceDetectionResult(
                    isValid = false,
                    message = "Please keep your head straight without tilting sideways."
                )
            }

            // Validate Face Size
            val box: Rect = face.boundingBox
            if (box.width() < FaceRecognitionConfig.MIN_FACE_WIDTH_PX ||
                box.height() < FaceRecognitionConfig.MIN_FACE_HEIGHT_PX
            ) {
                return@withContext FaceDetectionResult(
                    isValid = false,
                    message = "Please move closer to the camera."
                )
            }

            // Crop face with margin and resize to 112x112
            val marginX = (box.width() * FaceRecognitionConfig.FACE_CROP_MARGIN_RATIO).toInt()
            val marginY = (box.height() * FaceRecognitionConfig.FACE_CROP_MARGIN_RATIO).toInt()

            val cropLeft = max(0, box.left - marginX)
            val cropTop = max(0, box.top - marginY)
            val cropRight = min(bitmap.width, box.right + marginX)
            val cropBottom = min(bitmap.height, box.bottom + marginY)

            val cropWidth = cropRight - cropLeft
            val cropHeight = cropBottom - cropTop

            if (cropWidth <= 10 || cropHeight <= 10) {
                return@withContext FaceDetectionResult(
                    isValid = false,
                    message = "Face is too close to edge of frame."
                )
            }

            val croppedBitmap = Bitmap.createBitmap(bitmap, cropLeft, cropTop, cropWidth, cropHeight)
            val scaledBitmap = Bitmap.createScaledBitmap(
                croppedBitmap,
                FaceRecognitionConfig.INPUT_IMAGE_SIZE,
                FaceRecognitionConfig.INPUT_IMAGE_SIZE,
                true
            )

            if (croppedBitmap != scaledBitmap && !croppedBitmap.isRecycled) {
                croppedBitmap.recycle()
            }

            return@withContext FaceDetectionResult(
                isValid = true,
                faceBitmap = scaledBitmap,
                message = "Face detected successfully",
                confidence = 1.0f
            )
        } catch (e: Exception) {
            return@withContext FaceDetectionResult(
                isValid = false,
                message = "Face validation failed: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    override suspend fun generateEmbedding(faceBitmap: Bitmap): FloatArray? = withContext(Dispatchers.Default) {
        val currentInterpreter = interpreter ?: return@withContext null

        try {
            // Prepare 112x112 ARGB_8888 bitmap
            val inputSize = FaceRecognitionConfig.INPUT_IMAGE_SIZE
            val resized = if (faceBitmap.width == inputSize && faceBitmap.height == inputSize) {
                faceBitmap
            } else {
                Bitmap.createScaledBitmap(faceBitmap, inputSize, inputSize, true)
            }

            // Allocate direct ByteBuffer: 1 * 112 * 112 * 3 * 4 bytes
            val inputBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4).apply {
                order(ByteOrder.nativeOrder())
            }

            val intValues = IntArray(inputSize * inputSize)
            resized.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)

            // Normalize pixels to [-1.0, 1.0]: (pixel - 127.5) / 128.0
            for (pixel in intValues) {
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                inputBuffer.putFloat((r - FaceRecognitionConfig.PIXEL_MEAN) / FaceRecognitionConfig.PIXEL_STD)
                inputBuffer.putFloat((g - FaceRecognitionConfig.PIXEL_MEAN) / FaceRecognitionConfig.PIXEL_STD)
                inputBuffer.putFloat((b - FaceRecognitionConfig.PIXEL_MEAN) / FaceRecognitionConfig.PIXEL_STD)
            }

            // Output tensor: [1, 192]
            val outputArray = Array(1) { FloatArray(FaceRecognitionConfig.EMBEDDING_DIM) }

            synchronized(this@RealFaceRecognitionEngine) {
                currentInterpreter.run(inputBuffer, outputArray)
            }

            val rawEmbedding = outputArray[0]

            // Apply L2 Normalization
            var sumSquare = 0.0
            for (v in rawEmbedding) {
                sumSquare += (v * v).toDouble()
            }
            val norm = sqrt(sumSquare).toFloat().coerceAtLeast(1e-10f)

            return@withContext FloatArray(rawEmbedding.size) { i -> rawEmbedding[i] / norm }
        } catch (e: Exception) {
            return@withContext null
        }
    }

    override fun compareFaces(embedding1: FloatArray, embedding2: FloatArray): Float {
        if (embedding1.size != embedding2.size || embedding1.isEmpty()) {
            return 0.0f
        }

        var dotProduct = 0.0f
        var normA = 0.0f
        var normB = 0.0f

        for (i in embedding1.indices) {
            val a = embedding1[i]
            val b = embedding2[i]
            dotProduct += a * b
            normA += a * a
            normB += b * b
        }

        val denominator = sqrt(normA.toDouble()) * sqrt(normB.toDouble())
        if (denominator <= 0.0) return 0.0f

        val similarity = (dotProduct / denominator).toFloat()
        return similarity.coerceIn(-1.0f, 1.0f)
    }

    override fun release() {
        try {
            interpreter?.close()
        } catch (_: Exception) {}
        interpreter = null

        try {
            detector?.close()
        } catch (_: Exception) {}
        detector = null

        initialized = false
    }

    private fun loadModelFile(context: Context, modelFileName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelFileName)
        FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        }
    }
}
