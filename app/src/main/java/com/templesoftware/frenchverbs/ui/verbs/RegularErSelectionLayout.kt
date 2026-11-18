package com.templesoftware.frenchverbs.ui.verbs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.*
import com.templesoftware.frenchverbs.R

import com.templesoftware.frenchverbs.ui.app.VerbState

private data class ErCardContent(val id: String, val title: String, val description: String)
private data class ErGridMetrics(val title: TextStyle, val body: TextStyle, val padding: Dp,
    val gap: Dp, val heights: List<Dp>)

/** Measure translated text before choosing spacing; never trade readable type for a fit. */
@Composable
internal fun RegularErSelectionLayout(state: VerbState, onToggle: (String?) -> Unit, onNext: () -> Unit) {
    val selection = state.selection
    val cards = selection.visible("er").map { id ->
        val title = when (id) {
            "standard" -> R.string.er_selection_category_standard
            "ger" -> R.string.er_selection_category_ger
            "cer" -> R.string.er_selection_category_cer
            "yer" -> R.string.er_selection_category_yer
            else -> ChoiceResources.category.getValue(id)
        }
        val description = when (id) {
            "standard" -> R.string.er_selection_description_standard
            "ger" -> R.string.er_selection_description_ger
            else -> ChoiceResources.description.getValue(id)
        }
        val example = if (id == "cer") R.string.er_selection_example_cer else ChoiceResources.example.getValue(id)
        ErCardContent(id, stringResource(title),
            stringResource(example).substringBefore(" → ") + " · " + stringResource(description))
    }
    val all = ErCardContent("all", stringResource(R.string.all), "")
    val density = LocalDensity.current
    val measure = rememberTextMeasurer()
    val config = LocalConfiguration.current
    val compactLandscape = config.screenWidthDp > config.screenHeightDp && (config.screenHeightDp <= 360 || density.fontScale in 1.01f..1.5f)
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = if (compactLandscape) 8.dp else 16.dp, vertical = 2.dp).testTag("selection_viewport"),
        contentAlignment = Alignment.Center) {
        val landscape = maxWidth > maxHeight
        val tablet = maxWidth >= 600.dp && maxHeight >= 480.dp
        val width = minOf(maxWidth, if (landscape) 1000.dp else 720.dp)
        val columns = if (landscape || tablet) 3 else if (width >= 300.dp) 2 else 1
        // All spans the portrait phone row, leaving four balanced pairs of categories.
        val inlineActions = landscape && (maxHeight < 260.dp || density.fontScale in 1.01f..1.5f) && density.fontScale <= 1.5f
        val spanAll = !landscape && !tablet
        val rows = if (spanAll) cards.chunked(columns) else
            (if (inlineActions || !selection.all("er")) listOf(all) + cards else cards).chunked(columns)
        val showAll = spanAll && !selection.all("er")
        val nextLabel = stringResource(R.string.next)
        val footerHeight = maxOf(52.dp, with(density) { measure.measure(nextLabel, MaterialTheme.typography.labelLarge).size.height.toDp() + 24.dp })
        val allHeight = if (showAll) maxOf(48.dp, with(density) { (24.sp.toPx() + 16.dp.toPx()).toDp() }) else 0.dp
        val choices = listOf(Triple(if (tablet) 20 else 18, 12.dp, 12.dp), Triple(16, 8.dp, 8.dp), Triple(16, 6.dp, 6.dp), Triple(16, 6.dp, 2.dp))
        val candidates = choices.map { (size, padding, gap) ->
            val title = MaterialTheme.typography.titleMedium.copy(fontSize = size.sp, lineHeight = (size + 2).sp, fontWeight = FontWeight.Bold, lineBreak = LineBreak.Heading)
            val body = MaterialTheme.typography.bodyMedium.copy(fontSize = (if (tablet) 16 else 14).sp, lineHeight = (if (tablet) 20 else 16).sp, lineBreak = LineBreak.Heading)
            val cellWidth = (width - gap * (columns - 1)) / columns
            fun height(card: ErCardContent): Dp = with(density) {
                if (inlineActions && card.id == "all") return@with if (selection.all("er")) footerHeight else 48.dp + gap + footerHeight
                val titleWidth = (cellWidth - padding * 2 - 30.dp).roundToPx().coerceAtLeast(1)
                val bodyWidth = (cellWidth - padding * 2).roundToPx().coerceAtLeast(1)
                val heading = maxOf(24.dp.roundToPx(), measure.measure(card.title, title, constraints = Constraints(maxWidth = titleWidth)).size.height)
                val description = if (card.description.isEmpty()) 0 else measure.measure(card.description, body, constraints = Constraints(maxWidth = bodyWidth)).size.height + 4.dp.roundToPx()
                maxOf(48.dp, (heading + description).toDp() + padding * 2)
            }
            ErGridMetrics(title, body, padding, gap, rows.map { row -> row.maxOf { height(it) } })
        }
        fun total(m: ErGridMetrics) = m.heights.fold(0.dp) { a, h -> a + h } + m.gap * (rows.size - 1) +
            (if (showAll) allHeight + m.gap else 0.dp) + (if (inlineActions) 0.dp else footerHeight + m.gap * 2 + 8.dp) + 4.dp
        val metrics = candidates.firstOrNull { total(it) <= maxHeight } ?: candidates.last()
        val surplus = (maxHeight - total(metrics)).coerceAtLeast(0.dp)
        val extra = minOf(surplus / rows.size, if (tablet) 40.dp else if (landscape) 12.dp else 20.dp)
        Column(Modifier.width(width).fillMaxHeight().testTag("selection_page"), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).testTag("choices"),
                verticalArrangement = Arrangement.spacedBy(metrics.gap)) {
                if (state.storageError) Text(stringResource(R.string.storage_error), color = MaterialTheme.colorScheme.error)
                if (showAll) ErOptionCard(all, false, { onToggle(null) }, metrics, Modifier.fillMaxWidth().height(allHeight))
                Column(Modifier.testTag("option_grid"), verticalArrangement = Arrangement.spacedBy(metrics.gap)) {
                    rows.forEachIndexed { index, row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(metrics.gap)) {
                            row.forEach { card ->
                                if (inlineActions && card.id == "all") {
                                    Column(Modifier.weight(1f).height(metrics.heights[index] + extra), verticalArrangement = Arrangement.Center) {
                                        if (!selection.all("er")) {
                                            ErOptionCard(all, false, { onToggle(null) }, metrics, Modifier.fillMaxWidth().weight(1f))
                                            Spacer(Modifier.height(metrics.gap))
                                        }
                                        PrimaryAction(nextLabel, onNext, Modifier.fillMaxWidth().height(footerHeight).testTag("next"), selection.canContinue("er"))
                                    }
                                } else ErOptionCard(card, card.id in selection.er, { onToggle(card.id.takeUnless { it == "all" }) }, metrics,
                                    Modifier.weight(1f).height(metrics.heights[index] + extra))
                            }
                            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            if (!inlineActions) {
            Spacer(Modifier.height(metrics.gap * 2))
            PrimaryAction(stringResource(R.string.next), onNext,
                Modifier.widthIn(max = 420.dp).fillMaxWidth(0.8f).height(footerHeight).testTag("next"), selection.canContinue("er"))
            Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ErOptionCard(card: ErCardContent, checked: Boolean, onClick: () -> Unit,
    metrics: ErGridMetrics, modifier: Modifier) {
    Surface(modifier.testTag("choice_${card.id}").semantics { selected = checked }
        .toggleable(checked, role = Role.Checkbox, onValueChange = { onClick() }),
        color = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(if (checked) 2.dp else 1.dp, if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxSize().padding(metrics.padding), verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(card.title, style = metrics.title, modifier = Modifier.weight(1f))
                Box(Modifier.size(24.dp).testTag("selection_tick_${card.id}")
                    .border(if (checked) 2.dp else 1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                    if (checked) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
            }
            if (card.description.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(card.description, style = metrics.body, modifier = Modifier.fillMaxWidth().testTag("secondary_${card.id}"))
            }
        }
    }
}


