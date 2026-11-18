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

class LearnPass22Test {
    val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val rotationRules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private val out get()=File(inst.targetContext.getExternalFilesDir(null),"pass22").apply{mkdirs()}
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
    @Test fun secondBoxTiming() {
        val c=mutableStateOf(Case(915,412,1f,AppLanguage.ENGLISH_UK,"être",0))
        val opened=mutableStateOf(false)
        val metrics=java.util.Collections.synchronizedList(mutableListOf<String>())
        val listener=android.view.Window.OnFrameMetricsAvailableListener { _, m, _ ->
            metrics.add(listOf(android.view.FrameMetrics.TOTAL_DURATION,android.view.FrameMetrics.LAYOUT_MEASURE_DURATION,
                android.view.FrameMetrics.DRAW_DURATION,android.view.FrameMetrics.SYNC_DURATION,
                android.view.FrameMetrics.COMMAND_ISSUE_DURATION,android.view.FrameMetrics.SWAP_BUFFERS_DURATION,
                android.view.FrameMetrics.GPU_DURATION).joinToString(","){(m.getMetric(it)/1e6).toString()})
        }
        compose.activityRule.scenario.onActivity{it.window.addOnFrameMetricsAvailableListener(listener,android.os.Handler(android.os.Looper.getMainLooper()));it.setContent{if(opened.value)Frame(c.value)}}
        val log=File(out,"timing.csv");log.writeText("run,operation,first_draw_ms,frames_total_layout_draw_sync_command_swap_gpu_ms\n")
        try {
            for(run in 0..4) {
                fun timed(name:String,action:()->Unit) {
                    val latch=java.util.concurrent.CountDownLatch(1);var started=0L;var elapsed=0.0
                    val view=compose.activity.window.decorView
                    lateinit var draw:android.view.ViewTreeObserver.OnDrawListener
                    draw=android.view.ViewTreeObserver.OnDrawListener {
                        if(started!=0L){elapsed=(System.nanoTime()-started)/1e6;started=0;latch.countDown()
                            view.post{view.viewTreeObserver.removeOnDrawListener(draw)}}
                    }
                    compose.runOnIdle{metrics.clear();view.viewTreeObserver.addOnDrawListener(draw);started=System.nanoTime();action()}
                    compose.waitForIdle();assertTrue(name,latch.await(5,java.util.concurrent.TimeUnit.SECONDS))
                    log.appendText("$run,$name,$elapsed,${synchronized(metrics){metrics.toList().joinToString(";")}}\n")
                }
                timed("entry"){opened.value=true}
                timed("next"){c.value=c.value.copy(tense=1)}
                timed("previous"){c.value=c.value.copy(tense=0)}
                val toggle=compose.onNodeWithTag("learn_examples_toggle").fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
                timed("toggle_examples"){toggle()}
                compose.onNodeWithTag("learn_sentence_0").assertExists()
                val normal=compose.onNodeWithTag("learn_examples_toggle").fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
                timed("return_comparison"){normal()}
                timed("portrait_layout"){c.value=c.value.copy(w=412,h=915)}
                timed("landscape_layout"){c.value=c.value.copy(w=915,h=412)}
                compose.runOnIdle{opened.value=false};compose.waitForIdle()
            }
        } finally {compose.activityRule.scenario.onActivity{it.window.removeOnFrameMetricsAvailableListener(listener)}}
    }
    @Test fun selectionCost() {
        lateinit var measurer:androidx.compose.ui.text.TextMeasurer
        lateinit var a:androidx.compose.ui.text.TextStyle
        lateinit var b:androidx.compose.ui.text.TextStyle
        var d=1f
        compose.activityRule.scenario.onActivity{it.setContent{TempleTheme{
            measurer=androidx.compose.ui.text.rememberTextMeasurer()
            a=randomSentenceStyle(false,false).copy(fontSize=20.sp);b=randomSentenceStyle(true,true)
            d=LocalDensity.current.density
        }}}
        val log=File(out,"selection.csv");log.writeText("run,data_ms,selection_ms,measurement_ms,calls\n")
        compose.runOnIdle {
            val v=OfflineDictionary.verbs.first{it.id=="être"}
            val widths=mutableMapOf<SentenceBank.Sentence,Float>()
            for(run in 0..9) {
                val start=System.nanoTime();val banks=(0..5).map{SentenceBank.candidates(v,0,it)}
                val data=(System.nanoTime()-start)/1e6;var measureNs=0L;var calls=0
                val state=LearnExampleState();val begin=System.nanoTime()
                state.enter("test",v,0,{s ->
                    widths.getOrPut(s) {
                        val t=System.nanoTime();val txt=sentenceText(s,androidx.compose.ui.graphics.Color.Green)
                        val styles=listOf(a,b,b).distinct()
                        val width=styles.maxOf{measurer.measure(txt,it,softWrap=false).size.width}.toFloat()
                        measureNs+=System.nanoTime()-t;calls+=styles.size;width
                    }
                },300*d)
                log.appendText("$run,$data,${(System.nanoTime()-begin)/1e6},${measureNs/1e6},$calls\n")
                assertEquals(6,state.sentences(v,0).size);assertTrue(banks.all{it.isNotEmpty()})
            }
        }
    }
    @Test fun affectedTypographyAndToggle() {
        val current=mutableStateOf(Case(915,412,1f,AppLanguage.ENGLISH_UK,"être",0))
        compose.activityRule.scenario.onActivity{it.setContent{Frame(current.value)}}
        val log=File(out,"layout.txt");log.writeText("")
        for((w,h)in listOf(915 to 412,800 to 360))for(lang in listOf(AppLanguage.ENGLISH_UK,AppLanguage.FRENCH))
        for((verb,t)in listOf("être" to 0,"connaître" to 1,"regarder" to 4,"aller" to 5)) {
            compose.runOnIdle{current.value=Case(w,h,1f,lang,verb,t)}
            val other=when(t){1->0;4->1;5->3;else->t};val paired=t!=other
            val v=OfflineDictionary.verbs.first{it.id==verb}
            for((tag,tense)in if(paired)listOf("current" to t,"comparison" to other) else listOf("comparison" to t)) {
                for((j,e)in LearnExamples.forPanel(v,tense,paired,lang==AppLanguage.ENGLISH_UK).withIndex()) {
                    val r=layouts("learn_example_${tag}_$j").single()
                    assertEquals(e.text,r.layoutInput.text.text);assertFalse(r.hasVisualOverflow)
                    if(w==915)assertEquals("$verb $t $lang $tag $j",1,r.lineCount)
                    assertTrue(r.layoutInput.style.fontSize.value in 13f..(if(paired)14f else 15f))
                    if(verb=="être"&&t==0)assertEquals(15f,r.layoutInput.style.fontSize.value,0f)
                    if(e.translation)assertTrue(r.layoutInput.text.spanStyles.isEmpty()) else assertTrue(r.layoutInput.text.spanStyles.any{it.start==e.highlightStart&&it.end==e.highlightEnd})
                    if(paired)assertEquals(rect("learn_example_current_$j").top,rect("learn_example_comparison_$j").top,1f)
                    log.appendText("PASS $w $lang $verb $t $tag $j ${r.layoutInput.style.fontSize} lines=${r.lineCount}\n")
                }
            }
            val rows=(0..5).map{rect("learn_form_current_$it")}
            compose.onNodeWithTag("learn_examples_toggle").performClick()
            val chosen=(0..5).map{layouts("learn_sentence_$it").single().layoutInput.text.text}
            for(j in 0..5) {
                assertEquals(rows[j].top,rect("learn_form_current_$j").top,1f)
                assertEquals(rect("learn_form_current_$j").top+layouts("learn_form_current_$j").single().firstBaseline,
                    rect("learn_sentence_$j").top+layouts("learn_sentence_$j").single().firstBaseline,1f)
            }
            compose.runOnIdle{current.value=current.value.copy(w=h,h=w)}
            assertEquals(chosen,(0..5).map{layouts("learn_sentence_$it").single().layoutInput.text.text})
            compose.runOnIdle{current.value=current.value.copy(w=w,h=h)}
            compose.onNodeWithTag("learn_examples_toggle").performClick()
            compose.onNodeWithTag("learn_example_comparison_0",true).assertExists()
            compose.onNodeWithTag("learn_examples_toggle").performClick()
            for(j in 0..5)assertNotEquals(chosen[j],layouts("learn_sentence_$j").single().layoutInput.text.text)
            compose.onNodeWithTag("learn_examples_toggle").performClick()
        }
    }

}
