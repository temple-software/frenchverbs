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

class LearnPass24TypographyTest {
    val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val rotationRules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private val out get()=File(inst.targetContext.getExternalFilesDir(null),"pass24-typography").apply{mkdirs()}
    private data class Case(val w:Int,val h:Int,val font:Float,val lang:AppLanguage,val verb:String,val tense:Int,val actual:Boolean=false)
    private fun state(c:Case)=VerbState(loaded=true,session=(0..5).map{LearningItem(c.verb,it,1)},index=c.tense)
    private fun rect(tag:String)=compose.onNodeWithTag(tag,useUnmergedTree=true).fetchSemanticsNode().let{
        androidx.compose.ui.geometry.Rect(it.positionInRoot,androidx.compose.ui.geometry.Size(it.size.width.toFloat(),it.size.height.toFloat()))}
    private fun layouts(tag:String):List<TextLayoutResult> {
        val node=if(tag=="learn_next") compose.onNode(hasAnyAncestor(hasTestTag(tag)) and SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),useUnmergedTree=true).fetchSemanticsNode() else compose.onNodeWithTag(tag,useUnmergedTree=true).fetchSemanticsNode()
        val values=mutableListOf<TextLayoutResult>();node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(values);return values
    }
    private var testDensity=1f
    @Composable private fun Frame(c:Case) {
        BoxWithConstraints(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            val host=LocalDensity.current
            val d=minOf(with(host){maxWidth.toPx()}/c.w,with(host){maxHeight.toPx()}/c.h)
            testDensity=d
            val config=Configuration(LocalConfiguration.current).apply{screenWidthDp=c.w;screenHeightDp=c.h;fontScale=c.font;densityDpi=(d*160).roundToInt();orientation=if(c.w>c.h)2 else 1}
            CompositionLocalProvider(LocalDensity provides Density(d,c.font),LocalConfiguration provides config) {
                Box(Modifier.requiredSize(c.w.dp,(c.h-76).dp)){LocalizedContent(c.lang){TempleTheme {
                    val verb=OfflineDictionary.verbs.first{it.id==c.verb}
                    val comparison=if(c.actual)when(c.tense){1->0;4->1;5->3;else->c.tense} else c.tense
                    LearnLandscapePanels(verb,c.tense,comparison,c.lang==AppLanguage.ENGLISH_UK,c.h<=420,false,
                        ((c.w-10)/2f).dp,20.dp,Modifier.fillMaxWidth().heightIn(min=(c.h-76).dp),false,emptyList(),{})
                }}}
            }
        }
    }
    @Test fun sharedRenderedTypographyAndCentring() {
        val current=mutableStateOf(Case(915,412,1f,AppLanguage.ENGLISH_UK,"être",0))
        compose.activityRule.scenario.onActivity{it.setContent{Frame(current.value)}}
        val cases=mutableListOf<Case>()
        for((w,h)in listOf(800 to 360,1024 to 600))for(lang in listOf(AppLanguage.ENGLISH_UK,AppLanguage.FRENCH))
            for(v in listOf("être","connaître","commencer"))for(t in 0..5)cases+=Case(w,h,1f,lang,v,t)
        for(lang in listOf(AppLanguage.ENGLISH_UK,AppLanguage.FRENCH))for(t in 0..5) {
            cases+=Case(800,360,1.3f,lang,"être",t)
            cases+=Case(800,360,1f,lang,"être",t,actual=true)
        }
        val log=File(out,"rendered.txt");log.writeText("")
        for(c in cases) {
            compose.runOnIdle{current.value=c};compose.waitForIdle()
            val paired=c.actual && c.tense in listOf(1,4,5)
            val expected=if(paired)14f else if(c.h<=420)16f else 18f
            for(tag in if(paired)listOf("current","comparison") else listOf("comparison")) {
                val panel=rect("learn_panel_$tag");val divider=rect("learn_divider_$tag")
                val first=rect("learn_example_${tag}_0");val last=rect("learn_example_${tag}_1")
                val padding=(if(c.h<=420)6 else 10)*testDensity
                val centre=(first.top+last.bottom)/2
                val target=if(paired)(divider.bottom+panel.bottom-padding)/2 else (panel.top+padding+divider.top)/2
                assertEquals("$c centred",target,centre,1.5f)
                assertTrue(first.left>=panel.left);assertTrue(last.right<=panel.right)
                assertTrue(first.top>=panel.top+padding-1);assertTrue(last.bottom<=panel.bottom-padding+1)
                if(paired)assertTrue(first.top>=divider.bottom) else assertTrue(last.bottom<=divider.top)
                for(i in 0..1) {
                    val result=layouts("learn_example_${tag}_$i").single()
                    assertEquals("$c rendered font",expected,result.layoutInput.style.fontSize.value,0f)
                    assertFalse("$c overflow",result.hasVisualOverflow)
                    val t=if(tag=="current" || !paired)c.tense else when(c.tense){1->0;4->1;else->3}
                    val example=LearnExamples.forPanel(OfflineDictionary.verbs.first{it.id==c.verb},t,paired,c.lang==AppLanguage.ENGLISH_UK)[i]
                    assertEquals(example.text,result.layoutInput.text.text)
                    val spans=result.layoutInput.text.spanStyles
                    if(example.translation)assertTrue(spans.isEmpty()) else {
                        assertTrue(spans.any{it.start==example.highlightStart && it.end==example.highlightEnd && it.item.fontWeight==androidx.compose.ui.text.font.FontWeight.Bold && it.item.color!=androidx.compose.ui.graphics.Color.Unspecified})
                    }
                    log.appendText("PASS $c $tag line=$i font=${result.layoutInput.style.fontSize.value} lines=${result.lineCount} error=${centre-target} text=${example.text}\n")
                }
            }
            if(c.verb=="être" && c.font==1f && c.lang==AppLanguage.ENGLISH_UK && !c.actual && c.tense in listOf(0,2)) {
                val shot=inst.uiAutomation.takeScreenshot()
                File(out,"${c.w}-${c.tense}.png").outputStream().use{shot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};shot.recycle()
            }
        }
    }
}
