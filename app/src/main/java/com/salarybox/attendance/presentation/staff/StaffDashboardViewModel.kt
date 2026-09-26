package com.salarybox.attendance.presentation.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.salarybox.attendance.di.ServiceLocator
import com.salarybox.attendance.domain.model.AttendanceRecord
import com.salarybox.attendance.domain.model.StaffMember
import com.salarybox.attendance.domain.repository.AttendanceRepository
import com.salarybox.attendance.domain.repository.AuthRepository
import com.salarybox.attendance.domain.repository.StaffRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StaffDashboardUiState(
    val staff: StaffMember? = null,
    val isEnrolled: Boolean = false,
    val todayAttendance: List<AttendanceRecord> = emptyList(),
    val isLoading: Boolean = false
)

class StaffDashboardViewModel(
    private val staffRepo: StaffRepository,
    private val attendanceRepo: AttendanceRepository,
    private val authRepo: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StaffDashboardUiState(isLoading = true))
    val uiState: StateFlow<StaffDashboardUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val session = authRepo.getCurrentSession().firstOrNull()
            if (session?.staffId != null) {
                val staffId = session.staffId
                val staff = staffRepo.getStaffById(staffId)
                val isEnrolled = staffRepo.isEnrolled(staffId)
                
                attendanceRepo.getTodayAttendance().collect { records ->
                    val myRecords = records.filter { it.staffId == staffId }
                    _uiState.update {
                        it.copy(
                            staff = staff,
                            isEnrolled = isEnrolled,
                            todayAttendance = myRecords,
                            isLoading = false
                        )
                    }
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepo.logout()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                StaffDashboardViewModel(
                    ServiceLocator.staffRepository,
                    ServiceLocator.attendanceRepository,
                    ServiceLocator.authRepository
                )
            }
        }
    }
}
