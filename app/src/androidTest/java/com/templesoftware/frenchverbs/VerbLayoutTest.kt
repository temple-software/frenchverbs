package com.templesoftware.frenchverbs

import android.content.res.Configuration
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
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
import kotlin.math.roundToInt

class VerbLayoutTest {
    val compose = createAndroidComposeRule<MainActivity>()
    @get:Rule val rotationRules = org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private val all=Selection(tenses=Choices.tenses.toSet(),types=Choices.types.toSet())
    private val routes=listOf("tenses","types","important","er","families","activities","learn0","learn1","learn2","learn3","learn4","learn5")
    @Composable private fun Screen(route:String,language:AppLanguage,verb:String="prendre") {
        val state=VerbState(loaded=true,selection=all,session=(0..5).map{LearningItem(verb,it,0)},index=route.last().digitToIntOrNull()?:0)
        when {
            route.startsWith("learn")->LearnScreen(state,language,{},{})
            route=="activities"->ActivityScreen(language,{},{})
            route in listOf("repeat","conjugate","use")->PlaceholderScreen(route,language,{})
            else->SelectionScreen(route,state,language,{},{},{},{},{})
        }
    }
    private fun glyphs(label:String) {
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),useUnmergedTree=true).fetchSemanticsNodes().forEach { node ->
            val layouts=mutableListOf<TextLayoutResult>()
            node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)
            layouts.forEach { text ->
                val value=text.layoutInput.text.text
                assertFalse("Height clipped $label $value",text.didOverflowHeight)
                for(line in 0 until text.lineCount) {
                    assertTrue("Glyph overflow $label $value",text.getLineRight(line)-text.getLineLeft(line)<=text.size.width+1f)
                    assertFalse("Ellipsis $label $value",text.isLineEllipsized(line))
                }
                for(line in 0 until text.lineCount-1) {
                    val end=text.getLineEnd(line)
                    assertTrue("Split word $label $value at $end",end<=0||end>=value.length||!value[end-1].isLetter()||!value[end].isLetter())
                }
            }
        }
    }
    @Test fun phoneTabletPortraitLandscapeFontAndLanguageMatrix() {
        data class Case(val w:Int,val h:Int,val font:Float,val language:AppLanguage,val route:String)
        val cases=listOf(800 to 1280,600 to 960,384 to 854,360 to 800,412 to 915,360 to 640,320 to 568)
            .flatMap { listOf(it,it.second to it.first) }.flatMap { (w,h)->listOf(1f,1.3f,2f).flatMap { f->AppLanguage.entries.flatMap { l->routes.map { Case(w,h,f,l,it) } } } }
        val focused=InstrumentationRegistry.getArguments().getString("layoutCase")
        val activeCases=cases.filter { focused==null || "${it.w}x${it.h}:${it.font}"==focused }
        val selected=mutableStateOf(activeCases.first())
        compose.activityRule.scenario.onActivity { it.setContent {
            val c=selected.value
            BoxWithConstraints(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
                val host=LocalDensity.current
                val d=minOf(with(host){maxWidth.toPx()}/c.w,with(host){maxHeight.toPx()}/c.h)
                val config=Configuration(LocalConfiguration.current).apply { screenWidthDp=c.w;screenHeightDp=c.h;fontScale=c.font;densityDpi=(d*160).roundToInt();orientation=if(c.w>c.h)2 else 1 }
                CompositionLocalProvider(LocalDensity provides Density(d,c.font),LocalConfiguration provides config) {
                    Box(Modifier.requiredSize(c.w.dp,c.h.dp)) { LocalizedContent(c.language) { TempleTheme { key(c.route) { Screen(c.route,c.language,"reconnaître") } } } }
                }
            }
        } }
        val measurements=mutableListOf<String>()
        var tenseCenter=0f
        activeCases.forEach { c ->
            android.util.Log.i("FrenchVerbsMatrix",c.toString())
            compose.runOnIdle { selected.value=c }; compose.waitForIdle()
            glyphs(c.toString())
            if(c.route.startsWith("learn")) {
                compose.onNodeWithTag("learn_next").assertIsDisplayed()
                val nodes=compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.TestTag)).fetchSemanticsNodes()
                val rowTags=nodes.mapNotNull { it.config.getOrNull(SemanticsProperties.TestTag) }.filter { it.startsWith("form_") }
                assertEquals("Six rows per panel $c",12,rowTags.size)
                rowTags.filter { it.endsWith("_0") || it.endsWith("_5") }.forEach { tag->compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed() }
                glyphs(c.toString())
            } else if(c.route!="activities") {
                compose.onNodeWithTag("next").assertIsDisplayed()
                val viewport=compose.onNodeWithTag("selection_viewport").fetchSemanticsNode().boundsInRoot
                val button=compose.onNodeWithTag("next").fetchSemanticsNode().boundsInRoot
                val scroll=compose.onNodeWithTag("choices").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].maxValue()
                android.util.Log.i("SelectionGeometry","$c viewport=$viewport button=$button last="+compose.onNodeWithTag("choice_${all.visible(c.route).last()}").fetchSemanticsNode().boundsInRoot+" scroll=$scroll baseline=$tenseCenter")
                assertTrue("Safe footer $c",button.bottom<=viewport.bottom+1 && button.top>=viewport.top)
                assertEquals("Horizontally centred $c",viewport.center.x,button.center.x,1.5f)
                if(c.route=="tenses") {
                    tenseCenter=button.center.y
                    if(scroll==0f) {
                        // In a multi-column layout the final row can contain taller
                        // wrapped neighbours. Its bottom is the content's bottom edge.
                        val lastBottom=Choices.tenses.maxOf { compose.onNodeWithTag("choice_$it").fetchSemanticsNode().boundsInRoot.bottom }
                        val expected=(lastBottom+viewport.bottom)/2
                        if(viewport.bottom-lastBottom>=button.height+32*viewport.width/(c.w-32))
                            assertEquals("Tenses midpoint $c",expected,button.center.y,2f)
                    }
                } else if(scroll==0f) {
                    assertTrue("Preferred Tenses baseline $c",button.center.y>=tenseCenter-2)
                    val lastBounds=compose.onNodeWithTag("choice_${all.visible(c.route).last()}").fetchSemanticsNode().boundsInRoot
                    assertTrue("Content clearance $c",button.top>lastBounds.bottom)
                    if(lastBounds.bottom+button.height/2+32*viewport.width/c.w<tenseCenter)
                        assertEquals("Aligned with Tenses $c",tenseCenter,button.center.y,2f)
                }
                measurements += "$c scrollPx=$scroll nextCenter=${button.center.y}"
                compose.onNodeWithTag("choices").performSemanticsAction(SemanticsActions.ScrollBy) { it(0f,-100000f) }
                if(c.route=="tenses") { compose.onNodeWithTag("language_selector").assertIsDisplayed();compose.onNodeWithTag("open_about").assertIsDisplayed() }
                else { compose.onNodeWithTag("language_selector").assertDoesNotExist();compose.onNodeWithTag("open_about").assertDoesNotExist() }
                val last=all.visible(c.route).last()
                compose.onNodeWithTag("choice_$last").performScrollTo()
                compose.onNodeWithTag("choice_$last").assertIsDisplayed()
                glyphs(c.toString())
                if(c.w in listOf(800,1280) && c.h in listOf(800,1280) && c.font in listOf(1f,2f)) {
                    compose.onNodeWithTag("choices").performSemanticsAction(SemanticsActions.ScrollBy) { it(0f,-100000f) }
                    compose.waitForIdle()
                    val folder=File(inst.targetContext.getExternalFilesDir(null),"pass02").apply{mkdirs()}
                    val bitmap=inst.uiAutomation.takeScreenshot()
                    File(folder,"tablet-${c.w}-${c.h}-${c.font}-${c.language}-${c.route}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
                    bitmap.recycle()
                }
            }
        }
        val out=File(inst.targetContext.getExternalFilesDir(null),"pass02").apply{mkdirs()}
        File(out,"selection-measurements.txt").writeText(measurements.joinToString("\n"))
        File(out,"layout-matrix.txt").writeText("Passed ${activeCases.size} screen/size/font/language cases; actual glyph/word bounds, scrolling and pinned actions.\n")
    }
    @Test fun realEmulatorScreenshotsBothLanguagesAndOrientations() {
        val route=mutableStateOf("tenses"); val language=mutableStateOf(AppLanguage.ENGLISH_UK); val dark=mutableStateOf(false)
        val out=File(inst.targetContext.getExternalFilesDir(null),"pass02").apply{mkdirs()}
        for(orientation in listOf(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)) {
            compose.activityRule.scenario.onActivity { it.requestedOrientation=orientation }
            compose.waitUntil(10000){compose.activity.resources.configuration.orientation==if(orientation==1)1 else 2}
            compose.activityRule.scenario.onActivity { it.setContent { LocalizedContent(language.value){TempleTheme(darkTheme=dark.value){key(route.value){Screen(route.value,language.value,if(route.value=="learn3"||route.value=="learn5")"sortir" else "manger")}}} } }
            for(l in AppLanguage.entries) for(r in routes+listOf("repeat","conjugate","use")) {
                compose.runOnIdle { language.value=l;route.value=r };compose.waitForIdle();glyphs("real $l $r $orientation")
                if(r.startsWith("learn")) {
                    val footer=compose.onNodeWithTag("learn_next").getUnclippedBoundsInRoot()
                    val panels=compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.TestTag)).fetchSemanticsNodes()
                        .filter { it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("panel_")==true }
                    assertEquals(2,panels.size)
                    panels.forEach { panel->assertTrue("Full panel frame must fit at standard device size: $l $r $orientation",compose.onNodeWithTag(panel.config[SemanticsProperties.TestTag]).getUnclippedBoundsInRoot().bottom<=footer.top) }
                }
                val bitmap=inst.uiAutomation.takeScreenshot()
                File(out,"${l.languageTag}-$orientation-$r.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
            }
            compose.runOnIdle { dark.value=true;route.value="learn3" };compose.waitForIdle();glyphs("dark $orientation")
            val bitmap=inst.uiAutomation.takeScreenshot();File(out,"dark-$orientation.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
            compose.runOnIdle { dark.value=false }
        }
    }
}
