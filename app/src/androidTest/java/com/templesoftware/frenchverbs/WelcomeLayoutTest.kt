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
import com.templesoftware.frenchverbs.ui.verbs.WelcomeScreen
import com.templesoftware.frenchverbs.ui.theme.TempleTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import kotlin.math.roundToInt

class WelcomeLayoutTest {
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
    @Test fun compactSummaryBothLanguagesAndResponsiveLayouts() {
        var c by mutableStateOf(Case(412,915,AppLanguage.FRENCH))
        var selected by mutableStateOf(Selection())
        var again=0;var change=0
        compose.activityRule.scenario.onActivity{it.setContent{Frame(c){WelcomeScreen(selected,c.lang,{again++},{change++})}}}
        val full=Selection(tenses=Choices.tenses.toSet(),types=Choices.types.toSet(),important=Choices.important.toSet(),er=Choices.er.toSet(),families=Choices.families.toSet())
        val few=full.copy(important=Choices.important.take(3).toSet(),er=setOf("e_grave","accent","double"),families=setOf("connaitre","recevoir","conduire"))
        for(language in AppLanguage.entries)for((w,h,font) in listOf(Triple(412,915,1f),Triple(915,412,1f),Triple(800,360,1f),Triple(320,740,1.8f),Triple(1280,800,1f)))for(selection in listOf(full,few)) {
            compose.runOnIdle{c=Case(w,h,language,font);selected=selection}
            compose.onNodeWithTag("screen_title").assertTextEquals(if(language==AppLanguage.FRENCH)"Bon retour !"else "Welcome back")
            for(tag in listOf("welcome_tenses","welcome_change_description")+Choices.types.map{"welcome_category_$it"}) {
                val node=compose.onNodeWithTag(tag,true)
                node.performScrollTo().assertIsDisplayed()
                val layouts=mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                node.performSemanticsAction(SemanticsActions.GetTextLayoutResult){it(layouts)}
                assertFalse("text clipped $c $tag",layouts.single().hasVisualOverflow)
            }
            compose.onNodeWithTag("welcome_change").performScrollTo().assertIsDisplayed()
            val againSize=compose.onNodeWithTag("welcome_again").fetchSemanticsNode().size
            val changeSize=compose.onNodeWithTag("welcome_change").fetchSemanticsNode().size
            assertEquals("equal button dimensions $c",againSize,changeSize)
            if(w>h && w>=600 && font<1.5f) {
                val againNode=compose.onNodeWithTag("welcome_again").fetchSemanticsNode()
                val changeNode=compose.onNodeWithTag("welcome_change").fetchSemanticsNode()
                assertEquals("landscape button tops",againNode.positionInRoot.y,changeNode.positionInRoot.y,1f)
            }
            compose.onNodeWithTag("welcome_change_description",true).assertTextEquals(if(language==AppLanguage.FRENCH)"Changer les verbes et/ou les temps"else "Change verbs and/or tenses")
            if(selection==full)compose.onNodeWithTag("welcome_category_important",true).assertTextContains(if(language==AppLanguage.FRENCH)"10 sur 10 sélectionnés"else "10 out of 10 selected",substring=true)
            else compose.onNodeWithTag("welcome_category_important",true).assertTextContains("être, avoir, aller",substring=true)
        }
        compose.onNodeWithTag("welcome_change").performClick();assertEquals(1,change)
        compose.onNodeWithTag("welcome_tenses",true).performScrollTo().performTouchInput{click()};assertEquals(1,again)
    }
}
