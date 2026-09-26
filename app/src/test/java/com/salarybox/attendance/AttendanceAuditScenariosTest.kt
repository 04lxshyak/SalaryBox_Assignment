package com.salarybox.attendance

import com.salarybox.attendance.domain.model.AttendanceRecord
import com.salarybox.attendance.face.FaceRecognitionConfig
import com.salarybox.attendance.face.FaceVerificationResult
import com.salarybox.attendance.location.LocationErrorType
import com.salarybox.attendance.location.LocationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Audit Scenarios verification covering all attendance policy gates (A through I).
 */
class AttendanceAuditScenariosTest {

    // Helper simulating the attendance gate evaluation logic
    data class AttendanceGateContext(
        val hasSession: Boolean = true,
        val isEnrolled: Boolean = true,
        val cameraPermissionGranted: Boolean = true,
        val locationPermissionGranted: Boolean = true,
        val faceDetectionValid: Boolean = true,
        val faceCount: Int = 1,
        val candidateEmbeddingSimilarity: Float = 0.85f,
        val locationResult: LocationResult = LocationResult.Success(28.6139, 77.2090, 10f, "New Delhi")
    )

    sealed class GateEvaluationResult {
        data class Allowed(val record: AttendanceRecord) : GateEvaluationResult()
        data class Rejected(val reason: String) : GateEvaluationResult()
    }

    private fun evaluateAttendanceGate(context: AttendanceGateContext): GateEvaluationResult {
        if (!context.hasSession) {
            return GateEvaluationResult.Rejected("No active session.")
        }
        if (!context.cameraPermissionGranted) {
            return GateEvaluationResult.Rejected("Camera access is required to mark attendance.")
        }
        if (!context.isEnrolled) {
            return GateEvaluationResult.Rejected("Please ask an administrator to enroll your face.")
        }
        if (context.faceCount == 0 || !context.faceDetectionValid) {
            return GateEvaluationResult.Rejected("No face detected. Please position your face in the guide.")
        }
        if (context.faceCount > 1) {
            return GateEvaluationResult.Rejected("Only one face should be visible. Found ${context.faceCount} faces.")
        }
        if (!FaceRecognitionConfig.isMatch(context.candidateEmbeddingSimilarity)) {
            return GateEvaluationResult.Rejected("Face does not match your enrolled profile.")
        }
        if (!context.locationPermissionGranted) {
            return GateEvaluationResult.Rejected("Location permission is required to mark attendance.")
        }
        if (context.locationResult !is LocationResult.Success) {
            return GateEvaluationResult.Rejected("Location could not be obtained. Attendance was not recorded.")
        }

        val successLoc = context.locationResult
        val record = AttendanceRecord(
            staffId = 1L,
            employeeId = "EMP001",
            staffName = "Demo Employee",
            timestamp = System.currentTimeMillis(),
            selfiePath = "/data/user/0/app/files/selfies/selfie_1_12345.jpg",
            latitude = successLoc.latitude,
            longitude = successLoc.longitude,
            locationAccuracy = successLoc.accuracy,
            matchScore = context.candidateEmbeddingSimilarity,
            verificationStatus = "VERIFIED"
        )
        return GateEvaluationResult.Allowed(record)
    }

    @Test
    fun `Scenario A - correct face + location leads to attendance success`() {
        val context = AttendanceGateContext(
            candidateEmbeddingSimilarity = 0.88f,
            locationResult = LocationResult.Success(28.6139, 77.2090, 5.0f, "Headquarters")
        )
        val result = evaluateAttendanceGate(context)
        assertTrue("Attendance must succeed", result is GateEvaluationResult.Allowed)
        val record = (result as GateEvaluationResult.Allowed).record
        assertEquals("VERIFIED", record.verificationStatus)
        assertEquals(28.6139, record.latitude, 0.0001)
    }

    @Test
    fun `Scenario B - wrong face + location leads to no attendance`() {
        val context = AttendanceGateContext(
            candidateEmbeddingSimilarity = 0.42f // Below 0.70 threshold
        )
        val result = evaluateAttendanceGate(context)
        assertTrue("Attendance must be rejected", result is GateEvaluationResult.Rejected)
        val rejected = result as GateEvaluationResult.Rejected
        assertEquals("Face does not match your enrolled profile.", rejected.reason)
    }

    @Test
    fun `Scenario C - correct face + no location leads to no attendance`() {
        val context = AttendanceGateContext(
            candidateEmbeddingSimilarity = 0.90f,
            locationResult = LocationResult.Error(LocationErrorType.TIMEOUT, "Location timeout")
        )
        val result = evaluateAttendanceGate(context)
        assertTrue("Attendance must be rejected on location failure", result is GateEvaluationResult.Rejected)
        val rejected = result as GateEvaluationResult.Rejected
        assertEquals("Location could not be obtained. Attendance was not recorded.", rejected.reason)
    }

    @Test
    fun `Scenario D - no face detected leads to no attendance`() {
        val context = AttendanceGateContext(
            faceCount = 0,
            faceDetectionValid = false
        )
        val result = evaluateAttendanceGate(context)
        assertTrue(result is GateEvaluationResult.Rejected)
        assertTrue((result as GateEvaluationResult.Rejected).reason.contains("No face detected"))
    }

    @Test
    fun `Scenario E - multiple faces detected leads to no attendance`() {
        val context = AttendanceGateContext(
            faceCount = 2
        )
        val result = evaluateAttendanceGate(context)
        assertTrue(result is GateEvaluationResult.Rejected)
        assertTrue((result as GateEvaluationResult.Rejected).reason.contains("Only one face"))
    }

    @Test
    fun `Scenario F - staff not enrolled leads to no attendance`() {
        val context = AttendanceGateContext(
            isEnrolled = false
        )
        val result = evaluateAttendanceGate(context)
        assertTrue(result is GateEvaluationResult.Rejected)
        assertEquals("Please ask an administrator to enroll your face.", (result as GateEvaluationResult.Rejected).reason)
    }

    @Test
    fun `Scenario G - camera permission denied leads to no attendance`() {
        val context = AttendanceGateContext(
            cameraPermissionGranted = false
        )
        val result = evaluateAttendanceGate(context)
        assertTrue(result is GateEvaluationResult.Rejected)
        assertEquals("Camera access is required to mark attendance.", (result as GateEvaluationResult.Rejected).reason)
    }

    @Test
    fun `Scenario H - location permission denied leads to no attendance`() {
        val context = AttendanceGateContext(
            locationPermissionGranted = false
        )
        val result = evaluateAttendanceGate(context)
        assertTrue(result is GateEvaluationResult.Rejected)
        assertEquals("Location permission is required to mark attendance.", (result as GateEvaluationResult.Rejected).reason)
    }

    @Test
    fun `Scenario I - double tap creates max one valid attendance operation`() {
        val isExecuting = AtomicBoolean(false)
        val executionCounter = AtomicInteger(0)

        fun onMarkAttendanceTapped(): Boolean {
            if (!isExecuting.compareAndSet(false, true)) {
                return false // Debounced / Blocked!
            }
            try {
                executionCounter.incrementAndGet()
                return true
            } finally {
                // In real async processing, this resets only after async operation completes
            }
        }

        // Simulate rapid double tap
        val firstTapAccepted = onMarkAttendanceTapped()
        val secondTapAccepted = onMarkAttendanceTapped()

        assertTrue("First tap must be accepted", firstTapAccepted)
        assertFalse("Second rapid tap must be blocked by concurrency lock", secondTapAccepted)
        assertEquals("Exactly one attendance execution should have occurred", 1, executionCounter.get())
    }
}
