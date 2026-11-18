package com.templesoftware.frenchverbs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import com.templesoftware.frenchverbs.ui.layout.currentResponsiveLayoutInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import com.templesoftware.frenchverbs.R
import com.templesoftware.frenchverbs.data.preferences.AppLanguage
import com.templesoftware.frenchverbs.ui.navigation.NavigationAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TempleAppBar(
    title: String?,
    navigationAction: NavigationAction,
    language: AppLanguage,
    onNavigationClick: () -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    centeredTitle: Boolean = true,
    showLanguageSelector: Boolean = false,
) {
    val titleContent: @Composable () -> Unit = {
        if (title != null) {
            Text(title, modifier = Modifier.testTag("screen_title"), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        }
    }
    val navigationContent: @Composable () -> Unit = {
        NavigationControl(navigationAction, onNavigationClick)
    }
    val actionsContent: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
        if (showLanguageSelector && navigationAction == NavigationAction.NONE) LanguageSelector(language = language, onLanguageSelected = onLanguageSelected)
    }
    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        titleContentColor = MaterialTheme.colorScheme.primary,
        navigationIconContentColor = MaterialTheme.colorScheme.primary,
        actionIconContentColor = MaterialTheme.colorScheme.primary,
    )

    BoxWithConstraints {
        val appBarHeight = templeAppBarHeight(title ?: "", maxWidth)
        if (centeredTitle) {
            CenterAlignedTopAppBar(
                expandedHeight = appBarHeight,
                title = titleContent,
                navigationIcon = navigationContent,
                actions = actionsContent,
                colors = colors,
            )
        } else {
            TopAppBar(
                expandedHeight = appBarHeight,
                title = titleContent,
                navigationIcon = navigationContent,
                actions = actionsContent,
                colors = colors,
            )
        }
    }
}

@Composable
private fun NavigationControl(
    navigationAction: NavigationAction,
    onNavigationClick: () -> Unit,
) {
    when (navigationAction) {
        NavigationAction.NONE -> Unit
        NavigationAction.BACK -> IconButton(onClick = onNavigationClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.navigate_back),
            )
        }
        NavigationAction.HOME -> IconButton(onClick = onNavigationClick) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = stringResource(R.string.navigate_home),
            )
        }
    }
}

@Composable
private fun LanguageSelector(
    language: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val description = stringResource(R.string.choose_language,
        stringResource(language.nameResource))
    Box {
        // Compact 36dp visible menu; the whole 68x48dp slot remains tappable.
        // IconButton's capsule clip cuts the corners off this wider rectangle.
        // Keep the accessible hit area separate from the passive decoration.
        val menuShape = RoundedCornerShape(10.dp)
        Box(modifier = Modifier.size(68.dp, 48.dp)
            .clickable(role = androidx.compose.ui.semantics.Role.Button) { expanded = true }
            .testTag("language_selector").semantics { contentDescription = description },
            contentAlignment = Alignment.Center) {
                Row(Modifier.shadow(2.dp, menuShape)
                    .background(Color(0xFFF9FCFF), menuShape)
                    .border(1.25.dp, Color(0xFFA6BDD9), menuShape)
                    .height(36.dp).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LanguageFlag(language)
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null,
                        tint = Color(0xFF173A7A), modifier = Modifier.size(16.dp)
                            .testTag("language_menu_chevron"))
                }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            for (choice in AppLanguage.supported) {
                DropdownMenuItem(
                    text = { Text(stringResource(choice.nameResource)) },
                    leadingIcon = { LanguageFlag(choice) },
                    trailingIcon = { if (choice == language) Icon(Icons.Default.Check, contentDescription = null) },
                    onClick = { expanded = false; onLanguageSelected(choice) },
                    modifier = Modifier.heightIn(min = 48.dp).semantics { selected = choice == language },
                )
            }
        }
    }
}

/** Shared title measurement also lets selection screens align actions across headers. */
@Composable
fun templeAppBarHeight(title: String, width: androidx.compose.ui.unit.Dp): androidx.compose.ui.unit.Dp {
    val density = LocalDensity.current
    val titleWidth = with(density) { (width - 144.dp).roundToPx().coerceAtLeast(1) }
    val measured = rememberTextMeasurer().measure(title, MaterialTheme.typography.titleLarge,
        constraints = Constraints(maxWidth = titleWidth))
    val landscape = currentResponsiveLayoutInfo().isLandscape
    return maxOf(if (landscape) 48.dp else 64.dp,
        with(density) { measured.size.height.toDp() } + if (landscape) 4.dp else 16.dp)
}
