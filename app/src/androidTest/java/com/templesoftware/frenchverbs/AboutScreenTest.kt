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
import com.templesoftware.frenchverbs.data.preferences.AppLanguage
import com.templesoftware.frenchverbs.ui.about.*
import com.templesoftware.frenchverbs.config.AppContent
import com.templesoftware.frenchverbs.ui.app.LocalizedContent
import com.templesoftware.frenchverbs.ui.theme.TempleTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.math.roundToInt

class AboutScreenTest {
    val compose = createAndroidComposeRule<MainActivity>()
    @get:Rule val rotationRules = org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    private val inst get() = InstrumentationRegistry.getInstrumentation()
    private fun capturePrefix() = InstrumentationRegistry.getArguments().getString("pass") ?: "pass01"
    private data class Case(val w:Int,val h:Int,val font:Float,val lang:AppLanguage)
    private val tags = listOf("about_logo","about_title","about_intro","about_guide","about_privacy") +
        (0..4).map { "about_badge_$it" } + "about_more"
    private fun ready() {
        compose.waitUntil(15000){compose.onAllNodesWithTag("open_about").fetchSemanticsNodes().isNotEmpty()}
        compose.waitForIdle()
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
                    if (tag.startsWith("about_badge_text_") || text.layoutInput.text.text == "Confidentialité") {
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
    @Test fun responsiveMatrix() {
        ready()
        val cases=listOf(320 to 568,360 to 640,360 to 800,384 to 854,412 to 915,600 to 960,800 to 1280)
            .flatMap { listOf(it,it.second to it.first) }.flatMap { (w,h) ->
                listOf(1f,1.15f,1.3f,1.5f,2f).flatMap { f -> AppLanguage.entries.map { Case(w,h,f,it) } }
            }
        val selected=mutableStateOf(cases.first())
        compose.activityRule.scenario.onActivity { it.setContent {
            val c=selected.value
            BoxWithConstraints(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
                val host=LocalDensity.current
                val d=minOf(with(host){maxWidth.toPx()}/c.w,with(host){maxHeight.toPx()}/c.h)
                val config=Configuration(LocalConfiguration.current).apply {
                    screenWidthDp=c.w;screenHeightDp=c.h;fontScale=c.font;densityDpi=(d*160).roundToInt()
                    orientation=if(c.w>c.h)2 else 1
                }
                CompositionLocalProvider(LocalDensity provides Density(d,c.font),LocalConfiguration provides config) {
                    Box(Modifier.requiredSize(c.w.dp,(c.h-76).dp)) {
                        LocalizedContent(c.lang){TempleTheme{AboutScreen(c.lang,{})}}
                    }
                }
            }
        } }
        val measurements = mutableListOf<String>()
        cases.forEach { c ->
            android.util.Log.i("TemplateAbout", "Testing $c")
            compose.runOnIdle { selected.value=c };compose.waitForIdle()
            compose.onNodeWithTag("about_content").performScrollToIndexIfAvailable()
            fitsText()
            val mustFit=(c.w==360&&c.h==800 || (c.w to c.h) in listOf(800 to 360,854 to 384,915 to 412))&&c.font==1f
            measurements += "$c maxScrollPx=${scrollRange()}"
            if(mustFit) {
                assertEquals("One screen: $c",0f,scrollRange(),1f)
                assertEquals(0f, compose.onNodeWithTag("about_content").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value(), 0f)
                tags.forEach { compose.onNodeWithTag(it).assertIsDisplayed() }
            }
            val viewport=compose.onNodeWithTag("about_viewport").fetchSemanticsNode().boundsInRoot
            tags.forEach { tag ->
                compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
                val r=compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
                assertTrue("Horizontal containment $c $tag",r.left>=viewport.left-1&&r.right<=viewport.right+1)
            }
            val a=compose.onNodeWithTag("about_guide").fetchSemanticsNode().size
            val b=compose.onNodeWithTag("about_privacy").fetchSemanticsNode().size
            assertTrue(kotlin.math.abs(a.width-b.width)<=1);assertEquals(a.height,b.height)
            println("ABOUT PASS $c scroll=${scrollRange()}")
        }
        File(inst.targetContext.getExternalFilesDir(null), "${capturePrefix()}-about-matrix.txt")
            .writeText(measurements.joinToString("\n"))
    }
    @Test fun exactExternalLinksAndBrowserUnavailableBothLanguages() {
        ready()
        val language=mutableStateOf(AppLanguage.ENGLISH_UK)
        val fail=mutableStateOf(false)
        var launched:Intent?=null
        compose.activityRule.scenario.onActivity { it.setContent {
            LocalizedContent(language.value){TempleTheme{AboutScreen(language.value,{},content=AppContent.Default,launch={
                if(fail.value)throw ActivityNotFoundException() else launched=it
            })}}
        } }
        for(lang in AppLanguage.entries) {
            compose.runOnIdle{language.value=lang}
            for((tag,url) in listOf("about_guide" to "https://www.nice-education.com/french-verbs/","about_privacy" to "https://www.nice-education.com/french-verbs/privacy/","about_more" to AppContent.MORE_APPS_URL)) {
                compose.onNodeWithTag(tag).performScrollTo().performClick()
                assertEquals(Intent.ACTION_VIEW,launched!!.action);assertEquals(url,launched!!.dataString)
                assertTrue(launched!!.hasCategory(Intent.CATEGORY_BROWSABLE))
            }
            compose.runOnIdle{fail.value=true}
            compose.onNodeWithTag("about_more").performScrollTo().performClick()
            compose.onNodeWithTag("about_browser_unavailable").assertIsDisplayed()
            compose.onNodeWithText(if(lang==AppLanguage.FRENCH)"Fermer" else "Close").performClick()
            compose.runOnIdle{fail.value=false}
        }
    }
    @Test fun explicitlyMissingLinksNeverLaunchAnUnrelatedPage() {
        ready()
        var launches=0
        compose.activityRule.scenario.onActivity { it.setContent {
            LocalizedContent(AppLanguage.FRENCH) { TempleTheme {
                AboutScreen(AppLanguage.FRENCH, {}, launch={ launches++ },
                    content=AppContent(R.string.app_name, R.string.about_intro, null, null))
            } }
        } }
        for(tag in listOf("about_guide", "about_privacy")) {
            compose.onNodeWithTag(tag).performScrollTo().performClick()
            compose.onNodeWithText("Cette page sera disponible lorsque l’application sera prête.").assertIsDisplayed()
            compose.onNodeWithText("Fermer").performClick()
        }
        assertEquals(0, launches)
    }
    @Test fun actualNavigationAndBack() {
        compose.waitUntil(15000){compose.onAllNodesWithTag("open_about").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("privacy_policy").assertDoesNotExist()
        compose.onNodeWithTag("open_about").assertIsDisplayed().performClick()
        compose.onNodeWithTag("about_logo").assertIsDisplayed()
        compose.onNodeWithTag("language_selector").assertDoesNotExist()
        inst.runOnMainSync{compose.activity.onBackPressedDispatcher.onBackPressed()}
        compose.onNodeWithTag("open_about").assertIsDisplayed().performClick()
        compose.onNode(hasContentDescription("Back") or hasContentDescription("Retour")).performClick()
        compose.onNodeWithTag("open_about").assertIsDisplayed()
    }
    @Test fun externalBrowserRoundTrips() {
        ready()
        compose.onNodeWithTag("open_about").performClick()
        for(tag in listOf("about_more")) {
            compose.onNodeWithTag(tag).performScrollTo().performClick()
            val deadline=android.os.SystemClock.elapsedRealtime()+15000
            while(compose.activityRule.scenario.state==androidx.lifecycle.Lifecycle.State.RESUMED &&
                android.os.SystemClock.elapsedRealtime()<deadline) Thread.sleep(100)
            assertNotEquals("External browser opened: $tag",androidx.lifecycle.Lifecycle.State.RESUMED,compose.activityRule.scenario.state)
            Thread.sleep(1200)
            // Send one Back only: repeated events can also pop About while resume is pending.
            inst.uiAutomation.executeShellCommand("input keyevent KEYCODE_BACK").use { descriptor ->
                java.io.FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
            }
            val returnDeadline=android.os.SystemClock.elapsedRealtime()+15000
            while(compose.activityRule.scenario.state!=androidx.lifecycle.Lifecycle.State.RESUMED &&
                android.os.SystemClock.elapsedRealtime()<returnDeadline) Thread.sleep(100)
            assertEquals(androidx.lifecycle.Lifecycle.State.RESUMED,compose.activityRule.scenario.state)
            compose.waitForIdle()
            compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
        }
    }
    @Test fun deviceCaptureBothLanguages() {
        ready()
        val expected = InstrumentationRegistry.getArguments().getString("orientation")
        if (expected != null) {
            compose.activityRule.scenario.onActivity {
                it.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_USER
            }
            assertTrue(inst.uiAutomation.setRotation(expected.toInt()))
            compose.waitUntil(10000) { compose.activity.resources.configuration.orientation ==
                if(expected.toInt() % 2 == 1) Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT }
            compose.waitForIdle();Thread.sleep(400)
        }
        for(language in AppLanguage.entries) {
            compose.onNodeWithTag("language_selector").performClick()
            compose.onNodeWithText(if(language==AppLanguage.FRENCH) "Français" else "English").performClick()
            val root = compose.activity.window.decorView
            val safe = androidx.core.view.ViewCompat.getRootWindowInsets(root)!!.getInsets(
                androidx.core.view.WindowInsetsCompat.Type.systemBars() or androidx.core.view.WindowInsetsCompat.Type.displayCutout())
            val pixels = compose.onNodeWithTag("open_about").fetchSemanticsNode().boundsInRoot
            assertTrue("Info inside left/right safe area", pixels.left >= safe.left && pixels.right <= root.width-safe.right)
            assertTrue("Info above bottom system UI", pixels.bottom <= root.height-safe.bottom)
            val info=compose.onNodeWithTag("open_about").getUnclippedBoundsInRoot()
            assertTrue(info.width>=48.dp && info.height>=48.dp)
            val button=compose.onNodeWithTag("next").getUnclippedBoundsInRoot()
            assertTrue("Info does not overlap Next",info.top>=button.bottom || info.left>=button.right || info.right<=button.left)
            val home=inst.uiAutomation.takeScreenshot()
            File(inst.targetContext.getExternalFilesDir(null),"${capturePrefix()}-${InstrumentationRegistry.getArguments().getString("profile")}-${language.languageTag}-home.png")
                .outputStream().use{home.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
            home.recycle()
            compose.onNodeWithTag("open_about").performClick()
            compose.onNodeWithTag("about_logo").assertIsDisplayed()
            compose.waitForIdle();Thread.sleep(600)
            fitsText()
            val args=InstrumentationRegistry.getArguments()
            if(args.getString("mustFit")=="true") {
                assertEquals(0f,scrollRange(),1f);tags.forEach{compose.onNodeWithTag(it).assertIsDisplayed()}
            }
            val image=inst.uiAutomation.takeScreenshot()
            File(inst.targetContext.getExternalFilesDir(null),"${capturePrefix()}-${args.getString("profile")}-${language.languageTag}.png")
                .outputStream().use{image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
            image.recycle()
            val config=compose.activity.resources.configuration
            val wide=config.orientation==Configuration.ORIENTATION_LANDSCAPE
            if(wide) {
                val left=compose.onNodeWithTag("about_left_column").getUnclippedBoundsInRoot()
                val right=compose.onNodeWithTag("about_right_column").getUnclippedBoundsInRoot()
                assertTrue(left.right<=right.left)
            } else compose.onNodeWithTag("about_left_column").assertDoesNotExist()
            val metrics="${config.screenWidthDp}x${config.screenHeightDp}dp font=${config.fontScale} scroll=${scrollRange()} offset=${compose.onNodeWithTag("about_content").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()}\n"+
                tags.joinToString("\n"){ "$it: ${compose.onNodeWithTag(it).getUnclippedBoundsInRoot()}" }
            File(inst.targetContext.getExternalFilesDir(null),"${capturePrefix()}-${args.getString("profile")}-${language.languageTag}-metrics.txt").writeText(metrics)
            if(scrollRange()>0f) {
                compose.onNodeWithTag("about_more").performScrollTo().assertIsDisplayed()
                compose.waitForIdle();Thread.sleep(200)
                val bottom=inst.uiAutomation.takeScreenshot()
                File(inst.targetContext.getExternalFilesDir(null),"${capturePrefix()}-${args.getString("profile")}-${language.languageTag}-bottom.png")
                    .outputStream().use{bottom.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
                bottom.recycle()
            }
            println("DEVICE ${language.languageTag} scroll=${scrollRange()}")
            compose.onNode(hasContentDescription("Back") or hasContentDescription("Retour")).performClick()
        }
    }
}

private fun SemanticsNodeInteraction.performScrollToIndexIfAvailable() {
    performSemanticsAction(SemanticsActions.ScrollBy){it(0f,-100000f)}
}
