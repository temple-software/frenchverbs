package com.templesoftware.frenchverbs.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.onStart
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow

private val Context.userPreferencesDataStore by preferencesDataStore(name = "user_preferences")

class DataStoreUserPreferencesRepository internal constructor(
    private val dataStore: DataStore<Preferences>,
    systemLanguage: AppLanguage,
    private val platformLanguage: PlatformAppLanguage? = null,
) : UserPreferencesRepository {
    constructor(context: Context, systemLanguage: AppLanguage = AppLanguage.fromSystem()) :
        this(context.applicationContext.userPreferencesDataStore, systemLanguage, PlatformAppLanguage.from(context))

    private val fallbackLanguage = MutableStateFlow(systemLanguage)

    override val preferences: Flow<UserPreferences> = dataStore.data
        .onStart { synchronizePlatformLanguage() }
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .combine(fallbackLanguage) { values, fallback ->
            UserPreferences(AppLanguage.fromLanguageTag(values[LANGUAGE]) ?: fallback)
        }

    override fun updateSystemLanguage(language: AppLanguage) {
        fallbackLanguage.value = language
    }

    override suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit {
            it[LANGUAGE] = language.languageTag
            if (platformLanguage != null) it[PLATFORM_MIGRATED] = true
        }
        platformLanguage?.write(language)
    }

    /** Import an existing explicit choice once; afterwards respect Android Settings too. */
    suspend fun synchronizePlatformLanguage() {
        val platform = platformLanguage ?: return
        dataStore.edit { values ->
            val androidChoice = platform.read()
            val savedChoice = AppLanguage.fromLanguageTag(values[LANGUAGE])
            if (values[PLATFORM_MIGRATED] != true && androidChoice == null && savedChoice != null) {
                platform.write(savedChoice)
            } else if (androidChoice != null) {
                values[LANGUAGE] = androidChoice.languageTag
            } else {
                values.remove(LANGUAGE)
            }
            values[PLATFORM_MIGRATED] = true
        }
    }

    private companion object {
        val PLATFORM_MIGRATED = booleanPreferencesKey("platform_language_migrated")
        val LANGUAGE = stringPreferencesKey("language")
    }
}
