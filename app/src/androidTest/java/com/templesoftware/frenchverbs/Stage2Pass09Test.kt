package com.templesoftware.frenchverbs

import android.content.ActivityNotFoundException
import android.content.Intent
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
import androidx.navigation.compose.rememberNavController
import com.templesoftware.frenchverbs.data.preferences.AppLanguage
import com.templesoftware.frenchverbs.ui.about.AboutScreen
import com.templesoftware.frenchverbs.config.AppContent
import com.templesoftware.frenchverbs.ui.app.LocalizedContent
import com.templesoftware.frenchverbs.ui.navigation.AppNavHost
import com.templesoftware.frenchverbs.ui.verbs.ActivityScreen
import com.templesoftware.frenchverbs.ui.theme.TempleTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

/** Focused visual-review checks for About and the activity homepage only. */
class Stage2Pass09Test {
    val compose = createAndroidComposeRule<MainActivity>()
    @get:Rule val rotationRules = org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private val out get()=File(inst.targetContext.getExternalFilesDir(null),InstrumentationRegistry.getArguments().getString("pass") ?: "about-followup").apply{mkdirs()}
    private val tags=listOf("about_logo","about_title","about_intro","about_guide","about_privacy")+(0..4).map{"about_badge_$it"}+"about_more"
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
                    if (tag == "screen_title" || tag.startsWith("about_badge_text_") || text.layoutInput.text.text == "Confidentialité") {
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
    private fun scrollRange():Float = compose.onNodeWithTag("about_content").fetchSemanticsNode()
        .config[SemanticsProperties.VerticalScrollAxisRange].maxValue()

    @Test fun aboutSpacingDimensionsFontsAndLanguages() {
        val current=mutableStateOf(cases.first())
        compose.activityRule.scenario.onActivity{it.setContent{Frame(current.value){AboutScreen(current.value.lang,{})}}}
        val records=mutableListOf<String>()
        for(c in cases) {
            compose.runOnIdle{current.value=c};compose.waitForIdle()
            compose.onNodeWithTag("about_content").performSemanticsAction(SemanticsActions.ScrollBy){it(0f,-100000f)}
            fitsText()
            val scroll=scrollRange()
            if(c.font==1f && c.w>=360 && c.h>=360) {
                assertEquals("Fits $c",0f,scroll,1f)
                tags.forEach{compose.onNodeWithTag(it).assertIsDisplayed()}
            }
            val left=rect("about_guide");val right=rect("about_privacy")
            assertEquals(left.top,right.top,1f);assertEquals(left.height,right.height,1f)
            assertTrue(left.right<right.left)
            assertEquals(rect("about_badge_0").top,rect("about_badge_2").top,1f)
            assertEquals(rect("about_badge_3").top,rect("about_badge_4").top,1f)
            assertTrue(rect("about_badge_0").bottom<rect("about_badge_3").top)
            assertTrue(rect("about_badge_3").bottom<rect("about_more").top)
            assertTrue(rect("about_logo").bottom<rect("about_title").top)
            assertTrue(rect("about_title").bottom<rect("about_intro").top)
            if(c.w>c.h) {
                assertTrue(rect("about_left_column").right<rect("about_right_column").left)
                val column=rect("about_left_column");val other=rect("about_right_column")
                val scale=column.width/((minOf(1100,c.w-32)-16)*.42f)
                assertTrue("Links stay left $c",left.left>=column.left-1 && right.right<=column.right+1)
                assertTrue("Links below description $c",left.top>rect("about_intro").bottom)
                assertTrue("Connected heading $c",rect("about_intro").top-rect("about_title").bottom<=11*scale)
                assertTrue("Compact benefit rows $c",rect("about_benefit_row_3").top-rect("about_benefit_row_0").bottom<=9*scale)
                assertTrue("More apps stays right $c",rect("about_more").left>=other.left-1)
                assertEquals("Aligned buttons $c",left.top,rect("about_more").top,1.5f)
                val top=rect("about_content").top+8*scale
                assertEquals("Landscape logo at top $c",top,rect("about_logo").top,2f)
                val benefits=rect("about_benefits_block")
                assertEquals("Benefits centred above button $c",(top+left.top)/2,benefits.center.y,2f)
                val bottom=top+rect("about_logo").height+2*(rect("about_title").top-rect("about_logo").bottom)+(left.bottom-rect("about_title").top)
                if(scroll==0f) assertEquals("Left group centred below logo $c",rect("about_viewport").bottom-8*scale,bottom,3f)
            } else {
                compose.onNodeWithTag("about_left_column").assertDoesNotExist()
                val intro=rect("about_introduction_block");val benefits=rect("about_benefits_block")
                val scale=intro.width/(minOf(560,c.w-32))
                val viewport=rect("about_viewport")
                val inner=((viewport.height/scale)/60).coerceIn(10f,16f)
                assertEquals("Logo stays at top $c",16*scale,rect("about_logo").top-viewport.top,1.5f)
                assertEquals("Increased logo-title gap $c",(inner+20)*scale,rect("about_title").top-rect("about_logo").bottom,2f)
                assertEquals("Unchanged benefit-row gap $c",inner*scale,rect("about_benefit_row_3").top-rect("about_benefit_row_0").bottom,2f)
                assertTrue("Compact introduction gap $c",left.top-rect("about_intro").bottom<=17*scale)
                assertTrue("Modest block gap $c",benefits.top-intro.bottom in 37*scale..49*scale)
                assertTrue("Grouped benefit rows $c",rect("about_benefit_row_3").top-rect("about_benefit_row_0").bottom<=17*scale)
                assertTrue("More apps follows benefits $c",rect("about_more").top-benefits.bottom in 37*scale..49*scale)
                assertTrue(rect("about_intro").bottom<left.top)
            }
            for(tag in tags)compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("about_content").performSemanticsAction(SemanticsActions.ScrollBy){it(0f,-100000f)}
            records+="$c scroll=$scroll"
            capture("about-${c.w}-${c.h}-${c.font}-${c.lang}")
        }
        File(out,"about-matrix.txt").writeText("PASS ${cases.size} cases\n"+records.joinToString("\n"))
    }
    @Test fun actualPhonePortraitLandscapeCaptures() {
        val lang=mutableStateOf(AppLanguage.ENGLISH_UK);val about=mutableStateOf(true)
        val records=mutableListOf<String>()
        for(o in listOf(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)) {
            compose.activityRule.scenario.onActivity{it.requestedOrientation=o}
            compose.waitUntil(10000){compose.activity.resources.configuration.orientation==if(o==1)1 else 2}
            compose.activityRule.scenario.onActivity{it.setContent{LocalizedContent(lang.value){TempleTheme{
                if(about.value)AboutScreen(lang.value,{}) else ActivityScreen(lang.value,{}, {})
            }}}}
            for(l in AppLanguage.entries) for(a in listOf(true,false)) {
                compose.runOnIdle{lang.value=l;about.value=a};compose.waitForIdle();fitsText()
                if(a) {
                    assertEquals(0f,scrollRange(),1f)
                    tags.forEach{compose.onNodeWithTag(it).assertIsDisplayed()}
                } else listOf("learn","repeat","conjugate","use").forEach{compose.onNodeWithTag("open_$it").assertIsDisplayed()}
                capture("real-$o-$l-${if(a)"about" else "homepage"}")
                val cfg=compose.activity.resources.configuration
                records+="${cfg.screenWidthDp}x${cfg.screenHeightDp}dp orientation=$o language=$l about=$a PASS"
            }
        }
        File(out,"real-layout.txt").writeText(records.joinToString("\n"))
    }

    @Test fun actualAboutPortraitLandscapeCaptures() {
        val lang=mutableStateOf(AppLanguage.ENGLISH_UK)
        for(o in listOf(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)) {
            compose.activityRule.scenario.onActivity{it.requestedOrientation=o}
            compose.waitUntil(10000){compose.activity.resources.configuration.orientation==if(o==1)1 else 2}
            compose.activityRule.scenario.onActivity{it.setContent{LocalizedContent(lang.value){TempleTheme{AboutScreen(lang.value,{})}}}}
            for(l in AppLanguage.entries) {
                compose.runOnIdle{lang.value=l};compose.waitForIdle();fitsText()
                assertEquals(0f,scrollRange(),1f);tags.forEach{compose.onNodeWithTag(it).assertIsDisplayed()}
                capture("real-$o-$l-about")
            }
        }
    }

}
