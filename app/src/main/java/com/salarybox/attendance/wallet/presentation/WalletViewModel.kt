package com.salarybox.attendance.wallet.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salarybox.attendance.wallet.data.AppPreferences
import com.salarybox.attendance.wallet.data.WalletRepository
import com.salarybox.attendance.wallet.domain.AppSettings
import com.salarybox.attendance.wallet.domain.Provider
import com.salarybox.attendance.wallet.domain.Transaction
import com.salarybox.attendance.wallet.domain.User
import com.salarybox.attendance.wallet.domain.Wallet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val repository: WalletRepository,
    private val preferences: AppPreferences
) : ViewModel() {
    val loggedIn = preferences.isLoggedIn.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val settings: StateFlow<AppSettings> = preferences.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings(false, true))
    val wallet: StateFlow<Wallet> = repository.wallet.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Wallet(0))
    val transactions: StateFlow<List<Transaction>> = repository.transactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val providers: StateFlow<List<Provider>> = repository.providers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val profile: StateFlow<User> = repository.profile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), User("", "", ""))
    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun login(email: String, password: String) = runAction { repository.login(email, password) }
    fun logout() = viewModelScope.launch { repository.logout() }
    fun addMoney(amountPaise: Long) = runAction { repository.addMoney(amountPaise) }
    fun sendMoney(name: String, amountPaise: Long) = runAction { repository.sendMoney(name, amountPaise) }
    fun syncProviders() = runAction { repository.syncProviders() }
    fun setDarkMode(value: Boolean) = viewModelScope.launch { preferences.setDarkMode(value) }
    fun setNotifications(value: Boolean) = viewModelScope.launch { preferences.setNotifications(value) }
    fun updateProfile(name: String, phone: String) = viewModelScope.launch {
        runCatching { repository.updateProfile(name, phone) }.onSuccess { _messages.emit("Profile updated") }.onFailure { _messages.emit(it.message ?: "Unable to update profile") }
    }
    suspend fun provider(id: Int) = repository.provider(id)
    suspend fun transaction(id: Long) = repository.transaction(id)

    private fun runAction(action: suspend () -> Result<Unit>) = viewModelScope.launch {
        action().onSuccess { _messages.emit("Done") }.onFailure { _messages.emit(it.message ?: "Something went wrong") }
    }
}
