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
import kotlin.math.roundToInt

class SelectionPass05Test {
    val compose = createAndroidComposeRule<MainActivity>()
    @get:Rule val rotationRules = org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private val selection=mutableStateOf(Selection())
    private val language=mutableStateOf(AppLanguage.ENGLISH_UK)
    private val route=mutableStateOf("types")
    private var nextClicks=0
    private lateinit var measurer:TextMeasurer
    private val source=mutableMapOf<String,String>()
    private val descriptions=mutableMapOf<String,String>()
    private val out get()=File(inst.targetContext.getExternalFilesDir(null),"pass05").apply{mkdirs()}
    @Composable private fun Screen() {
        LocalizedContent(language.value) { TempleTheme {
            measurer=rememberTextMeasurer()
            Choices.families.forEach { source[it]=androidx.compose.ui.res.stringResource(ChoiceResources.members.getValue(it)) }
            Choices.er.forEach { source[it]=androidx.compose.ui.res.stringResource(ChoiceResources.example.getValue(it));descriptions[it]=androidx.compose.ui.res.stringResource(ChoiceResources.description.getValue(it)) }
            SelectionScreen(route.value,VerbState(loaded=true,selection=selection.value),language.value,{},
                {id->selection.value=if(id==null)selection.value.toggleAll(route.value)else selection.value.toggle(route.value,id)},
                {nextClicks++},{},{})
        } }
    }
    private fun mount()=compose.activityRule.scenario.onActivity { it.setContent { Screen() } }
    private fun node(tag:String)=compose.onNodeWithTag(tag,useUnmergedTree=true).fetchSemanticsNode()
    private fun fullRect(tag:String):androidx.compose.ui.geometry.Rect {
        val n=node(tag);return androidx.compose.ui.geometry.Rect(n.positionInRoot,androidx.compose.ui.geometry.Size(n.size.width.toFloat(),n.size.height.toFloat()))
    }
    private fun textLayout(tag:String):TextLayoutResult {
        val results=mutableListOf<TextLayoutResult>()
        compose.onNodeWithTag(tag,useUnmergedTree=true).performSemanticsAction(SemanticsActions.GetTextLayoutResult){it(results)}
        return results.single()
    }
    private fun reveal(id:String):SemanticsNodeInteraction {
        val card=fullRect("choice_$id");val viewport=node("choices").boundsInRoot
        compose.onNodeWithTag("choices").performSemanticsAction(SemanticsActions.ScrollBy){it(0f,card.center.y-viewport.center.y)}
        return compose.onNodeWithTag("choice_$id").performScrollTo()
    }
    private fun click(id:String)=reveal(id).performClick()
    private fun screenshot(name:String) {
        val b=inst.uiAutomation.takeScreenshot();File(out,"$name.png").outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)};b.recycle()
    }
    private fun verify(section:String,landscape:Boolean,font:Float,label:String):String {
        compose.onNodeWithTag("next").assertIsDisplayed()
        if(section!="tenses") {compose.onNodeWithTag("language_selector").assertDoesNotExist();compose.onNodeWithTag("open_about").assertDoesNotExist()}
        else {compose.onNodeWithTag("open_about").assertIsDisplayed()}
        if(section in listOf("tenses","er")) {
            compose.onNodeWithText("Choose what you want to learn").assertDoesNotExist()
            compose.onNodeWithText("Choisis ce que tu veux apprendre").assertDoesNotExist()
        }
        if(section in listOf("types","families"))compose.onNodeWithTag("selection_intro").assertDoesNotExist()
        if(section in listOf("important","families"))compose.onNodeWithTag("choice_all").assertDoesNotExist()
        if(section=="families") {
            compose.onNodeWithTag("family_divider_1").assertExists();compose.onNodeWithTag("family_divider_2").assertExists()
            listOf("Priority","Important","Extra","Priorité","À connaître","Pour aller plus loin").forEach { compose.onNodeWithText(it).assertDoesNotExist() }
        }
        val viewport=node(if(section=="tenses")"tenses_safe_area" else "selection_viewport").boundsInRoot;val footer=fullRect("next")
        assertTrue("Safe Next $label",footer.bottom<=viewport.bottom+1 && footer.top>=viewport.top)
        val counts=mutableListOf<String>()
        selection.value.visible(section).forEach { id ->
            reveal(id).assertIsDisplayed()
            val card=fullRect("choice_$id");val tick=fullRect(if(section=="tenses")"tense_tick_$id" else "selection_tick_$id")
            if(!(section=="important" && landscape))assertEquals("Centred tick $label $id",card.center.y,tick.center.y,1.5f)
            if(section=="important" && landscape) {
                val title=textLayout("important_label_$id")
                assertEquals(1,title.lineCount);assertFalse(title.didOverflowWidth)
                assertEquals(card.center.x,fullRect("important_label_$id").center.x,1.5f)
            }
            if(section=="tenses")assertEquals(1,textLayout("tense_example_$id").lineCount)
            if(section=="families" || section=="er") {
                val text=textLayout("secondary_$id");val actual=text.layoutInput.text.text
                assertFalse("Height $label $id",text.didOverflowHeight)
                assertFalse("Ellipsis $label $id",text.isLineEllipsized(0))
                val width=node("secondary_slot_$id").size.width
                var expected=""
                compose.runOnIdle {
                    if(section=="families") expected=source.getValue(id).split(", ").let { parts->(parts.size downTo 1).map{parts.take(it).joinToString(", ")}.first { measurer.measure(it,text.layoutInput.style,softWrap=false,maxLines=1).size.width<=width } }
                    else {
                        val full=source.getValue(id)+" · "+descriptions.getValue(id)
                        expected=if(measurer.measure(full,text.layoutInput.style,softWrap=false,maxLines=1).size.width<=width)full else source.getValue(id).substringBefore(" → ")+" · "+descriptions.getValue(id)
                    }
                }
                assertEquals("Measured fitting $label $id",expected,actual)
                if(section=="families" || font==1f)assertEquals("Single line $label $id",1,text.lineCount)
                counts+="$id:$actual"
            }
        }
        if(landscape && section in listOf("types","important","families")) {
            compose.onNodeWithTag("choices").performSemanticsAction(SemanticsActions.ScrollBy){it(0f,-100000f)}
            val ids=selection.value.visible(section);val perRow=when(section){"types"->2;"important"->5;else->3}
            val rects=ids.map { fullRect("choice_$it") }
            rects.chunked(perRow).forEach { row->row.forEach { assertEquals("Aligned row $label",row.first().top,it.top,1.5f);assertEquals("Equal width $label",row.first().width,it.width,1.5f) } }
            assertEquals(if(section=="families")3 else 2,rects.map{it.top.roundToInt()}.distinct().size)
            if(section=="types") assertEquals(rects[0].height,rects[2].height,1.5f)
        }
        val scroll=node("choices").config[SemanticsProperties.VerticalScrollAxisRange].maxValue()
        if(label.startsWith("real-0-") && section in listOf("types","important","er","families"))assertEquals("Fits actual landscape $label",0f,scroll,0f)
        if(section=="families") {
            assertTrue(fullRect("choice_connaitre").bottom<=fullRect("family_divider_1").top)
            assertTrue(fullRect("family_divider_1").bottom<=fullRect("choice_ecrire").top)
            assertTrue(fullRect(if(landscape)"choice_ouvrir" else "choice_recevoir").bottom<=fullRect("family_divider_2").top)
            assertTrue(fullRect("family_divider_2").bottom<=fullRect(if(landscape)"choice_recevoir" else "choice_conduire").top)
        }
        return "$label scroll=$scroll "+counts.joinToString(" | ")
    }
    @Test fun focusedDimensionsFontsAndLanguages() {
        data class Case(val w:Int,val h:Int,val font:Float,val lang:AppLanguage,val section:String)
        val cases=listOf(360 to 800,800 to 360,915 to 412,800 to 1280,1280 to 800)
            .flatMap{(w,h)->listOf(1f,2f).flatMap{f->AppLanguage.entries.flatMap{l->listOf("tenses","types","important","er","families").map{Case(w,h,f,l,it)}}}}
        val current=mutableStateOf(cases.first())
        compose.activityRule.scenario.onActivity{it.setContent{
            val c=current.value
            BoxWithConstraints(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
                val host=LocalDensity.current;val d=minOf(with(host){maxWidth.toPx()}/c.w,with(host){maxHeight.toPx()}/c.h)
                val cfg=Configuration(LocalConfiguration.current).apply{screenWidthDp=c.w;screenHeightDp=c.h;fontScale=c.font;densityDpi=(d*160).roundToInt();orientation=if(c.w>c.h)2 else 1}
                CompositionLocalProvider(LocalDensity provides Density(d,c.font),LocalConfiguration provides cfg){Box(Modifier.requiredSize(c.w.dp,c.h.dp)){Screen()}}
            }
        }}
        val records=mutableListOf<String>()
        cases.forEach{c->
            compose.runOnIdle{current.value=c;route.value=c.section;language.value=c.lang;selection.value=Selection(tenses=setOf("present","future"))};compose.waitForIdle()
            records+=verify(c.section,c.w>c.h,c.font,c.toString())
            if(c.w in listOf(915,800) && c.h in listOf(412,1280)) {
                compose.onNodeWithTag("choices").performSemanticsAction(SemanticsActions.ScrollBy){it(0f,-100000f)}
                compose.waitForIdle();screenshot("matrix-${c.w}-${c.h}-${c.font}-${c.lang}-${c.section}")
            }
        }
        File(out,"matrix.txt").writeText("PASS ${cases.size} cases, five modified screens only\n"+records.joinToString("\n"))
    }
    @Test fun individualSelectionsAllAndFutureCondition() {
        mount()
        compose.onNodeWithTag("choice_important").assertIsOn();click("important");compose.onNodeWithTag("next").assertIsNotEnabled()
        click("ir");compose.onNodeWithTag("next").assertIsEnabled()
        compose.runOnIdle{route.value="important";selection.value=Selection()}
        Choices.important.forEachIndexed{i,id->val n=reveal(id);if(i<4)n.assertIsOn()else n.assertIsOff()}
        Choices.important.take(4).forEach(::click);compose.onNodeWithTag("next").assertIsNotEnabled();compose.onNodeWithTag("choice_all").assertDoesNotExist()
        Choices.important.forEach{click(it);compose.onNodeWithTag("choice_$it").assertIsOn()}
        compose.runOnIdle{route.value="er";selection.value=Selection()}
        compose.onNodeWithTag("choice_special").assertDoesNotExist();click("all");compose.onNodeWithTag("choice_all").assertDoesNotExist()
        click("cer");compose.onNodeWithTag("choice_all").assertIsOff();click("cer");compose.onNodeWithTag("choice_all").assertDoesNotExist()
        compose.runOnIdle{selection.value=selection.value.toggle("tenses","future")}
        compose.onNodeWithTag("choice_all").assertIsOff();click("special");compose.onNodeWithTag("choice_all").assertDoesNotExist()
        compose.runOnIdle{selection.value=selection.value.toggle("tenses","future")};compose.onNodeWithTag("choice_special").assertDoesNotExist();compose.onNodeWithTag("choice_all").assertDoesNotExist()
        compose.runOnIdle{route.value="families";selection.value=Selection()}
        Choices.families.forEachIndexed{i,id->val n=reveal(id);if(i<3)n.assertIsOn()else n.assertIsOff()}
        Choices.families.take(3).forEach(::click);compose.onNodeWithTag("next").assertIsNotEnabled()
        Choices.families.forEach{click(it);compose.onNodeWithTag("choice_$it").assertIsOn()}
    }
    @Test fun tensesControls() {
        route.value="tenses";selection.value=Selection(tenses=emptySet());mount()
        compose.onNodeWithTag("next").assertIsNotEnabled()
        compose.onNodeWithTag("language_selector").assertIsDisplayed()
        compose.onNodeWithTag("open_about").assertIsDisplayed()
        Choices.tenses.forEach { reveal(it).assertIsOff() }
        val safe=node("tenses_safe_area").boundsInRoot;val info=fullRect("open_about")
        assertTrue(info.center.x>safe.center.x && info.center.y>safe.center.y)
        assertTrue(info.right<=safe.right+1 && info.bottom<=safe.bottom+1)
        click("present");compose.onNodeWithTag("next").assertIsEnabled()
        click("present");compose.onNodeWithTag("next").assertIsNotEnabled()
        screenshot("tenses-disabled-next")
    }
    @Test fun actualPhonePortraitLandscapeCaptures() {
        val measurements=mutableListOf<String>()
        for(o in listOf(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)){
            compose.activityRule.scenario.onActivity{it.requestedOrientation=o};compose.waitUntil(10000){compose.activity.resources.configuration.orientation==if(o==1)1 else 2};mount()
            for(l in AppLanguage.entries)for(section in listOf("tenses","types","important","er","families")){
                compose.runOnIdle{language.value=l;route.value=section;selection.value=Selection(tenses=setOf("present","future"))};compose.waitForIdle()
                measurements+=verify(section,o==0,1f,"real-$o-$l-$section")
                if(section=="er") {
                    compose.runOnIdle { selection.value=Selection(tenses=setOf("present")) }
                    measurements+=verify(section,o==0,1f,"real-$o-$l-er-present")
                    compose.runOnIdle { selection.value=Selection(tenses=setOf("present","future")) }
                }
                compose.onNodeWithTag("choices").performSemanticsAction(SemanticsActions.ScrollBy){it(0f,-100000f)}
                compose.waitForIdle();screenshot("real-$o-$l-$section")
            }
        }
        File(out,"real-layout.txt").writeText(measurements.joinToString("\n"))
    }
}
