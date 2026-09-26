package com.salarybox.attendance.presentation.staff

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.salarybox.attendance.data.storage.InternalFileStorage
import com.salarybox.attendance.di.ServiceLocator
import com.salarybox.attendance.domain.model.AttendanceRecord
import com.salarybox.attendance.domain.model.StaffMember
import com.salarybox.attendance.domain.repository.AttendanceRepository
import com.salarybox.attendance.domain.repository.AuthRepository
import com.salarybox.attendance.domain.repository.StaffRepository
import com.salarybox.attendance.face.FaceRecognitionConfig
import com.salarybox.attendance.face.FaceRecognitionEngine
import com.salarybox.attendance.face.FaceVerificationResult
import com.salarybox.attendance.location.LocationResult
import com.salarybox.attendance.location.LocationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

enum class AttendanceProcessStep {
    INITIALIZING,
    READY,
    DETECTING_FACE,
    CHECKING_IMAGE,
    GENERATING_EMBEDDING,
    VERIFYING_IDENTITY,
    ACQUIRING_LOCATION,
    SUCCESS,
    ERROR
}

data class AttendanceMarkUiState(
    val step: AttendanceProcessStep = AttendanceProcessStep.INITIALIZING,
    val statusMessage: String = "Initializing Camera...",
    val staff: StaffMember? = null,
    val isEnrolled: Boolean = true,
    val isProcessing: Boolean = false,
    val successRecord: AttendanceRecord? = null,
    val locationDescription: String? = null,
    val errorMessage: String? = null,
    val canRetry: Boolean = true
)

