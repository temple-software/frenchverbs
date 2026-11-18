package com.templesoftware.frenchverbs

import android.content.res.Configuration
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.*
import androidx.test.platform.app.InstrumentationRegistry
import com.templesoftware.frenchverbs.data.preferences.AppLanguage
import com.templesoftware.frenchverbs.ui.app.LocalizedContent
import com.templesoftware.frenchverbs.ui.verbs.ActivityScreen
import com.templesoftware.frenchverbs.ui.theme.TempleTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.math.roundToInt

/** Homepage-only typography, layout and callback verification. */
class HomepagePass11Test {
    val compose = createAndroidComposeRule<MainActivity>()
    @get:Rule val rotationRules = org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private val out get()=File(inst.targetContext.getExternalFilesDir(null),"pass11").apply{mkdirs()}
    private var frameDensity=1f
    private data class Case(val w:Int,val h:Int,val font:Float,val lang:AppLanguage)
    private val cases=listOf(360 to 800,800 to 360,412 to 915,915 to 412,800 to 1280,1280 to 800,320 to 568)
        .flatMap{(w,h)->listOf(1f,2f).flatMap{f->AppLanguage.entries.map{Case(w,h,f,it)}}}
    private fun capture(name:String) {
        val b=inst.uiAutomation.takeScreenshot()
        File(out,"$name.png").outputStream().use{b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};b.recycle()
    }
    private fun rect(tag:String)=compose.onNodeWithTag(tag,useUnmergedTree=true).fetchSemanticsNode().let{
        androidx.compose.ui.geometry.Rect(it.positionInRoot,androidx.compose.ui.geometry.Size(it.size.width.toFloat(),it.size.height.toFloat()))
    }
    @Composable private fun Frame(c:Case,content:@Composable ()->Unit) {
        BoxWithConstraints(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            val host=LocalDensity.current
            val d=minOf(with(host){maxWidth.toPx()}/c.w,with(host){maxHeight.toPx()}/c.h)
            SideEffect { frameDensity=d }
            val config=Configuration(LocalConfiguration.current).apply {
                screenWidthDp=c.w;screenHeightDp=c.h;fontScale=c.font;densityDpi=(d*160).roundToInt();orientation=if(c.w>c.h)2 else 1
            }
            CompositionLocalProvider(LocalDensity provides Density(d,c.font),LocalConfiguration provides config) {
                Box(Modifier.requiredSize(c.w.dp,(c.h-76).dp)) {LocalizedContent(c.lang){TempleTheme{content()}}}
            }
        }
    }
    private fun fitsText() {
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),useUnmergedTree=true)
            .fetchSemanticsNodes().forEach { node ->
                val layouts=mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)
                layouts.forEach { text ->
                    assertFalse("Clipped: ${text.layoutInput.text}, size=${text.size}, lines=${text.lineCount}, style=${text.layoutInput.style}",text.didOverflowHeight)
                    for(line in 0 until text.lineCount) assertTrue("Wide: ${text.layoutInput.text}",
                        text.getLineRight(line)-text.getLineLeft(line)<=text.size.width+1f)
                    for(line in 0 until text.lineCount) assertFalse(text.isLineEllipsized(line))
                    val tag = node.config.getOrNull(SemanticsProperties.TestTag).orEmpty()
                    if (tag == "screen_title") {
                        val value = text.layoutInput.text.text
                        for (line in 0 until text.lineCount - 1) {
                            val end = text.getLineEnd(line)
                            assertTrue("Split word: $value at $end", end <= 0 || end >= value.length ||
                                value[end - 1].isWhitespace() || value[end].isWhitespace())
                        }
                    }
                }
            }
    }
    @Test fun homepageTitlesLayoutsAndCallbacks() {
        val current=mutableStateOf(cases.first());var selected="";var homes=0
        compose.activityRule.scenario.onActivity{it.setContent{Frame(current.value){ActivityScreen(current.value.lang,{homes++},{selected=it})}}}
        val records=mutableListOf<String>()
        val geometry=mutableListOf<String>()
        for(c in cases) {
            compose.runOnIdle{current.value=c};compose.waitForIdle()
            val expected=if(c.lang==AppLanguage.FRENCH)"Pratiquer le français" else "Practice French"
            compose.onNodeWithTag("screen_title").assertTextEquals(expected).assertIsDisplayed();fitsText()
            compose.onNodeWithTag("language_selector").assertDoesNotExist()
            compose.onNodeWithTag("open_about").assertDoesNotExist()
            val title=rect("screen_title")
            for((index,route) in listOf("learn","repeat","conjugate","use").withIndex()) {
                compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex)).performScrollToIndex(index)
                val button=compose.onNodeWithTag("open_$route")
                button.performScrollTo().assertIsDisplayed().performClick()
                assertEquals(route,selected)
                val nodes=compose.onAllNodes(hasAnyAncestor(hasTestTag("open_$route")) and SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),useUnmergedTree=true).fetchSemanticsNodes()
                assertEquals(1,nodes.size)
                val results=mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                nodes.single().config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results)
                val text=results.single()
                assertEquals(19.sp,text.layoutInput.style.fontSize)
                assertEquals(androidx.compose.ui.text.font.FontWeight.SemiBold,text.layoutInput.style.fontWeight)
                val expectedLabels=if(c.lang==AppLanguage.FRENCH) listOf("Apprendre","Répéter","Conjuguer","Utiliser") else listOf("Learn","Repeat","Conjugate","Use")
                assertEquals(expectedLabels[index],text.layoutInput.text.text)
                assertEquals("One-line label $c $route",1,text.lineCount)
                assertFalse("Label overflow $c $route: $text",text.didOverflowHeight)
                val r=rect("open_$route")
                val columns=if(c.w>c.h && c.font<1.8f)2 else 1
                val expectedWidth=(minOf(760,c.w-40)-16-16*(columns-1)).toFloat()/columns
                assertEquals("Approved button width $c $route",expectedWidth*frameDensity,r.width,2f)
                assertEquals("Approved button height $c $route",76*frameDensity,r.height,1f)
                val n=nodes.single()
                assertEquals(r.center.x,n.positionInRoot.x+n.size.width/2f,2f)
                assertEquals(r.center.y,n.positionInRoot.y+n.size.height/2f,2f)
                geometry+="$c $route $r"

                assertTrue(title.bottom<=rect("open_$route").top)
            }
            compose.onNode(hasContentDescription("Home") or hasContentDescription("Accueil")).performClick()
            compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex)).performScrollToIndex(0)
            if(c.font==1f) {
                val first=rect("open_learn");val second=rect("open_repeat")
                val third=rect("open_conjugate");val fourth=rect("open_use")
                if(c.w>c.h) {
                    assertEquals(first.top,second.top,1f)
                    assertEquals(third.top,fourth.top,1f)
                    assertEquals("Approved horizontal gap $c",16*frameDensity,second.left-first.right,2f)
                    assertEquals("Approved vertical gap $c",18*frameDensity,third.top-first.bottom,2f)
                } else {
                    assertEquals(first.left,second.left,1f)
                    assertEquals("Approved vertical gap $c",18*frameDensity,second.top-first.bottom,2f)
                    assertEquals("Approved vertical gap $c",18*frameDensity,third.top-second.bottom,2f)
                    assertEquals("Approved vertical gap $c",18*frameDensity,fourth.top-third.bottom,2f)
                }
            }
            capture("homepage-${c.w}-${c.h}-${c.font}-${c.lang}")
            records+="$c title=$expected callbacks=PASS"
        }
        assertEquals(cases.size,homes)
        File(out,"geometry.txt").writeText(geometry.joinToString("\n"))
        File(out,"homepage-matrix.txt").writeText("PASS ${cases.size} cases\n"+records.joinToString("\n"))
    }

    @Test fun actualHomepageCaptures() {
        val lang=mutableStateOf(AppLanguage.ENGLISH_UK)
        for(o in listOf(1,0)) {
            compose.activityRule.scenario.onActivity{it.requestedOrientation=o}
            compose.waitUntil(10000){compose.activity.resources.configuration.orientation==if(o==1)1 else 2}
            compose.activityRule.scenario.onActivity{it.setContent{LocalizedContent(lang.value){TempleTheme{ActivityScreen(lang.value,{},{})}}}}
            for(l in AppLanguage.entries) {
                compose.runOnIdle{lang.value=l};compose.waitForIdle();fitsText()
                listOf("learn","repeat","conjugate","use").forEach{compose.onNodeWithTag("open_$it").assertIsDisplayed()}
                capture("real-$o-$l-homepage")
            }
        }
    }
}
