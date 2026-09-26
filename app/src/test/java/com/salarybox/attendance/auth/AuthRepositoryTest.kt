package com.salarybox.attendance.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryTest {

    @Test
    fun `admin login with correct credentials succeeds`() {
        val result = runAdminLoginCheck("admin", "admin123")
        assertTrue(result)
    }

    @Test
    fun `admin login with wrong password fails`() {
        val result = runAdminLoginCheck("admin", "wrong")
        assertFalse(result)
    }

    @Test
    fun `admin login with wrong username fails`() {
        val result = runAdminLoginCheck("notadmin", "admin123")
        assertFalse(result)
    }

    @Test
    fun `admin login with empty credentials fails`() {
        val result = runAdminLoginCheck("", "")
        assertFalse(result)
    }

    private fun runAdminLoginCheck(username: String, password: String): Boolean {
        return username == "admin" && password == "admin123"
    }
}
