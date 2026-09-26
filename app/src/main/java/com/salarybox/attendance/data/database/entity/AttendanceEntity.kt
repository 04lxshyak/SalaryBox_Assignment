package com.salarybox.attendance.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.salarybox.attendance.domain.model.AttendanceRecord

@Entity(
    tableName = "attendance",
    foreignKeys = [
        ForeignKey(
            entity = StaffEntity::class,
            parentColumns = ["id"],
            childColumns = ["staffId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["staffId"]),
        Index(value = ["timestamp"])
    ]
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val staffId: Long,
    val timestamp: Long,
    val selfiePath: String,
    val latitude: Double,
    val longitude: Double,
    val locationAccuracy: Float,
    val matchScore: Float,
    val verificationStatus: String
)

fun AttendanceEntity.toAttendanceRecord(employeeId: String = "", staffName: String = ""): AttendanceRecord {
    return AttendanceRecord(
        id = id,
        staffId = staffId,
        employeeId = employeeId,
        staffName = staffName,
        timestamp = timestamp,
        selfiePath = selfiePath,
        latitude = latitude,
        longitude = longitude,
        locationAccuracy = locationAccuracy,
        matchScore = matchScore,
        verificationStatus = verificationStatus
    )
}

fun AttendanceRecord.toEntity(): AttendanceEntity {
    return AttendanceEntity(
        id = id,
        staffId = staffId,
        timestamp = timestamp,
        selfiePath = selfiePath,
        latitude = latitude,
        longitude = longitude,
        locationAccuracy = locationAccuracy,
        matchScore = matchScore,
        verificationStatus = verificationStatus
    )
}
