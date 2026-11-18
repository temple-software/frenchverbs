package com.templesoftware.frenchverbs

import android.content.res.Configuration
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.*
import com.templesoftware.frenchverbs.data.preferences.AppLanguage
import com.templesoftware.frenchverbs.domain.quiz.*
import com.templesoftware.frenchverbs.domain.verbs.*
import com.templesoftware.frenchverbs.ui.app.LocalizedContent
import com.templesoftware.frenchverbs.ui.quiz.ConjugateResultsScreen
import com.templesoftware.frenchverbs.ui.theme.TempleTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import kotlin.math.roundToInt

class ConjugateResults41LayoutTest {
    private val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val rules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    private data class Case(val w:Int,val h:Int,val lang:AppLanguage,val font:Float=1f,val columns:Int=2)
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
    private fun rect(tag:String):Rect {
        val node=compose.onNodeWithTag(tag,true).fetchSemanticsNode()
        return Rect(node.positionInRoot,androidx.compose.ui.geometry.Size(node.size.width.toFloat(),node.size.height.toFloat()))
    }
    private fun noOverflow(tag:String) {
        val layouts=mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose.onNodeWithTag(tag,true).performSemanticsAction(SemanticsActions.GetTextLayoutResult){it(layouts)}
        assertFalse(tag,layouts.single().hasVisualOverflow)
    }
    @Test fun responsiveGridCountsOrderWrappingAndReachability() {
        var c by mutableStateOf(Case(915,412,AppLanguage.FRENCH))
        var s by mutableStateOf(results41Fixture())
        var home=0
        compose.activityRule.scenario.onActivity{it.setContent{Frame(c){ConjugateResultsScreen(s,c.lang,{t->s=s.copy(resultToggles=if(t in s.resultToggles)s.resultToggles-t else s.resultToggles+t)},{home++})}}}
        val cases=listOf(Case(915,412,AppLanguage.FRENCH),Case(800,360,AppLanguage.ENGLISH_UK),
            Case(1280,800,AppLanguage.FRENCH,columns=3),Case(640,360,AppLanguage.FRENCH,columns=1),
            Case(800,360,AppLanguage.ENGLISH_UK,1.8f,1),Case(1280,800,AppLanguage.ENGLISH_UK,1.5f,2),
            Case(412,915,AppLanguage.FRENCH,columns=1),Case(412,915,AppLanguage.ENGLISH_UK,columns=1))
        for(count in listOf(1,2,3,4,6))for(case in cases) {
            compose.runOnIdle{c=case;s=results41Fixture(count)}
            compose.onNodeWithTag("conj_results_stars").performScrollTo().assertIsDisplayed()
            val portrait=case.w<case.h
            if(portrait)compose.onNodeWithTag("conj_results_grid").assertDoesNotExist()
            else {
                val cards=(0 until count).map{rect("conj_result_card_$it")}
                val firstRow=cards.takeWhile{kotlin.math.abs(it.top-cards[0].top)<2}
                assertEquals("$case count=$count",minOf(count,case.columns),firstRow.size)
                for(i in cards.indices) {
                    if(i%case.columns>0)assertTrue("reading order",cards[i].left>cards[i-1].left)
                    if(i>=case.columns)assertTrue("next row",cards[i].top>=cards[i-case.columns].bottom)
                    for(j in 0 until i)assertFalse("cards overlap $i $j",cards[i].overlaps(cards[j]))
                }
                val grid=rect("conj_results_grid")
                cards.forEach{assertTrue(it.left>=grid.left-1 && it.right<=grid.right+1)}
                if(count==1)assertEquals(grid.center.x,cards.single().center.x,2f)
            }
            for(t in 0 until count) {
                noOverflow("conj_result_heading_$t")
                compose.onNodeWithTag("conj_result_heading_$t",true).performScrollTo().assertIsDisplayed()
            }
            // Long lists with verified compound/long forms, all expanded.
            compose.runOnIdle {
                val longLines=s.completedLines.map{line->line.copy(conjugation=QuizQuestion.from(LearningItem("reconnaître",line.tense,0)).rows[line.person].text)}
                s=s.copy(completedLines=longLines,wrongLines=longLines.map{it.id},resultToggles=(0 until count).toSet())
            }
            for(t in 0 until count) {
                noOverflow("conj_result_heading_$t")
                for(p in 0..5)noOverflow("conj_result_text_${t*6+p}")
                compose.onNodeWithTag("conj_result_line_${t*6+5}",true).performScrollTo().assertIsDisplayed()
            }
            if(!portrait) {
                val cards=(0 until count).map{rect("conj_result_card_$it")}
                for(i in cards.indices)for(j in 0 until i)assertFalse("expanded overlap",cards[i].overlaps(cards[j]))
            }
            compose.onNodeWithTag("conj_results_home").performScrollTo().assertIsDisplayed()
        }
        compose.onNodeWithTag("conj_results_home").performClick();assertEquals(1,home)
    }
    @Test fun independentCardsRetainNaturalHeight() {
        var s by mutableStateOf(results41Fixture())
        compose.activityRule.scenario.onActivity{it.setContent{Frame(Case(915,412,AppLanguage.FRENCH)){ConjugateResultsScreen(s,AppLanguage.FRENCH,{t->s=s.copy(resultToggles=if(t in s.resultToggles)s.resultToggles-t else s.resultToggles+t)},{})}}}
        compose.onNodeWithTag("conj_result_line_0",true).assertExists()
        compose.onNodeWithTag("conj_result_summary_1",true).performScrollTo().performTouchInput{click()}
        compose.onNodeWithTag("conj_result_line_6",true).assertExists()
        compose.onNodeWithTag("conj_result_line_0",true).assertExists()
        assertTrue(rect("conj_result_card_1").height>rect("conj_result_card_0").height)
        compose.onNodeWithTag("conj_result_heading_0",true).performScrollTo().performTouchInput{click()}
        compose.onNodeWithTag("conj_result_summary_0",true).assertExists()
        compose.onNodeWithTag("conj_result_line_6",true).performScrollTo().performTouchInput{click()}
        compose.onNodeWithTag("conj_result_summary_1",true).assertExists()
    }
}
