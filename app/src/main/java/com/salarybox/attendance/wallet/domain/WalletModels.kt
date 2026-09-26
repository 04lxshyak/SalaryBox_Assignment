package com.salarybox.attendance.wallet.domain

import java.text.NumberFormat
import java.util.Locale

data class User(val name: String, val email: String, val phone: String)
data class Provider(val id: Int, val name: String, val detail: String, val category: String)
data class Wallet(val balancePaise: Long)
data class Transaction(
    val id: Long,
    val title: String,
    val subtitle: String,
    val amountPaise: Long,
    val isCredit: Boolean,
    val timestamp: Long,
    val status: String = "Completed"
)
data class AppSettings(val darkMode: Boolean, val notificationsEnabled: Boolean)

fun Long.asRupees(): String = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(this / 100.0)

object WalletInputValidator {
    fun loginError(email: String, password: String): String? = when {
        !email.contains("@") || email.length > 120 -> "Enter a valid email address"
        password.length !in 4..128 -> "Password must have at least 4 characters"
        else -> null
    }
    fun amountError(amountPaise: Long): String? = if (amountPaise !in 100..10_000_000) "Enter an amount between Rs. 1 and Rs. 100,000" else null
    fun recipientError(name: String): String? = if (name.trim().length !in 2..60) "Enter a recipient name" else null
}
