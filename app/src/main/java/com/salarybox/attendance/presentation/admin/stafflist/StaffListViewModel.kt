package com.salarybox.attendance.presentation.admin.stafflist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.salarybox.attendance.di.ServiceLocator
import com.salarybox.attendance.domain.repository.AttendanceRepository
import com.salarybox.attendance.domain.repository.StaffRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StaffListItem(
    val staffId: Long,
    val employeeId: String,
    val name: String,
    val isEnrolled: Boolean,
    val latestAttendance: String?
)

data class StaffListUiState(
    val staffList: List<StaffListItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class StaffListViewModel(
    private val staffRepo: StaffRepository,
    private val attendanceRepo: AttendanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StaffListUiState(isLoading = true))
    val uiState: StateFlow<StaffListUiState> = _uiState.asStateFlow()

    private var allStaffItems: List<StaffListItem> = emptyList()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    init {
        loadStaff()
    }

    private fun loadStaff() {
        viewModelScope.launch {
            try {
                staffRepo.getAllStaff().collect { staffMembers ->
                    val items = staffMembers.map { staff ->
                        val isEnrolled = staffRepo.isEnrolled(staff.id)
                        val latestAtt = attendanceRepo.getLatestAttendance(staff.id)
                        val formattedDate = latestAtt?.timestamp?.let { dateFormat.format(Date(it)) }
                        StaffListItem(
                            staffId = staff.id,
                            employeeId = staff.employeeId,
                            name = staff.name,
                            isEnrolled = isEnrolled,
                            latestAttendance = formattedDate
                        )
                    }
                    allStaffItems = items
                    applySearch(_uiState.value.searchQuery)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applySearch(query)
    }

    private fun applySearch(query: String) {
        val filtered = if (query.isBlank()) {
            allStaffItems
        } else {
            allStaffItems.filter {
                it.name.contains(query, ignoreCase = true) || it.employeeId.contains(query, ignoreCase = true)
            }
        }
        _uiState.update { it.copy(staffList = filtered, isLoading = false) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                StaffListViewModel(ServiceLocator.staffRepository, ServiceLocator.attendanceRepository)
            }
        }
    }
}
