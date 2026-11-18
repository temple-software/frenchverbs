package com.templesoftware.frenchverbs

import android.content.res.Configuration
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.*
import androidx.test.platform.app.InstrumentationRegistry
import com.templesoftware.frenchverbs.data.preferences.AppLanguage
import com.templesoftware.frenchverbs.domain.verbs.*
import com.templesoftware.frenchverbs.ui.app.*
import com.templesoftware.frenchverbs.ui.theme.TempleTheme
import com.templesoftware.frenchverbs.ui.verbs.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

class TensesScreenTest {
    val compose = createAndroidComposeRule<MainActivity>()
    @get:Rule val rotationRules = org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private val selection=mutableStateOf(Selection())
    private val language=mutableStateOf(AppLanguage.ENGLISH_UK)
    private var nextClicks=0
    private var infoClicks=0
    private lateinit var measurer:TextMeasurer
    private val examples=mutableMapOf<String,String>()
    private val out get()=File(inst.targetContext.getExternalFilesDir(null),"pass03").apply{mkdirs()}
    @Composable private fun Screen(dark:Boolean=false) {
        LocalizedContent(language.value) { TempleTheme(darkTheme=dark) {
            measurer=rememberTextMeasurer()
            Choices.tenses.forEach { examples[it]=androidx.compose.ui.res.stringResource(ChoiceResources.example.getValue(it)) }
            SelectionScreen("tenses",VerbState(loaded=true,selection=selection.value),language.value,
                {language.value=it},{selection.value=selection.value.toggle("tenses",it!!)},
                {nextClicks++},{},{infoClicks++})
        } }
    }
    private fun bounds(tag:String)=compose.onNodeWithTag(tag,useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
    private fun layouts(tag:String):TextLayoutResult {
        val list=mutableListOf<TextLayoutResult>()
        compose.onNodeWithTag(tag,useUnmergedTree=true).performSemanticsAction(SemanticsActions.GetTextLayoutResult){it(list)}
        return list.single()
    }
    private fun checkAnchors(label:String) {
        val safe=bounds("tenses_safe_area"); val info=bounds("open_about");val next=bounds("next")
        val d=info.width/48f
        assertEquals("Right margin $label",16*d,safe.right-info.right,1.5f)
        assertEquals("Bottom margin $label",16*d,safe.bottom-info.bottom,1.5f)
        assertTrue("Safe info $label",info.right<=safe.right && info.bottom<=safe.bottom)
        assertTrue("Info separate from Next $label",next.bottom<=info.top || next.right<=info.left || next.left>=info.right)
        assertTrue("Safe Next $label",next.top>=safe.top && next.bottom<=safe.bottom)
    }
    private fun checkCards(label:String):Map<String,Int> {
        val counts=mutableMapOf<String,Int>()
        Choices.tenses.forEach { id ->
            compose.onNodeWithTag("choice_$id").performScrollTo().assertIsDisplayed()
            val card=compose.onNodeWithTag("choice_$id",useUnmergedTree=true).fetchSemanticsNode();val tick=compose.onNodeWithTag("tense_tick_$id",useUnmergedTree=true).fetchSemanticsNode()
            assertEquals("Tick centred $label $id",card.positionInRoot.y+card.size.height/2f,tick.positionInRoot.y+tick.size.height/2f,1.5f)
            val line=layouts("tense_example_$id");val value=line.layoutInput.text.text
            assertEquals("One line $label $id",1,line.lineCount)
            assertFalse("No clipped glyphs $label $id text=$value width=${line.didOverflowWidth} height=${line.didOverflowHeight} size=${line.size} constraints=${line.layoutInput.constraints}",line.didOverflowHeight || line.didOverflowWidth)
            assertFalse(line.isLineEllipsized(0))
            val parts=examples.getValue(id).split(", ")
            assertTrue("Complete prefix $label $id",(1..parts.size).any { parts.take(it).joinToString(", ")==value })
            val available=bounds("tense_examples_slot_$id").width.roundToInt()
            var expected=""
            compose.runOnIdle {
                expected=(parts.size downTo 1).map { parts.take(it).joinToString(", ") }.first {
                    measurer.measure(AnnotatedString(it),line.layoutInput.style,softWrap=false,maxLines=1).size.width<=available
                }
            }
            assertEquals("Longest fitting prefix $label $id",expected,value)
            counts[id]=value.split(", ").size
        }
        checkAnchors(label)
        return counts
    }
    private fun screenshot(name:String) {
        val bitmap=inst.uiAutomation.takeScreenshot()
        File(out,"$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
    }
    @Test fun firstScreenPhoneTabletFontLanguageMatrix() {
        data class Case(val w:Int,val h:Int,val font:Float,val lang:AppLanguage)
        val cases=listOf(320 to 568,360 to 800,412 to 915,800 to 1280).flatMap { listOf(it,it.second to it.first) }
            .flatMap { (w,h)->listOf(1f,1.3f,2f).flatMap { f->AppLanguage.entries.map { Case(w,h,f,it) } } }
        val current=mutableStateOf(cases.first())
        compose.activityRule.scenario.onActivity { it.setContent {
            val c=current.value
            BoxWithConstraints(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
                val host=LocalDensity.current
                val d=minOf(with(host){maxWidth.toPx()}/c.w,with(host){maxHeight.toPx()}/c.h)
                val config=Configuration(LocalConfiguration.current).apply { screenWidthDp=c.w;screenHeightDp=c.h;fontScale=c.font;densityDpi=(d*160).roundToInt();orientation=if(c.w>c.h)2 else 1 }
                CompositionLocalProvider(LocalDensity provides Density(d,c.font),LocalConfiguration provides config) {
                    Box(Modifier.requiredSize(c.w.dp,c.h.dp)) { Screen() }
                }
            }
        } }
        val records=mutableListOf<String>();val presentCounts=mutableSetOf<Int>()
        cases.forEach { c ->
            compose.runOnIdle { current.value=c;language.value=c.lang };compose.waitForIdle()
            val counts=checkCards(c.toString());presentCounts+=counts.getValue("present");records+="$c $counts"
            if(c.font!=1.3f && c.w in listOf(320,800,1280)) {
                compose.onNodeWithTag("choices").performSemanticsAction(SemanticsActions.ScrollBy){it(0f,-100000f)}
                compose.waitForIdle();screenshot("matrix-${c.w}-${c.h}-${c.font}-${c.lang}")
            }
        }
        assertTrue("Width/font changes alter example count",presentCounts.size>=2)
        File(out,"matrix.txt").writeText("PASS ${cases.size} Tenses cases\n"+records.joinToString("\n"))
    }
    @Test fun disabledGreyCannotNavigateAndLanguageSwitchKeepsState() {
        val dark=mutableStateOf(false)
        compose.activityRule.scenario.onActivity { it.setContent { Screen(dark.value) } }
        compose.onNodeWithTag("choice_present").performClick()
        for(d in listOf(false,true)) {
            compose.runOnIdle { dark.value=d };compose.waitForIdle()
            compose.onNodeWithTag("next").assertIsNotEnabled().performTouchInput { click() }
            assertEquals(0,nextClicks)
            val pixels=compose.onNodeWithTag("next").captureToImage().toPixelMap()
            for(y in listOf(.2f,.5f,.8f)) {
                val color=pixels[(pixels.width*.12f).roundToInt(),(pixels.height*y).roundToInt()]
                assertEquals(214/255f,color.red,.01f);assertEquals(color.red,color.green,.001f);assertEquals(color.red,color.blue,.001f)
            }
            screenshot("disabled-${if(d) "dark" else "light"}")
        }
        compose.onNodeWithTag("language_selector").performClick();compose.onNodeWithText("Français").performClick()
        compose.onNodeWithTag("next").assertIsNotEnabled();assertEquals(AppLanguage.FRENCH,language.value)
        checkAnchors("French disabled")
        compose.onNodeWithTag("choice_present").performClick()
        compose.onNodeWithTag("next").assertIsEnabled().performClick();assertEquals(1,nextClicks)
        compose.onNodeWithTag("open_about").performClick();assertEquals(1,infoClicks)
        screenshot("enabled-French")
    }
    @Test fun realRotationBothLanguagesAndNavigationInsets() {
        for(orientation in listOf(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)) {
            compose.activityRule.scenario.onActivity { it.requestedOrientation=orientation }
            compose.waitUntil(10000){compose.activity.resources.configuration.orientation==if(orientation==1)1 else 2}
            compose.activityRule.scenario.onActivity { it.setContent { Screen() } }
            for(l in AppLanguage.entries) {
                compose.runOnIdle { language.value=l };compose.waitForIdle()
                checkCards("real $orientation $l")
                compose.onNodeWithTag("choices").performSemanticsAction(SemanticsActions.ScrollBy){it(0f,-100000f)}
                compose.waitForIdle();screenshot("real-$orientation-$l")
            }
        }
    }
}
