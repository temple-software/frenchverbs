package com.templesoftware.frenchverbs.ui.verbs

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import com.templesoftware.frenchverbs.R
import com.templesoftware.frenchverbs.data.preferences.AppLanguage
import com.templesoftware.frenchverbs.domain.verbs.Choices
import com.templesoftware.frenchverbs.ui.app.VerbState
import com.templesoftware.frenchverbs.ui.components.HomeInformationButton
import com.templesoftware.frenchverbs.ui.components.TempleScreenScaffold
import com.templesoftware.frenchverbs.ui.navigation.NavigationAction

/** First-screen-only presentation; examples, ticks and disabled Next retain Pass 03 styling. */
@Composable
internal fun TensesScreen(state:VerbState,language:AppLanguage,onLanguage:(AppLanguage)->Unit,
    onToggle:(String?)->Unit,onNext:()->Unit,onAbout:()->Unit) {
    TempleScreenScaffold(stringResource(R.string.app_name),NavigationAction.NONE,{},language,onLanguage) {
        Box(Modifier.fillMaxSize().testTag("tenses_safe_area")) {
            BoxWithConstraints(Modifier.widthIn(max=1000.dp).fillMaxSize().align(Alignment.TopCenter).padding(horizontal=16.dp)) {
                val scale=LocalDensity.current.fontScale
                val columns=when { maxWidth>=720.dp*scale->3;maxWidth>=480.dp*scale->2;else->1 }
                SelectionPageLayout(options={
                    Column(Modifier.verticalScroll(rememberScrollState()).testTag("choices"),verticalArrangement=Arrangement.spacedBy(LocalSelectionGap.current)) {
                        if(state.storageError) Text(stringResource(R.string.storage_error),color=MaterialTheme.colorScheme.error)
                        Choices.tenses.chunked(columns).forEach { row ->
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                                row.forEach { id ->
                                    Box(Modifier.weight(1f)) { TenseCard(id,id in state.selection.tenses,{onToggle(id)}) }
                                }
                                repeat(columns-row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                },actions={
                    Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) {
                        TensesNext(state.selection.canContinue("tenses"),onNext,
                            Modifier.padding(horizontal=56.dp).widthIn(max=420.dp).fillMaxWidth().testTag("next"))
                    }
                })
            }
            // Anchored to the full safe viewport, independent of content and Next.
            HomeInformationButton(onAbout,Modifier.align(Alignment.BottomEnd).padding(16.dp))
        }
    }
}

@Composable
private fun TensesNext(enabled:Boolean,onClick:()->Unit,modifier:Modifier) {
    if(enabled) PrimaryAction(stringResource(R.string.next),onClick,modifier)
    else Box(modifier.heightIn(min=52.dp).clip(RoundedCornerShape(18.dp))
        .background(Color(0xFFD6D6D6)).clickable(enabled=false,role=Role.Button,onClick=onClick)
        .padding(horizontal=18.dp,vertical=10.dp),contentAlignment=Alignment.Center) {
        Text(stringResource(R.string.next),color=Color(0xFF595959),style=MaterialTheme.typography.labelLarge,textAlign=TextAlign.Center)
    }
}

@Composable
private fun TenseCard(id:String,checked:Boolean,onClick:()->Unit) {
    val short=LocalConfiguration.current.screenHeightDp<=360
    Surface(Modifier.fillMaxWidth().testTag("choice_$id").semantics { selected=checked }
        .toggleable(checked,role=Role.Checkbox,onValueChange={onClick()}),
        color=if(checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        shape=RoundedCornerShape(20.dp),
        border=BorderStroke(if(checked) 2.dp else 1.dp,if(checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.heightIn(min=if(short)48.dp else 60.dp).padding(horizontal=if(short)8.dp else 14.dp,vertical=if(short)3.dp else if(LocalDensity.current.fontScale in 1.01f..1.5f)6.dp else 10.dp),
            verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                Text(stringResource(ChoiceResources.tense.getValue(id)),style=if(short)MaterialTheme.typography.titleMedium.copy(fontSize=14.sp,lineHeight=18.sp) else MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                FittingTenseExamples(id,stringResource(ChoiceResources.example.getValue(id)))
            }
            Box(Modifier.size(if(short)24.dp else 30.dp).testTag("tense_tick_$id")
                .border(if(checked) 2.dp else 1.dp,MaterialTheme.colorScheme.primary,RoundedCornerShape(8.dp)),contentAlignment=Alignment.Center) {
                if(checked) Icon(Icons.Default.Check,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(28.dp))
            }
        }
    }
}

@Composable
private fun FittingTenseExamples(id:String,examples:String) {
    val measurer=rememberTextMeasurer()
    val style=MaterialTheme.typography.bodyMedium
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("tense_examples_slot_$id")) {
        val available=constraints.maxWidth
        val parts=examples.split(", ")
        val candidates=(parts.size downTo 1).map { parts.take(it).joinToString(", ") }
        val fitting=candidates.firstOrNull {
            measurer.measure(AnnotatedString(it),style,softWrap=false,maxLines=1).size.width<=available
        }
        // At exceptionally constrained widths even one conjugation may not fit.
        // Wrap the complete conjugation instead of requiring horizontal scrolling.
        Text(fitting?:parts.first(),style=style,softWrap=true,
            modifier=Modifier.testTag("tense_example_$id").then(
                Modifier.fillMaxWidth()))
    }
}
