package com.salarybox.attendance.presentation.admin.enrollment

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.salarybox.attendance.data.storage.InternalFileStorage
import com.salarybox.attendance.di.ServiceLocator
import com.salarybox.attendance.domain.model.FaceEnrollment
import com.salarybox.attendance.domain.repository.StaffRepository
import com.salarybox.attendance.face.FaceRecognitionConfig
import com.salarybox.attendance.face.FaceRecognitionEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.sqrt

enum class EnrollmentState {
    INITIALIZING, READY, CAPTURING, PROCESSING, SAMPLE_CAPTURED, COMPLETED, ERROR, CANCELLED
}

data class FaceEnrollmentUiState(
    val staffId: Long = 0,
    val currentStep: Int = 0,
    val totalSteps: Int = FaceRecognitionConfig.REQUIRED_ENROLLMENT_SAMPLES,
    val state: EnrollmentState = EnrollmentState.INITIALIZING,
    val capturedSamples: List<Bitmap> = emptyList(),
    val embeddings: List<FloatArray> = emptyList(),
    val errorMessage: String? = null,
    val progress: Float = 0f,
    val instructions: String = "Position your face directly within the oval guide"
)

class FaceEnrollmentViewModel(
    private val staffId: Long,
    private val staffRepo: StaffRepository,
    private val faceEngine: FaceRecognitionEngine,
    private val fileStorage: InternalFileStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(FaceEnrollmentUiState(staffId = staffId))
    val uiState: StateFlow<FaceEnrollmentUiState> = _uiState.asStateFlow()

    fun initializeEngine(context: Context) {
        viewModelScope.launch {
            try {
                if (!faceEngine.isInitialized()) {
                    faceEngine.initialize(context)
                }
                _uiState.update { it.copy(state = EnrollmentState.READY) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        state = EnrollmentState.ERROR,
                        errorMessage = "Failed to initialize face recognition engine: ${e.message}"
                    )
                }
            }
        }
    }

    fun captureSample(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.update { it.copy(state = EnrollmentState.PROCESSING) }
            try {
                val detection = faceEngine.detectAndValidateFace(bitmap)
                if (detection.isValid && detection.faceBitmap != null) {
                    val embedding = faceEngine.generateEmbedding(detection.faceBitmap)
                    if (embedding != null) {
                        val currentSamples = _uiState.value.capturedSamples + bitmap
                        val currentEmbeddings = _uiState.value.embeddings + embedding
                        val step = _uiState.value.currentStep + 1

                        if (step >= _uiState.value.totalSteps) {
                            finalizeEnrollment(currentSamples, currentEmbeddings)
                        } else {
                            val nextInstruction = when (step) {
                                1 -> "Sample 1 recorded! Look slightly to the right or smile for sample 2."
                                2 -> "Sample 2 recorded! Look straight ahead for the final sample."
                                else -> "Position your face directly within the oval guide"
                            }
                            _uiState.update {
                                it.copy(
                                    currentStep = step,
                                    capturedSamples = currentSamples,
                                    embeddings = currentEmbeddings,
                                    state = EnrollmentState.SAMPLE_CAPTURED,
                                    progress = step.toFloat() / it.totalSteps,
                                    instructions = nextInstruction
                                )
                            }
                        }
                    } else {
                        throw Exception("Failed to generate embedding from face crop.")
                    }
                } else {
                    throw Exception(detection.message.ifBlank { "Invalid face sample. Please look directly at the camera." })
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        state = EnrollmentState.ERROR,
                        errorMessage = e.message ?: "Failed to validate face sample"
                    )
                }
            }
        }
    }

    private suspend fun finalizeEnrollment(samples: List<Bitmap>, embeddings: List<FloatArray>) {
        try {
            // Delete previous enrollment images if re-enrolling
            fileStorage.deleteEnrollmentImages(staffId)

            val paths = samples.mapIndexed { index, bmp ->
                fileStorage.saveEnrollmentImage(bmp, staffId, index + 1)
            }

            // Average embeddings across all valid samples
            val avgEmbedding = FloatArray(embeddings[0].size)
            for (i in avgEmbedding.indices) {
                var sum = 0f
                for (emb in embeddings) {
                    sum += emb[i]
                }
                avgEmbedding[i] = sum / embeddings.size
            }

            // L2 normalize the averaged embedding template
            var sumSquare = 0.0
            for (v in avgEmbedding) {
                sumSquare += (v * v).toDouble()
            }
            val norm = sqrt(sumSquare).toFloat().coerceAtLeast(1e-10f)
            val normalizedTemplate = FloatArray(avgEmbedding.size) { i -> avgEmbedding[i] / norm }

            val enrollment = FaceEnrollment(
                id = 0,
                staffId = staffId,
                embedding = normalizedTemplate,
                modelVersion = FaceRecognitionConfig.MODEL_VERSION,
                sampleCount = samples.size,
                enrolledAt = System.currentTimeMillis(),
                imagePaths = paths
            )
            staffRepo.saveEnrollment(enrollment)

            _uiState.update {
                it.copy(
                    state = EnrollmentState.COMPLETED,
                    progress = 1.0f,
                    capturedSamples = samples,
                    embeddings = embeddings,
                    instructions = "Face enrollment completed successfully!"
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    state = EnrollmentState.ERROR,
                    errorMessage = "Failed to save enrollment: ${e.message}"
                )
            }
        }
    }

    fun continueToNextSample() {
        if (_uiState.value.state == EnrollmentState.SAMPLE_CAPTURED) {
            _uiState.update { it.copy(state = EnrollmentState.READY) }
        }
    }

    fun retry() {
        _uiState.update { it.copy(state = EnrollmentState.READY, errorMessage = null) }
    }

    fun cancel() {
        _uiState.update { it.copy(state = EnrollmentState.CANCELLED) }
    }

    fun resetEnrollment() {
        _uiState.update {
            FaceEnrollmentUiState(staffId = staffId, state = EnrollmentState.READY)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val savedStateHandle = createSavedStateHandle()
                val staffId = savedStateHandle.get<Long>("staffId") ?: 0L
                FaceEnrollmentViewModel(
                    staffId,
                    ServiceLocator.staffRepository,
                    ServiceLocator.faceRecognitionEngine,
                    ServiceLocator.fileStorage
                )
            }
        }
    }
}
