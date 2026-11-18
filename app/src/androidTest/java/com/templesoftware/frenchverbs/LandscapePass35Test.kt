package com.templesoftware.frenchverbs

import android.content.res.Configuration
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.*
import androidx.lifecycle.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.platform.app.InstrumentationRegistry
import com.templesoftware.frenchverbs.data.preferences.*
import com.templesoftware.frenchverbs.domain.quiz.*
import com.templesoftware.frenchverbs.domain.verbs.*
import com.templesoftware.frenchverbs.ui.app.*
import com.templesoftware.frenchverbs.ui.navigation.AppNavHost
import com.templesoftware.frenchverbs.ui.quiz.*
import com.templesoftware.frenchverbs.ui.theme.TempleTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.math.roundToInt
import kotlin.random.Random


import com.templesoftware.frenchverbs.ui.verbs.*
import com.templesoftware.frenchverbs.domain.repeat.*
class LandscapePass35Test {
    private val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val rules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    private val out get()=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"pass35").apply{mkdirs()}
    private data class Case(val w:Int,val h:Int,val lang:AppLanguage,val font:Float=1f)
    @Composable private fun Frame(c:Case,content:@Composable ()->Unit) {
        BoxWithConstraints(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            val host=LocalDensity.current
            val d=minOf(with(host){maxWidth.toPx()}/c.w,with(host){maxHeight.toPx()}/c.h)
            val config=Configuration(LocalConfiguration.current).apply{screenWidthDp=c.w;screenHeightDp=c.h;fontScale=c.font;densityDpi=(d*160).roundToInt();orientation=if(c.w>c.h)2 else 1}
            CompositionLocalProvider(LocalDensity provides Density(d,c.font),LocalConfiguration provides config) {
                Box(Modifier.requiredSize(c.w.dp,(c.h-60).dp)){LocalizedContent(c.lang){TempleTheme{content()}}}
            }
        }
    }
    private fun capture(name:String) {
        val image=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(out,"$name.png").outputStream().use{image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};image.recycle()
    }
    private fun layout(tag:String):androidx.compose.ui.text.TextLayoutResult {
        val list=mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose.onNodeWithTag(tag,true).performSemanticsAction(SemanticsActions.GetTextLayoutResult){it(list)}
        return list.single()
    }
    @Test fun repeatTitlesLargerRowsAndPulsePresentation() {
        val timeline=RepeatTimeline()
        val c=mutableStateOf(Case(915,412,AppLanguage.ENGLISH_UK))
        val s=mutableStateOf(RepeatState(listOf(LearningItem("être",0,0),LearningItem("prendre",1,0)),elapsed=timeline.waitingAt,playback=RepeatPlayback.WAITING))
        compose.activityRule.scenario.onActivity{it.setContent{Frame(c.value){RepeatScreen(s.value,timeline,c.value.lang,{},{})}}}
        for(lang in AppLanguage.entries)for(case in listOf(Case(915,412,lang),Case(800,360,lang),Case(1280,800,lang),Case(412,915,lang),Case(800,360,lang,1.3f)))for(item in listOf(LearningItem("être",0,0),LearningItem("reconnaître",5,0),LearningItem("construire",3,0))) {
            compose.runOnIdle{c.value=case;s.value=s.value.copy(session=listOf(item,LearningItem("prendre",1,0)))}
            val landscape=case.w>case.h
            val title=layout("repeat_title")
            assertFalse("title overflow $case $item",title.hasVisualOverflow)
            if(landscape) {
                assertTrue(title.layoutInput.text.text.contains(item.verbId));assertTrue(title.layoutInput.text.text.startsWith(if(lang==AppLanguage.FRENCH)"Répéter : " else "Repeat: "))
                assertTrue(title.layoutInput.text.text.contains(when(item.tense){0->"le présent";5->"le plus-que-parfait";else->"le passé composé"}))
                if(case.font==1f)assertEquals(1,title.lineCount)
                compose.onNodeWithTag("repeat_verb").assertDoesNotExist();compose.onNodeWithTag("repeat_tense").assertDoesNotExist()
                assertTrue(layout("repeat_row_0").layoutInput.style.fontSize.value>=24f)
            } else {
                compose.onNodeWithTag("repeat_title").assertTextEquals(if(lang==AppLanguage.FRENCH)"Répéter"else "Repeat")
                compose.onNodeWithTag("repeat_verb").assertExists();compose.onNodeWithTag("repeat_tense").assertExists()
                assertEquals(25f,layout("repeat_row_0").layoutInput.style.fontSize.value)
            }
            for(i in 0..5)assertFalse("row overflow $case $item $i",layout("repeat_row_$i").hasVisualOverflow)
            compose.onNodeWithTag("repeat_navigation").assertIsDisplayed()
            if(landscape && case.h!=360 && case.font==1f) {
                compose.onNodeWithTag("repeat_row_5").assertIsDisplayed()
                val rows=(0..5).map{compose.onNodeWithTag("repeat_row_$it",true).fetchSemanticsNode().boundsInRoot}
                val gaps=rows.zipWithNext().map{(a,b)->b.top-a.bottom}
                assertTrue(gaps.max()-gaps.min()<3f)
                val viewport=compose.onNodeWithTag("repeat_viewport").fetchSemanticsNode().boundsInRoot
                assertTrue(kotlin.math.abs((rows.first().top-viewport.top)-(viewport.bottom-rows.last().bottom))<3f)
                val range=compose.onNodeWithTag("repeat_scroll").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange].maxValue()
                assertEquals(0f,range,1f)
            }
            capture("repeat-${case.w}-${case.h}-$lang-${case.font}-${item.verbId}")
        }
        compose.runOnIdle{c.value=Case(915,412,AppLanguage.FRENCH)}
        for(phase in timeline.phases.filter{it.name.startsWith("instruction") || it.name=="subject" || it.name=="verb"}) {
            compose.runOnIdle{s.value=s.value.copy(elapsed=phase.start+phase.duration/2,playback=RepeatPlayback.PLAYING)}
            val spans=layout("repeat_title").layoutInput.text.spanStyles
            assertEquals(timeline.headingVerbAlpha(s.value.elapsed),spans[0].item.color.alpha,.01f)
            assertEquals(timeline.headingTenseAlpha(s.value.elapsed),spans[1].item.color.alpha,.01f)
            for(row in 0..5) {
                val rowSpans=layout("repeat_row_$row").layoutInput.text.spanStyles
                assertEquals(timeline.subjectAlpha(row,s.value.elapsed),rowSpans[0].item.color.alpha,.01f)
                assertEquals(timeline.verbAlpha(row,s.value.elapsed),rowSpans[2].item.color.alpha,.01f)
            }
        }
    }
    @Test fun conjugateCompletionSpacingLabelsAdvanceAndFinalHome() {
        fun completed()=ConjugateViewModel(SavedStateHandle(mapOf("quizSession" to arrayListOf(LearningItem("parler",0,0).encode(),LearningItem("être",0,0).encode()),"quizLocked" to arrayListOf(0,1,2,3,4,5),"quizActive" to -1)))
        val model=mutableStateOf(completed());val c=mutableStateOf(Case(915,412,AppLanguage.ENGLISH_UK))
        compose.activityRule.scenario.onActivity{it.setContent{Frame(c.value){val m=model.value;val s by m.state.collectAsState();ConjugateScreen(s,m.question!!,c.value.lang,{},m::letter,m::clear,m::submit,m::nextQuestion,m::edit)}}}
        for(lang in AppLanguage.entries)for(case in listOf(Case(915,412,lang),Case(1280,800,lang),Case(412,915,lang))) {
            compose.runOnIdle{c.value=case;model.value=completed()}
            val label=if(case.w>case.h){if(lang==AppLanguage.FRENCH)"Suite →"else "Next →"}else{if(lang==AppLanguage.FRENCH)"Question suivante"else "Next question"}
            compose.onNodeWithText(label).assertIsDisplayed();compose.onNodeWithTag("quiz_success").assertIsDisplayed()
            val result=mutableListOf<androidx.compose.ui.text.TextLayoutResult>();compose.onNodeWithText(label).performSemanticsAction(SemanticsActions.GetTextLayoutResult){it(result)}
            assertEquals(1,result.single().lineCount);val t=result.single();if(case.w>case.h)assertFalse("$case label=$label size=${t.size} right=${t.getLineRight(0)} bottom=${t.getLineBottom(0)} width=${t.didOverflowWidth} height=${t.didOverflowHeight}",t.hasVisualOverflow)
            if(case.w>case.h) {
                val title=compose.onNodeWithTag("quiz_tense").fetchSemanticsNode().boundsInRoot
                val success=compose.onNodeWithTag("quiz_success").fetchSemanticsNode().boundsInRoot
                assertTrue(success.top-title.bottom>20f)
            }
            capture("completion-${case.w}-$lang")
            val sequence=model.value.state.value.session
            compose.onNodeWithTag("quiz_next_question").performClick();assertEquals(1,model.value.state.value.index);assertSame(sequence,model.value.state.value.session)
            compose.runOnIdle{repeat(6){model.value.question!!.rows[model.value.state.value.active].answer.forEach(model.value::letter);model.value.submit()}}
            compose.onNodeWithTag("quiz_finish").assertIsDisplayed();compose.onNodeWithTag("quiz_next_question").assertDoesNotExist()
            compose.onNodeWithText(if(lang==AppLanguage.FRENCH)"Accueil"else "Home").assertIsDisplayed()
        }
    }
}
