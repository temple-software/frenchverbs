package com.templesoftware.frenchverbs.ui.verbs

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.*
import kotlin.math.roundToInt

internal val LocalSelectionGap = staticCompositionLocalOf { 6.dp }

/** Measure compact content first, then share spare height between row gaps and
 * the space above/below the group. Next keeps the same preferred anchor on all
 * five screens, moving down only when the compact content needs more room. */
@Composable
internal fun SelectionPageLayout(options:@Composable ()->Unit,actions:@Composable ()->Unit, groupTogether:Boolean=false) {
    val scaled=LocalDensity.current.fontScale in 1.01f..1.5f
    SubcomposeLayout(Modifier.fillMaxSize().testTag("selection_page")) { constraints ->
        val loose=constraints.copy(minWidth=constraints.maxWidth,minHeight=0)
        val footer=subcompose("next",actions).single().measure(loose)
        if (groupTogether || (constraints.maxWidth>=600.dp.roundToPx() && constraints.maxHeight>=480.dp.roundToPx())) {
            val separation=24.dp.roundToPx()
            val content=subcompose("grouped-options") {
                CompositionLocalProvider(LocalSelectionGap provides 16.dp) { options() }
            }.single().measure(loose.copy(maxHeight=(constraints.maxHeight-footer.height-separation).coerceAtLeast(0)))
            val top=((constraints.maxHeight-content.height-footer.height-separation)/2).coerceAtLeast(0)
            return@SubcomposeLayout layout(constraints.maxWidth,constraints.maxHeight) {
                content.placeRelative(0,top)
                footer.placeRelative(0,top+content.height+separation)
            }
        }
        val short=constraints.maxHeight<250.dp.roundToPx() || (scaled && constraints.maxHeight<400.dp.roundToPx())
        val baseGap=if(short || scaled)2.dp else 6.dp
        val limit=(constraints.maxHeight-footer.height-(if(short)4.dp else 10.dp).roundToPx()).coerceAtLeast(0)
        val separation=(if(short)6.dp else 16.dp).roundToPx()
        val maximum=(limit-separation).coerceAtLeast(0)
        val compact=subcompose("compact-measurement") {
            Box(Modifier.clearAndSetSemantics { testTag="selection_measurement" }) {
                CompositionLocalProvider(LocalSelectionGap provides baseGap) { options() }
            }
        }.single().measure(loose.copy(maxHeight=maximum))
        val preferred=((constraints.maxHeight-footer.height)*0.82f).roundToInt()
        // Dense grids still receive breathing room when the safe viewport permits it.
        val breathing=(maximum-compact.height).coerceIn(0,24.dp.roundToPx())
        val nextY=maxOf(preferred,compact.height+separation+breathing).coerceIn(0,limit)
        val area=(nextY-separation+2.dp.roundToPx()).coerceAtLeast(0)
        val spare=(area-compact.height).coerceAtLeast(0)
        // At most 20dp between rows; keep some spare height around the group.
        val rowGap=baseGap+((spare.toDp()-2.dp).coerceAtLeast(0.dp)/12).coerceAtMost(14.dp)
        val content=subcompose("options") {
            CompositionLocalProvider(LocalSelectionGap provides rowGap) { options() }
        }.single().measure(loose.copy(maxHeight=area))
        layout(constraints.maxWidth,constraints.maxHeight) {
            content.placeRelative(0,((area-content.height)/2).coerceAtLeast(0))
            footer.placeRelative(0,nextY)
        }
    }
}

