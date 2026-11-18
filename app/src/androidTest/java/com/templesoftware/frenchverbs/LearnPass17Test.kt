package com.templesoftware.frenchverbs

import android.content.res.Configuration
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.*
import androidx.lifecycle.SavedStateHandle
import androidx.test.platform.app.InstrumentationRegistry
import com.templesoftware.frenchverbs.data.preferences.*
import com.templesoftware.frenchverbs.domain.verbs.*
import com.templesoftware.frenchverbs.ui.app.*
import com.templesoftware.frenchverbs.ui.theme.TempleTheme
import com.templesoftware.frenchverbs.ui.verbs.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.math.roundToInt

class LearnPass17Test {
    val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val rotationRules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private val out get()=File(inst.targetContext.getExternalFilesDir(null),"pass17").apply{mkdirs()}
    private data class Case(val w:Int,val h:Int,val font:Float,val lang:AppLanguage,val verb:String,val tense:Int,val dark:Boolean=false)
    private fun state(c:Case)=VerbState(loaded=true,session=(0..5).map{LearningItem(c.verb,it,1)},index=c.tense)
    private fun rect(tag:String)=compose.onNodeWithTag(tag,useUnmergedTree=true).fetchSemanticsNode().let{
        androidx.compose.ui.geometry.Rect(it.positionInRoot,androidx.compose.ui.geometry.Size(it.size.width.toFloat(),it.size.height.toFloat()))}
    private fun layouts(tag:String):List<TextLayoutResult> {
        val node=if(tag=="learn_next") compose.onNode(hasAnyAncestor(hasTestTag(tag)) and SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),useUnmergedTree=true).fetchSemanticsNode() else compose.onNodeWithTag(tag,useUnmergedTree=true).fetchSemanticsNode()
        val values=mutableListOf<TextLayoutResult>();node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(values);return values
    }
    @Composable private fun Frame(c:Case) {
        BoxWithConstraints(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            val host=LocalDensity.current
            val d=minOf(with(host){maxWidth.toPx()}/c.w,with(host){maxHeight.toPx()}/c.h)
            val config=Configuration(LocalConfiguration.current).apply{screenWidthDp=c.w;screenHeightDp=c.h;fontScale=c.font;densityDpi=(d*160).roundToInt();orientation=if(c.w>c.h)2 else 1}
            CompositionLocalProvider(LocalDensity provides Density(d,c.font),LocalConfiguration provides config) {
                Box(Modifier.requiredSize(c.w.dp,(c.h-76).dp)){LocalizedContent(c.lang){TempleTheme(darkTheme=c.dark){LearnScreen(state(c),c.lang,{},{})}}}
            }
        }
    }
    private fun capture(name:String) {
        val bitmap=inst.uiAutomation.takeScreenshot()
        File(out,"$name.png").outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
    }
    private val records=mutableListOf<String>()
    private fun verify(c:Case,fit:Boolean) {
        compose.onNodeWithTag("learn_panels").performSemanticsAction(SemanticsActions.ScrollBy){it(0f,-100000f)}
        compose.onNodeWithTag("learn_heading_current").assertExists()
        compose.onNodeWithTag("learn_heading_comparison").assertDoesNotExist()
        compose.onNodeWithTag("learn_example_current_0").assertDoesNotExist()
        val verb=OfflineDictionary.verbs.first{it.id==c.verb}
        val density=layouts("learn_form_current_0").single().layoutInput.density.density
        val deltas=mutableListOf<Float>()
        for(i in 0..5) {
            val keys=listOf("learn_form_current_$i","learn_form_comparison_$i")
            val baselines=keys.map { key -> rect(key).top+layouts(key).single().firstBaseline }
            val delta=kotlin.math.abs(baselines[0]-baselines[1]);deltas+=delta
            assertEquals("Row $i baseline $c",baselines[0],baselines[1],1f)
            assertEquals("Row $i top $c",rect(keys[0]).top,rect(keys[1]).top,1f)
            assertEquals("Row $i height $c",rect(keys[0]).height,rect(keys[1]).height,1f)
            keys.forEach { key ->
                compose.onNodeWithTag(key).assertTextEquals(verb.tenses[c.tense].forms[i].text)
                val result=layouts(key).single()
                assertFalse("Clipped $key $c",result.hasVisualOverflow)
                assertEquals(if(c.h<=420)17f else 18f,result.layoutInput.style.fontSize.value,0f)
                if(fit)compose.onNodeWithTag(key).assertIsDisplayed()
            }
            val form=verb.tenses[c.tense].forms[i]
            val spans=layouts(keys[1]).single().layoutInput.text.spanStyles
            assertTrue(spans.any{it.start==form.highlightStart&&it.end==form.highlightEnd&&it.item.fontWeight==androidx.compose.ui.text.font.FontWeight.Bold})
            if(i>0) {
                val gap=rect(keys[1]).top-rect("learn_form_comparison_${i-1}").bottom
                assertTrue("Compact gap $gap $c",gap>=-1 && gap<=4*density+1)
            }
        }
        LearnExamples.forPanel(verb,c.tense,false).forEachIndexed { i,e ->
            val key="learn_example_comparison_$i"
            compose.onNodeWithTag(key).assertTextEquals(e.text)
            if(fit)compose.onNodeWithTag(key).assertIsDisplayed()
            val result=layouts(key).single()
            assertFalse(result.hasVisualOverflow)
            assertEquals(if(c.h<=420)13f else 15f,result.layoutInput.style.fontSize.value,0f)
            assertTrue(result.layoutInput.text.spanStyles.any{it.start==e.highlightStart&&it.end==e.highlightEnd})
        }
        assertTrue(rect("learn_example_comparison_1").top-rect("learn_example_comparison_0").bottom<=4*density+1)
        assertTrue(rect("learn_form_comparison_0").top-rect("learn_divider_comparison").bottom<=6*density+1)
        assertEquals(rect("learn_panel_current").top,rect("learn_panel_comparison").top,0f)
        assertEquals(rect("learn_panel_current").bottom,rect("learn_panel_comparison").bottom,0f)
        assertTrue(rect("learn_panel_current").height>=rect("learn_viewport").height*.88f)
        compose.onNodeWithTag("learn_next").assertIsDisplayed()
        compose.onNodeWithTag("learn_previous").assertIsDisplayed()
        val range=compose.onNodeWithTag("learn_panels").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].maxValue()
        if(fit) {
            assertEquals("No scroll $c",0f,range,1f)
            assertTrue(rect("learn_next").top-rect("learn_panel_current").bottom>=10*density-1)
        } else {
            compose.onNodeWithTag("learn_form_comparison_5").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("learn_example_comparison_0").performScrollTo().assertIsDisplayed()
        }
        records+="$c baseline deltas (physical px)=$deltas scroll=$range PASS"
    }
    @Test fun sameTenseLandscapeMatrix() {
        val current=mutableStateOf(Case(915,412,1f,AppLanguage.ENGLISH_UK,"regarder",0))
        compose.activityRule.scenario.onActivity{it.setContent{Frame(current.value)}}
        val errors=mutableListOf<String>()
        for((w,h,f) in listOf(Triple(915,412,1f),Triple(800,360,1f),Triple(1280,800,1f),Triple(915,412,2f)))
            for(l in AppLanguage.entries)for((v,t) in listOf("regarder" to 0,"être" to 2,"aller" to 3)) {
                val c=Case(w,h,f,l,v,t)
                compose.runOnIdle{current.value=c};compose.waitForIdle()
                try{verify(c,f==1f)}catch(e:AssertionError){errors+="$c ${e.message}"}
                capture("matrix-$w-$f-$l-$t")
            }
        File(out,"alignment.txt").writeText(records.joinToString("\n")+"\nERRORS\n"+errors.joinToString("\n"))
        assertTrue(errors.joinToString("\n"),errors.isEmpty())
    }
    @Test fun actualEmulatorSameTense() {
        val current=mutableStateOf(Case(915,412,1f,AppLanguage.ENGLISH_UK,"regarder",0))
        compose.activityRule.scenario.onActivity{it.requestedOrientation=0}
        compose.waitUntil(10000){compose.activity.resources.configuration.orientation==2}
        compose.activityRule.scenario.onActivity{it.setContent{LocalizedContent(current.value.lang){TempleTheme{LearnScreen(state(current.value),current.value.lang,{},{})}}}}
        for(l in AppLanguage.entries)for((v,t) in listOf("regarder" to 0,"être" to 2,"aller" to 3)) {
            val c=Case(915,412,1f,l,v,t)
            compose.runOnIdle{current.value=c};compose.waitForIdle();verify(c,true)
            capture("actual-$l-$t")
        }
        File(out,"actual.txt").writeText(records.joinToString("\n"))
    }
    @Test fun unchangedLayoutsSmoke() {
        val current=mutableStateOf(Case(412,915,1f,AppLanguage.ENGLISH_UK,"regarder",0))
        compose.activityRule.scenario.onActivity{it.setContent{Frame(current.value)}}
        for(l in AppLanguage.entries)for((w,h,t) in listOf(Triple(412,915,0),Triple(915,412,1))) {
            val c=Case(w,h,1f,l,"regarder",t)
            compose.runOnIdle{current.value=c};compose.waitForIdle()
            compose.onNodeWithTag("learn_next").assertIsDisplayed()
            if(t==0) {
                compose.onNodeWithTag("learn_heading_comparison").assertDoesNotExist()
                assertEquals(20f,layouts("learn_forms_current").single().layoutInput.style.fontSize.value,0f)
                assertEquals(17f,layouts("learn_example_comparison_0").single().layoutInput.style.fontSize.value,0f)
                assertTrue(rect("learn_example_comparison_1").bottom<rect("learn_forms_comparison").top)
            } else {
                compose.onNodeWithTag("learn_heading_current").assertExists()
                compose.onNodeWithTag("learn_heading_comparison").assertExists()
                for(tag in listOf("current","comparison")) {
                    assertTrue(rect("learn_form_${tag}_5").bottom<=rect("learn_divider_$tag").top)
                    for(i in 0..1)compose.onNodeWithTag("learn_example_${tag}_$i").assertIsDisplayed()
                }
                for(i in 0..1)assertEquals(rect("learn_example_current_$i").top,rect("learn_example_comparison_$i").top,1f)
            }
            capture("smoke-$w-$l")
        }
        File(out,"smoke.txt").writeText("PASS four limited smoke cases: same-tense portrait and different-tense landscape in both languages.")
    }
}
