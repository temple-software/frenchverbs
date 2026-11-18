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

class ConjugateInputPass34Test {
    private val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val rules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    private val out get()=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"pass34").apply{mkdirs()}
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
    @Test fun keypadNineThroughFifteenInBothLanguagesAndOrientations() {
        val c=mutableStateOf(Case(412,915,AppLanguage.ENGLISH_UK));val count=mutableStateOf(9);var submitted=0
        compose.activityRule.scenario.onActivity{it.setContent{Frame(c.value){
            Column(Modifier.widthIn(max=360.dp).fillMaxWidth()) { LetterKeypad(('a'..'o').take(count.value),c.value.lang==AppLanguage.ENGLISH_UK,c.value.w>c.value.h,true,{},{},{submitted++}) }
        }}}
        for(lang in AppLanguage.entries)for(case in listOf(Case(412,915,lang),Case(915,412,lang)))for(n in 9..15) {
            compose.runOnIdle{c.value=case;count.value=n}
            val keys=('a'..'o').take(n)
            keys.forEach{compose.onNodeWithTag("quiz_key_$it").assertIsDisplayed()}
            compose.onAllNodesWithTag("quiz_enter").assertCountEquals(1)
            compose.onNodeWithTag("quiz_clear").assertIsDisplayed()
            val enter=compose.onNodeWithTag("quiz_enter").fetchSemanticsNode().boundsInRoot
            val last=compose.onNodeWithTag("quiz_key_${keys.last()}").fetchSemanticsNode().boundsInRoot
            val first=compose.onNodeWithTag("quiz_key_a").fetchSemanticsNode().boundsInRoot
            val second=compose.onNodeWithTag("quiz_key_b").fetchSemanticsNode().boundsInRoot
            val spare=if(n>9)(3-n%3)%3 else 0
            if(spare>0) {
                assertTrue(kotlin.math.abs(enter.top-last.top)<2f)
                assertTrue(kotlin.math.abs(enter.width-(last.width*spare+(second.left-first.right)*(spare-1)))<3f)
                assertTrue(enter.left>last.right)
                val clear=compose.onNodeWithTag("quiz_clear").fetchSemanticsNode().boundsInRoot
                val grid=compose.onNodeWithTag("quiz_keypad").fetchSemanticsNode().boundsInRoot
                assertTrue(clear.top>=last.bottom);assertTrue(kotlin.math.abs(clear.center.x-grid.center.x)<2f);assertTrue(clear.width<=grid.width*.51f)
            } else assertTrue(enter.top>=last.bottom)
            val before=submitted;compose.onNodeWithTag("quiz_enter").performClick();assertEquals(before+1,submitted)
            val label=if(lang==AppLanguage.FRENCH)"Valider" else "Enter"
            compose.onNodeWithText(label).assertIsDisplayed()
            capture("keypad-$n-${case.w}-$lang")
        }
    }
    @Test fun largerLettersFitStandardLongHintedAndCompletedAnswers() {
        val all=OfflineDictionary.verbs.flatMap{v->(0..5).map{QuizQuestion.from(LearningItem(v.id,it,0))}}
        val q=mutableStateOf(QuizQuestion.from(LearningItem("pouvoir",1,0)))
        val c=mutableStateOf(Case(412,915,AppLanguage.FRENCH))
        compose.activityRule.scenario.onActivity{it.setContent{Frame(c.value){
            val question=q.value
            val state=QuizState(session=listOf(LearningItem(question.verb.id,question.tense,0)),entries=question.rows.map{it.answer},locked=setOf(1),hints=listOf("","",question.rows[2].answer.take(2),"","",""))
            ConjugateScreen(state,question,c.value.lang,{},{},{},{},{})
        }}}
        for(case in listOf(Case(412,915,AppLanguage.FRENCH),Case(915,412,AppLanguage.ENGLISH_UK),Case(320,740,AppLanguage.FRENCH),Case(412,915,AppLanguage.FRENCH,1.3f)))for(question in listOf(QuizQuestion.from(LearningItem("pouvoir",1,0)),all.maxBy{it.boxCount})) {
            compose.runOnIdle{c.value=case;q.value=question}
            for(row in 0..2)for(i in question.rows[row].answer.indices) {
                val tag="quiz_box_${row}_$i"
                val box=compose.onNodeWithTag(tag,true)
                if(!box.isDisplayed())box.performScrollTo()
                val layouts=mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                compose.onNodeWithTag("${tag}_letter",true).performSemanticsAction(SemanticsActions.GetTextLayoutResult){it(layouts)}
                val layout=layouts.single();assertFalse("clipped $case ${question.verb.id} $tag ${layout.size}",layout.hasVisualOverflow)
                if(case.w==412 && case.font==1f && question.verb.id=="pouvoir")assertTrue(layout.layoutInput.style.fontSize.value>=17f)
                assertEquals(androidx.compose.ui.text.style.TextAlign.Center,layout.layoutInput.style.textAlign)
                val letter=compose.onNodeWithTag("${tag}_letter",true).getUnclippedBoundsInRoot();val bounds=box.getUnclippedBoundsInRoot()
                assertTrue(kotlin.math.abs(((letter.top+letter.bottom-bounds.top-bounds.bottom)/2).value)<1f)
            }
            capture("letters-${case.w}-${case.font}-${question.verb.id}")
        }
    }
    @Test fun tappingBoxesEditsProtectsHintsAndNeverSubmits() {
        val saved=SavedStateHandle(mapOf("quizSession" to arrayListOf(LearningItem("pouvoir",1,0).encode()),"quizEntries" to arrayListOf("pouvais","","","","","")))
        val m=ConjugateViewModel(saved)
        compose.activityRule.scenario.onActivity{it.setContent{Frame(Case(412,915,AppLanguage.FRENCH)){
            val s by m.state.collectAsState();ConjugateScreen(s,m.question!!,AppLanguage.FRENCH,{},m::letter,m::clear,m::submit,m::nextQuestion,m::edit)
        }}}
        fun tap(row:Int,index:Int)=compose.onNodeWithTag("quiz_box_${row}_$index",true).performTouchInput{click()}
        fun type(text:String){text.forEach{compose.onNodeWithTag("quiz_key_$it").performClick()}}
        tap(0,3);assertEquals("pou",m.state.value.entries[0]);type("vais")
        tap(0,4);assertEquals("pouv",m.state.value.entries[0]);type("ais")
        tap(0,6);assertEquals("pouvai",m.state.value.entries[0]);tap(0,6);tap(0,8);assertEquals("pouvai",m.state.value.entries[0])
        tap(0,0);assertEquals("",m.state.value.entries[0]);type("pouvais")
        assertEquals(List(6){0},m.state.value.mistakes);assertEquals(0L,m.state.value.feedbackRemaining)
        compose.onNodeWithTag("quiz_enter").performClick();assertTrue(0 in m.state.value.locked)
        val locked=m.state.value;tap(0,0);assertEquals(locked,m.state.value)
        tap(3,0);assertEquals(3,m.state.value.active);tap(1,0);assertEquals(1,m.state.value.active)
        compose.runOnIdle{repeat(2){m.letter('v');m.submit();m.tick(650)}}
        val hint=m.state.value.hints[1];assertEquals("pouva",hint)
        type("is");tap(1,0);assertEquals(hint,m.state.value.entries[1]);type("is");tap(1,5);assertEquals(hint,m.state.value.entries[1])
        type("i");compose.onNodeWithTag("quiz_clear").performClick();assertEquals(hint,m.state.value.entries[1])
        type("p");compose.onNodeWithTag("quiz_enter").performClick()
        compose.onNodeWithTag("quiz_box_1_0",true).assertIsNotEnabled()
        val red=m.state.value;tap(1,0);assertEquals(red,m.state.value)
        for(tag in listOf("quiz_enter","quiz_clear","quiz_row_next")+m.question!!.keys.map{"quiz_key_$it"})compose.onNodeWithTag(tag).assertIsNotEnabled()
        compose.runOnIdle{m.edit(1,0);assertEquals(red,m.state.value);m.tick(649);assertEquals(1L,m.state.value.feedbackRemaining);m.tick(1)}
        compose.runOnIdle{assertEquals(m.state.value,ConjugateViewModel(saved).state.value)}
        capture("protected-hints")
    }
}
