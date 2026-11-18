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
class Stage2Pass08Test {
    val compose = createAndroidComposeRule<MainActivity>()
    @get:Rule val rotationRules = org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private val out get()=File(inst.targetContext.getExternalFilesDir(null),"pass08").apply{mkdirs()}
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
            assertTrue(left.bottom<rect("about_badge_0").top)
            assertTrue(rect("about_logo").bottom<rect("about_title").top)
            assertTrue(rect("about_title").bottom<rect("about_intro").top)
            if(c.w>c.h) {
                assertTrue(rect("about_left_column").right<rect("about_right_column").left)
                assertEquals(rect("about_left_column").height,rect("about_right_column").height,1f)
            } else {
                compose.onNodeWithTag("about_left_column").assertDoesNotExist()
                assertTrue(rect("about_intro").bottom<left.top)
            }
            if(scroll==0f && c.w>c.h) {
                val viewport=rect("about_viewport")
                assertTrue("Bottom distribution $c",rect("about_more").bottom>viewport.top+viewport.height*.88f)
            }
            for(tag in tags)compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("about_content").performSemanticsAction(SemanticsActions.ScrollBy){it(0f,-100000f)}
            records+="$c scroll=$scroll"
            capture("about-${c.w}-${c.h}-${c.font}-${c.lang}")
        }
        File(out,"about-matrix.txt").writeText("PASS ${cases.size} cases\n"+records.joinToString("\n"))
    }
    @Test fun homepageTitlesLayoutsAndCallbacks() {
        val current=mutableStateOf(cases.first());var selected="";var homes=0
        compose.activityRule.scenario.onActivity{it.setContent{Frame(current.value){ActivityScreen(current.value.lang,{homes++},{selected=it})}}}
        val records=mutableListOf<String>()
        for(c in cases) {
            compose.runOnIdle{current.value=c};compose.waitForIdle()
            val expected=if(c.lang==AppLanguage.FRENCH)"Pratiquer le français" else "Practice French"
            compose.onNodeWithTag("screen_title").assertTextEquals(expected).assertIsDisplayed();fitsText()
            val title=rect("screen_title")
            for((index,route) in listOf("learn","repeat","conjugate","use").withIndex()) {
                compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex)).performScrollToIndex(index)
                val button=compose.onNodeWithTag("open_$route")
                button.performScrollTo().assertIsDisplayed().performClick()
                assertEquals(route,selected)
                assertTrue(title.bottom<=rect("open_$route").top)
            }
            compose.onNode(hasContentDescription("Home") or hasContentDescription("Accueil")).performClick()
            compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex)).performScrollToIndex(0)
            capture("homepage-${c.w}-${c.h}-${c.font}-${c.lang}")
            records+="$c title=$expected callbacks=PASS"
        }
        assertEquals(cases.size,homes)
        File(out,"homepage-matrix.txt").writeText("PASS ${cases.size} cases\n"+records.joinToString("\n"))
    }
    @Test fun exactConfiguredLinksAndUnavailableBrowserBothLanguages() {
        val language=mutableStateOf(AppLanguage.ENGLISH_UK);val fail=mutableStateOf(false);var launched:Intent?=null
        compose.activityRule.scenario.onActivity{it.setContent{LocalizedContent(language.value){TempleTheme{
            AboutScreen(language.value,{},launch={if(fail.value)throw ActivityNotFoundException() else launched=it})
        }}}}
        for(lang in AppLanguage.entries) {
            compose.runOnIdle{language.value=lang}
            for((tag,url) in listOf("about_guide" to "https://www.nice-education.com/french-verbs/",
                "about_privacy" to "https://www.nice-education.com/french-verbs/privacy/","about_more" to "https://www.nice-education.com")) {
                compose.onNodeWithTag(tag).performScrollTo().performClick()
                assertEquals(Intent.ACTION_VIEW,launched!!.action);assertEquals(url,launched!!.dataString)
                assertTrue(launched!!.hasCategory(Intent.CATEGORY_BROWSABLE))
                compose.onNodeWithTag("about_browser_unavailable").assertDoesNotExist()
            }
            compose.runOnIdle{fail.value=true}
            compose.onNodeWithTag("about_more").performScrollTo().performClick()
            compose.onNodeWithTag("about_browser_unavailable").assertIsDisplayed()
            compose.onNodeWithText(if(lang==AppLanguage.FRENCH)"Fermer" else "Close").performClick()
            compose.runOnIdle{fail.value=false}
        }
    }
    @Test fun externalBrowserRoundTripsBothLanguages() {
        val prefs=com.templesoftware.frenchverbs.data.preferences.DataStoreUserPreferencesRepository(inst.targetContext)
        val original=kotlinx.coroutines.runBlocking { prefs.preferences.first().language }
        try {
            compose.waitUntil(15000){compose.onAllNodesWithTag("open_about").fetchSemanticsNodes().isNotEmpty()}
            for(lang in AppLanguage.entries) {
                compose.onNodeWithTag("language_selector").performClick()
                compose.onNodeWithText(if(lang==AppLanguage.FRENCH)"Français" else "English").performClick()
                compose.onNodeWithTag("open_about").performClick()
                for(tag in listOf("about_guide","about_privacy","about_more")) {
                    val appTaskId=compose.activity.taskId
                    compose.onNodeWithTag(tag).performScrollTo().performClick()
                    // Wait for the external activity to be foreground, not merely
                    // the app's initial pause during Chrome's intent dispatch.
                    fun foreground():String = inst.uiAutomation.executeShellCommand("dumpsys activity activities").use{fd->
                        java.io.FileInputStream(fd.fileDescriptor).bufferedReader().use{it.readText()}
                    }.lineSequence().firstOrNull{it.contains("topResumedActivity=")}.orEmpty()
                    val deadline=android.os.SystemClock.elapsedRealtime()+30000
                    var top=foreground()
                    while(!top.contains("com.android.chrome/org.chromium.chrome.browser.ChromeTabbedActivity") && android.os.SystemClock.elapsedRealtime()<deadline){Thread.sleep(200);top=foreground()}
                    assertTrue("External Chrome foreground $tag: $top",top.contains("com.android.chrome/org.chromium.chrome.browser.ChromeTabbedActivity"))
                    println("BROWSER $lang $tag browser-foreground state=${compose.activityRule.scenario.state} $top")
                    // Return to the existing app task, as selecting it in Recents does.
                    // Browser Back may instead traverse Chrome's own tab/history stack.
                    val manager=inst.targetContext.getSystemService(android.app.ActivityManager::class.java)
                    manager.appTasks.first{it.taskInfo?.taskId==appTaskId}.moveToFront()
                    val backDeadline=android.os.SystemClock.elapsedRealtime()+30000
                    while(compose.activityRule.scenario.state!=androidx.lifecycle.Lifecycle.State.RESUMED && android.os.SystemClock.elapsedRealtime()<backDeadline)Thread.sleep(100)
                    println("BROWSER $lang $tag returned-to-app state=${compose.activityRule.scenario.state} ${foreground()}")
                    assertEquals(androidx.lifecycle.Lifecycle.State.RESUMED,compose.activityRule.scenario.state)
                    compose.onNodeWithTag("about_title").performScrollTo().assertTextEquals(if(lang==AppLanguage.FRENCH)"Verbes français" else "French Verbs")
                    compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
                }
                compose.onNode(hasContentDescription("Back") or hasContentDescription("Retour")).performClick()
            }
        } finally { kotlinx.coroutines.runBlocking { prefs.setLanguage(original) } }
    }
    @Test fun realHomepageHomeRoute() {
        lateinit var nav:androidx.navigation.NavHostController
        compose.activityRule.scenario.onActivity{it.setContent{LocalizedContent(AppLanguage.ENGLISH_UK){TempleTheme{
            nav=rememberNavController();AppNavHost(nav,AppLanguage.ENGLISH_UK,{})
        }}}}
        compose.waitUntil(15000){compose.onAllNodesWithTag("open_about").fetchSemanticsNodes().isNotEmpty()}
        compose.runOnIdle{nav.navigate("activities")}
        compose.onNodeWithTag("screen_title").assertTextEquals("Practice French")
        compose.onNodeWithContentDescription("Home").performClick()
        compose.runOnIdle{assertEquals("tenses",nav.currentDestination!!.route)}
        compose.onNodeWithTag("open_about").performClick()
        compose.onNodeWithTag("about_logo").assertIsDisplayed()
    }
    @Test fun realAboutRecreation() {
        compose.waitUntil(15000){compose.onAllNodesWithTag("open_about").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("open_about").performClick()
        compose.onNodeWithTag("about_logo").assertIsDisplayed()
        // Real production root restores the About route after recreation.
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("about_logo").assertIsDisplayed()
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

}
