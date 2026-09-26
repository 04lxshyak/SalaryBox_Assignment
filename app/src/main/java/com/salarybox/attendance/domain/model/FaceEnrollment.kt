package com.salarybox.attendance.domain.model

data class FaceEnrollment(
    val id: Long = 0,
    val staffId: Long,
    val embedding: FloatArray,
    val modelVersion: String = "MobileFaceNet-v1",
    val sampleCount: Int = 1,
    val enrolledAt: Long = System.currentTimeMillis(),
    val imagePaths: List<String> = emptyList()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FaceEnrollment

        if (id != other.id) return false
        if (staffId != other.staffId) return false
        if (!embedding.contentEquals(other.embedding)) return false
        if (modelVersion != other.modelVersion) return false
        if (sampleCount != other.sampleCount) return false
        if (enrolledAt != other.enrolledAt) return false
        if (imagePaths != other.imagePaths) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + staffId.hashCode()
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + modelVersion.hashCode()
        result = 31 * result + sampleCount
        result = 31 * result + enrolledAt.hashCode()
        result = 31 * result + imagePaths.hashCode()
        return result
    }
}
