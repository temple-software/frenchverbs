package com.templesoftware.frenchverbs.ui.app

import com.templesoftware.frenchverbs.data.preferences.AppLanguage

data class ApplicationUiState(
    val language: AppLanguage,
    val preferencesLoaded: Boolean = false,
)
