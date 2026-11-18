package com.templesoftware.frenchverbs.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.templesoftware.frenchverbs.data.preferences.AppLanguage
import com.templesoftware.frenchverbs.ui.navigation.NavigationAction
import com.templesoftware.frenchverbs.ui.theme.educationalColors

/** Only Home supplies language selection. Secondary screens have no language callback. */
@Composable
fun TempleScreenScaffold(title: String, navigation: NavigationAction, onNavigate: () -> Unit,
    language: AppLanguage, onLanguageSelected: ((AppLanguage) -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit) {
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing, topBar = {
        TempleAppBar(title, navigation, language, onNavigate, onLanguageSelected ?: {},
            showLanguageSelector = navigation == NavigationAction.NONE && onLanguageSelected != null)
    }) { insets ->
        Box(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets), contentAlignment = Alignment.Center, content = content)
    }
}

@Composable
fun TempleAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.educationalColors
    PremiumGradientButton(onClick, colors.primaryGradientTop, colors.primaryGradientBottom,
        colors.glossyHighlight, modifier.heightIn(min = 48.dp), tactilePress = true) {
        Text(text, color = androidx.compose.ui.graphics.Color.White,
            style = MaterialTheme.typography.labelLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
