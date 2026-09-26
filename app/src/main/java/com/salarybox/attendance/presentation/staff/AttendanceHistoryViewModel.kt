package com.salarybox.attendance.presentation.staff

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.salarybox.attendance.di.ServiceLocator
import com.salarybox.attendance.domain.model.AttendanceRecord
import com.salarybox.attendance.domain.repository.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AttendanceHistoryUiState(
    val history: List<AttendanceRecord> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class AttendanceHistoryViewModel(
    private val staffId: Long,
    private val attendanceRepo: AttendanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AttendanceHistoryUiState(isLoading = true))
    val uiState: StateFlow<AttendanceHistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            try {
                attendanceRepo.getAttendanceByStaffId(staffId).collect { records ->
                    _uiState.update {
                        it.copy(
                            history = records.sortedByDescending { r -> r.timestamp },
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
                AttendanceHistoryViewModel(
                    staffId,
                    ServiceLocator.attendanceRepository
                )
            }
        }
    }
}
