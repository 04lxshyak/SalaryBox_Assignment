package com.salarybox.attendance.presentation.admin.staffprofile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.salarybox.attendance.di.ServiceLocator
import com.salarybox.attendance.domain.model.AttendanceRecord
import com.salarybox.attendance.domain.model.FaceEnrollment
import com.salarybox.attendance.domain.model.StaffMember
import com.salarybox.attendance.domain.repository.AttendanceRepository
import com.salarybox.attendance.domain.repository.StaffRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StaffProfileUiState(
    val staff: StaffMember? = null,
    val enrollment: FaceEnrollment? = null,
    val latestAttendance: AttendanceRecord? = null,
    val attendanceHistory: List<AttendanceRecord> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class StaffProfileViewModel(
    private val staffId: Long,
    private val staffRepo: StaffRepository,
    private val attendanceRepo: AttendanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StaffProfileUiState(isLoading = true))
    val uiState: StateFlow<StaffProfileUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val staff = staffRepo.getStaffById(staffId)
                val enrollment = staffRepo.getEnrollment(staffId)
                
                attendanceRepo.getAttendanceByStaffId(staffId).collect { records ->
                    _uiState.update {
                        it.copy(
                            staff = staff,
                            enrollment = enrollment,
                            attendanceHistory = records,
                            latestAttendance = records.maxByOrNull { r -> r.timestamp },
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val savedStateHandle = createSavedStateHandle()
                val staffId = savedStateHandle.get<Long>("staffId") ?: 0L
                StaffProfileViewModel(
                    staffId,
                    ServiceLocator.staffRepository,
                    ServiceLocator.attendanceRepository
                )
            }
        }
    }
}
