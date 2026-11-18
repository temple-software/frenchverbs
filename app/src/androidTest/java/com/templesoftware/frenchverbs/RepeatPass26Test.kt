package com.templesoftware.frenchverbs

import android.content.res.Configuration
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.platform.app.InstrumentationRegistry
import com.templesoftware.frenchverbs.data.preferences.*
import com.templesoftware.frenchverbs.domain.repeat.*
import com.templesoftware.frenchverbs.domain.verbs.*
import com.templesoftware.frenchverbs.ui.app.*
import com.templesoftware.frenchverbs.ui.navigation.AppNavHost
import com.templesoftware.frenchverbs.ui.theme.TempleTheme
import com.templesoftware.frenchverbs.ui.verbs.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.random.Random
import kotlin.math.roundToInt

class RepeatPass26Test {
    val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val rules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    private val out get()=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"pass26").apply{mkdirs()}
    private fun capture(name:String) {
        val b=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(out,"$name.png").outputStream().use{b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};b.recycle()
    }
    @Test fun sequenceModesRestorationAndCancellation() {
        compose.runOnIdle {
            val handle=SavedStateHandle();val m=RepeatViewModel(handle)
            val selection=Selection(Choices.tenses.toSet(),Choices.types.toSet(),Choices.important.toSet(),Choices.er.toSet(),Choices.families.toSet())
            val expected=LearningSequence.create(selection,random=Random(26))
            assertTrue(m.startFull(selection,Random(26)));assertEquals(expected,m.state.value.session)
            m.tick(m.timeline.duration);assertEquals(1,m.state.value.index);assertEquals(0L,m.state.value.elapsed)
            m.tick(4000);val restored=RepeatViewModel(handle);assertEquals(m.state.value,restored.state.value)
            repeat(expected.size-1){restored.tick(restored.timeline.duration)}
            assertTrue(restored.state.value.complete);val end=restored.state.value;restored.tick(999999);assertEquals(end,restored.state.value)
            val origin=LearningItem("aller",5,1);m.startSingle(origin);m.tick(5000)
            assertEquals(listOf(origin),m.state.value.session);m.tick(m.timeline.duration)
            assertTrue(m.state.value.complete);assertEquals(0,m.state.value.index)
            m.startSingle(origin);m.tick(1250);m.cancel();val cancelled=m.state.value;m.tick(999999);assertEquals(cancelled,m.state.value)
            assertTrue(m.startFull(selection,Random(27)));assertEquals(0L,m.state.value.elapsed);assertFalse(m.state.value.complete)
            assertNotEquals(expected.map{it.verbId},m.state.value.session.map{it.verbId})
        }
    }
    @Test fun controlledFrameClockAndSubjectReveal() {
        compose.mainClock.autoAdvance=false
        val model=RepeatViewModel(SavedStateHandle())
        compose.runOnUiThread{model.startSingle(LearningItem("avoir",3,0))}
        compose.activityRule.scenario.onActivity{it.setContent{
            val s by model.state.collectAsState()
            LocalizedContent(AppLanguage.ENGLISH_UK){TempleTheme{RepeatClock(model);RepeatScreen(s,model.timeline,AppLanguage.ENGLISH_UK,{},{})}}
        }}
        compose.mainClock.advanceTimeBy(3500)
        compose.onNodeWithTag("repeat_row_0").assertTextEquals("j’")
        compose.onNodeWithTag("repeat_row_1").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
        val first=compose.onNodeWithTag("repeat_row_0").fetchSemanticsNode().positionInRoot
        compose.mainClock.advanceTimeBy(1200)
        compose.onNodeWithTag("repeat_row_0").assertTextEquals("j’ai eu")
        assertEquals(first,compose.onNodeWithTag("repeat_row_0").fetchSemanticsNode().positionInRoot)
        compose.mainClock.advanceTimeBy(16000)
        compose.onNodeWithTag("repeat_rows").assertDoesNotExist()
        assertTrue(model.state.value.complete);assertEquals(0,model.state.value.index)
        compose.mainClock.autoAdvance=true
    }
    @Test fun lifecyclePausesAndResumesTheSamePhase() {
        compose.mainClock.autoAdvance=false
        val model=RepeatViewModel(SavedStateHandle())
        compose.runOnUiThread{model.startSingle(LearningItem("prendre",0,0))}
        compose.activityRule.scenario.onActivity{it.setContent{
            val state by model.state.collectAsState()
            LocalizedContent(AppLanguage.FRENCH){TempleTheme{RepeatClock(model);RepeatScreen(state,model.timeline,AppLanguage.FRENCH,{},{})}}
        }}
        compose.mainClock.advanceTimeBy(5000)
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        val paused=model.state.value
        compose.mainClock.advanceTimeBy(10000)
        assertEquals(paused,model.state.value)
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        compose.mainClock.advanceTimeBy(400)
        assertEquals(paused.index,model.state.value.index)
        assertTrue(model.state.value.elapsed in (paused.elapsed+1)..(paused.elapsed+450))
        compose.runOnUiThread{model.cancel()}
        compose.mainClock.advanceTimeBy(500)
        val stopped=model.state.value
        compose.mainClock.advanceTimeBy(5000)
        assertEquals(stopped,model.state.value)
        compose.mainClock.autoAdvance=true
    }
    @Test fun fullRepeatAdvancesOnceAndFinishesWithFrenchAction() {
        compose.mainClock.autoAdvance=false
        val model=RepeatViewModel(SavedStateHandle())
        compose.runOnUiThread{model.startFull(Selection(tenses=setOf("present"),types=setOf("important"),important=setOf("prendre","avoir")))}
        var tried=0
        compose.activityRule.scenario.onActivity{it.setContent{
            val state by model.state.collectAsState()
            LocalizedContent(AppLanguage.FRENCH){TempleTheme{RepeatClock(model);RepeatScreen(state,model.timeline,AppLanguage.FRENCH,{},{tried++})}}
        }}
        compose.mainClock.advanceTimeBy(20000)
        assertEquals(1,model.state.value.index);assertFalse(model.state.value.complete)
        compose.onNodeWithTag("repeat_title").assertTextEquals("prendre")
        compose.mainClock.advanceTimeBy(20000)
        assertTrue(model.state.value.complete);assertEquals(1,model.state.value.index)
        compose.onNodeWithTag("repeat_try").assertTextEquals("À ton tour !").performSemanticsAction(SemanticsActions.OnClick){it()}
        assertEquals(1,tried)
        val end=model.state.value;compose.mainClock.advanceTimeBy(40000);assertEquals(end,model.state.value)
        compose.mainClock.autoAdvance=true
    }
    private data class Case(val w:Int,val h:Int,val lang:AppLanguage,val font:Float=1f)
    @Composable private fun Frame(c:Case, s:RepeatState, timeline:RepeatTimeline) {
        BoxWithConstraints(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            val host=LocalDensity.current
            val d=minOf(with(host){maxWidth.toPx()}/c.w,with(host){maxHeight.toPx()}/c.h)
            val config=Configuration(LocalConfiguration.current).apply{screenWidthDp=c.w;screenHeightDp=c.h;fontScale=c.font;densityDpi=(d*160).roundToInt();orientation=if(c.w>c.h)2 else 1}
            CompositionLocalProvider(LocalDensity provides Density(d,c.font),LocalConfiguration provides config) {
                Box(Modifier.requiredSize(c.w.dp,(c.h-76).dp)){LocalizedContent(c.lang){TempleTheme{RepeatScreen(s,timeline,c.lang,{},{})}}}
            }
        }
    }
    @Test fun responsiveBilingualCompoundLayouts() {
        val timeline=RepeatTimeline();val hold=timeline.phases.first{it.name=="completeHold"}.start
        val c=mutableStateOf(Case(412,915,AppLanguage.ENGLISH_UK))
        val s=mutableStateOf(RepeatState(listOf(LearningItem("aller",5,0)),elapsed=hold))
        compose.activityRule.scenario.onActivity{it.setContent{Frame(c.value,s.value,timeline)}}
        val cases=AppLanguage.entries.flatMap { lang -> listOf(360 to 800,412 to 915,800 to 360,915 to 412,800 to 1280,1280 to 800).map{Case(it.first,it.second,lang)}+listOf(Case(412,915,lang,1.3f),Case(800,360,lang,1.3f)) }
        val log=File(out,"layouts.txt");log.writeText("")
        for(case in cases) {
            compose.runOnIdle{c.value=case};compose.waitForIdle()
            compose.onNodeWithTag("repeat_title").assertTextEquals(if(case.lang==AppLanguage.FRENCH)"aller" else "aller / to go")
            val rows=(0..5).map{compose.onNodeWithTag("repeat_row_$it").fetchSemanticsNode()}
            for(i in 0..5)compose.onNodeWithTag("repeat_row_$i").assertTextEquals(OfflineDictionary.verbs.first{it.id=="aller"}.tenses[5].forms[i].text)
            rows.zipWithNext().forEach{(a,b)->assertTrue(a.positionInRoot.y+a.size.height<=b.positionInRoot.y+1)}
            val range=compose.onNodeWithTag("repeat_scroll").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].maxValue()
            if(case.font==1f){assertEquals("scroll $case",0f,range,1f);compose.onNodeWithTag("repeat_row_5").assertIsDisplayed()}
            val before=rows.map{it.positionInRoot}
            compose.runOnIdle{s.value=s.value.copy(elapsed=500)}
            compose.onNodeWithTag("repeat_instruction").assertTextEquals(if(case.lang==AppLanguage.FRENCH)"Répète à voix haute" else "Repeat out loud")
            assertEquals(before,(0..5).map{compose.onNodeWithTag("repeat_row_$it").fetchSemanticsNode().positionInRoot})
            compose.runOnIdle{s.value=s.value.copy(elapsed=hold)}
            compose.waitForIdle()
            if(case.font==1f)capture("${case.w}-${case.h}-${case.lang}")
            log.appendText("PASS $case scroll=$range exact compound forms, stable separated rows, title and instruction\n")
        }
    }
    @Test fun realNavigationSingleReturnAndFullCompletion() {
        lateinit var nav:NavHostController
        compose.activityRule.scenario.onActivity{it.setContent{LocalizedContent(AppLanguage.ENGLISH_UK){TempleTheme{
            nav=rememberNavController();AppNavHost(nav,AppLanguage.ENGLISH_UK,{})
        }}}}
        compose.waitForIdle()
        compose.waitUntil(10000){nav.currentBackStackEntry!=null}
        compose.runOnIdle{nav.navigate("activities")}
        compose.onNodeWithTag("open_learn").performSemanticsAction(SemanticsActions.OnClick){it()}
        compose.onNodeWithTag("learn_next").performSemanticsAction(SemanticsActions.OnClick){it()}
        compose.onNodeWithTag("learn_examples_toggle").performScrollTo().performSemanticsAction(SemanticsActions.OnClick){it()}
        val examples=(0..5).map{compose.onNodeWithTag("learn_sentence_$it").fetchSemanticsNode().config[SemanticsProperties.Text]}
        val progress=compose.onNodeWithTag("learn_progress").fetchSemanticsNode().config[SemanticsProperties.Text]
        lateinit var repeatModel:RepeatViewModel;lateinit var learnModel:VerbViewModel
        compose.activityRule.scenario.onActivity{repeatModel=ViewModelProvider(it)[RepeatViewModel::class.java];learnModel=ViewModelProvider(it)[VerbViewModel::class.java]}
        val origin=learnModel.state.value
        compose.mainClock.autoAdvance=false
        compose.onNodeWithTag("learn_repeat").assertIsEnabled().performSemanticsAction(SemanticsActions.OnClick){it()};compose.mainClock.advanceTimeBy(1600);compose.waitForIdle()
        compose.onNodeWithTag("repeat_title").assertExists()
        compose.runOnIdle{assertEquals(listOf(origin.current),repeatModel.state.value.session);repeatModel.tick(repeatModel.timeline.duration)}
        compose.mainClock.advanceTimeBy(1600);compose.waitForIdle()
        compose.onNodeWithTag("learn_repeat").assertExists()
        assertEquals(origin,learnModel.state.value)
        assertEquals(progress,compose.onNodeWithTag("learn_progress").fetchSemanticsNode().config[SemanticsProperties.Text])
        assertEquals(examples,(0..5).map{compose.onNodeWithTag("learn_sentence_$it").fetchSemanticsNode().config[SemanticsProperties.Text]})
        compose.onNodeWithTag("learn_repeat").performSemanticsAction(SemanticsActions.OnClick){it()};compose.mainClock.advanceTimeBy(1600);compose.waitForIdle()
        compose.runOnIdle{compose.activity.onBackPressedDispatcher.onBackPressed()};compose.mainClock.advanceTimeBy(1600);compose.waitForIdle()
        compose.onNodeWithTag("learn_repeat").assertExists()
        compose.onNodeWithTag("learn_repeat").performSemanticsAction(SemanticsActions.OnClick){it()};compose.mainClock.advanceTimeBy(1600);compose.waitForIdle()
        compose.onNodeWithTag("repeat_home").performSemanticsAction(SemanticsActions.OnClick){it()};compose.mainClock.advanceTimeBy(1600);compose.waitForIdle()
        compose.onNodeWithTag("open_repeat").assertExists().performSemanticsAction(SemanticsActions.OnClick){it()};compose.mainClock.advanceTimeBy(1600);compose.waitForIdle()
        compose.onNodeWithTag("repeat_title").assertExists()
        compose.runOnIdle{assertFalse(repeatModel.state.value.single);repeat(repeatModel.state.value.session.size){repeatModel.tick(repeatModel.timeline.duration)}}
        compose.mainClock.advanceTimeBy(1600);compose.waitForIdle()
        compose.onNodeWithTag("repeat_try").assertTextEquals("Try yourself").performSemanticsAction(SemanticsActions.OnClick){it()};compose.mainClock.advanceTimeBy(1600);compose.waitForIdle()
        compose.runOnIdle{assertEquals("conjugate",nav.currentDestination?.route)}
        compose.runOnIdle{nav.popBackStack("activities",false)};compose.mainClock.advanceTimeBy(1600);compose.waitForIdle()
        compose.onNodeWithTag("open_repeat").performSemanticsAction(SemanticsActions.OnClick){it()};compose.mainClock.advanceTimeBy(1600);compose.waitForIdle()
        compose.runOnIdle{compose.activity.onBackPressedDispatcher.onBackPressed()};compose.mainClock.advanceTimeBy(1600);compose.waitForIdle()
        compose.onNodeWithTag("open_repeat").assertExists()
        val stopped=repeatModel.state.value
        compose.mainClock.advanceTimeBy(2000)
        assertEquals(stopped,repeatModel.state.value);assertEquals(origin,learnModel.state.value)
        compose.mainClock.autoAdvance=true
    }
}
