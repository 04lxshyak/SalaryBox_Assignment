package com.salarybox.attendance.presentation.admin.addstaff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.salarybox.attendance.di.ServiceLocator
import com.salarybox.attendance.domain.model.StaffMember
import com.salarybox.attendance.domain.repository.StaffRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddStaffUiState(
    val name: String = "",
    val employeeId: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val nameError: String? = null,
    val employeeIdError: String? = null
)

class AddStaffViewModel(
    private val staffRepo: StaffRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddStaffUiState())
    val uiState: StateFlow<AddStaffUiState> = _uiState.asStateFlow()

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, nameError = null, error = null) }
    }

    fun onEmployeeIdChange(id: String) {
        _uiState.update { it.copy(employeeId = id, employeeIdError = null, error = null) }
    }

    fun addStaff() {
        val state = _uiState.value
        val nameTrimmed = state.name.trim()
        val idTrimmed = state.employeeId.trim()

        var hasError = false
        if (nameTrimmed.isEmpty()) {
            _uiState.update { it.copy(nameError = "Name cannot be empty") }
            hasError = true
        }
        if (idTrimmed.isEmpty()) {
            _uiState.update { it.copy(employeeIdError = "Employee ID cannot be empty") }
            hasError = true
        }
        if (hasError) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val existing = staffRepo.getStaffByEmployeeId(idTrimmed)
            if (existing != null) {
                _uiState.update { it.copy(isLoading = false, employeeIdError = "Employee ID already exists") }
                return@launch
            }

            val newStaff = StaffMember(
                id = 0,
                employeeId = idTrimmed,
                name = nameTrimmed,
                isActive = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            try {
                staffRepo.addStaff(newStaff)
                _uiState.update { it.copy(isLoading = false, success = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AddStaffViewModel(ServiceLocator.staffRepository)
            }
        }
    }
}
