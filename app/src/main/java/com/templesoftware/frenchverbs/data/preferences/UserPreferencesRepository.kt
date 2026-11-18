package com.templesoftware.frenchverbs.data.preferences

import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val preferences: Flow<UserPreferences>

    fun updateSystemLanguage(language: AppLanguage)

    suspend fun setLanguage(language: AppLanguage)
}
