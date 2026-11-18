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
import com.templesoftware.frenchverbs.domain.use.*
import com.templesoftware.frenchverbs.ui.use.*
class UsePass37Test {
    private val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val rules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    private val out get()=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"pass37").apply{mkdirs()}
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
        compose.waitForIdle()
        val image=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(out,"$name.png").outputStream().use{image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};image.recycle()
    }
    private fun layout(tag:String):androidx.compose.ui.text.TextLayoutResult {
        val list=mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose.onNodeWithTag(tag,true).performSemanticsAction(SemanticsActions.GetTextLayoutResult){it(list)}
        return list.single()
    }
    @Test fun synchronousTypingEditingHintsFeedbackAndAcceptedClick() {
        val q=UseSentences.candidates(LearningItem("protéger",0,0),3,false,false).first()
        val saved=SavedStateHandle(mapOf("useQuestions" to ArrayList(List(10){q.encode()})))
        val model=mutableStateOf(UseViewModel(saved));val c=mutableStateOf(Case(412,915,AppLanguage.FRENCH))
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        lateinit var player:UseKeyClick
        compose.runOnUiThread{player=UseKeyClick(context);player.setActive(true)}

        val streams=mutableListOf<Int>()
        try {
            compose.waitUntil(20000){player.ready}
            compose.activityRule.scenario.onActivity{it.setContent{Frame(c.value){val m=model.value;val s by m.state.collectAsState();UseScreen(s,c.value.lang,{}, {ch->m.letterWithClick(ch){streams+=player.play()}},m::clear,m::submit,m::edit)}}}
            fun entry()=model.value.state.value.input.entries[q.person]
            fun verify() {
                compose.onNodeWithTag("use_live_sentence",true).assertTextEquals(q.prefix+useDisplayEntry(q.form,entry())+q.suffix)
                for(i in entry().indices)compose.onNodeWithTag("use_box_${i}_letter",true).assertTextEquals(entry()[i].toString())
            }
            verify()
            "protég".forEach{compose.onNodeWithTag("quiz_key_$it").performClick();verify()}
            assertEquals(6,streams.size);assertTrue("native SoundPool playback must start",streams.all{it>0})
            val original=layout("use_live_sentence");val bounds=compose.onNodeWithTag("use_live_sentence",true).fetchSemanticsNode().boundsInRoot
            compose.mainClock.advanceTimeBy(600)
            assertEquals(original.layoutInput.text,layout("use_live_sentence").layoutInput.text)
            assertEquals(bounds,compose.onNodeWithTag("use_live_sentence",true).fetchSemanticsNode().boundsInRoot);assertEquals(6,streams.size)
            compose.onNodeWithTag("use_box_4",true).performClick();assertEquals("prot",entry());verify()
            compose.onNodeWithTag("quiz_clear").performClick();assertEquals("",entry());verify();assertEquals(6,streams.size)
            repeat(2) {
                compose.onNodeWithTag("quiz_key_s").performClick();compose.onNodeWithTag("quiz_enter").performClick();verify()
                compose.onNodeWithTag("quiz_key_s").assertIsNotEnabled()
                val count=streams.size
                compose.runOnIdle{model.value.letterWithClick('s'){streams+=player.play()}}
                assertEquals(count,streams.size)
                compose.runOnIdle{model.value.tick(649)};assertEquals(1L,model.value.state.value.input.feedbackRemaining)
                compose.runOnIdle{model.value.tick(1)};verify()
            }
            val hint=model.value.state.value.input.hints[q.person];assertTrue(hint.isNotEmpty())
            compose.onNodeWithTag("quiz_clear").performClick();verify();assertEquals(hint,entry())
            val beforeRestore=streams.size
            compose.runOnIdle{model.value=UseViewModel(saved);c.value=c.value.copy(lang=AppLanguage.ENGLISH_UK)};verify();assertEquals(beforeRestore,streams.size)
            q.answer.removePrefix(hint).forEach{compose.onNodeWithTag("quiz_key_$it").performClick()};verify()
            compose.onNodeWithTag("quiz_enter").performClick();assertEquals(800L,model.value.state.value.successRemaining)
            verify();assertEquals(-1,model.value.state.value.outcomes[0])
            val before=streams.size
            compose.runOnIdle{model.value.letterWithClick('s'){streams+=player.play()};model.value.submit()};assertEquals(before,streams.size)
            compose.runOnIdle{model.value.tick(799)};assertEquals(0,model.value.state.value.index)
            compose.runOnIdle{model.value.tick(1)};assertEquals(1,model.value.state.value.index);verify()
            compose.runOnIdle{model.value.letterWithClick('#'){streams+=player.play()}}
            assertEquals(before,streams.size)
            compose.runOnIdle{repeat(q.boxCount+2){model.value.letterWithClick('s'){streams+=player.play()}}}
            assertEquals(before+q.boxCount,streams.size)
            File(out,"audio.txt").writeText("PASS: accepted physical letter actions each issued one native SoundPool stream >0; rejected/full/red/green actions emitted none; edit/Clear/recomposition/restoration emitted none. Streams="+streams.joinToString())
        } finally {compose.runOnUiThread{player.setActive(false);assertEquals(0,player.play());player.release();assertEquals(0,player.play())}}
    }
    @Test fun layoutsAndCompoundLiveText() {
        val c=mutableStateOf(Case(412,915,AppLanguage.FRENCH))
        val q=mutableStateOf(UseSentences.candidates(LearningItem("pouvoir",1,0),0,false,false).first())
        val state=mutableStateOf(UseActions.start(List(10){q.value}))
        compose.activityRule.scenario.onActivity{it.setContent{Frame(c.value){UseScreen(state.value,c.value.lang,{}, {state.value=UseActions.letter(state.value,it)}, {state.value=UseActions.clear(state.value)}, {state.value=UseActions.submit(state.value)}, {state.value=UseActions.edit(state.value,it)})}}}
        for(lang in AppLanguage.entries)for(case in listOf(Case(412,915,lang),Case(915,412,lang),Case(800,360,lang),Case(800,1280,lang),Case(1280,800,lang),Case(412,915,lang,1.3f)))for(compound in listOf(false,true)) {
            compose.runOnIdle{c.value=case;q.value=UseSentences.candidates(if(compound)LearningItem("aller",5,0)else LearningItem("pouvoir",1,0),if(compound)5 else 0,compound,false).first();state.value=UseActions.start(List(10){q.value});q.value.answer.forEach{state.value=UseActions.letter(state.value,it)}}
            compose.onNodeWithTag("use_live_sentence",true).assertTextEquals(q.value.sentence)
            assertFalse(layout("use_live_sentence").hasVisualOverflow)
            val sentence=compose.onNodeWithTag("use_sentence").fetchSemanticsNode().boundsInRoot
            val boxes=compose.onNodeWithTag("use_answer_boxes").fetchSemanticsNode().boundsInRoot
            assertTrue("boxes must be below sentence",boxes.top>=sentence.bottom)
            compose.onNodeWithTag("quiz_enter").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("quiz_clear").performScrollTo().assertIsDisplayed()
            capture("entry-${case.w}-${case.h}-$lang-${case.font}-$compound")
        }
    }
}
