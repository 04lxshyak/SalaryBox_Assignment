package com.salarybox.attendance.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.salarybox.attendance.domain.model.StaffMember

@Entity(
    tableName = "staff",
    indices = [Index(value = ["employeeId"], unique = true)]
)
data class StaffEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val employeeId: String,
    val name: String,
    val password: String = "1234",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

fun StaffEntity.toStaffMember(): StaffMember {
    return StaffMember(
        id = id,
        employeeId = employeeId,
        name = name,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun StaffMember.toEntity(): StaffEntity {
    return StaffEntity(
        id = id,
        employeeId = employeeId,
        name = name,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
        // note: password keeps its default unless we need to update it
    )
}
