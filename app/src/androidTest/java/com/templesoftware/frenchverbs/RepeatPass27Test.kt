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

class RepeatPass27Test {
    val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val rules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    private val out get()=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"pass27").apply{mkdirs()}
    private fun capture(name:String) {
        val b=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(out,"$name.png").outputStream().use{b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};b.recycle()
    }
    @Test fun waitingClockRestorationAndOnePress() {
        compose.mainClock.autoAdvance=false
        val handle=SavedStateHandle();val model=RepeatViewModel(handle)
        compose.runOnUiThread{model.startFull(Selection(tenses=setOf("present","imperfect"),types=setOf("important"),important=setOf("prendre")))}
        compose.activityRule.scenario.onActivity{it.setContent{
            val state by model.state.collectAsState()
            LocalizedContent(AppLanguage.ENGLISH_UK){TempleTheme{RepeatClock(model);RepeatScreen(state,model.timeline,AppLanguage.ENGLISH_UK,{},{},onNext={model.next()})}}
        }}
        compose.mainClock.advanceTimeBy(model.timeline.timing.afterTense/2)
        compose.onNodeWithTag("repeat_instruction").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
        compose.onNodeWithTag("repeat_next").assertDoesNotExist()
        compose.runOnIdle{model.tick(model.timeline.waitingAt-model.state.value.elapsed-100)}
        compose.onNodeWithTag("repeat_next").assertDoesNotExist()
        compose.mainClock.advanceTimeBy(200)
        compose.onNodeWithTag("repeat_next").assertIsEnabled()
        compose.onNodeWithTag("repeat_next_label",true).assertTextEquals("Next: prendre in imperfect")
        val waiting=model.state.value
        assertEquals(RepeatPlayback.WAITING,waiting.playback)
        compose.mainClock.advanceTimeBy(90000);assertEquals(waiting,model.state.value)
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        compose.mainClock.advanceTimeBy(10000)
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        compose.mainClock.advanceTimeBy(1000);assertEquals(waiting,model.state.value)
        compose.runOnIdle {
            val restored=RepeatViewModel(handle);assertEquals(waiting,restored.state.value)
            assertTrue(model.next());repeat(5){assertFalse(model.next())}
        }
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithTag("repeat_next").assertIsNotEnabled()
        compose.mainClock.advanceTimeBy(500)
        assertEquals(1,model.state.value.index);assertEquals(waiting.session,model.state.value.session)
        compose.onNodeWithTag("repeat_next").assertDoesNotExist()
        compose.mainClock.advanceTimeBy(model.timeline.waitingAt+100)
        compose.onNodeWithTag("repeat_try").assertExists();compose.onNodeWithTag("repeat_next").assertDoesNotExist()
        compose.onNodeWithTag("repeat_row_5").assertTextEquals("ils prenaient")
        val end=model.state.value;compose.mainClock.advanceTimeBy(90000);assertEquals(end,model.state.value)
        compose.mainClock.autoAdvance=true
    }
    @Test fun longNextLabelsBothLanguages() {
        val c=mutableStateOf(Case(360,800,AppLanguage.ENGLISH_UK))
        val timeline=RepeatTimeline()
        val state=RepeatState(listOf(LearningItem("aller",3,0),LearningItem("connaître",5,0)),elapsed=timeline.waitingAt,playback=RepeatPlayback.WAITING)
        compose.activityRule.scenario.onActivity{it.setContent{Frame(c.value,state,timeline)}}
        for(lang in AppLanguage.entries)for((w,h) in listOf(320 to 740,360 to 800,800 to 360,1280 to 800)) {
            compose.runOnIdle{c.value=Case(w,h,lang)};compose.waitForIdle()
            val results=mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            compose.onNodeWithTag("repeat_next_label",true).performSemanticsAction(SemanticsActions.GetTextLayoutResult){it(results)}
            val text=results.single();assertFalse("$lang $w ${text.layoutInput.text}",text.hasVisualOverflow)
            assertTrue(text.layoutInput.text.text.contains("connaître"));assertTrue(text.layoutInput.text.text.contains(if(lang==AppLanguage.FRENCH)"plus-que-parfait" else "pluperfect"))
            compose.onNodeWithTag("repeat_next_arrow",true).assertIsDisplayed()
        }
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
        val s=mutableStateOf(RepeatState(listOf(LearningItem("aller",5,0)),elapsed=hold,playback=RepeatPlayback.PLAYING))
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
            val viewport=compose.onNodeWithTag("repeat_viewport").fetchSemanticsNode()
            val block=compose.onNodeWithTag("repeat_rows").fetchSemanticsNode()
            val nav=compose.onNodeWithTag("repeat_navigation").fetchSemanticsNode()
            assertEquals(viewport.positionInRoot.x+viewport.size.width/2f,block.positionInRoot.x+block.size.width/2f,2f)
            assertTrue(rows.last().positionInRoot.y+rows.last().size.height<=nav.positionInRoot.y+1 || case.font>1)
            val heading=compose.onNodeWithTag("repeat_tense").fetchSemanticsNode()
            assertTrue(heading.positionInRoot.y-viewport.positionInRoot.y < viewport.size.height*.15f)
            val before=rows.map{it.positionInRoot}
            compose.runOnIdle{s.value=s.value.copy(elapsed=1500)}
            compose.onNodeWithTag("repeat_instruction").assertTextEquals(if(case.lang==AppLanguage.FRENCH)"Répète à voix haute" else "Repeat out loud")
            assertEquals(before,(0..5).map{compose.onNodeWithTag("repeat_row_$it").fetchSemanticsNode().positionInRoot})
            compose.runOnIdle{s.value=s.value.copy(elapsed=timeline.waitingAt,playback=RepeatPlayback.WAITING)}
            compose.onNodeWithTag("repeat_try").assertExists()
            assertEquals(before,(0..5).map{compose.onNodeWithTag("repeat_row_$it").fetchSemanticsNode().positionInRoot})
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
        compose.runOnIdle{assertFalse(repeatModel.state.value.single);repeat(repeatModel.state.value.session.size){
            repeatModel.tick(repeatModel.timeline.waitingAt)
            if(repeatModel.state.value.next!=null){repeatModel.next();repeatModel.tick(repeatModel.timeline.timing.finalFadeOut)}
        }}
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
