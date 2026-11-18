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

class LearnPass19UiTest {
    val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val rotationRules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private val out get()=File(inst.targetContext.getExternalFilesDir(null),"pass19").apply{mkdirs()}
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
    @Test fun bilingualLayoutsAndFrenchToggle() {
        val current=mutableStateOf(Case(915,412,1f,AppLanguage.ENGLISH_UK,"regarder",0))
        compose.activityRule.scenario.onActivity{it.setContent{androidx.compose.runtime.key(current.value){Frame(current.value)}}}
        val cases=AppLanguage.entries.flatMap { lang -> listOf(915 to 412,412 to 915).flatMap { (w,h) ->
            listOf("regarder","avoir","aller","prendre","recevoir","pouvoir").mapIndexed{t,v->Case(w,h,1f,lang,v,t)}
        }}+listOf(Case(1280,800,1f,AppLanguage.ENGLISH_UK,"appuyer",5),Case(800,1280,1f,AppLanguage.FRENCH,"paraître",3),
            Case(800,360,1f,AppLanguage.ENGLISH_UK,"éteindre",5),Case(915,412,2f,AppLanguage.ENGLISH_UK,"apercevoir",5),Case(412,915,2f,AppLanguage.FRENCH,"aller",3))
        File(out,"matrix.txt").writeText("")
        for(c in cases) {
            compose.runOnIdle{current.value=c};compose.waitForIdle()
            val comp=when(c.tense){1->0;4->1;5->3;else->c.tense};val paired=comp!=c.tense
            val verb=OfflineDictionary.verbs.first{it.id==c.verb}
            for((tag,t) in (if(paired)listOf("current" to c.tense,"comparison" to comp) else listOf("comparison" to comp))) {
                val expected=LearnExamples.forPanel(verb,t,paired,c.lang==AppLanguage.ENGLISH_UK)
                for(i in 0..1) {
                    val r=layouts("learn_example_${tag}_$i").single()
                    assertEquals(expected[i].text,r.layoutInput.text.text)
                    assertFalse("Example overflow $c",r.hasVisualOverflow)
                    if(c.font==1f)assertEquals("Example wraps $c ${expected[i].text}",1,r.lineCount)
                    if(expected[i].translation)assertTrue(r.layoutInput.text.spanStyles.isEmpty())
                    else assertTrue(r.layoutInput.text.spanStyles.any{it.start==expected[i].highlightStart && it.end==expected[i].highlightEnd})
                }
            }
            if(!paired)compose.onNodeWithTag("learn_example_current_0").assertDoesNotExist()
            if(c.font==1f && c.tense in listOf(0,5))capture("normal-${c.w}-${c.lang}-${c.tense}")
            compose.onNodeWithTag("learn_examples_toggle").performScrollTo()
            val key=if(c.w>c.h)"learn_form_comparison_0" else "learn_forms_comparison"
            val origin=rect(key).left;val font=layouts(key).single().layoutInput.style.fontSize
            val button=rect("learn_examples_toggle")
            val leftOrigin=if(c.w>c.h)(0..5).map{rect("learn_form_current_$it")} else emptyList()
            compose.onNodeWithTag("learn_examples_toggle").performClick()
            val first=(0..5).map{layouts("learn_sentence_$it").single().layoutInput.text.text}
            for(i in 0..5) {
                val r=layouts("learn_sentence_$i").single()
                assertTrue(SentenceBank.candidates(verb,c.tense,i).any{it.text==r.layoutInput.text.text})
                assertFalse(r.hasVisualOverflow);assertEquals(font,r.layoutInput.style.fontSize)
                assertEquals(origin,rect("learn_sentence_$i").left,1f)
                if(c.font==1f)assertEquals("Six-row wrapping $c ${r.layoutInput.text.text}",1,r.lineCount)
                if(c.w>c.h) {
                    assertEquals(rect("learn_form_current_$i").top+layouts("learn_form_current_$i").single().firstBaseline,
                        rect("learn_sentence_$i").top+r.firstBaseline,1f)
                    assertEquals(leftOrigin[i],rect("learn_form_current_$i"))
                }
            }
            assertEquals(button,rect("learn_examples_toggle"))
            compose.onNodeWithTag("learn_next").assertIsDisplayed()
            compose.onNodeWithTag("learn_examples_toggle").performScrollTo().performClick().performClick()
            for(i in 0..5)assertNotEquals(first[i],layouts("learn_sentence_$i").single().layoutInput.text.text)
            File(out,"matrix.txt").appendText("PASS $c normal bilingual/French; six French rows, spans, origins, font, refresh and navigation\n")
        }
    }

