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
class ConjugateAlignment39Test {
    private val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val rules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    private val out get()=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"pass39").apply{mkdirs()}
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
    private fun layout(tag:String):androidx.compose.ui.text.TextLayoutResult {
        val list=mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose.onNodeWithTag(tag,true).performSemanticsAction(SemanticsActions.GetTextLayoutResult){it(list)}
        return list.single()
    }
    @Test fun adjacentRowsAcrossLayouts() {
        val c=mutableStateOf(Case(412,915,AppLanguage.FRENCH))
        val item=mutableStateOf(LearningItem("jouer",0,0))
        compose.activityRule.scenario.onActivity{it.setContent{Frame(c.value){val q=QuizQuestion.from(item.value);ConjugateScreen(QuizState(session=listOf(item.value)),q,c.value.lang,{},{},{},{},{})}}}
        val cases=listOf(Case(412,915,AppLanguage.FRENCH),Case(320,740,AppLanguage.FRENCH),Case(915,412,AppLanguage.FRENCH),Case(800,360,AppLanguage.FRENCH),Case(800,1280,AppLanguage.FRENCH),Case(1280,800,AppLanguage.FRENCH),Case(412,915,AppLanguage.FRENCH,1.3f),Case(412,915,AppLanguage.ENGLISH_UK),Case(915,412,AppLanguage.ENGLISH_UK))
        for(case in cases)for(qitem in listOf(LearningItem("jouer",0,0),LearningItem("finir",0,0),LearningItem("pouvoir",1,0),LearningItem("reconnaÃ®tre",5,0))) {
            compose.runOnIdle{c.value=case;item.value=qitem}
            val q=QuizQuestion.from(qitem)
            if(qitem.verbId=="jouer" && case.w>=412 && case.font==1f && case.h!=360) {
                compose.onNodeWithTag("quiz_row_0").assertIsDisplayed();compose.onNodeWithTag("quiz_row_5").assertIsDisplayed()
            }
            val starts=mutableListOf<Float>()
            for(row in 0..5) {
                compose.onNodeWithTag("quiz_fixed_$row",true).performScrollTo()
                val text=layout("quiz_fixed_$row");// Compose can flag paragraph-width overflow even when all rendered glyph bounds fit.
                for(line in 0 until text.lineCount)assertTrue("rendered text clipped $case $qitem $row",text.getLineRight(line)<=text.size.width+1f && text.getLineLeft(line)>=-1f && text.getLineBottom(line)<=text.size.height+1f)
                val fixed=compose.onNodeWithTag("quiz_fixed_$row",true).fetchSemanticsNode().boundsInRoot
                val box=compose.onNodeWithTag("quiz_box_${row}_0",true).fetchSemanticsNode().boundsInRoot
                val scale=text.size.height/text.layoutInput.style.lineHeight.value/case.font
                if(box.top<fixed.bottom && box.bottom>fixed.top)assertEquals("adjacent gap $case $qitem $row",6f*scale,box.left-fixed.right,2f)
                if(qitem.verbId=="jouer"&&case.w>=412&&case.font==1f) {
                    assertEquals("short prefix must not wrap",1,text.lineCount)
                    assertTrue("boxes inline",box.top<fixed.bottom && box.bottom>fixed.top)
                }
                if(box.top<fixed.bottom && box.bottom>fixed.top)assertEquals("prefix is right aligned",text.size.width.toFloat(),text.getLineRight(0),2f)
                starts+=box.left
                compose.onNodeWithTag("quiz_box_${row}_${q.boxCount-1}",true).performScrollTo().assertIsDisplayed()
            }
            if(qitem.verbId=="jouer"&&case.w>=412&&case.font==1f)assertTrue("box columns must align",starts.max()-starts.min()<2)
            compose.onNodeWithTag("quiz_row_next").performScrollTo().assertIsDisplayed()
            capture("rows-${case.w}-${case.h}-${case.lang}-${case.font}-${qitem.verbId}")
        }
    }
    @Test fun boxesEnterNextGreenAndHintsRemainInteractive() {
        val m=ConjugateViewModel(SavedStateHandle(mapOf("quizSession" to arrayListOf(LearningItem("jouer",0,0).encode()))))
        compose.activityRule.scenario.onActivity{it.setContent{Frame(Case(412,915,AppLanguage.FRENCH)){val s by m.state.collectAsState();ConjugateScreen(s,m.question!!,AppLanguage.FRENCH,{},m::letter,m::clear,m::submit,m::nextQuestion,m::edit)}}}
        compose.onNodeWithTag("quiz_key_e").performClick();compose.onNodeWithTag("quiz_enter").performScrollTo().performClick()
        assertTrue(0 in m.state.value.locked);compose.onNodeWithTag("quiz_box_0_0",true).performScrollTo().assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription,"Correct"))
        compose.runOnIdle{"es".forEach(m::letter)}
        compose.onNodeWithTag("quiz_box_1_1",true).performClick();assertEquals("e",m.state.value.entries[1])
        compose.onNodeWithTag("quiz_clear").performScrollTo().performClick()
        compose.onNodeWithTag("quiz_row_next").performScrollTo().performClick();assertEquals(2,m.state.value.active)
        compose.runOnIdle{repeat(2){m.letter('s');m.submit(null);m.tick(650)}}
        assertTrue(m.state.value.hints[2].isNotEmpty())
        compose.onNodeWithTag("quiz_box_2_0",true).performScrollTo().assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription,"Indice verrouillé"))
        capture("green-and-hints")
    }
}
