package com.ownscreen.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Plain DataStore Preferences — no encryption. The OwnDroid API key is a local
 * device-automation key (not a cloud credential), so this is deliberately simple.
 *
 * There's deliberately no "tracking enabled" flag here: tracking starts automatically as soon as
 * Usage Access is granted (see ServiceStarter) — Usage Access is already the real, explicit gate
 * Android makes the user pass through, so a second manual toggle in front of it would be
 * redundant. Likewise there's no "enforcement enabled" flag: blocking is a direct manual action
 * per app (see AppDetailViewModel), not a background policy, so there's nothing to toggle.
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    companion object {
        val OWNDROID_API_KEY = stringPreferencesKey("owndroid_api_key")
        val POLL_INTERVAL_SECONDS = intPreferencesKey("poll_interval_seconds")
        const val DEFAULT_POLL_INTERVAL_SECONDS = 30
    }

    val apiKeyFlow: Flow<String> = dataStore.data.map { it[OWNDROID_API_KEY] ?: "" }
    val pollIntervalSecondsFlow: Flow<Int> =
        dataStore.data.map { it[POLL_INTERVAL_SECONDS] ?: DEFAULT_POLL_INTERVAL_SECONDS }

    suspend fun setApiKey(key: String) {
        dataStore.edit { it[OWNDROID_API_KEY] = key }
    }

    suspend fun setPollIntervalSeconds(seconds: Int) {
        dataStore.edit { it[POLL_INTERVAL_SECONDS] = seconds.coerceIn(15, 300) }
    }
}