class AttendanceMarkViewModel(
    private val authRepo: AuthRepository,
    private val staffRepo: StaffRepository,
    private val attendanceRepo: AttendanceRepository,
    private val faceEngine: FaceRecognitionEngine,
    private val locationService: LocationService,
    private val fileStorage: InternalFileStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(AttendanceMarkUiState())
    val uiState: StateFlow<AttendanceMarkUiState> = _uiState.asStateFlow()

    private val isExecuting = AtomicBoolean(false)
    private var enrolledEmbeddings: List<FloatArray> = emptyList()
    private var currentStaffId: Long = 0L

    init {
        initializeSessionAndEnrollment()
    }

    fun initializeFaceEngine(context: Context) {
        if (faceEngine.isInitialized()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    step = AttendanceProcessStep.INITIALIZING,
                    statusMessage = "Preparing face recognition...",
                    isProcessing = true,
                    errorMessage = null
                )
            }
            try {
                faceEngine.initialize(context.applicationContext)
                _uiState.update {
                    it.copy(
                        step = AttendanceProcessStep.READY,
                        statusMessage = "Position your face within the guide and tap Verify Attendance.",
                        isProcessing = false
                    )
                }
            } catch (_: Exception) {
                failAttendance(
                    errorState = AttendanceProcessStep.ERROR,
                    message = "Face recognition could not be started. Attendance was not recorded."
                )
            }
        }
    }

    private fun initializeSessionAndEnrollment() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    step = AttendanceProcessStep.INITIALIZING,
                    statusMessage = "Verifying session..."
                )
            }

            val session = authRepo.getCurrentSession().firstOrNull()
            if (session == null || session.staffId == null) {
                _uiState.update {
                    it.copy(
                        step = AttendanceProcessStep.ERROR,
                        errorMessage = "No active staff session. Please log in again.",
                        canRetry = false
                    )
                }
                return@launch
            }

            currentStaffId = session.staffId
            val staff = staffRepo.getStaffById(currentStaffId)
            val enrollment = staffRepo.getEnrollment(currentStaffId)

            if (enrollment == null || enrollment.embedding.isEmpty()) {
                _uiState.update {
                    it.copy(
                        step = AttendanceProcessStep.ERROR,
                        staff = staff,
                        isEnrolled = false,
                        errorMessage = "Please ask an administrator to enroll your face.",
                        statusMessage = "Face Not Enrolled",
                        canRetry = false
                    )
                }
                return@launch
            }

            // Load enrolled template embeddings (support multi-template)
            enrolledEmbeddings = listOf(enrollment.embedding)

            _uiState.update {
                it.copy(
                    step = AttendanceProcessStep.READY,
                    staff = staff,
                    isEnrolled = true,
                    statusMessage = "Position your face within the guide and tap Verify Attendance.",
                    errorMessage = null
                )
            }
        }
    }

    /**
     * Executes the complete, atomic attendance verification pipeline:
     * 1. Check duplicate lock
     * 2. Detect face in captured photo
     * 3. Validate face quality & pose
     * 4. Crop, align & generate normalized embedding
     * 5. Compare with enrolled embedding & check threshold
     * 6. Acquire GPS location with timeout
     * 7. Save selfie to app-private storage
     * 8. Persist Attendance record in Room
     * 9. Transition to SUCCESS
     */
    fun processAttendanceCapture(capturedBitmap: Bitmap) {
        // Double-tap & concurrency protection: Fail fast if already processing
        if (!isExecuting.compareAndSet(false, true)) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    errorMessage = null
                )
            }

            try {
                if (!faceEngine.isInitialized()) {
                    failAttendance(
                        errorState = AttendanceProcessStep.ERROR,
                        message = "Face recognition is not ready. Please retry once it is available."
                    )
                    return@launch
                }

                // Step 2 & 3: Face Detection and Quality Validation
                _uiState.update {
                    it.copy(
                        step = AttendanceProcessStep.DETECTING_FACE,
                        statusMessage = "Detecting Face..."
                    )
                }

                val detectionResult = faceEngine.detectAndValidateFace(capturedBitmap)
                if (!detectionResult.isValid || detectionResult.faceBitmap == null) {
                    failAttendance(
                        errorState = AttendanceProcessStep.ERROR,
                        message = detectionResult.message.ifBlank { "Face is not clear enough. Please try again." }
                    )
                    return@launch
                }

                val faceCrop = detectionResult.faceBitmap

                // Step 4: Generate Face Embedding
                _uiState.update {
                    it.copy(
                        step = AttendanceProcessStep.GENERATING_EMBEDDING,
                        statusMessage = "Generating Embedding..."
                    )
                }

                val candidateEmbedding = faceEngine.generateEmbedding(faceCrop)
                if (candidateEmbedding == null) {
                    failAttendance(
                        errorState = AttendanceProcessStep.ERROR,
                        message = "Could not process face. Please try again."
                    )
                    return@launch
                }

                // Step 5: Verify Identity
                _uiState.update {
                    it.copy(
                        step = AttendanceProcessStep.VERIFYING_IDENTITY,
                        statusMessage = "Verifying Identity..."
                    )
                }

                val verificationResult = faceEngine.verifyIdentity(candidateEmbedding, enrolledEmbeddings)
                if (verificationResult !is FaceVerificationResult.Match) {
                    failAttendance(
                        errorState = AttendanceProcessStep.ERROR,
                        message = verificationResult.userMessage
                    )
                    return@launch
                }

                val matchScore = verificationResult.matchScore

                // Step 6: Acquire Location with Timeout
                _uiState.update {
                    it.copy(
                        step = AttendanceProcessStep.ACQUIRING_LOCATION,
                        statusMessage = "Acquiring GPS location..."
                    )
                }

                val locationResult = locationService.getCurrentLocation()
                if (locationResult !is LocationResult.Success) {
                    val locationError = locationResult as LocationResult.Error
                    failAttendance(
                        errorState = AttendanceProcessStep.ERROR,
                        message = locationError.message
                    )
                    return@launch
                }

                // Step 7: Save selfie bitmap to app-private storage
                val selfiePath = try {
                    fileStorage.saveSelfie(capturedBitmap, currentStaffId)
                } catch (e: Exception) {
                    failAttendance(
                        errorState = AttendanceProcessStep.ERROR,
                        message = "Failed to save attendance selfie securely. Attendance not recorded."
                    )
                    return@launch
                }

                // Step 8: Persist Attendance in Room
                val now = System.currentTimeMillis()
                val staff = _uiState.value.staff
                val attendanceRecord = AttendanceRecord(
                    staffId = currentStaffId,
                    employeeId = staff?.employeeId ?: "",
                    staffName = staff?.name ?: "",
                    timestamp = now,
                    selfiePath = selfiePath,
                    latitude = locationResult.latitude,
                    longitude = locationResult.longitude,
                    locationAccuracy = locationResult.accuracy,
                    matchScore = matchScore,
                    verificationStatus = "VERIFIED"
                )

                attendanceRepo.recordAttendance(attendanceRecord)

                // Step 9: Success state
                _uiState.update {
                    it.copy(
                        step = AttendanceProcessStep.SUCCESS,
                        statusMessage = "Attendance Verified & Marked Successfully!",
                        successRecord = attendanceRecord,
                        locationDescription = locationResult.addressDescription
                            ?: "Lat: %.4f, Lng: %.4f (±%.1fm)".format(
                                locationResult.latitude,
                                locationResult.longitude,
                                locationResult.accuracy
                            ),
                        isProcessing = false,
                        errorMessage = null
                    )
                }
            } catch (t: Throwable) {
                failAttendance(
                    errorState = AttendanceProcessStep.ERROR,
                    message = "An unexpected error occurred. Attendance was not recorded."
                )
            } finally {
                isExecuting.set(false)
            }
        }
    }

    private fun failAttendance(errorState: AttendanceProcessStep, message: String) {
        _uiState.update {
            it.copy(
                step = errorState,
                statusMessage = "Verification Failed",
                errorMessage = message,
                isProcessing = false,
                canRetry = true
            )
        }
    }

    fun retry() {
        if (_uiState.value.isEnrolled) {
            _uiState.update {
                it.copy(
                    step = AttendanceProcessStep.READY,
                    statusMessage = "Position your face within the guide and tap Verify Attendance.",
                    errorMessage = null,
                    isProcessing = false
                )
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AttendanceMarkViewModel(
                    authRepo = ServiceLocator.authRepository,
                    staffRepo = ServiceLocator.staffRepository,
                    attendanceRepo = ServiceLocator.attendanceRepository,
                    faceEngine = ServiceLocator.faceRecognitionEngine,
                    locationService = ServiceLocator.locationService,
                    fileStorage = ServiceLocator.fileStorage
                )
            }
        }
    }
}
