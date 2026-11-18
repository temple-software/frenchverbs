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

class ConjugatePass31Test {
    val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val rules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    private val out get()=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"pass32").apply{mkdirs()}
    private data class Case(val w:Int,val h:Int,val lang:AppLanguage,val font:Float=1f)
    @Composable private fun Frame(c:Case,s:QuizState,q:QuizQuestion,onLetter:(Char)->Unit={},onClear:()->Unit={},onSubmit:(Int?)->Unit={}) {
        BoxWithConstraints(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            val host=LocalDensity.current
            val d=minOf(with(host){maxWidth.toPx()}/c.w,with(host){maxHeight.toPx()}/c.h)
            val config=Configuration(LocalConfiguration.current).apply{screenWidthDp=c.w;screenHeightDp=c.h;fontScale=c.font;densityDpi=(d*160).roundToInt();orientation=if(c.w>c.h)2 else 1}
            CompositionLocalProvider(LocalDensity provides Density(d,c.font),LocalConfiguration provides config) {
                Box(Modifier.requiredSize(c.w.dp,(c.h-60).dp)){LocalizedContent(c.lang){TempleTheme{ConjugateScreen(s,q,c.lang,{},onLetter,onClear,onSubmit,{})}}}
            }
        }
    }
    private fun capture(name:String) {
        val bitmap=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(out,"$name.png").outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
    }
    @Test fun answerControlsAndFeedbackAreRenderedAndDisabledTogether() {
        val saved=SavedStateHandle(mapOf("quizSession" to arrayListOf(LearningItem("parler",0,0).encode())))
        val m=ConjugateViewModel(saved)
        compose.activityRule.scenario.onActivity{it.setContent{val s by m.state.collectAsState();Frame(Case(412,915,AppLanguage.ENGLISH_UK),s,m.question!!,m::letter,m::clear,m::submit)}}
        compose.onNodeWithTag("quiz_enter").performClick();assertEquals(1,m.state.value.active)
        compose.onNodeWithTag("quiz_row_3").performClick();assertEquals(3,m.state.value.active)
        compose.onNodeWithTag("quiz_row_next").performClick();assertEquals(4,m.state.value.active)
        compose.onNodeWithTag("quiz_key_e").performClick();assertEquals(0L,m.state.value.feedbackRemaining)
        compose.onNodeWithTag("quiz_clear").performClick();assertEquals("",m.state.value.entries[4])
        compose.onNodeWithTag("quiz_key_e").performClick();compose.onNodeWithTag("quiz_key_z").performClick()
        compose.onNodeWithTag("quiz_row_0").performClick();assertEquals(setOf(4),m.state.value.locked);assertEquals(0,m.state.value.active)
        compose.onNodeWithTag("quiz_key_s").performClick();compose.onNodeWithTag("quiz_enter").performClick()
        compose.onNodeWithTag("quiz_enter").assertIsNotEnabled();compose.onNodeWithTag("quiz_clear").assertIsNotEnabled()
        compose.onNodeWithTag("quiz_row_next").assertIsNotEnabled()
        m.question!!.keys.forEach{compose.onNodeWithTag("quiz_key_$it").assertIsNotEnabled()}
        compose.onNodeWithTag("quiz_box_0_0",true).assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,"Incorrect"))
        val wrong=m.state.value
        compose.runOnIdle {m.letter('e');m.clear();m.submit();m.submit(2);assertEquals(wrong,m.state.value);m.tick(649)}
        compose.onNodeWithTag("quiz_enter").assertIsNotEnabled()
        compose.runOnIdle{m.tick(1)}
        compose.onNodeWithTag("quiz_box_0_0",true).assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,"Active"))
        assertEquals("",m.state.value.entries[0]);assertEquals("ez",m.state.value.entries[4])
        compose.runOnIdle{assertEquals(m.state.value,ConjugateViewModel(saved).state.value)}
        capture("controls")
    }
    @Test fun bilingualResponsiveRegularIrregularAndCompoundLayouts() {
        val c=mutableStateOf(Case(412,915,AppLanguage.ENGLISH_UK))
        val s=mutableStateOf(QuizState(session=listOf(LearningItem("être",0,0))))
        val q=mutableStateOf(QuizQuestion.from(s.value.item!!))
        compose.activityRule.scenario.onActivity{it.setContent{Frame(c.value,s.value,q.value)}}
        val cases=AppLanguage.entries.flatMap{lang->listOf(Case(412,915,lang),Case(915,412,lang),Case(800,360,lang),Case(800,1280,lang),Case(1280,800,lang),Case(412,915,lang,1.3f))}
        File(out,"layouts.txt").writeText("")
        for(case in cases) {
            compose.runOnIdle{c.value=case}
            for(item in listOf(LearningItem("parler",0,0),LearningItem("être",0,0),LearningItem("aller",5,0),LearningItem("reconnaître",5,0))) {
                compose.runOnIdle{s.value=QuizState(session=listOf(item));q.value=QuizQuestion.from(item)}
                compose.onNodeWithTag("quiz_title").assertTextEquals(if(case.lang==AppLanguage.FRENCH)"Conjuguer" else "Conjugate")
                for(row in 0..5)for(box in 0 until q.value.boxCount)compose.onNodeWithTag("quiz_box_${row}_$box",true).assertExists()
                for(tag in listOf("quiz_verb","quiz_tense","quiz_title")) {
                    val results=mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                    compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.GetTextLayoutResult){it(results)}
                    assertFalse("$case $item $tag size=${results.single().size} paragraph=${results.single().multiParagraph.width} x ${results.single().multiParagraph.height}",results.single().hasVisualOverflow)
                }
                val panel=compose.onNodeWithTag("quiz_panel").fetchSemanticsNode()
                val keypad=compose.onNodeWithTag("quiz_keypad").fetchSemanticsNode()
                if(case.w>case.h) {
                    assertTrue(panel.positionInRoot.x+panel.size.width<=keypad.positionInRoot.x)
                    val heading=compose.onNodeWithTag("quiz_verb").fetchSemanticsNode().boundsInRoot
                    assertTrue(heading.left>=panel.boundsInRoot.right)
                    assertTrue(panel.boundsInRoot.top<=heading.top)
                    val right=compose.onNodeWithTag("quiz_right_column").fetchSemanticsNode().boundsInRoot
                    if(item.verbId=="parler")assertTrue("short answers retain equal columns",kotlin.math.abs(panel.boundsInRoot.width-right.width)<2f)
                    assertTrue("answer panel never narrower than keypad column",panel.boundsInRoot.width+2f>=right.width)
                    assertTrue("keypad column retains usable width",right.width>=(panel.boundsInRoot.width+right.width)*.30f)
                    assertTrue("centred keypad",kotlin.math.abs(keypad.boundsInRoot.center.x-right.center.x)<2f)
                    if(case.w==915 || case.w==1280) {
                        compose.onNodeWithTag("quiz_row_5").assertIsDisplayed()
                        val first=compose.onNodeWithTag("quiz_row_0").fetchSemanticsNode().boundsInRoot
                        val last=compose.onNodeWithTag("quiz_row_5").fetchSemanticsNode().boundsInRoot
                        assertTrue("balanced vertical padding",kotlin.math.abs((first.top-panel.boundsInRoot.top)-(panel.boundsInRoot.bottom-last.bottom))<3f)
                    }
                    for(active in listOf(0,3,5)) {
                        compose.runOnIdle{s.value=s.value.copy(active=active)}
                        val next=compose.onNodeWithTag("quiz_row_next").fetchSemanticsNode().boundsInRoot
                        val box=compose.onNodeWithTag("quiz_box_${active}_${q.value.boxCount-1}",true).fetchSemanticsNode().boundsInRoot
                        if(next.height>0 && box.height>0) {
                            assertTrue("Next beside final box",next.left>=box.right && next.left-box.right<right.width/3)
                            val nextFull=compose.onNodeWithTag("quiz_row_next").getUnclippedBoundsInRoot()
                            val boxFull=compose.onNodeWithTag("quiz_box_${active}_${q.value.boxCount-1}",true).getUnclippedBoundsInRoot()
                            assertTrue("Next vertically aligned",kotlin.math.abs(((nextFull.top+nextFull.bottom)/2-(boxFull.top+boxFull.bottom)/2).value)<1f)
                        }
                    }
                    compose.runOnIdle{s.value=s.value.copy(active=0)}
                }
                else assertTrue(panel.positionInRoot.y+panel.size.height<=keypad.positionInRoot.y)
                // Unfinished last row and keypad actions remain reachable, including scroll fallback.
                if(!compose.onNodeWithTag("quiz_row_5").isDisplayed())compose.onNodeWithTag("quiz_row_5").performScrollTo()
                compose.onNodeWithTag("quiz_row_5").assertIsDisplayed()
                if(!compose.onNodeWithTag("quiz_enter").isDisplayed())compose.onNodeWithTag("quiz_enter").performScrollTo()
                compose.onNodeWithTag("quiz_enter").assertIsDisplayed()
                capture("${case.w}-${case.h}-${case.lang}-${case.font}-${item.verbId}-${item.tense}")
                File(out,"layouts.txt").appendText("PASS $case $item equal boxes=${q.value.boxCount}, required keys=${q.value.keys}, headers fit, rows/actions reachable\n")
            }
        }
    }
    @Test fun lockedHintsRenderAndSurviveClearAndRestore() {
        val saved=SavedStateHandle(mapOf("quizSession" to arrayListOf(LearningItem("être",0,0).encode())))
        val m=ConjugateViewModel(saved)
        compose.activityRule.scenario.onActivity{it.setContent{val s by m.state.collectAsState();Frame(Case(412,915,AppLanguage.ENGLISH_UK),s,m.question!!,m::letter,m::clear,m::submit)}}
        compose.runOnIdle{repeat(2){m.letter('e');m.submit();m.tick(650)}}
        compose.onNodeWithTag("quiz_box_0_0",true).assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,"Locked hint"))
        compose.onNodeWithTag("quiz_box_0_1",true).assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,"Active"))
        compose.onNodeWithTag("quiz_key_u").performClick()
        compose.onNodeWithTag("quiz_clear").performClick()
        assertEquals("s",m.state.value.entries[0])
        compose.runOnIdle{assertEquals(m.state.value,ConjugateViewModel(saved).state.value)}
        capture("locked-orange-hint")
    }
    @Test fun bothEntryPointsCompletionAndCancellation() {
        lateinit var nav:NavHostController
        compose.activityRule.scenario.onActivity{it.setContent{LocalizedContent(AppLanguage.ENGLISH_UK){TempleTheme{nav=rememberNavController();AppNavHost(nav,AppLanguage.ENGLISH_UK,{})}}}}
        compose.waitForIdle();compose.waitUntil(10000){nav.currentBackStackEntry!=null}
        compose.runOnIdle{nav.navigate("activities")}
        compose.onNodeWithTag("open_conjugate").performClick()
        compose.onNodeWithTag("quiz_title").assertExists()
        lateinit var quiz:ConjugateViewModel;lateinit var repeatModel:RepeatViewModel
        compose.activityRule.scenario.onActivity{quiz=ViewModelProvider(it)[ConjugateViewModel::class.java];repeatModel=ViewModelProvider(it)[RepeatViewModel::class.java]}
        val session=quiz.state.value.session
        compose.runOnIdle {
            repeat(6){val answer=quiz.question!!.rows[quiz.state.value.active].answer;answer.forEach(quiz::letter);quiz.submit()}
        }
        compose.onNodeWithTag("quiz_success").assertExists()
        if(session.size>1){compose.onNodeWithTag("quiz_next_question").performClick();assertSame(session,quiz.state.value.session);assertEquals(1,quiz.state.value.index)}
        compose.onNodeWithTag("quiz_home").performClick();compose.onNodeWithTag("open_repeat").performClick()
        compose.runOnIdle {repeat(repeatModel.state.value.session.size){repeatModel.tick(repeatModel.timeline.waitingAt);if(repeatModel.state.value.next!=null){repeatModel.next();repeatModel.tick(repeatModel.timeline.timing.finalFadeOut)}}}
        compose.onNodeWithTag("repeat_try").performClick();compose.onNodeWithTag("quiz_title").assertExists()
        compose.runOnIdle{compose.activity.onBackPressedDispatcher.onBackPressed()}
        compose.onNodeWithTag("open_conjugate").assertExists();assertFalse(quiz.state.value.running)
    }
    @Test fun extendedKeypadAndLongestAnswersRemainReachable() {
        val all=OfflineDictionary.verbs.flatMap{v->(0..5).map{QuizQuestion.from(LearningItem(v.id,it,0))}}
        val c=mutableStateOf(Case(320,740,AppLanguage.FRENCH))
        val q=mutableStateOf(all.maxBy{it.boxCount})
        compose.activityRule.scenario.onActivity{it.setContent{Frame(c.value,QuizState(session=listOf(LearningItem(q.value.verb.id,q.value.tense,0))),q.value)}}
        for(case in listOf(Case(320,740,AppLanguage.FRENCH),Case(915,412,AppLanguage.ENGLISH_UK),Case(800,360,AppLanguage.FRENCH),Case(1280,800,AppLanguage.FRENCH))) {
        compose.runOnIdle{c.value=case}
        for(question in listOf(all.maxBy{it.boxCount},all.maxBy{it.keys.size})) {
            compose.runOnIdle{q.value=question}
            compose.onNodeWithTag("quiz_row_5").performScrollTo().assertIsDisplayed()
            val panel=compose.onNodeWithTag("quiz_panel").fetchSemanticsNode().boundsInRoot
            for(box in 0 until question.boxCount) {
                val bounds=compose.onNodeWithTag("quiz_box_5_$box",true).fetchSemanticsNode().boundsInRoot
                assertTrue("horizontal box clipping",bounds.left>=panel.left && bounds.right<=panel.right)
            }
            question.keys.forEach{compose.onNodeWithTag("quiz_key_$it").performScrollTo().assertIsDisplayed()}
            compose.onNodeWithTag("quiz_enter").performScrollTo().assertIsDisplayed()
            capture("extended-${case.w}-${case.h}-${question.verb.id}-${question.tense}")
        }
        }
    }

}