    @Test fun measuredCoverageAndDiversity() {
        lateinit var measurer:androidx.compose.ui.text.TextMeasurer
        lateinit var style:androidx.compose.ui.text.TextStyle
        var density=1f
        compose.activityRule.scenario.onActivity{it.setContent{TempleTheme{
            measurer=androidx.compose.ui.text.rememberTextMeasurer(cacheSize=512)
            style=randomSentenceStyle(false,false).copy(fontSize=20.sp)
            density=LocalDensity.current.density
        }}}
        compose.waitForIdle()
        val errors=mutableListOf<String>();val selected=mutableListOf<SentenceBank.Sentence>()
        val history=mutableListOf<SentenceSelection.Recent>();val rng=kotlin.random.Random(1901)
        var minFit=999;var repeats=0
        compose.runOnIdle {
            for(v in OfflineDictionary.verbs)for(t in 0..5)for(p in 0..5) {
                val bank=SentenceBank.candidates(v,t,p)
                val widths=bank.associateWith{measurer.measure(sentenceText(it,androidx.compose.ui.graphics.Color.Green),style,softWrap=false).size.width.toFloat()}
                val fit=widths.values.count{it<=300*density};minFit=minOf(minFit,fit)
                if(fit<2)errors+="${v.id} $t $p fitting=$fit shortest=${widths.values.min()/density}"
                var previous:String?=null
                repeat(8) {
                    val s=bank[SentenceSelection.choose(bank,previous,{widths.getValue(it)},300*density,rng,history)]
                    if(s.text==previous)repeats++
                    previous=s.text;selected+=s;history+=SentenceSelection.remembered(s)
                    if(history.size>36)history.removeAt(0)
                }
            }
        }
        fun top(xs:List<String>)=xs.groupingBy{it}.eachCount().entries.sortedByDescending{it.value}.take(20).joinToString()
        File(out,"measured-audit.txt").writeText("3420 combinations at actual annotated 20sp / 300dp; min fitting=$minFit; selections=${selected.size}; repeats=$repeats\n"+
            "Failures: ${errors.joinToString()}\nObjects: ${top(selected.map{it.semanticKey})}\nThemes: ${top(selected.map{it.theme})}\nStructures: ${top(selected.map{it.structure})}\nIntroductions: ${top(selected.map{it.introduction.ifEmpty{"[none]"}})}\n"+
            listOf("parc","billes","livre","vélo").joinToString("\n"){word->"$word="+selected.count{Regex("\\b${word}s?\\b").containsMatchIn(it.text)}})
        assertTrue(errors.joinToString("\n"),errors.isEmpty());assertEquals(0,repeats)
    }
    @Test fun allNormalExampleWidths() {
        lateinit var measurer:androidx.compose.ui.text.TextMeasurer
        lateinit var style:androidx.compose.ui.text.TextStyle
        var density=1f
        compose.activityRule.scenario.onActivity{it.setContent{TempleTheme{
            measurer=androidx.compose.ui.text.rememberTextMeasurer(cacheSize=128)
            style=learnExampleStyle(false,false,false)
            density=LocalDensity.current.density
        }}}
        compose.waitForIdle()
        val errors=mutableListOf<String>();var count=0
        compose.runOnIdle {
            for(v in OfflineDictionary.verbs)for(t in 0..5)for(english in listOf(false,true))for(paired in listOf(false,true)) {
                for(e in LearnExamples.forPanel(v,t,paired,english))for(landscape in listOf(false,true)) {
                    val width=if(landscape)if(paired)275 else 323 else if(paired)300 else 348
                    val result=measurer.measure(exampleText(e,androidx.compose.ui.graphics.Color.Green),
                        style.copy(fontSize=if(landscape)13.sp else 17.sp),softWrap=false)
                    count++
                    if(result.size.width>width*density)errors+="${v.id} $t paired=$paired landscape=$landscape: ${e.text} width=${result.size.width/density} available=$width"
                }
            }
        }
        File(out,"normal-widths.txt").writeText("Normal French/bilingual sentence measurements=$count; failures=${errors.size}\n"+errors.joinToString("\n"))
        assertTrue(errors.joinToString("\n"),errors.isEmpty())
    }

    @Test fun restorationRotationAndEntry() {
        val current=mutableStateOf(Case(915,412,1f,AppLanguage.ENGLISH_UK,"aller",3))
        val restoration=androidx.compose.ui.test.junit4.StateRestorationTester(compose)
        compose.activityRule.scenario.onActivity{(it.findViewById<android.view.ViewGroup>(android.R.id.content)).removeAllViews()}
        restoration.setContent{Frame(current.value)}
        compose.onNodeWithTag("learn_examples_toggle").performClick()
        fun texts()=(0..5).map{layouts("learn_sentence_$it").single().layoutInput.text.text}
        val original=texts()
        compose.runOnIdle{current.value=current.value.copy(w=412,h=915)}
        assertEquals(original,texts())
        compose.onNodeWithTag("learn_examples_toggle").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,"up"))
        restoration.emulateSavedInstanceStateRestore();assertEquals(original,texts())
        compose.runOnIdle{current.value=current.value.copy(w=915,h=412)};assertEquals(original,texts())
        compose.runOnIdle{current.value=current.value.copy(tense=4)}
        compose.onNodeWithTag("learn_sentence_0").assertDoesNotExist()
        compose.onNodeWithTag("learn_progress").assertTextEquals("80%")
        compose.runOnIdle{current.value=current.value.copy(tense=3)}
        compose.onNodeWithTag("learn_sentence_0").assertDoesNotExist()
        compose.onNodeWithTag("learn_examples_toggle").performClick()
        for(i in 0..5)assertNotEquals(original[i],texts()[i])
        File(out,"state.txt").writeText("PASS saved-state restoration, recomposition/orientation stability, entry reset, no immediate repeats and unchanged progress.")
    }
}
