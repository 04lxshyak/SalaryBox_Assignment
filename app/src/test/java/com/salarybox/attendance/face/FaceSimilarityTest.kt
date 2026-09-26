package com.salarybox.attendance.face

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class FaceSimilarityTest {

    private val engine = object : FaceRecognitionEngine {
        override suspend fun initialize(context: android.content.Context) {}
        override fun isInitialized(): Boolean = true
        override suspend fun detectAndValidateFace(bitmap: android.graphics.Bitmap): FaceDetectionResult =
            FaceDetectionResult(true)
        override suspend fun generateEmbedding(faceBitmap: android.graphics.Bitmap): FloatArray? = null
        override fun compareFaces(embedding1: FloatArray, embedding2: FloatArray): Float {
            if (embedding1.size != embedding2.size || embedding1.isEmpty()) return 0f
            var dot = 0f
            var norm1 = 0f
            var norm2 = 0f
            for (i in embedding1.indices) {
                dot += embedding1[i] * embedding2[i]
                norm1 += embedding1[i] * embedding1[i]
                norm2 += embedding2[i] * embedding2[i]
            }
            val denom = sqrt(norm1.toDouble()) * sqrt(norm2.toDouble())
            return if (denom == 0.0) 0f else (dot / denom).toFloat()
        }
        override fun release() {}
    }

    private fun normalize(v: FloatArray): FloatArray {
        var sumSq = 0.0
        for (x in v) sumSq += (x * x).toDouble()
        val norm = sqrt(sumSq).toFloat().coerceAtLeast(1e-10f)
        return FloatArray(v.size) { i -> v[i] / norm }
    }

    @Test
    fun `l2 normalization produces unit norm vector`() {
        val raw = floatArrayOf(3f, 4f, 0f, 0f)
        val normalized = normalize(raw)
        var sumSq = 0.0
        for (x in normalized) sumSq += (x * x).toDouble()
        assertEquals(1.0, sumSq, 0.0001)
        assertEquals(0.6f, normalized[0], 0.0001f)
        assertEquals(0.8f, normalized[1], 0.0001f)
    }

    @Test
    fun `identical embeddings have similarity 1`() {
        val embedding = normalize(floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f))
        val similarity = engine.compareFaces(embedding, embedding)
        assertEquals(1.0f, similarity, 0.001f)
        assertTrue(FaceRecognitionConfig.isMatch(similarity))
    }

    @Test
    fun `opposite embeddings have similarity -1`() {
        val a = floatArrayOf(1f, 0f, 0f)
        val b = floatArrayOf(-1f, 0f, 0f)
        val similarity = engine.compareFaces(a, b)
        assertEquals(-1.0f, similarity, 0.001f)
        assertFalse(FaceRecognitionConfig.isMatch(similarity))
    }

    @Test
    fun `orthogonal embeddings have similarity 0`() {
        val a = floatArrayOf(1f, 0f, 0f)
        val b = floatArrayOf(0f, 1f, 0f)
        val similarity = engine.compareFaces(a, b)
        assertEquals(0.0f, similarity, 0.001f)
        assertFalse(FaceRecognitionConfig.isMatch(similarity))
    }

    @Test
    fun `centralized threshold logic behaves correctly`() {
        assertEquals(0.70f, FaceRecognitionConfig.SIMILARITY_THRESHOLD, 0.001f)
        assertTrue(FaceRecognitionConfig.isMatch(0.70f))
        assertTrue(FaceRecognitionConfig.isMatch(0.85f))
        assertFalse(FaceRecognitionConfig.isMatch(0.699f))
        assertFalse(FaceRecognitionConfig.isMatch(0.40f))
    }

    @Test
    fun `multiple enrollment templates verify against best match`() {
        // Enrolled person with 3 template samples
        val template1 = normalize(floatArrayOf(0.9f, 0.1f, 0.0f, 0.0f))
        val template2 = normalize(floatArrayOf(0.85f, 0.2f, 0.1f, 0.0f))
        val template3 = normalize(floatArrayOf(0.88f, 0.15f, 0.05f, 0.0f))
        val enrolledTemplates = listOf(template1, template2, template3)

        // Live candidate from same person (similar to template 2)
        val candidateSamePerson = normalize(floatArrayOf(0.84f, 0.22f, 0.08f, 0.0f))
        val result = engine.verifyIdentity(candidateSamePerson, enrolledTemplates)

        assertTrue("Expected Match but got $result", result is FaceVerificationResult.Match)
        val match = result as FaceVerificationResult.Match
        assertTrue(match.matchScore >= FaceRecognitionConfig.SIMILARITY_THRESHOLD)
    }

    @Test
    fun `person A enrolled, person A matches and person B is rejected`() {
        // Person A enrolled embedding
        val personAEnrolled = normalize(floatArrayOf(0.95f, 0.1f, 0.05f, 0.02f, 0.01f))
        val templates = listOf(personAEnrolled)

        // Person A live selfie
        val personALive = normalize(floatArrayOf(0.92f, 0.12f, 0.04f, 0.03f, 0.01f))
        val resultA = engine.verifyIdentity(personALive, templates)
        assertTrue("Person A should match", resultA is FaceVerificationResult.Match)

        // Person B live selfie (different facial features)
        val personBLive = normalize(floatArrayOf(0.05f, 0.85f, 0.30f, 0.10f, 0.05f))
        val resultB = engine.verifyIdentity(personBLive, templates)
        assertTrue("Person B should be rejected", resultB is FaceVerificationResult.LowSimilarity)
        val lowSim = resultB as FaceVerificationResult.LowSimilarity
        assertTrue(lowSim.matchScore < FaceRecognitionConfig.SIMILARITY_THRESHOLD)
    }

    @Test
    fun `missing enrollment returns NoEnrollment result`() {
        val candidate = normalize(floatArrayOf(1f, 0f, 0f))
        val result = engine.verifyIdentity(candidate, emptyList())
        assertEquals(FaceVerificationResult.NoEnrollment, result)
        assertEquals("Please ask an administrator to enroll your face.", result.userMessage)
    }

    @Test
    fun `structured result messages are user friendly`() {
        val noFace = FaceVerificationResult.NoFace
        assertTrue(noFace.userMessage.contains("No face detected"))

        val multiFace = FaceVerificationResult.MultipleFaces(2)
        assertTrue(multiFace.userMessage.contains("Only one face"))

        val poorQuality = FaceVerificationResult.PoorQuality("Please keep your head level.")
        assertEquals("Please keep your head level.", poorQuality.userMessage)

        val mismatch = FaceVerificationResult.LowSimilarity(0.45f)
        assertEquals("Face does not match your enrolled profile.", mismatch.userMessage)
    }
}
