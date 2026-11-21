package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppSpace
import com.example.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "parko_session_prefs")

class SessionPreferences(private val context: Context) {
    private object Keys {
        val USER_ID = stringPreferencesKey("logged_in_user_id")
        val ACTIVE_SPACE = stringPreferencesKey("active_space")
        val REMEMBER_ME = booleanPreferencesKey("remember_me")
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val loggedInUserId: Flow<String?> = context.dataStore.data.map { it[Keys.USER_ID] }

    val activeSpace: Flow<AppSpace> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.ACTIVE_SPACE] ?: AppSpace.PERSONAL.name
        AppSpace.entries.firstOrNull { it.name == raw } ?: AppSpace.PERSONAL
    }

    val language: Flow<String> = context.dataStore.data.map { it[Keys.LANGUAGE] ?: "Français" }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        ThemeMode.entries.firstOrNull { it.name == raw } ?: ThemeMode.SYSTEM
    }

    suspend fun saveSession(userId: String, rememberMe: Boolean, defaultSpace: AppSpace = AppSpace.PERSONAL) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_ID] = userId
            prefs[Keys.REMEMBER_ME] = rememberMe
            prefs[Keys.ACTIVE_SPACE] = defaultSpace.name
        }
    }

    suspend fun setActiveSpace(space: AppSpace) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACTIVE_SPACE] = space.name
        }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = lang
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.USER_ID)
            prefs[Keys.ACTIVE_SPACE] = AppSpace.PERSONAL.name
        }
    }
}
