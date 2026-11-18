package com.templesoftware.frenchverbs.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.templesoftware.frenchverbs.data.preferences.AppLanguage
import com.templesoftware.frenchverbs.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

class ApplicationViewModel(
    private val repository: UserPreferencesRepository,
    initialLanguage: AppLanguage,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ApplicationUiState(language = initialLanguage))
    val uiState: StateFlow<ApplicationUiState> = _uiState.asStateFlow()

    init {
        repository.preferences
            .map { preferences ->
                ApplicationUiState(
                    language = preferences.language,
                    preferencesLoaded = true,
                )
            }
            .onEach { _uiState.value = it }
            .launchIn(viewModelScope)
    }

    fun updateSystemLanguage(language: AppLanguage) = repository.updateSystemLanguage(language)

    fun selectLanguage(language: AppLanguage) {
        _uiState.value = _uiState.value.copy(language = language)
        viewModelScope.launch { repository.setLanguage(language) }
    }

    class Factory(
        private val repository: UserPreferencesRepository,
        private val initialLanguage: AppLanguage,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ApplicationViewModel::class.java))
            return ApplicationViewModel(repository, initialLanguage) as T
        }
    }
}
