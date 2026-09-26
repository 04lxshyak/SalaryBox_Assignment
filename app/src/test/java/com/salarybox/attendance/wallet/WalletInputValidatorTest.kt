package com.salarybox.attendance.wallet

import com.salarybox.attendance.wallet.domain.WalletInputValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WalletInputValidatorTest {
    @Test fun login_requires_valid_email_and_password() {
        assertEquals("Enter a valid email address", WalletInputValidator.loginError("not-an-email", "password"))
        assertEquals("Password must have at least 4 characters", WalletInputValidator.loginError("test@example.com", "123"))
        assertNull(WalletInputValidator.loginError("test@example.com", "1234"))
    }

    @Test fun money_amount_has_explicit_limits() {
        assertEquals("Enter an amount between Rs. 1 and Rs. 100,000", WalletInputValidator.amountError(99))
        assertNull(WalletInputValidator.amountError(100))
        assertEquals("Enter an amount between Rs. 1 and Rs. 100,000", WalletInputValidator.amountError(10_000_001))
    }

    @Test fun send_recipient_is_required() {
        assertEquals("Enter a recipient name", WalletInputValidator.recipientError(" "))
        assertNull(WalletInputValidator.recipientError("Asha"))
    }
}
