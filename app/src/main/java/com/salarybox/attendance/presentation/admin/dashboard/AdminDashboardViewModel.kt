package com.salarybox.attendance.presentation.admin.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.salarybox.attendance.di.ServiceLocator
import com.salarybox.attendance.domain.repository.AttendanceRepository
import com.salarybox.attendance.domain.repository.AuthRepository
import com.salarybox.attendance.domain.repository.StaffRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminDashboardUiState(
    val totalStaff: Int = 0,
    val enrolledStaff: Int = 0,
    val todayAttendance: Int = 0,
    val isLoading: Boolean = false
)

class AdminDashboardViewModel(
    private val staffRepo: StaffRepository,
    private val attendanceRepo: AttendanceRepository,
    private val authRepo: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminDashboardUiState(isLoading = true))
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    totalStaff = staffRepo.getStaffCount(),
                    enrolledStaff = staffRepo.getEnrolledCount(),
                    todayAttendance = attendanceRepo.getTodayAttendanceCount(),
                    isLoading = false
                )
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
                AdminDashboardViewModel(
                    ServiceLocator.staffRepository,
                    ServiceLocator.attendanceRepository,
                    ServiceLocator.authRepository
                )
            }
        }
    }
}
