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
class UseRefinementsTest {
    private val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val rules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    private val out get()=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"use-refinements").apply{mkdirs()}
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
    @Test fun backKeypadAndResults() {
        val q=UseSentences.candidates(LearningItem("pouvoir",1,0),0,false,false).first()
        val c=mutableStateOf(Case(412,915,AppLanguage.FRENCH))
        val s=mutableStateOf(UseActions.start(List(10){q}));var exits=0;var paused=false
        compose.activityRule.scenario.onActivity{it.setContent{Frame(c.value){UseScreen(s.value,c.value.lang,{exits++},{},{},{},{},onPause={paused=it})}}}
        compose.onNodeWithTag("use_back").performClick();compose.onNodeWithTag("use_exit_stay").assertIsDisplayed();assertTrue(paused);assertEquals(0,exits)
        compose.onNodeWithTag("use_exit_stay").performClick();assertFalse(paused)
        compose.runOnUiThread{compose.activity.onBackPressedDispatcher.onBackPressed()}
        compose.onNodeWithTag("use_exit_confirm").performClick();assertEquals(1,exits)
        compose.onNodeWithTag("use_exit_stay").performClick()
        val clear=compose.onNodeWithTag("quiz_clear").fetchSemanticsNode().boundsInRoot
        val enter=compose.onNodeWithTag("quiz_enter").fetchSemanticsNode().boundsInRoot
        assertTrue(enter.top>clear.bottom)
        for(lang in AppLanguage.entries)for(case in listOf(Case(412,915,lang),Case(915,412,lang))) {
            compose.runOnIdle{c.value=case;s.value=s.value.copy(index=10,outcomes=listOf(-1,-1,1,1,1,1,1,1,1,1),selectedTenseCount=2)}
            compose.onNodeWithTag("use_stars").assertContentDescriptionEquals(if(lang==AppLanguage.FRENCH)"4 étoiles sur 5"else "4 out of 5 stars")
            compose.onNodeWithTag("use_review_0").assertIsDisplayed();compose.onNodeWithTag("use_review_1").assertIsDisplayed();compose.onNodeWithTag("use_review_2").assertDoesNotExist()
            compose.onAllNodesWithText("je pouvais (l’imparfait)").assertCountEquals(2)
            capture("results-${case.w}-$lang")
        }
        compose.runOnIdle{s.value=s.value.copy(selectedTenseCount=1)}
        compose.onAllNodesWithText("je pouvais").assertCountEquals(2)
        compose.onNodeWithTag("use_back").performClick();assertEquals(2,exits)
        compose.onNodeWithTag("use_exit_confirm").assertDoesNotExist()
    }
}
