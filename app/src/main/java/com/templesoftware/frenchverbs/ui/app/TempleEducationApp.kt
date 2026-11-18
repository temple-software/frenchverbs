package com.templesoftware.frenchverbs.ui.app

import android.content.res.Configuration
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.templesoftware.frenchverbs.data.preferences.AppLanguage
import com.templesoftware.frenchverbs.data.preferences.DataStoreUserPreferencesRepository
import com.templesoftware.frenchverbs.ui.navigation.AppNavHost
import com.templesoftware.frenchverbs.ui.theme.TempleTheme
import java.util.Locale

@Composable
fun TempleEducationApp() {
    val context = LocalContext.current
    val initialLanguage = AppLanguage.fromSystem(LocalConfiguration.current.locales[0])
    val repository = remember(context) { DataStoreUserPreferencesRepository(context, initialLanguage) }
    val applicationViewModel: ApplicationViewModel = viewModel(
        factory = ApplicationViewModel.Factory(repository, initialLanguage),
    )
    val uiState by applicationViewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(initialLanguage) { applicationViewModel.updateSystemLanguage(initialLanguage) }

    val scope = androidx.compose.runtime.rememberCoroutineScope()
    androidx.lifecycle.compose.LifecycleResumeEffect(repository) {
        val job = scope.launch {
            repository.synchronizePlatformLanguage()
        }
        onPauseOrDispose { job.cancel() }
    }
    if (!uiState.preferencesLoaded) return

    LocalizedContent(language = uiState.language) {
        TempleTheme {
            AppNavHost(
                navController = rememberNavController(),
                language = uiState.language,
                onLanguageSelected = applicationViewModel::selectLanguage,
            )
        }
    }
}

@Composable
internal fun LocalizedContent(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val baseContext = LocalContext.current
    val baseConfiguration = LocalConfiguration.current
    val locale = remember(language) { Locale.forLanguageTag(language.languageTag) }
    val configuration = remember(
        baseConfiguration,
        locale,
    ) {
        Configuration(baseConfiguration).apply { setLocale(locale) }
    }
    val localizedContext = remember(baseContext, configuration) {
        baseContext.createConfigurationContext(configuration)
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides configuration,
        content = content,
    )
}
