package com.salarybox.attendance.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.salarybox.attendance.domain.model.AuthSession
import com.salarybox.attendance.domain.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.sessionDataStore by preferencesDataStore(name = "session_prefs")

class SessionManager(private val context: Context) {

    companion object {
        val USER_ID = stringPreferencesKey("USER_ID")
        val USER_NAME = stringPreferencesKey("USER_NAME")
        val USER_ROLE = stringPreferencesKey("USER_ROLE")
        val STAFF_ID = longPreferencesKey("STAFF_ID")
    }

    suspend fun saveSession(session: AuthSession) {
        context.sessionDataStore.edit { prefs ->
            prefs[USER_ID] = session.userId
            prefs[USER_NAME] = session.userName
            prefs[USER_ROLE] = session.role.name
            if (session.staffId != null) {
                prefs[STAFF_ID] = session.staffId
            } else {
                prefs.remove(STAFF_ID)
            }
        }
    }

    fun getSession(): Flow<AuthSession?> {
        return context.sessionDataStore.data.map { prefs ->
            val userId = prefs[USER_ID] ?: return@map null
            val userName = prefs[USER_NAME] ?: return@map null
            val userRoleStr = prefs[USER_ROLE] ?: return@map null
            val staffId = prefs[STAFF_ID]
            
            AuthSession(
                userId = userId,
                userName = userName,
                role = UserRole.valueOf(userRoleStr),
                staffId = staffId
            )
        }
    }

    suspend fun clearSession() {
        context.sessionDataStore.edit { prefs ->
            prefs.clear()
        }
    }

    fun isLoggedIn(): Flow<Boolean> {
        return context.sessionDataStore.data.map { prefs ->
            prefs[USER_ID] != null
        }
    }
}
