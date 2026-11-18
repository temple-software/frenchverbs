package com.templesoftware.frenchverbs.ui.about

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.templesoftware.frenchverbs.R
import com.templesoftware.frenchverbs.data.preferences.AppLanguage
import com.templesoftware.frenchverbs.ui.components.PremiumGradientButton
import com.templesoftware.frenchverbs.ui.components.TempleScreenScaffold
import com.templesoftware.frenchverbs.ui.navigation.NavigationAction
import com.templesoftware.frenchverbs.config.AppContent

/** Informational content may scroll at large fonts; educational screens keep their own fit policy. */
@Composable
fun AboutScreen(language: AppLanguage, onBack: () -> Unit, launch: ((Intent) -> Unit)? = null, content: AppContent = AppContent.Default) {
    val context = LocalView.current.context
    var unavailable by rememberSaveable { mutableStateOf<String?>(null) }
    val open: (String?) -> Unit = { url ->
        if (url == null) { unavailable = "" } else
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri()).addCategory(Intent.CATEGORY_BROWSABLE)
            if (launch != null) launch(intent) else context.startActivity(intent)
        } catch (_: ActivityNotFoundException) { unavailable = url
        } catch (_: SecurityException) { unavailable = url }
    }
    TempleScreenScaffold(stringResource(R.string.about_screen), NavigationAction.BACK, onBack, language) {
        // The scaffold already applies horizontal system insets; do not apply them twice.
        BoxWithConstraints(Modifier.fillMaxSize()
            .testTag("about_viewport"), contentAlignment = Alignment.TopCenter) {
            val density = LocalDensity.current
            val measurer = rememberTextMeasurer()
            val landscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val contentWidth = minOf(if (landscape) 1100.dp else 560.dp, maxWidth - 32.dp)
            val columnGap = 16.dp
            val shortLandscape=landscape && maxHeight<260.dp
            val width = if (landscape) (contentWidth - columnGap) * (if(shortLandscape).5f else .58f) else contentWidth
            val introWidth = if (landscape) contentWidth - columnGap - width else width
            val accessible=density.fontScale>1.5f
            val benefitRows=if(accessible)listOf(0..1,2..3,4..4) else listOf(0..2,3..4)
            val badgePadding = if (landscape) 2.dp else 6.dp
            val badgeIcon = if (landscape) 18.dp else 22.dp
            val badgeGap = if (landscape) 2.dp else 4.dp
            val preferredBadgeStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp, lineHeight = (if (landscape) 14 else 16).sp,
                letterSpacing = 0.sp,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            val labels = listOf(R.string.about_free, R.string.about_no_ads, R.string.about_safe,
                R.string.about_nice, R.string.about_school).map { stringResource(it) }
            val icons = listOf(R.drawable.nice_badge_gift, R.drawable.nice_badge_no_adverts,
                R.drawable.nice_badge_shield, R.drawable.nice_badge_pin, R.drawable.nice_badge_school)
            val backgrounds = listOf(0xFFDCF7ED, 0xFFFFE5EB, 0xFFDEF4FF, 0xFFF1E5FD, 0xFFFFF5C5)
            // Keep whole French words together even at 200%, rather than breaking them mid-word.
            // All badges share one measured size, with a readable physical 12.5dp floor.
            val badgeStyle = remember(labels, width, density, landscape) {
                if(accessible)return@remember preferredBadgeStyle
                fun fits(ratio: Float) = labels.mapIndexed { index, text ->
                    val columns = benefitRows.first{index in it}.count()
                    val available = with(density) { ((width - 8.dp * (columns - 1)) / columns - badgePadding * 2).toPx() }
                    val style = preferredBadgeStyle.copy(fontSize = (12.5f * ratio).sp)
                    text.split(' ').all { measurer.measure(it, style, softWrap = false).size.width <= available - 2f }
                }.all { it }
                // Measure each candidate: Android's accessibility font scaling is nonlinear.
                var low = with(density) { 12.5.dp.toSp().value } / 12.5f
                var high = 1f
                repeat(12) { val mid = (low + high) / 2; if (fits(mid)) low = mid else high = mid }
                val ratio = if (fits(1f)) 1f else low
                preferredBadgeStyle.copy(fontSize = (12.5f * ratio).sp, lineHeight = ((if (landscape) 14f else 16f) * ratio).sp)
            }
            val badgeHeight = remember(labels, width, density, badgeStyle, landscape) {
                labels.mapIndexed { index, text ->
                    val columns = benefitRows.first{index in it}.count()
                    val textWidth = (width - 8.dp * (columns - 1)) / columns - badgePadding * 2
                    with(density) { measurer.measure(text, badgeStyle,
                        constraints = Constraints(maxWidth = textWidth.roundToPx())).size.height.toDp() }
                }.maxOrNull()!! + badgeIcon + badgeGap + badgePadding * 2 + 2.dp + (if (landscape) 0.dp else 2.dp)
            }
            val preferredLinkStyle = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, lineHeight = 19.sp,
                letterSpacing = 0.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            val guide = stringResource(R.string.about_guide)
            val privacy = stringResource(R.string.about_privacy)
            val minimumLinkStyle = preferredLinkStyle.copy(fontSize = with(density) { 15.dp.toSp() })
            val stackLinks = accessible || (guide + " " + privacy).split(' ').any {
                measurer.measure(it, minimumLinkStyle, softWrap = false).size.width >
                    with(density) { ((introWidth - 10.dp) / 2 - 16.dp).toPx() } - 2f
            }
            val linkTextWidth = (if (stackLinks) introWidth else (introWidth - 10.dp) / 2) - 16.dp
            val linkStyle = remember(guide, privacy, linkTextWidth, density) {
                if(accessible)return@remember preferredLinkStyle
                val available = with(density) { linkTextWidth.toPx() }
                fun fits(ratio: Float): Boolean {
                    val style = preferredLinkStyle.copy(fontSize = (15f * ratio).sp)
                    return (guide + " " + privacy).split(' ').all {
                        measurer.measure(it, style, softWrap = false).size.width <= available - 2f
                    }
                }
                var low = with(density) { 15.dp.toSp().value } / 15f
                var high = 1f
                repeat(12) { val mid = (low + high) / 2; if (fits(mid)) low = mid else high = mid }
                val ratio = if (fits(1f)) 1f else low
                preferredLinkStyle.copy(fontSize = (15f * ratio).sp, lineHeight = (19f * ratio).sp)
            }
            val linkHeight = maxOf(48.dp, with(density) {
                listOf(guide, privacy).maxOf { measurer.measure(it, linkStyle,
                    constraints = Constraints(maxWidth = linkTextWidth.roundToPx())).size.height }.toDp()
            } + 24.dp)
            @Composable fun Logo() {
                Image(painterResource(language.logoResource),
                    stringResource(R.string.about_logo), Modifier.width(minOf(if (landscape) 220.dp else 280.dp, introWidth * .85f))
                        .aspectRatio(323f / 68f).testTag("about_logo")
                        .then(if (MaterialTheme.colorScheme.background.luminance() < .5f)
                            Modifier.background(Color(0xFFF9FCFF), RoundedCornerShape(8.dp)).padding(6.dp)
                            else Modifier))
            }
            @Composable fun Heading() {
                Text(stringResource(content.title), Modifier.padding(top = if (landscape) 0.dp else if(density.fontScale>1f)8.dp else 20.dp).fillMaxWidth().testTag("about_title"),
                    color = MaterialTheme.colorScheme.onBackground, fontSize = (if (landscape) 22 else 26).sp, lineHeight = (if (landscape) 26 else 31).sp,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(stringResource(content.description), Modifier.fillMaxWidth().testTag("about_intro"),
                    color = MaterialTheme.colorScheme.onBackground, fontSize = (if (landscape) 14f else 14.5f).sp, lineHeight = (if (landscape) 18 else 20).sp, textAlign = TextAlign.Center)
            }
            @Composable fun Links() {
                if (stackLinks) Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AboutLink(guide, { open(content.guideUrl) }, Modifier.fillMaxWidth().height(linkHeight).testTag("about_guide"), linkStyle)
                    AboutLink(privacy, { open(content.privacyUrl) }, Modifier.fillMaxWidth().height(linkHeight).testTag("about_privacy"), linkStyle)
                } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AboutLink(guide, { open(content.guideUrl) }, Modifier.weight(1f).height(linkHeight).testTag("about_guide"), linkStyle)
                    AboutLink(privacy, { open(content.privacyUrl) }, Modifier.weight(1f).height(linkHeight).testTag("about_privacy"), linkStyle)
                }
            }
            @Composable fun Benefits() {
                benefitRows.forEach { row ->
                    Row(Modifier.fillMaxWidth().testTag("about_benefit_row_${row.first}"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { index ->
                            Column(Modifier.weight(1f).height(badgeHeight)
                                .background(Color(backgrounds[index]), RoundedCornerShape(14.dp))
                                .padding(badgePadding).testTag("about_badge_$index"),
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Top) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Image(painterResource(icons[index]), null, Modifier.size(badgeIcon))
                                    if (index == 3) Row(Modifier.size(16.dp, 12.dp)) {
                                        listOf(0xFF074DAD, 0xFFFFFFFF, 0xFFEB3B4E).forEach {
                                            Box(Modifier.weight(1f).fillMaxHeight().background(Color(it)))
                                        }
                                    }
                                }
                                Spacer(Modifier.height(badgeGap))
                                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text(labels[index], Modifier.fillMaxWidth().testTag("about_badge_text_$index"),
                                        style = badgeStyle, color = Color(0xFF10204A))
                                }
                            }
                        }
                    }
                }
            }
            @Composable fun MoreApps() {
                AboutLink(stringResource(R.string.about_more), { open(AppContent.MORE_APPS_URL) },
                    Modifier.fillMaxWidth().heightIn(min = if (landscape) 48.dp else 52.dp).testTag("about_more"), linkStyle, sun = true)
            }
            val scroll = rememberScrollState()
            val edgePadding = if (landscape && density.fontScale>1f)6.dp else if (landscape) 8.dp else 16.dp
            if (landscape) {
                val introGap = (maxHeight / 40).coerceIn(4.dp, 10.dp)
                // Measure natural content first, then share a button baseline across columns.
                // Grow the scrollable content at large fonts instead of squeezing or clipping it.
                val viewportHeight = maxHeight - edgePadding * 2
                Layout(
                    modifier = Modifier.width(contentWidth).fillMaxHeight().verticalScroll(scroll)
                        .padding(vertical = edgePadding).testTag("about_content"),
                    content = {
                        Box(Modifier.width(introWidth), contentAlignment = Alignment.TopCenter) { Logo() }
                        Column(Modifier.width(introWidth).testTag("about_left_column"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(introGap)) { Heading() }
                        Box(Modifier.width(introWidth)) { Links() }
                        Box(Modifier.width(width).testTag("about_right_column")) {
                            Column(Modifier.fillMaxWidth().testTag("about_benefits_block"),
                                verticalArrangement = Arrangement.spacedBy(8.dp)) { Benefits() }
                        }
                        Box(Modifier.width(width)) { MoreApps() }
                    }
                ) { measurables, constraints ->
                    val leftWidth = introWidth.roundToPx()
                    val rightWidth = width.roundToPx()
                    val items = measurables.mapIndexed { index, measurable ->
                        measurable.measure(Constraints.fixedWidth(if (index < 3) leftWidth else rightWidth))
                    }
                    val (logo, heading, links, benefits, more) = items
                    val gap = introGap.roundToPx()
                    val buttonHeight = maxOf(links.height, more.height)
                    val groupHeight = heading.height + gap + buttonHeight
                    val height = maxOf(viewportHeight.roundToPx(), logo.height + gap * 2 + groupHeight,
                        2 * (benefits.height + 16.dp.roundToPx()) - logo.height - heading.height - gap + buttonHeight)
                    val groupTop = logo.height + (height - logo.height - groupHeight) / 2
                    val buttonTop = groupTop + heading.height + gap
                    val rightX = leftWidth + columnGap.roundToPx()
                    layout(constraints.maxWidth, height) {
                        logo.placeRelative(0, 0)
                        heading.placeRelative(0, groupTop)
                        links.placeRelative(0, buttonTop)
                        benefits.placeRelative(rightX, (buttonTop - benefits.height) / 2)
                        more.placeRelative(rightX, buttonTop)
                    }
                }
            } else {
                val innerGap = if(density.fontScale>1f)6.dp else (maxHeight / 60).coerceIn(10.dp, 16.dp)
                val blockGap = if(density.fontScale>1f)12.dp else (maxHeight / 30).coerceIn(18.dp, 28.dp) + 20.dp
                Column(Modifier.width(width).fillMaxHeight().verticalScroll(scroll)
                    .padding(vertical = edgePadding).testTag("about_content"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(blockGap)) {
                    Column(Modifier.fillMaxWidth().testTag("about_introduction_block"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(innerGap)) {
                        Logo()
                        Heading()
                        Links()
                    }
                    Column(Modifier.fillMaxWidth().testTag("about_benefits_block"),
                        verticalArrangement = Arrangement.spacedBy(innerGap)) { Benefits() }
                    MoreApps()
                }

            }
        }
    }
    unavailable?.let { url ->
        val title = stringResource(R.string.about_screen)
        val message = if (url.isEmpty()) stringResource(R.string.about_link_pending)
            else stringResource(R.string.privacy_browser_unavailable, url)
        val close = stringResource(R.string.privacy_close)
        AlertDialog(onDismissRequest = { unavailable = null },
            title = { Text(title) },
            text = { SelectionContainer { Text(message) } },
            confirmButton = { TextButton(onClick = { unavailable = null }) { Text(close) } },
            modifier = Modifier.testTag("about_browser_unavailable"))
    }
}

@Composable
private fun AboutLink(label: String, click: () -> Unit, modifier: Modifier, style: TextStyle, sun: Boolean = false) {
    PremiumGradientButton(click, Color(0xFF4695ED), Color(0xFF0757BB), Color.White,
        modifier, tactilePress = true, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)) {
        if (sun) {
            Image(painterResource(R.drawable.nice_sun), null, Modifier.size(32.dp, 24.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(label, color = Color.White, style = style, modifier = Modifier.weight(1f, fill = !sun))
    }
}
