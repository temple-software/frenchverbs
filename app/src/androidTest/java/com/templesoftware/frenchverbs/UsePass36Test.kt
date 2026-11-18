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
class UsePass36Test {
    private val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val rules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    private val out get()=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"pass36").apply{mkdirs()}
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
    @Test fun layoutsKeypadEditingFeedbackAndCompletion() {
        val c=mutableStateOf(Case(412,915,AppLanguage.ENGLISH_UK))
        val q=mutableStateOf(UseSentences.candidates(LearningItem("pouvoir",1,0),0,false,false).first())
        val state=mutableStateOf(UseActions.start(List(10){q.value}))
        compose.activityRule.scenario.onActivity{it.setContent{Frame(c.value){UseScreen(state.value,c.value.lang,{}, {state.value=UseActions.letter(state.value,it)}, {state.value=UseActions.clear(state.value)}, {state.value=UseActions.submit(state.value)}, {state.value=UseActions.edit(state.value,it)})}}}
        for(lang in AppLanguage.entries)for(case in listOf(Case(412,915,lang),Case(915,412,lang),Case(800,360,lang),Case(800,1280,lang),Case(1280,800,lang),Case(412,915,lang,1.3f))) {
            compose.runOnIdle{c.value=case}
            for(i in 0..9)compose.onNodeWithTag("use_score_$i").assertIsDisplayed()
            compose.onNodeWithTag("use_sentence").assertIsDisplayed();compose.onNodeWithTag("quiz_enter").assertIsDisplayed();compose.onNodeWithTag("quiz_clear").performScrollTo().assertIsDisplayed()
            assertFalse(layout("use_title").hasVisualOverflow)
            capture("use-${case.w}-${case.h}-$lang-${case.font}")
        }
        compose.runOnIdle{c.value=Case(412,915,AppLanguage.FRENCH)}
        "pouvai".forEach{compose.onNodeWithTag("quiz_key_$it").performClick()}
        compose.onNodeWithTag("use_box_3",true).performClick();assertEquals("pou",state.value.input.entries[0])
        compose.onNodeWithTag("quiz_clear").performClick();assertEquals("",state.value.input.entries[0])
        compose.onNodeWithTag("quiz_key_s").performClick();compose.onNodeWithTag("quiz_enter").performClick()
        compose.onNodeWithTag("quiz_enter").assertIsNotEnabled();assertEquals(650L,state.value.input.feedbackRemaining);capture("wrong")
        compose.runOnIdle{state.value=UseActions.tick(state.value,650)}
        q.value.answer.forEach{compose.onNodeWithTag("quiz_key_$it").performClick()};compose.onNodeWithTag("quiz_enter").performClick();assertEquals(-1,state.value.outcomes[0]);capture("correct-after-retry")
        compose.runOnIdle{state.value=UseActions.tick(state.value,800)};assertEquals(1,state.value.index)
        compose.runOnIdle{repeat(9){q.value.answer.forEach{ch->state.value=UseActions.letter(state.value,ch)};state.value=UseActions.submit(state.value);state.value=UseActions.tick(state.value,800)}}
        compose.onNodeWithTag("use_finished").assertIsDisplayed();assertEquals(9,state.value.score)
        // Compound feminine answer, extended keyboard and fixed space, both orientations.
        for(case in listOf(Case(412,915,AppLanguage.FRENCH),Case(915,412,AppLanguage.ENGLISH_UK))) {
            compose.runOnIdle{c.value=case;q.value=UseSentences.candidates(LearningItem("aller",5,0),5,true,false).first();state.value=UseActions.start(List(10){q.value})}
            compose.onNodeWithTag("quiz_enter").assertIsDisplayed();compose.onNodeWithTag("use_sentence").assertIsDisplayed();capture("compound-${case.w}")
            assertEquals("étaientallées",q.value.answer)
        }
    }
    @Test fun savedStateRestoresEveryDecisionAndBothTimers() {
        val saved=SavedStateHandle();val m=UseViewModel(saved)
        m.start(Selection(tenses=setOf("perfect"),important=setOf("aller")),Random(36))
        val q=m.state.value.current!!;m.letter(q.keys.first{it!=q.answer.first()});m.submit();m.tick(217)
        assertEquals(433L,m.state.value.input.feedbackRemaining)
        val restored=UseViewModel(saved);assertEquals(m.state.value,restored.state.value)
        restored.tick(433);q.answer.forEach(restored::letter);restored.submit();restored.tick(310)
        assertEquals(490L,restored.state.value.successRemaining);val again=UseViewModel(saved);assertEquals(restored.state.value,again.state.value)
        again.tick(490);assertEquals(1,again.state.value.index);assertEquals(-1,again.state.value.outcomes[0]);assertEquals(m.state.value.questions,again.state.value.questions)
    }
}
