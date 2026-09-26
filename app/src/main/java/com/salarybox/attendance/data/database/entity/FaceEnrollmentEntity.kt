package com.salarybox.attendance.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.salarybox.attendance.domain.model.FaceEnrollment

@Entity(
    tableName = "face_enrollment",
    foreignKeys = [
        ForeignKey(
            entity = StaffEntity::class,
            parentColumns = ["id"],
            childColumns = ["staffId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["staffId"], unique = true)]
)
data class FaceEnrollmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val staffId: Long,
    val embeddingData: String,
    val modelVersion: String,
    val sampleCount: Int,
    val enrolledAt: Long,
    val imagePaths: String
)

fun FaceEnrollmentEntity.toFaceEnrollment(): FaceEnrollment {
    val embeddingArray = if (embeddingData.isNotEmpty()) {
        embeddingData.split(",").mapNotNull { it.toFloatOrNull() }.toFloatArray()
    } else {
        FloatArray(0)
    }

    val imagesList = if (imagePaths.isNotEmpty()) {
        imagePaths.split(",")
    } else {
        emptyList()
    }

    return FaceEnrollment(
        id = id,
        staffId = staffId,
        embedding = embeddingArray,
        modelVersion = modelVersion,
        sampleCount = sampleCount,
        enrolledAt = enrolledAt,
        imagePaths = imagesList
    )
}

fun FaceEnrollment.toEntity(): FaceEnrollmentEntity {
    return FaceEnrollmentEntity(
        id = id,
        staffId = staffId,
        embeddingData = embedding.joinToString(","),
        modelVersion = modelVersion,
        sampleCount = sampleCount,
        enrolledAt = enrolledAt,
        imagePaths = imagePaths.joinToString(",")
    )
}
