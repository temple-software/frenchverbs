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

class LearnStage3RegressionTest {
    val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val rotationRules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private val out get()=File(inst.targetContext.getExternalFilesDir(null),"pass25-ui").apply{mkdirs()}
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
    @Test fun responsivePanelsAndToggle() {
        val current=mutableStateOf(Case(412,915,1f,AppLanguage.ENGLISH_UK,"être",0))
        compose.activityRule.scenario.onActivity{it.setContent{Frame(current.value)}}
        val cases=mutableListOf<Case>()
        for((w,h)in listOf(360 to 800,412 to 915,800 to 360,915 to 412,800 to 1280,1280 to 800))
            for(lang in listOf(AppLanguage.ENGLISH_UK,AppLanguage.FRENCH))for(t in listOf(0,3,5))cases+=Case(w,h,1f,lang,if(t==0)"être" else "aller",t)
        for((w,h)in listOf(412 to 915,915 to 412))for(lang in listOf(AppLanguage.ENGLISH_UK,AppLanguage.FRENCH))cases+=Case(w,h,1.3f,lang,"connaître",5)
        val log=File(out,"responsive.txt");log.writeText("")
        val errors=mutableListOf<String>()
        for(c in cases) { try {
            compose.runOnIdle{current.value=c};compose.waitForIdle()
            compose.onNodeWithTag("learn_panels").performSemanticsAction(SemanticsActions.ScrollBy){it(0f,-100000f)}
            val landscape=c.w>c.h
            val range=compose.onNodeWithTag("learn_panels").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].maxValue()
            if(c.font==1f)assertEquals("unintended scroll $c",0f,range,1f)
            for(tag in listOf("learn_previous","learn_repeat","learn_next"))compose.onNodeWithTag(tag).assertIsDisplayed()
            compose.onNodeWithTag("learn_repeat").assertIsNotEnabled()
            val verb=OfflineDictionary.verbs.first{it.id==c.verb}
            val comparison=when(c.tense){1->0;4->1;5->3;else->c.tense}
            for((tag,t)in listOf("current" to c.tense,"comparison" to comparison)) {
                if(tag=="current" || comparison!=c.tense) {
                    val text=layouts("learn_heading_$tag").single().layoutInput.text.text
                    assertEquals(c.lang==AppLanguage.ENGLISH_UK,text.contains(" ("))
                }
                val forms=verb.tenses[t].forms
                if(landscape)for(i in 0..5) {
                    val r=layouts("learn_form_${tag}_$i").single()
                    assertEquals(forms[i].text,r.layoutInput.text.text)
                    assertFalse(r.hasVisualOverflow)
                    if(tag=="comparison" || c.tense in listOf(1,4,5))assertTrue(r.layoutInput.text.spanStyles.any{it.start==forms[i].highlightStart&&it.end==forms[i].highlightEnd})
                    assertEquals(rect("learn_form_current_$i").top+layouts("learn_form_current_$i").single().firstBaseline,
                        rect("learn_form_comparison_$i").top+layouts("learn_form_comparison_$i").single().firstBaseline,1f)
                } else assertEquals(forms.joinToString("\n"){it.text},layouts("learn_forms_$tag").single().layoutInput.text.text)
            }
            compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),useUnmergedTree=true).fetchSemanticsNodes().forEach{n->
                val results=mutableListOf<TextLayoutResult>();n.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results)
                results.forEach{result->
                    // Cached paragraphs can retain a wider layout box than their actual glyphs.
                    assertTrue("$c text height ${result.layoutInput.text}",result.multiParagraph.height<=result.size.height+1f)
                    for(line in 0 until result.lineCount) {
                        assertFalse(result.isLineEllipsized(line))
                        assertTrue("$c glyph width ${result.layoutInput.text}",result.getLineRight(line)-result.getLineLeft(line)<=result.size.width+1f)
                    }
                }
            }
            if(c.tense==0 || c.font>1f) {
                val before=compose.onNodeWithTag("learn_progress").fetchSemanticsNode().config[SemanticsProperties.Text]
                compose.onNodeWithTag("learn_examples_toggle").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,if(landscape)"right" else "down"))
                compose.onNodeWithTag("learn_examples_toggle").performScrollTo().performClick()
                compose.onNodeWithTag("learn_examples_toggle").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,if(landscape)"left" else "up"))
                for(i in 0..5) {
                    val r=layouts("learn_sentence_$i").single();assertFalse(r.hasVisualOverflow)
                    assertTrue(SentenceBank.candidates(verb,c.tense,i).any{it.text==r.layoutInput.text.text})
                }
                assertEquals(before,compose.onNodeWithTag("learn_progress").fetchSemanticsNode().config[SemanticsProperties.Text])
                compose.onNodeWithTag("learn_examples_toggle").performScrollTo().performClick()
            }
            log.appendText("PASS $c scroll=$range headings/forms/highlights/no overflow/navigation/toggle\n")
        } catch(e:AssertionError) { errors+="$c: ${e.message}";log.appendText("FAIL $c: ${e.message}\n") } }
        assertTrue(errors.joinToString("\n"),errors.isEmpty())
    }
    @Test fun smallLandscapeSizing() {
        val c=Case(800,360,1f,AppLanguage.ENGLISH_UK,"aller",5)
        compose.activityRule.scenario.onActivity{it.setContent{Frame(c)}}
        compose.waitForIdle()
        val log=File(out,"small-sizing.txt")
        log.writeText("viewport=${rect("learn_viewport")} panel=${rect("learn_panel_current")} nav=${rect("learn_navigation")} density=$testDensity\n")
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),useUnmergedTree=true).fetchSemanticsNodes().forEach{n->
            val results=mutableListOf<TextLayoutResult>();n.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results)
            results.forEach{log.appendText("${it.layoutInput.text} font=${it.layoutInput.style.fontSize} lineHeight=${it.layoutInput.style.lineHeight} lines=${it.lineCount} size=${it.size} position=${n.positionInRoot}\n")}
        }
        val shot=inst.uiAutomation.takeScreenshot();File(out,"small.png").outputStream().use{shot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};shot.recycle()
    }
    @Test fun progressFreshEntryAndSavedNavigation() {
        for(total in listOf(2,3,6,11,132,570)) {
            assertEquals(0,learnProgress(0,total));assertEquals(100,learnProgress(total-1,total))
            for(i in 1 until total-1)assertEquals((kotlin.math.floor(i.toDouble()/(total-1)*20+.5).toInt()*5).coerceIn(5,95),learnProgress(i,total))
        }
        assertEquals(100,learnProgress(0,1)) // Existing singleton/final-item behaviour.
        val selection=Selection(tenses=Choices.tenses.toSet(),types=setOf("important","er"),important=setOf("être"),er=setOf("standard"))
        val repo=object:SelectionRepository {
            override val selections=MutableStateFlow(selection)
            override suspend fun save(selection:Selection){error("Learn must not modify selections")}
        }
        lateinit var model:VerbViewModel
        lateinit var handle:SavedStateHandle
        compose.runOnIdle{handle=SavedStateHandle();model=VerbViewModel(repo,handle)}
        compose.waitUntil(10000){model.state.value.loaded}
        compose.runOnIdle{assertTrue(model.startLearn())}
        val original=model.state.value.session
        compose.activityRule.scenario.onActivity{it.setContent{
            val state by model.state.collectAsState()
            LocalizedContent(AppLanguage.ENGLISH_UK){TempleTheme{LearnScreen(state,AppLanguage.ENGLISH_UK,{},onNext={model.advance()},onPrevious={model.previous()})}}
        }}
        compose.onNodeWithTag("learn_previous").assertIsNotEnabled()
        compose.onNodeWithTag("learn_progress").assertTextEquals("0%")
        repeat(3){compose.onNodeWithTag("learn_next").performClick()}
        compose.onNodeWithTag("learn_previous").performClick()
        compose.runOnIdle{assertEquals(2,model.state.value.index);assertEquals(original,model.state.value.session)}
        compose.runOnIdle{
            val restored=VerbViewModel(repo,SavedStateHandle(mapOf("session" to handle.get<ArrayList<String>>("session"),"index" to handle.get<Int>("index"))))
            assertEquals(original,restored.state.value.session);assertEquals(model.state.value.current,restored.state.value.current)
            while(model.advance())Unit
            assertFalse(model.advance())
        }
        compose.onNodeWithTag("learn_progress").assertTextEquals("100%")
        compose.onNodeWithTag("learn_next_label",true).assertTextEquals("Now repeat")
        compose.onNodeWithTag("learn_repeat").assertIsNotEnabled()
        compose.runOnIdle{
            assertTrue(model.startLearn());assertEquals(0,model.state.value.index)
            assertNotEquals(original.last().verbId,model.state.value.session.last().verbId)
            assertEquals(selection,model.state.value.selection)
        }
        compose.onNodeWithTag("learn_progress").assertTextEquals("0%")
        File(out,"state.txt").writeText("PASS rounded progress/endpoints; real Back/Next; saved session/index restoration; final advance bounded; fresh sequence/category choice; preferences never written.\n")
    }
}

