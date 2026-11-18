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

class LearnPass20UiTest {
    val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val rotationRules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private val out get()=File(inst.targetContext.getExternalFilesDir(null),"pass20").apply{mkdirs()}
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
    @Test fun contextualLayoutsAndLanguageSwitching() {
        val current=mutableStateOf(Case(915,412,1f,AppLanguage.ENGLISH_UK,"être",0))
        compose.activityRule.scenario.onActivity{it.setContent{
            androidx.compose.runtime.key(current.value.verb,current.value.tense,current.value.w){Frame(current.value)}
        }}
        File(out,"matrix.txt").writeText("")
        for((w,h) in listOf(915 to 412,412 to 915))for((id,t) in listOf("être" to 0,"avoir" to 1,"aller" to 2,"aller" to 3,"connaître" to 4,"finir" to 5,"regarder" to 1,"grandir" to 4)) {
            val c=Case(w,h,1f,AppLanguage.ENGLISH_UK,id,t)
            compose.runOnIdle{current.value=c};compose.waitForIdle()
            val verb=OfflineDictionary.verbs.first{it.id==id};val other=when(t){1->0;4->1;5->3;else->t};val paired=other!=t
            val formTag=if(w>h)"learn_form_current_0" else "learn_forms_current"
            val originalForm=layouts(formTag).single().layoutInput.text
            val originalProgress=compose.onNodeWithTag("learn_progress").fetchSemanticsNode().config[SemanticsProperties.Text]
            for(language in listOf(AppLanguage.ENGLISH_UK,AppLanguage.FRENCH,AppLanguage.ENGLISH_UK)) {
                compose.runOnIdle{current.value=current.value.copy(lang=language)}
                val tags=if(paired)listOf("current" to t,"comparison" to other) else listOf("comparison" to other)
                for((tag,tense) in tags)for((i,e)in LearnExamples.forPanel(verb,tense,paired,language==AppLanguage.ENGLISH_UK).withIndex()) {
                    val r=layouts("learn_example_${tag}_$i").single()
                    assertEquals(e.text,r.layoutInput.text.text);assertEquals("$c ${e.text}",1,r.lineCount)
                    assertFalse(r.hasVisualOverflow)
                    if(e.translation)assertTrue(r.layoutInput.text.spanStyles.isEmpty())
                    else assertTrue(r.layoutInput.text.spanStyles.any{it.start==e.highlightStart&&it.end==e.highlightEnd})
                }
                assertEquals(originalForm,layouts(formTag).single().layoutInput.text)
                assertEquals(originalProgress,compose.onNodeWithTag("learn_progress").fetchSemanticsNode().config[SemanticsProperties.Text])
                compose.onNodeWithTag("learn_next").assertIsDisplayed()
            }
            compose.onNodeWithTag("learn_examples_toggle").performScrollTo().performClick()
            fun sentences()=(0..5).map{layouts("learn_sentence_$it").single().layoutInput.text.text}
            val chosen=sentences()
            for(language in listOf(AppLanguage.FRENCH,AppLanguage.ENGLISH_UK)) {
                compose.runOnIdle{current.value=current.value.copy(lang=language)}
                assertEquals(chosen,sentences())
                for(i in 0..5) {
                    val r=layouts("learn_sentence_$i").single()
                    assertTrue(SentenceBank.candidates(verb,t,i).any{it.text==chosen[i]})
                    assertEquals(1,r.lineCount);assertFalse(r.hasVisualOverflow)
                    if(w>h)assertEquals(rect("learn_form_current_$i").top+layouts("learn_form_current_$i").single().firstBaseline,
                        rect("learn_sentence_$i").top+r.firstBaseline,1f)
                }
            }
            compose.onNodeWithTag("learn_examples_toggle").performScrollTo().performClick().performClick()
            for(i in 0..5)assertNotEquals(chosen[i],sentences()[i])
            compose.onNodeWithTag("learn_examples_toggle").performClick()
            File(out,"matrix.txt").appendText("PASS $c: normal EN/FR/EN, tense pairs, highlights, original conjugations/progress, six French rows, stable choices across language switching, refresh, baseline alignment and navigation.\n")
        }
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

}
