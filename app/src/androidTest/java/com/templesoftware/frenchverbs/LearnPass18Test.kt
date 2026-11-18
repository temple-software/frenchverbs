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

class LearnPass18Test {
    val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val rotationRules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private val out get()=File(inst.targetContext.getExternalFilesDir(null),"pass18").apply{mkdirs()}
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
    @Test fun renderedBankCoverage() {
        lateinit var measurer:androidx.compose.ui.text.TextMeasurer
        lateinit var style:androidx.compose.ui.text.TextStyle
        compose.activityRule.scenario.onActivity{it.setContent{TempleTheme{
            measurer=androidx.compose.ui.text.rememberTextMeasurer(cacheSize=128)
            style=randomSentenceStyle(false,false)
        }}}
        compose.waitForIdle()
        val errors=mutableListOf<String>();var total=0
        compose.runOnIdle {
            for(v in OfflineDictionary.verbs)for(t in 0..5)for(p in 0..5) {
                val bank=SentenceBank.candidates(v,t,p)
                val widths=bank.map{measurer.measure(sentenceText(it,androidx.compose.ui.graphics.Color.Green),style,softWrap=false).size.width}
                val density=compose.activity.resources.displayMetrics.density
                val fit=widths.count{it<=300*density}
                if(fit<2)errors+="${v.id} tense=$t person=$p fit=$fit shortestDp=${widths.min()/density} ${bank[widths.indexOf(widths.min())].text}"
                total++
            }
        }
        File(out,"width-coverage.txt").writeText("combinations=$total\n"+errors.joinToString("\n"))
        assertTrue(errors.take(30).joinToString("\n"),errors.isEmpty())
    }
    private fun check(c:Case) {
        compose.onNodeWithTag("learn_panels").performSemanticsAction(SemanticsActions.ScrollBy){it(0f,-100000f)}
        compose.onNodeWithTag("learn_examples_toggle").performScrollTo()
        val button=rect("learn_examples_toggle")
        val primary=rect("learn_panel_current")
        val formTag=if(c.w>c.h)"learn_form_current_0" else "learn_forms_current"
        val primaryText=layouts(formTag).single().layoutInput.text
        val formBefore=rect(formTag)
        val expectedArrow=if(c.w>c.h)"right" else "down"
        compose.onNodeWithTag("learn_examples_toggle").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,expectedArrow)).performClick()
        compose.waitForIdle()
        assertEquals(button,rect("learn_examples_toggle"))
        assertEquals(primaryText,layouts(formTag).single().layoutInput.text)
        if(c.font==1f) {assertEquals(primary,rect("learn_panel_current"));assertEquals(formBefore,rect(formTag))}
        compose.onNodeWithTag("learn_examples_toggle").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,if(c.w>c.h)"left" else "up"))
        if(c.font==1f) {
            val range=compose.onNodeWithTag("learn_panels").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].maxValue()
            assertEquals("All normal-size content fits $c",0f,range,1f)
            for(i in 0..5)compose.onNodeWithTag("learn_sentence_$i").assertIsDisplayed()
        }
        val text=mutableListOf<String>();val deltas=mutableListOf<Float>()
        for(i in 0..5) {
            val key="learn_sentence_$i";val r=layouts(key).single();text+=r.layoutInput.text.text
            assertFalse("height overflow $c $i",r.didOverflowHeight)
            if(c.font==1f)assertEquals("one line $c $i ${r.layoutInput.text}",1,r.lineCount)
            assertFalse(r.isLineEllipsized(0));assertTrue(r.layoutInput.text.spanStyles.isNotEmpty())
            if(c.w>c.h) {
                val a=rect("learn_form_current_$i").top+layouts("learn_form_current_$i").single().firstBaseline
                val b=rect(key).top+r.firstBaseline
                assertEquals("baseline $c $i",a,b,1f);deltas+=kotlin.math.abs(a-b)
            }
            compose.onNodeWithTag(key).performScrollTo().assertIsDisplayed()
        }
        compose.onNodeWithTag("learn_next").assertIsDisplayed()
        if(c.w>c.h)assertEquals(rect("learn_panel_current").height,rect("learn_panel_comparison").height,0f)
        compose.onNodeWithTag("learn_heading_comparison").assertIsNotDisplayed()
        compose.onNodeWithTag("learn_examples_toggle").performScrollTo().performClick()
        compose.onNodeWithTag("learn_sentence_0").assertDoesNotExist()
        compose.onNodeWithTag("learn_examples_toggle").performClick()
        for(i in 0..5)assertNotEquals("fresh row $i $c",text[i],layouts("learn_sentence_$i").single().layoutInput.text.text)
        val stable=(0..5).map{layouts("learn_sentence_$it").single().layoutInput.text.text}
        compose.waitForIdle()
        assertEquals(stable,(0..5).map{layouts("learn_sentence_$it").single().layoutInput.text.text})
        File(out,"matrix.txt").appendText("$c deltas=$deltas PASS\n")
    }
    @Test fun toggleLayouts() {
        val current=mutableStateOf(Case(915,412,1f,AppLanguage.ENGLISH_UK,"regarder",0))
        compose.activityRule.scenario.onActivity{it.setContent{androidx.compose.runtime.key(current.value){Frame(current.value)}}}
        File(out,"matrix.txt").writeText("")
        for(l in AppLanguage.entries)for((w,h) in listOf(915 to 412,412 to 915))for((v,t)in listOf("regarder" to 0,"manger" to 1,"être" to 2,"aller" to 3,"recevoir" to 4,"prendre" to 5)) {
            val c=Case(w,h,1f,l,v,t);compose.runOnIdle{current.value=c};compose.waitForIdle();check(c)
            if(t in listOf(0,5))capture("layout-$w-$l-$t")
        }
        for(c in listOf(Case(800,360,1f,AppLanguage.ENGLISH_UK,"aller",3),Case(1280,800,1f,AppLanguage.FRENCH,"recevoir",5),Case(800,1280,1f,AppLanguage.ENGLISH_UK,"paraître",3),Case(915,412,2f,AppLanguage.FRENCH,"aller",3),Case(412,915,2f,AppLanguage.ENGLISH_UK,"prendre",5))) {
            compose.runOnIdle{current.value=c};compose.waitForIdle();check(c);capture("extra-${c.w}-${c.h}-${c.font}")
        }
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
    @Test fun realNavigationKeepsSequenceAndResetsMode() {
        val selection=Selection(tenses=Choices.tenses.toSet(),types=setOf("important"),important=setOf("être"))
        val repo=object:SelectionRepository {
            override val selections=MutableStateFlow(selection)
            override suspend fun save(selection:Selection){error("Learn must not save selections")}
        }
        lateinit var model:VerbViewModel
        lateinit var handle:SavedStateHandle
        compose.runOnIdle{handle=SavedStateHandle();model=VerbViewModel(repo,handle)}
        compose.waitUntil(10000){model.state.value.loaded}
        compose.runOnIdle{assertTrue(model.startLearn())}
        val sequence=model.state.value.session
        compose.activityRule.scenario.onActivity{it.setContent{
            val state by model.state.collectAsState()
            LocalizedContent(AppLanguage.ENGLISH_UK){TempleTheme{LearnScreen(state,AppLanguage.ENGLISH_UK,{},
                onNext={model.advance()},onPrevious={model.previous()})}}
        }}
        compose.onNodeWithTag("learn_examples_toggle").performScrollTo().performClick()
        compose.onNodeWithTag("learn_sentence_0").assertExists()
        compose.onNodeWithTag("learn_next").performClick()
        compose.onNodeWithTag("learn_sentence_0").assertDoesNotExist()
        compose.onNodeWithTag("learn_progress").assertTextEquals("20%")
        compose.onNodeWithTag("learn_examples_toggle").performScrollTo().performClick()
        compose.onNodeWithTag("learn_previous").performClick()
        compose.onNodeWithTag("learn_sentence_0").assertDoesNotExist()
        compose.onNodeWithTag("learn_progress").assertTextEquals("0%")
        compose.runOnIdle{assertEquals(sequence,model.state.value.session);assertEquals(0,handle.get<Int>("index"))}
        File(out,"navigation.txt").writeText("PASS real ViewModel Back/Next retain sequence and progress, reset Examples, and never write saved selections.")
    }

    @Test fun intermediateTabletAndRotatedWidths() {
        val current=mutableStateOf(Case(800,600,1f,AppLanguage.ENGLISH_UK,"commencer",5))
        compose.activityRule.scenario.onActivity{it.setContent{androidx.compose.runtime.key(current.value.lang){Frame(current.value)}}}
        for(l in AppLanguage.entries) {
            compose.runOnIdle{current.value=Case(600,800,1f,l,"commencer",5)}
            compose.onNodeWithTag("learn_examples_toggle").performScrollTo().performClick()
            val original=(0..5).map{layouts("learn_sentence_$it").single().layoutInput.text.text}
            compose.runOnIdle{current.value=current.value.copy(w=800,h=600)}
            for(i in 0..5) {
                val result=layouts("learn_sentence_$i").single()
                assertEquals(original[i],result.layoutInput.text.text)
                assertEquals(1,result.lineCount);assertFalse(result.hasVisualOverflow)
                assertEquals(rect("learn_form_current_$i").top+layouts("learn_form_current_$i").single().firstBaseline,
                    rect("learn_sentence_$i").top+result.firstBaseline,1f)
            }
            compose.onNodeWithTag("learn_examples_toggle").performClick()
            check(current.value)
        }
        File(out,"tablet-width.txt").writeText("PASS 600x800 -> 800x600, English/French; actual 17sp tablet metrics, stable sentences, one line and six aligned rows.")
    }

    @Test fun correctedTextOriginAndTypography() {
        val current=mutableStateOf(Case(915,412,1f,AppLanguage.ENGLISH_UK,"être",0))
        compose.activityRule.scenario.onActivity{it.setContent{androidx.compose.runtime.key(current.value){Frame(current.value)}}}
        for(l in AppLanguage.entries)for((w,h)in listOf(915 to 412,412 to 915))for(t in listOf(0,5)) {
            val c=Case(w,h,1f,l,if(t==0)"être" else "prendre",t)
            compose.runOnIdle{current.value=c};compose.waitForIdle()
            val landscape=w>h
            val origins=if(landscape)(0..5).map{rect("learn_form_comparison_$it")} else emptyList()
            val key=if(landscape)"learn_form_comparison_0" else "learn_forms_comparison"
            val font=layouts(key).single().layoutInput.style.fontSize
            val left=rect(key).left
            val button=rect("learn_examples_toggle")
            compose.onNodeWithTag("learn_examples_toggle").performClick()
            for(i in 0..5) {
                val r=layouts("learn_sentence_$i").single()
                assertEquals(font,r.layoutInput.style.fontSize)
                assertEquals(left,rect("learn_sentence_$i").left,1f)
                if(landscape)assertEquals(origins[i].top,rect("learn_sentence_$i").top,1f)
                assertEquals(1,r.lineCount);assertFalse(r.hasVisualOverflow)
                compose.onNodeWithTag("learn_sentence_$i").assertIsDisplayed()
            }
            assertEquals(button,rect("learn_examples_toggle"))
            compose.onNodeWithTag("learn_next").assertIsDisplayed()
            capture("corrected-$w-$l-$t")
        }
        File(out,"correction.txt").writeText("PASS eight English/French portrait/landscape cases: same left origins and font sizes as replaced conjugations; same landscape row tops; one line, no overflow; stable button.")
    }

}

