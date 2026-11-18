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
class UsePass38Test {
    private val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val rules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    private val out get()=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"pass38").apply{mkdirs()}
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
    @Test fun audioEntryAndBackDialog() {
        val q=UseSentences.candidates(LearningItem("pouvoir",1,0),0,false,false).first().copy(chosenBoxCount=10)
        val saved=SavedStateHandle(mapOf("useQuestions" to ArrayList(List(10){q.encode()})))
        val m=UseViewModel(saved);val c=mutableStateOf(Case(412,915,AppLanguage.FRENCH));var exits=0
        lateinit var player:UseKeyClick
        compose.activityRule.scenario.onActivity{it.setContent{Frame(c.value){
            player=rememberUseAudio();val click=rememberUseKeyClick();val s by m.state.collectAsState()
            UseScreen(s,c.value.lang,{exits++},{m.letterWithClick(it,click)},m::clear,m::submit,m::edit,onPause=m::pause,letterReady=player.ready)
        }}}
        compose.waitForIdle();compose.waitUntil(20000){player.ready};compose.waitForIdle()
        val diagnostics=StringBuilder("loadMs=${player.loadedAfterMillis}\n")
        // Actual UI letter actions at different rates, including identical letters.
        for(delay in listOf(300L,80L,0L)) {
            compose.onNodeWithTag("quiz_clear").performClick()
            val attempts=player.attempts;val starts=player.starts
            repeat(6){compose.onNodeWithTag("quiz_key_p").performClick();if(delay>0)Thread.sleep(delay)}
            assertEquals(6,player.attempts-attempts);assertEquals(6,player.starts-starts)
            diagnostics.append("delay=$delay accepted=6 attempts=6 starts=6\n")
        }
        val before=player.attempts
        compose.runOnIdle{m.letterWithClick('#'){player.play()}}
        compose.onNodeWithTag("quiz_clear").performClick();assertEquals(before,player.attempts)
        compose.onNodeWithTag("quiz_key_p").performClick()
        val entry=m.state.value
        for(lang in AppLanguage.entries)for(case in listOf(Case(412,915,lang),Case(915,412,lang))) {
            compose.runOnIdle{c.value=case};compose.waitForIdle()
            capture("normal-${case.w}-$lang")
            compose.onNodeWithTag("use_back").performClick()
            compose.onAllNodesWithTag("use_exit_stay").assertCountEquals(1)
            assertFalse(m.state.value.running)
            // Modal surface owns events; background keeps green enabled styling.
            compose.onNodeWithTag("quiz_key_p").assertIsEnabled()
            assertEquals(entry.input,m.state.value.input)
            capture("dialog-${case.w}-$lang")
            compose.onNodeWithTag("use_exit_stay").performClick()
            assertEquals(entry,m.state.value)
            capture("continued-${case.w}-$lang")
        }
        compose.runOnIdle{m.clear();repeat(2){m.letter('p');m.submit();m.tick(650)}}
        assertTrue(m.state.value.input.hints[q.person].isNotEmpty())
        val hinted=m.state.value
        compose.runOnUiThread{compose.activity.onBackPressedDispatcher.onBackPressed()}
        compose.onNodeWithTag("use_exit_stay").performClick();assertEquals(hinted,m.state.value)
        compose.runOnIdle{m.letter('p');m.submit();m.tick(200)}
        val red=m.state.value;val count=player.attempts
        compose.onNodeWithTag("use_back").performClick()
        compose.runOnIdle{m.tick(5000);m.letterWithClick('p'){player.play()}}
        assertEquals(red.input,m.state.value.input);assertEquals(count,player.attempts)
        compose.onNodeWithTag("use_exit_stay").performClick();assertEquals(red,m.state.value)
        compose.runOnIdle{m.tick(450)};assertEquals(0L,m.state.value.input.feedbackRemaining)
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        val inactive=player.starts;compose.runOnUiThread{player.play()};assertEquals(inactive,player.starts)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitForIdle();val resumed=player.starts
        compose.onNodeWithTag("quiz_key_p").performClick();assertEquals(resumed+1,player.starts)
        compose.onNodeWithTag("use_back").performClick();compose.onNodeWithTag("use_exit_confirm").performClick();assertEquals(1,exits)
        diagnostics.append("background suppressed; resumed accepted click started; no recomposition clicks\n")
        File(out,"audio.txt").writeText(diagnostics.toString())
    }
    @Test fun greenPauseAndResultsBack() {
        val q=UseSentences.candidates(LearningItem("pouvoir",1,0),0,false,false).first()
        val state=mutableStateOf(UseActions.start(List(10){q}));var exits=0
        compose.activityRule.scenario.onActivity{it.setContent{Frame(Case(412,915,AppLanguage.ENGLISH_UK)){
            UseScreen(state.value,AppLanguage.ENGLISH_UK,{exits++},{},{},{},{},onPause={state.value=state.value.copy(running=!it)})
        }}}
        compose.runOnIdle{q.answer.forEach{state.value=UseActions.letter(state.value,it)};state.value=UseActions.submit(state.value);state.value=UseActions.tick(state.value,200)}
        val green=state.value;assertEquals(600L,green.successRemaining)
        compose.onNodeWithTag("use_back").performClick()
        compose.runOnIdle{state.value=UseActions.tick(state.value,5000)}
        assertEquals(600L,state.value.successRemaining)
        compose.onNodeWithTag("use_exit_stay").performClick();assertEquals(green,state.value)
        compose.runOnIdle{state.value=UseActions.tick(state.value,600)};assertEquals(1,state.value.index)
        compose.runOnIdle{state.value=state.value.copy(index=10)}
        compose.onNodeWithTag("use_back").performClick();assertEquals(1,exits)
        compose.onNodeWithTag("use_exit_confirm").assertDoesNotExist()
    }

}
