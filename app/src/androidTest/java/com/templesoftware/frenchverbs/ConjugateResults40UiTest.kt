package com.templesoftware.frenchverbs

import android.content.res.Configuration
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.*
import androidx.lifecycle.SavedStateHandle
import androidx.test.platform.app.InstrumentationRegistry
import com.templesoftware.frenchverbs.data.preferences.AppLanguage
import com.templesoftware.frenchverbs.domain.quiz.*
import com.templesoftware.frenchverbs.domain.verbs.*
import com.templesoftware.frenchverbs.ui.app.*
import com.templesoftware.frenchverbs.ui.quiz.*
import com.templesoftware.frenchverbs.ui.theme.TempleTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import kotlin.math.roundToInt

class ConjugateResults40UiTest {
    private val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val rules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    private data class Case(val w:Int,val h:Int,val language:AppLanguage,val font:Float=1f)
    @Composable private fun Frame(c:Case,content:@Composable ()->Unit) {
        BoxWithConstraints(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            val host=LocalDensity.current
            val d=minOf(with(host){maxWidth.toPx()}/c.w,with(host){maxHeight.toPx()}/c.h)
            val config=Configuration(LocalConfiguration.current).apply{screenWidthDp=c.w;screenHeightDp=c.h;fontScale=c.font;densityDpi=(d*160).roundToInt();orientation=if(c.w>c.h)2 else 1}
            CompositionLocalProvider(LocalDensity provides Density(d,c.font),LocalConfiguration provides config) {
                Box(Modifier.requiredSize(c.w.dp,(c.h-60).dp)){LocalizedContent(c.language){TempleTheme{content()}}}
            }
        }
    }
    @Test fun dropdownsLanguagesAndResponsiveLayouts() {
        var case by mutableStateOf(Case(412,915,AppLanguage.FRENCH))
        var s by mutableStateOf(results40Fixture())
        var home=0
        compose.activityRule.scenario.onActivity{it.setContent{Frame(case){ConjugateResultsScreen(s,case.language,{t->s=s.copy(resultToggles=if(t in s.resultToggles)s.resultToggles-t else s.resultToggles+t)},{home++})}}}
        for(lang in AppLanguage.entries)for((w,h,font) in listOf(Triple(412,915,1f),Triple(915,412,1f),Triple(800,1280,1f),Triple(1280,800,1f),Triple(320,740,1.5f))) {
            compose.runOnIdle{case=Case(w,h,lang,font);s=results40Fixture()}
            compose.onNodeWithTag("conj_results_stars").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("conj_results_score").assertTextContains("9 / 18",substring=true)
            compose.onNodeWithTag("conj_result_line_0",true).assertExists()
            compose.onNodeWithTag("conj_result_line_6",true).assertExists()
            compose.onNodeWithTag("conj_result_summary_3",true).performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("conj_result_summary_3",true).performTouchInput{click()}
            compose.onNodeWithTag("conj_result_line_12",true).performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("conj_result_line_0",true).assertExists()
            compose.onNodeWithTag("conj_result_line_12",true).performTouchInput{click()}
            compose.onNodeWithTag("conj_result_summary_3",true).assertExists()
            compose.onNodeWithTag("conj_result_heading_0",true).performScrollTo().performTouchInput{click()}
            compose.onNodeWithTag("conj_result_summary_0",true).assertExists()
            compose.onNodeWithTag("conj_result_line_6",true).assertExists()
            // Expand all wrong lines for long-list scrolling and text bounds checks.
            compose.runOnIdle{s=s.copy(wrongLines=(0..17).toList(),resultToggles=setOf(0,1,3))}
            for(i in 0..17) {
                val text=compose.onNodeWithTag("conj_result_text_$i",true)
                text.performScrollTo().assertIsDisplayed()
                val layouts=mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                text.performSemanticsAction(SemanticsActions.GetTextLayoutResult){it(layouts)}
                assertFalse("text overflow $case row $i",layouts.single().hasVisualOverflow)
            }
            compose.onNodeWithTag("conj_results_home").performScrollTo().assertIsDisplayed()
        }
        compose.onNodeWithTag("conj_results_home").performClick();assertEquals(1,home)
        compose.onNodeWithTag("conj_results_back").performClick();assertEquals(2,home)
        compose.runOnIdle{s=results40Fixture().copy(wrongLines=emptyList())}
        compose.onNodeWithTag("conj_results_perfect").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("conj_result_group_0").assertDoesNotExist()
        assertEquals("1 wrong answer — press to see it",conjugateErrorSummary(1,true))
        assertEquals("1 erreur — appuyez pour la voir",conjugateErrorSummary(1,false))
        assertEquals("4 erreurs — appuyez pour les voir",conjugateErrorSummary(4,false))
    }
    @Test fun finalAnswerRoutesToResultsAndSavedHandleRestoresHistory() {
        val items=arrayListOf(LearningItem("jouer",0,0).encode(),LearningItem("aller",3,0).encode())
        val saved=SavedStateHandle(mapOf("quizSession" to items))
        var m=ConjugateViewModel(saved)
        compose.activityRule.scenario.onActivity{it.setContent{TempleTheme{ConjugateActivityContent(m,AppLanguage.ENGLISH_UK,{})}}}
        compose.runOnIdle {
            // Repeated mistakes on one line, then complete two actual questions.
            repeat(2){m.letter('s');m.submit();m.tick(650)}
            while(!m.state.value.done){val s=m.state.value;m.question!!.rows[s.active].answer.drop(s.hints[s.active].length).forEach(m::letter);m.submit()}
            m.nextQuestion()
            while(m.state.value.locked.size<5){val s=m.state.value;m.question!!.rows[s.active].answer.forEach(m::letter);m.submit()}
            m.question!!.rows[5].answer.forEach(m::letter)
        }
        compose.onNodeWithTag("quiz_enter").performScrollTo().performClick()
        compose.onNodeWithTag("conj_results").assertExists()
        assertEquals(12,m.state.value.completedLines.size)
        assertEquals(1,ConjugateResults(m.state.value).score.wrongLines)
        compose.onNodeWithTag("conj_result_heading_0",true).performScrollTo().performTouchInput{click()}
        val before=m.state.value
        // New VM and SavedStateHandle, not just reuse of the existing object.
        compose.runOnIdle{m=ConjugateViewModel(SavedStateHandle(saved.keys().associateWith{saved.get<Any?>(it)}))}
        assertEquals(before,m.state.value)
        assertEquals(4,ConjugateResults(m.state.value).score.stars)
        assertEquals(setOf(0),m.state.value.resultToggles)
    }
    @Test fun productionResultsBackHomeAndAndroidBack() {
        lateinit var nav:androidx.navigation.NavHostController
        compose.activityRule.scenario.onActivity{it.setContent{LocalizedContent(AppLanguage.ENGLISH_UK){TempleTheme{
            nav=androidx.navigation.compose.rememberNavController()
            com.templesoftware.frenchverbs.ui.navigation.AppNavHost(nav,AppLanguage.ENGLISH_UK,{})
        }}}}
        compose.waitForIdle();compose.waitUntil(10000){nav.currentBackStackEntry!=null}
        compose.runOnIdle{nav.navigate("activities")}
        lateinit var quiz:ConjugateViewModel
        compose.activityRule.scenario.onActivity{quiz=androidx.lifecycle.ViewModelProvider(it)[ConjugateViewModel::class.java]}
        for(action in 0..2) {
            compose.runOnIdle {
                quiz.start(Selection(important=setOf("être")))
                nav.navigate("conjugate")
                repeat(6){quiz.question!!.rows[quiz.state.value.active].answer.forEach(quiz::letter);quiz.submit()}
            }
            compose.onNodeWithTag("conj_results").assertExists()
            when(action) {
                0->compose.onNodeWithTag("conj_results_back").performClick()
                1->compose.onNodeWithTag("conj_results_home").performScrollTo().performClick()
                else->compose.runOnIdle{compose.activity.onBackPressedDispatcher.onBackPressed()}
            }
            compose.onNodeWithTag("open_conjugate").assertExists()
            assertEquals("activities",nav.currentDestination?.route)
        }
    }

}

