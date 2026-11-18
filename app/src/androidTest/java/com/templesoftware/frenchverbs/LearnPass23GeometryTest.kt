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

class LearnPass23GeometryTest {
    val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val rotationRules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private val out get()=File(inst.targetContext.getExternalFilesDir(null),"pass23-geometry").apply{mkdirs()}
    private data class Case(val w:Int,val h:Int,val font:Float,val lang:AppLanguage,val verb:String,val tense:Int,val dark:Boolean=false)
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
                Box(Modifier.requiredSize(c.w.dp,(c.h-76).dp)){LocalizedContent(c.lang){TempleTheme(darkTheme=c.dark){LearnScreen(state(c),c.lang,{},{})}}}
            }
        }
    }
    @Test fun centredRegionsAndPreservedToggle() {
        val current=mutableStateOf(Case(915,412,1f,AppLanguage.ENGLISH_UK,"être",0))
        compose.activityRule.scenario.onActivity{it.setContent{Frame(current.value)}}
        val cases=mutableListOf<Case>()
        for((w,h)in listOf(915 to 412,800 to 360,1024 to 600))for(lang in listOf(AppLanguage.ENGLISH_UK,AppLanguage.FRENCH))
            for((v,t)in listOf("être" to 0,"être" to 1,"connaître" to 1))cases+=Case(w,h,1f,lang,v,t)
        for(lang in listOf(AppLanguage.ENGLISH_UK,AppLanguage.FRENCH))for(t in listOf(0,1))cases+=Case(915,412,1.3f,lang,"être",t)
        val log=File(out,"geometry.txt");log.writeText("")
        for(c in cases) {
            compose.runOnIdle{current.value=c};compose.waitForIdle()
            val paired=c.tense==1
            val padding=(if(rect("learn_viewport").height/testDensity<210)2 else if(c.h<=420)6 else 10)*testDensity
            val centres=mutableListOf<Float>()
            for(tag in if(paired)listOf("current","comparison") else listOf("comparison")) {
                val panel=rect("learn_panel_$tag");val divider=rect("learn_divider_$tag")
                val first=rect("learn_example_${tag}_0");val last=rect("learn_example_${tag}_1")
                val groupCentre=(first.top+last.bottom)/2
                val regionCentre=if(paired)(divider.bottom+panel.bottom-padding)/2 else (panel.top+padding+divider.top)/2
                assertEquals("$c $tag group centred",regionCentre,groupCentre,1.5f)
                assertTrue(first.left>=panel.left);assertTrue(last.right<=panel.right)
                assertTrue(first.top>=panel.top+padding-1);assertTrue(last.bottom<=panel.bottom-padding+1)
                if(paired)assertTrue(first.top>=divider.bottom) else assertTrue(last.bottom<=divider.top)
                for(i in 0..1)assertFalse(layouts("learn_example_${tag}_$i").single().hasVisualOverflow)
                val size=layouts("learn_example_${tag}_0").single().layoutInput.style.fontSize.value
                if(c.verb=="être"&&c.tense==0&&c.font==1f)assertEquals(if(c.h<=420)16f else 18f,size,0f)
                centres+=groupCentre
                log.appendText("PASS $c $tag font=$size groupCentre=$groupCentre regionCentre=$regionCentre error=${groupCentre-regionCentre} panel=$panel divider=$divider\n")
            }
            if(paired)assertEquals(centres[0],centres[1],1.5f)
            if(c.w==915 && c.font==1f && c.verb=="être") {
                val before=(0..5).map{rect("learn_form_current_$it").top}
                compose.onNodeWithTag("learn_examples_toggle").performClick()
                val chosen=(0..5).map{layouts("learn_sentence_$it").single().layoutInput.text.text}
                for(i in 0..5){assertEquals(before[i],rect("learn_form_current_$i").top,1f)
                    assertEquals(rect("learn_form_current_$i").top+layouts("learn_form_current_$i").single().firstBaseline,
                        rect("learn_sentence_$i").top+layouts("learn_sentence_$i").single().firstBaseline,1f)}
                compose.runOnIdle{current.value=c.copy(w=c.h,h=c.w)}
                assertEquals(chosen,(0..5).map{layouts("learn_sentence_$it").single().layoutInput.text.text})
                compose.runOnIdle{current.value=c}
                compose.onNodeWithTag("learn_examples_toggle").performClick().performClick()
                for(i in 0..5)assertNotEquals(chosen[i],layouts("learn_sentence_$i").single().layoutInput.text.text)
                compose.onNodeWithTag("learn_examples_toggle").performClick()
            }
        }
    }
    @Test fun textTransformationCost() {
        val v=OfflineDictionary.verbs.first{it.id=="être"}
        val sentences=(0..5).map{SentenceBank.candidates(v,0,it).first()}
        val examples=LearnExamples.forPanel(v,0,false,true)
        val log=File(out,"text-transform-ms.txt");log.writeText("")
        compose.runOnIdle {
            repeat(20){run ->
                val start=System.nanoTime()
                val a=sentences.map{sentenceText(it,androidx.compose.ui.graphics.Color.Green)}
                val b=examples.map{exampleText(it,androidx.compose.ui.graphics.Color.Green)}
                val ms=(System.nanoTime()-start)/1e6
                assertEquals(6,a.size);assertEquals(2,b.size)
                log.appendText("$run,$ms\n")
            }
        }
    }

}
