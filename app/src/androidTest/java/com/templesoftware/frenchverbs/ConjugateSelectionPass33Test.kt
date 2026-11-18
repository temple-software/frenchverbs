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

class ConjugateSelectionPass33Test {
    private val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val rules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    private val all=Selection(Choices.tenses.toSet(),Choices.types.toSet(),Choices.important.toSet(),Choices.er.toSet(),Choices.families.toSet())
    private val out get()=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"pass33").apply{mkdirs()}
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
    @Test fun bilingualSelectionLayoutsAndHeadings() {
        val original=ConjugateSelectionActions.candidates(LearningSequence.create(all,random=Random(4)),all)
        val today=ConjugateSelectionActions.begin(original)
        val c=mutableStateOf(Case(412,915,AppLanguage.ENGLISH_UK))
        val s=mutableStateOf(today)
        compose.activityRule.scenario.onActivity{it.setContent{Frame(c.value){ConjugateSelectionScreen(s.value,c.value.lang,{},{},{},{},{},{},{})}}}
        val pages=listOf(today,ConjugateSelectionActions.openTenses(today),ConjugateSelectionActions.openTypes(today),ConjugateSelectionActions.chooseTense(ConjugateSelectionActions.openTenses(today),0))
        File(out,"layouts.txt").writeText("")
        for(lang in AppLanguage.entries)for(case in listOf(Case(412,915,lang),Case(915,412,lang),Case(800,360,lang),Case(800,1280,lang),Case(1280,800,lang),Case(412,915,lang,1.3f),Case(800,360,lang,1.3f))) {
            compose.runOnIdle{c.value=case}
            for((index,page) in pages.withIndex()) {
                compose.runOnIdle{s.value=page}
                val ids=when(page.page){ConjugateChoicePage.TODAY->listOf("tense","type","random");ConjugateChoicePage.TENSE->page.originalTenses.map{"tense_$it"};else->page.categories.map{"type_$it"}}
                for(id in ids) {
                    val button=compose.onNodeWithTag("conj_select_$id")
                    if(!button.isDisplayed())button.performScrollTo()
                    button.assertIsDisplayed()
                    val text=mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                    compose.onNodeWithTag("conj_label_$id",true).performSemanticsAction(SemanticsActions.GetTextLayoutResult){it(text)}
                    assertFalse("clipped label $case $id size=${text.single().size} paragraph=${text.single().multiParagraph.width} x ${text.single().multiParagraph.height}",text.single().hasVisualOverflow)
                    val layout=text.single()
                    if(layout.lineCount>1)assertTrue("single-letter final line $case $id",layout.layoutInput.text.text.substring(layout.getLineStart(layout.lineCount-1),layout.getLineEnd(layout.lineCount-1)).trim().length>1)
                }
                if(case.w==915 && page.page!=ConjugateChoicePage.TYPE)compose.onNodeWithTag("conj_select_heading").assertIsDisplayed()
                capture("${case.w}-${case.h}-$lang-${case.font}-$index")
                File(out,"layouts.txt").appendText("PASS $case ${page.page} tense=${page.tense}, ${ids.size} buttons readable/reachable\n")
            }
        }
        compose.runOnIdle {c.value=Case(412,915,AppLanguage.ENGLISH_UK);s.value=today.copy(original=original.take(3),page=ConjugateChoicePage.TENSE)}
        compose.onNodeWithTag("conj_select_heading").assertTextEquals("être")
        compose.runOnIdle{s.value=today.copy(original=original.take(6),page=ConjugateChoicePage.TENSE)}
        compose.onNodeWithTag("conj_select_heading").assertTextEquals("one verb selected")
        compose.runOnIdle{c.value=c.value.copy(lang=AppLanguage.FRENCH)}
        compose.onNodeWithTag("conj_select_heading").assertTextEquals("un verbe sélectionné")
    }
    @Test fun choicesRestoreAndAcceptedListDrivesExactlySixQuestions() {
        val saved=SavedStateHandle();val m=ConjugateViewModel(saved);m.start(all,Random(3))
        compose.activityRule.scenario.onActivity{it.setContent{Frame(Case(412,915,AppLanguage.ENGLISH_UK)){
            val s by m.selection.collectAsState();val quiz by m.state.collectAsState()
            if(!s.done)ConjugateSelectionScreen(s,AppLanguage.ENGLISH_UK,{},{m.selectionBack()},m::chooseTenses,m::chooseTypes,m::chooseRandom,m::chooseTense,m::chooseCategory)
            else ConjugateScreen(quiz,m.question!!,AppLanguage.ENGLISH_UK,{},m::letter,m::clear,m::submit,m::nextQuestion)
        }}}
        val original=m.selection.value.original
        compose.onNodeWithTag("conj_select_tense").performClick()
        compose.onNodeWithTag("conj_select_tense_0").performClick()
        assertEquals(0,m.selection.value.tense)
        compose.onNodeWithTag("conj_select_heading").performClick()
        for(t in 0..5)compose.onNodeWithTag("conj_select_tense_$t").assertExists()
        assertEquals(original.map{it.item},m.selection.value.current)
        compose.onNodeWithTag("conj_select_tense_1").performClick()
        compose.runOnIdle{assertEquals(m.selection.value,ConjugateViewModel(saved).selection.value)}
        compose.onNodeWithTag("conj_select_type_important").performClick()
        val accepted=m.selection.value.accepted;assertEquals(6,accepted.size);assertEquals(accepted,m.state.value.session)
        compose.onNodeWithTag("quiz_title").assertExists()
        val first=m.question!!.rows[0].answer.first()
        compose.onNodeWithTag("quiz_key_$first").performClick();compose.onNodeWithTag("quiz_clear").performClick();assertEquals("",m.state.value.entries[0])
        compose.runOnIdle {
            m.letter(first);val before=m.state.value;m.chooseCategory("er");m.chooseRandom();assertEquals(before,m.state.value)
            assertEquals(m.state.value,ConjugateViewModel(saved).state.value);assertEquals(accepted,ConjugateViewModel(saved).selection.value.accepted)
            m.clear()
        }
        for(question in 0..5) {
            compose.runOnIdle{repeat(6){m.question!!.rows[m.state.value.active].answer.forEach(m::letter);m.submit()}}
            compose.onNodeWithTag("quiz_success").assertExists()
            if(question<5)compose.onNodeWithTag("quiz_next_question").performClick()
        }
        compose.onNodeWithTag("quiz_finish").assertExists();assertTrue(m.state.value.last);assertEquals(100,m.state.value.progress)
        assertEquals(original,m.selection.value.original);assertEquals(accepted,m.state.value.session)
    }
    @Test fun bothExistingEntryRoutesUseSelectionAndPreservePreferences() {
        lateinit var nav:NavHostController
        compose.activityRule.scenario.onActivity{it.setContent{LocalizedContent(AppLanguage.ENGLISH_UK){TempleTheme{nav=rememberNavController();AppNavHost(nav,AppLanguage.ENGLISH_UK,{})}}}}
        compose.waitUntil(10000){compose.onAllNodesWithTag("next").fetchSemanticsNodes().isNotEmpty()}
        lateinit var quiz:ConjugateViewModel;lateinit var verbs:VerbViewModel;lateinit var repeatModel:RepeatViewModel
        compose.activityRule.scenario.onActivity{quiz=ViewModelProvider(it)[ConjugateViewModel::class.java];verbs=ViewModelProvider(it)[VerbViewModel::class.java];repeatModel=ViewModelProvider(it)[RepeatViewModel::class.java]}
        val preferences=verbs.state.value.selection;val learnBefore=verbs.state.value.session
        fun accept() {
            compose.runOnIdle{when(quiz.selection.value.page){ConjugateChoicePage.TODAY->quiz.chooseRandom();ConjugateChoicePage.TYPE->quiz.chooseCategory(quiz.selection.value.categories.first());else->Unit}}
            compose.onNodeWithTag("quiz_title").assertExists();assertTrue(quiz.state.value.session.size in 1..6)
            assertEquals(quiz.selection.value.accepted,quiz.state.value.session)
        }
        compose.runOnIdle{nav.navigate("activities")};compose.onNodeWithTag("open_conjugate").performClick();accept()
        File(out,"saved-entry.txt").writeText("Saved preferences produce ${quiz.selection.value.original.size} original questions; accepted ${quiz.state.value.session.size}; direct=${quiz.selection.value.original.size<=6}")
        compose.onNodeWithTag("quiz_home").performClick();compose.onNodeWithTag("open_repeat").performClick()
        compose.runOnIdle{repeat(repeatModel.state.value.session.size){repeatModel.tick(repeatModel.timeline.waitingAt);if(repeatModel.state.value.next!=null){repeatModel.next();repeatModel.tick(repeatModel.timeline.timing.finalFadeOut)}}}
        compose.onNodeWithTag("repeat_try").performClick();accept()
        assertEquals(preferences,verbs.state.value.selection);assertEquals(learnBefore,verbs.state.value.session)
        compose.runOnIdle{compose.activity.onBackPressedDispatcher.onBackPressed()};compose.onNodeWithTag("open_conjugate").assertExists()
    }
}
