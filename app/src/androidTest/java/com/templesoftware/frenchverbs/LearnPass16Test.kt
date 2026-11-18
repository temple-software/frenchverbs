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

class LearnPass16Test {
    val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val rotationRules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private val out get()=File(inst.targetContext.getExternalFilesDir(null),"pass16").apply{mkdirs()}
    private data class Case(val w:Int,val h:Int,val font:Float,val lang:AppLanguage,val verb:String,val tense:Int,val dark:Boolean=false)
    private val items=listOf("regarder" to 0,"manger" to 1,"être" to 2,"aller" to 3,"recevoir" to 4,"prendre" to 5)
    private val cases=(listOf(360 to 800,800 to 360,412 to 915,915 to 412,800 to 1280,1280 to 800).flatMap{(w,h)->
        AppLanguage.entries.flatMap{l->items.map{(v,t)->Case(w,h,1f,l,v,t)}}}+
        listOf(320 to 568,412 to 915,915 to 412).flatMap{(w,h)->AppLanguage.entries.flatMap{l->items.map{(v,t)->Case(w,h,2f,l,v,t)}}})
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
    private fun check(c:Case,requireFit:Boolean) {
        compose.onNodeWithTag("learn_panels").performSemanticsAction(SemanticsActions.ScrollBy){it(0f,-100000f)}
        val count="${learnProgress(c.tense,6)}%"
        compose.onNodeWithTag("learn_progress").assertTextEquals(count).assertIsDisplayed()
        compose.onAllNodesWithText(count).assertCountEquals(1)
        assertTrue(rect("screen_title").right<=rect("learn_progress").left)
        assertTrue(rect("learn_progress").bottom<=rect("learn_viewport").top)
        compose.onNodeWithTag("learn_previous").assertIsDisplayed()
        compose.onNodeWithTag("learn_next").assertIsDisplayed()
        assertEquals(rect("learn_previous").top,rect("learn_next").top,1f)
        assertEquals(rect("learn_previous").bottom,rect("learn_next").bottom,1f)
        if(c.tense==0)compose.onNodeWithTag("learn_previous").assertIsNotEnabled()
        if(c.tense<5) {
            val label=layouts("learn_next").single().layoutInput.text
            val tenseName=if(c.lang==AppLanguage.ENGLISH_UK) listOf("present","imperfect","simple future","perfect tense","past historic","pluperfect")[c.tense+1]
                else listOf("présent","imparfait","futur simple","passé composé","passé simple","plus-que-parfait")[c.tense+1]
            val prefix=if(c.lang==AppLanguage.ENGLISH_UK) "Next: " else "Suivant : "
            val link=if(c.lang==AppLanguage.ENGLISH_UK) " in " else if(c.tense+1==1) " à l’" else " au "
            assertEquals(prefix+c.verb+link+tenseName,label.text)
            val green=label.spanStyles.single{it.start==prefix.length&&it.end==prefix.length+c.verb.length}.item.color
            val blue=label.spanStyles.single{it.start==prefix.length+c.verb.length+link.length&&it.end==label.length}.item.color
            assertNotEquals(green,blue)
        }
        val range=compose.onNodeWithTag("learn_panels").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].maxValue()
        if(requireFit)assertEquals("Standard layout fits $c",0f,range,1f)
        val comparison=when(c.tense){1->0;4->1;5->3;else->c.tense}
        for((tag,tense) in listOf("current" to c.tense,"comparison" to comparison)) {
            val verb=OfflineDictionary.verbs.first{it.id==c.verb}
            val data=verb.tenses[tense]
            if(tag=="current"||comparison!=c.tense) {
                val heading=layouts("learn_heading_$tag").single()
                if(c.lang==AppLanguage.ENGLISH_UK)assertTrue(heading.layoutInput.text.text.contains(" (")&&heading.layoutInput.text.text.endsWith(")"))
                else assertFalse(heading.layoutInput.text.text.contains("("))
                if(c.font==1f)assertEquals("Inline heading $c",1,heading.lineCount)
            } else compose.onNodeWithTag("learn_heading_$tag").assertDoesNotExist()
            if(tag=="comparison"||comparison!=c.tense) {
                val examples=LearnExamples.forPanel(verb,tense,comparison!=c.tense)
                examples.forEachIndexed { i,sentence ->
                    val key="learn_example_${tag}_$i"
                    compose.onNodeWithTag(key).performScrollTo().assertIsDisplayed().assertTextEquals(sentence.text)
                    val text=layouts(key).single().layoutInput.text
                    assertTrue(text.spanStyles.any{it.start==sentence.highlightStart&&it.end==sentence.highlightEnd})
                }
                if(comparison!=c.tense && (c.w>c.h || tag=="current")) {
                    assertTrue(rect("learn_forms_$tag").bottom<=rect("learn_divider_$tag").top)
                    assertTrue(rect("learn_divider_$tag").bottom<=rect("learn_example_${tag}_0").top)
                } else {
                    assertTrue(rect("learn_example_${tag}_1").bottom<=rect("learn_divider_$tag").top)
                    assertTrue(rect("learn_divider_$tag").bottom<=rect("learn_forms_$tag").top)
                }
            } else {
                compose.onNodeWithTag("learn_example_${tag}_0").assertDoesNotExist()
                compose.onNodeWithTag("learn_divider_$tag").assertDoesNotExist()
            }
            compose.onNodeWithTag("learn_instruction").assertDoesNotExist()
            val highlighted=tag=="comparison"||c.tense in listOf(1,4,5)
            val separateRows=compose.onAllNodesWithTag("learn_form_${tag}_0").fetchSemanticsNodes().isNotEmpty()
            if(separateRows) {
                val gaps=mutableListOf<Float>()
                data.forms.forEachIndexed { i,form ->
                    val key="learn_form_${tag}_$i"
                    compose.onNodeWithTag(key).performScrollTo().assertIsDisplayed().assertTextEquals(form.text)
                    val annotated=layouts(key).single().layoutInput.text
                    if(highlighted)assertTrue(annotated.spanStyles.any{it.start==form.highlightStart&&it.end==form.highlightEnd})
                }
                for(i in 0..4)gaps+=rect("learn_form_${tag}_${i+1}").top-rect("learn_form_${tag}_$i").bottom
                assertTrue("Rows do not overlap $c",gaps.min()>=-1f)
                assertTrue("Rows are evenly distributed $c",gaps.max()-gaps.min()<=2f)
                assertEquals(rect("learn_forms_$tag").top,rect("learn_form_${tag}_0").top,1f)
                assertEquals(rect("learn_forms_$tag").bottom,rect("learn_form_${tag}_5").bottom,1f)
            } else {
                compose.onNodeWithTag("learn_forms_$tag").performScrollTo().assertIsDisplayed()
                    .assertTextEquals(data.forms.joinToString("\n"){it.text})
                val result=layouts("learn_forms_$tag").single()
                if(c.w<=c.h)assertTrue("Larger portrait forms",result.layoutInput.style.fontSize.value>=19f)
                val annotated=result.layoutInput.text
                var offset=0
                data.forms.forEach { form ->
                    if(highlighted)assertTrue(annotated.spanStyles.any{it.start==offset+form.highlightStart&&it.end==offset+form.highlightEnd})
                    offset+=form.text.length+1
                }
            }
            val textKey=if(separateRows)"learn_form_${tag}_0" else "learn_forms_$tag"
            val density=layouts(textKey).single().layoutInput.density.density
            assertTrue("Horizontal inset $c",rect(textKey).left-rect("learn_panel_$tag").left>=18*density-1f)
            if(tag=="comparison"||comparison!=c.tense) {
                val example=layouts("learn_example_${tag}_0").single()
                if(c.w<=c.h)assertTrue("Larger portrait examples",example.layoutInput.style.fontSize.value>=16f)
                assertEquals(rect(textKey).left,rect("learn_example_${tag}_0").left,1f)
                assertEquals(rect(textKey).left,rect("learn_divider_$tag").left,1f)
                if(c.w>c.h&&comparison!=c.tense)
                    assertTrue("Footer bottom padding $c",rect("learn_panel_$tag").bottom-rect("learn_example_${tag}_1").bottom>=4*density-1f)
            }
        }
        if(c.w>c.h) {
            assertEquals("Landscape panel tops $c",rect("learn_panel_current").top,rect("learn_panel_comparison").top,1f)
            assertEquals("Landscape panel bottoms $c",rect("learn_panel_current").bottom,rect("learn_panel_comparison").bottom,1f)
            assertTrue("Landscape fills viewport $c",rect("learn_panel_current").height>=rect("learn_viewport").height*.88f)
            if(requireFit) {
                val key=if(compose.onAllNodesWithTag("learn_form_current_0").fetchSemanticsNodes().isNotEmpty())"learn_form_current_0" else "learn_forms_current"
                val d=layouts(key).single().layoutInput.density.density
                assertTrue("Clear gap above navigation $c",rect("learn_next").top-rect("learn_panel_current").bottom>=10*d-1f)
            }
            if(comparison!=c.tense) for(i in 0..1) assertEquals("Aligned example $i $c",rect("learn_example_current_$i").top,rect("learn_example_comparison_$i").top,1f)
        }
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),useUnmergedTree=true).fetchSemanticsNodes().forEach{node->
            val values=mutableListOf<TextLayoutResult>();node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(values)
            values.forEach{result->
                assertFalse("Clipped $c ${result.layoutInput.text}",result.didOverflowHeight)
                for(line in 0 until result.lineCount){assertFalse(result.isLineEllipsized(line));assertTrue("Wide $c ${result.layoutInput.text}",result.getLineRight(line)-result.getLineLeft(line)<=result.size.width+1)}
            }
        }
        compose.onNodeWithText("Example").assertDoesNotExist();compose.onNodeWithText("Exemple").assertDoesNotExist()
        compose.onNodeWithText("Example +").assertDoesNotExist();compose.onNodeWithText("Exemple +").assertDoesNotExist()
        compose.onNodeWithTag("learn_panels").performSemanticsAction(SemanticsActions.ScrollBy){it(0f,-100000f)}
    }
    @Test fun representativeLayoutsExamplesAndHighlights() {
        val current=mutableStateOf(cases.first())
        compose.activityRule.scenario.onActivity{it.setContent{Frame(current.value)}}
        val records=mutableListOf<String>()
        for(c in cases) {
            compose.runOnIdle{current.value=c};compose.waitForIdle()
            check(c,c.font==1f)
            val range=compose.onNodeWithTag("learn_panels").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].maxValue()
            records+="$c scroll=$range PASS"
            if(c.verb=="prendre")capture("matrix-${c.w}-${c.h}-${c.font}-${c.lang}")
        }
        File(out,"matrix.txt").writeText("PASS ${cases.size} cases\n"+records.joinToString("\n"))
    }
    @Test fun actualPhoneBothOrientationsAndLanguages() {
        val current=mutableStateOf(Case(412,915,1f,AppLanguage.ENGLISH_UK,"prendre",5))
        for(o in listOf(1,0)) {
            compose.activityRule.scenario.onActivity{it.requestedOrientation=o}
            compose.waitUntil(10000){compose.activity.resources.configuration.orientation==if(o==1)1 else 2}
            compose.activityRule.scenario.onActivity{it.setContent{LocalizedContent(current.value.lang){TempleTheme{LearnScreen(state(current.value),current.value.lang,{},{})}}}}
            for(l in AppLanguage.entries)for((v,t)in items) {
                val c=Case(if(o==1)412 else 915,if(o==1)915 else 412,1f,l,v,t)
                compose.runOnIdle{current.value=c};compose.waitForIdle();check(c,true)
                capture("actual-$o-$l-$v-$t")
            }
        }
    }
    @Test fun shortLandscapeFits() {
        val current=mutableStateOf(Case(800,360,1f,AppLanguage.ENGLISH_UK,"manger",0))
        compose.activityRule.scenario.onActivity{it.setContent{Frame(current.value)}}
        val errors=mutableListOf<String>()
        for(l in AppLanguage.entries)for((v,t)in items) {
            val c=Case(800,360,1f,l,v,t)
            compose.runOnIdle{current.value=c};compose.waitForIdle();capture("diagnostic-short-${c.verb}-${c.tense}");try { check(c,true) } catch(e:AssertionError) { errors+="$c: ${e.message}" }
            capture("short-$l-$v-$t")
        }
        File(out,"short-results.txt").writeText(if(errors.isEmpty())"PASS 12 cases" else errors.joinToString("\n"))
        assertTrue(errors.joinToString("\n"),errors.isEmpty())
    }
    @Test fun longVerbHeaderRemainsComplete() {
        val current=mutableStateOf(Case(320,568,1f,AppLanguage.ENGLISH_UK,"reconnaître",5))
        compose.activityRule.scenario.onActivity{it.setContent{Frame(current.value)}}
        for(l in AppLanguage.entries)for(f in listOf(1f,2f)) {
            val c=Case(320,568,f,l,"reconnaître",5)
            compose.runOnIdle{current.value=c};compose.waitForIdle();check(c,false)
            compose.onNodeWithTag("screen_title").assertTextEquals(if(l==AppLanguage.FRENCH)"reconnaître" else "reconnaître / to recognise")
        }
    }
    @Test fun backNextPreserveSessionAndSavedIndex() {
        val selection=Selection(tenses=Choices.tenses.toSet(),types=setOf("important","er"),important=setOf("être"),er=setOf("standard"))
        val repo=object:SelectionRepository{
            override val selections=MutableStateFlow(selection)
            override suspend fun save(selection:Selection){error("Learn navigation must not write selections")}
        }
        lateinit var model:VerbViewModel
        lateinit var saved:SavedStateHandle
        var repeat=0
        compose.runOnIdle{saved=SavedStateHandle();model=VerbViewModel(repo,saved)}
        compose.waitUntil(10000){model.state.value.loaded}
        compose.runOnIdle{assertTrue(model.startLearn())}
        val original=model.state.value.session.toList()
        compose.activityRule.scenario.onActivity{it.setContent{
            val state by model.state.collectAsState()
            LocalizedContent(AppLanguage.ENGLISH_UK){TempleTheme{LearnScreen(state,AppLanguage.ENGLISH_UK,{},
                onNext={if(!model.advance())repeat++},onPrevious={model.previous()})}}
        }}
        compose.onNodeWithTag("learn_previous").assertIsNotEnabled().performClick()
        compose.runOnIdle{assertEquals(0,model.state.value.index);assertFalse(model.previous())}
        repeat(9){compose.onNodeWithTag("learn_next").performClick()}
        fun visibleText()=compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text),useUnmergedTree=true).fetchSemanticsNodes().flatMap{it.config[SemanticsProperties.Text].map{value->value.text}}.sorted()
        val tenth=model.state.value.current
        val tenthPresentation=visibleText()
        compose.onNodeWithTag("learn_progress").assertTextEquals("${learnProgress(9,original.size)}%")
        compose.onNodeWithTag("learn_previous").performClick()
        compose.onNodeWithTag("learn_progress").assertTextEquals("${learnProgress(8,original.size)}%")
        compose.runOnIdle{assertEquals(original[8],model.state.value.current);assertEquals(8,saved.get<Int>("index"))}
        compose.onNodeWithTag("learn_next").performClick()
        assertEquals(tenthPresentation,visibleText())
        compose.runOnIdle{assertEquals(tenth,model.state.value.current);assertEquals(original,model.state.value.session)}
        compose.runOnIdle{
            val restored=VerbViewModel(repo,SavedStateHandle(mapOf("session" to saved.get<ArrayList<String>>("session"),"index" to saved.get<Int>("index"))))
            assertEquals(original,restored.state.value.session);assertEquals(tenth,restored.state.value.current)
            while(model.advance())Unit
        }
        compose.onNodeWithTag("learn_progress").assertTextEquals("100%")
        compose.onNodeWithTag("learn_next").assertTextEquals("Now repeat →")
        compose.onNodeWithTag("learn_previous").assertIsEnabled().performClick()
        compose.onNodeWithTag("learn_next").performClick()
        compose.onNodeWithTag("learn_next").performClick()
        compose.runOnIdle{assertEquals(1,repeat);assertEquals(original,model.state.value.session)}
    }
    @Test fun allVocabularyExampleAnnotationsAndProgress() {
        var examplesChecked=0
        for(verb in OfflineDictionary.verbs) for(tense in 0..5) {
            val comparison=when(tense){1->0;4->1;5->3;else->tense}
            val first=LearnExamples.forPanel(verb,tense,comparison!=tense)
            val second=if(comparison!=tense)LearnExamples.forPanel(verb,comparison,true) else emptyList()
            val page=first+second
            assertEquals(2,first.map{it.person}.distinct().size)
            if(comparison!=tense) first.zip(second).forEach{(a,b)->
                assertEquals(a.person,b.person);assertEquals(a.complement,b.complement)
                assertNotEquals(a.tense,b.tense)
                assertFalse(a.text.contains(","));assertFalse(b.text.contains(","))
            }
            assertEquals(page.size,page.map{it.text}.distinct().size)
            assertEquals(if(comparison==tense)1 else 0,page.count{it.person==3})
            for(e in page) {
                val f=verb.tenses[e.tense].forms[e.person]
                assertEquals(f.marked,e.marked)
                assertEquals(f.marked,e.text.substring(e.highlightStart,e.highlightEnd))
                assertTrue(e.before.endsWith(f.before))
                val suffix=if(verb.auxiliary=="être"&&e.tense in listOf(3,5)) when(e.person){2->"e";3->"s";else->""} else f.after
                assertTrue(e.after.startsWith(suffix+" "))
                assertFalse(e.text.contains("("));assertTrue(e.text.endsWith("."))
                assertFalse(e.text.contains("il/elle"));assertFalse(e.text.contains("  "))
                if(e.tense in listOf(3,5))assertEquals(verb.participle,e.marked)
                examplesChecked++
            }
        }
        val regarder=OfflineDictionary.verbs.first{it.id=="regarder"}
        assertEquals(listOf("Je regarde un film.","Nous regardons les étoiles."),LearnExamples.forPanel(regarder,0).map{it.text})
        assertEquals(listOf("e","ons"),LearnExamples.forPanel(regarder,0).map{it.marked})
        val aller=OfflineDictionary.verbs.first{it.id=="aller"}
        assertEquals("Tu es allé au parc.",LearnExamples.forPanel(aller,3,true)[1].text)
        assertEquals("Tu étais allé au parc.",LearnExamples.forPanel(aller,5,true)[1].text)
        assertEquals(100,learnProgress(0,1));assertEquals(15,learnProgress(18,132))
        for(total in listOf(2,3,6,11,132,570)) {
            assertEquals(0,learnProgress(0,total));assertEquals(100,learnProgress(total-1,total))
            for(i in 1 until total-1){assertTrue(learnProgress(i,total) in 5..95);assertEquals(0,learnProgress(i,total)%5)}
        }
        File(out,"data.txt").writeText("PASS $examplesChecked paired/same-tense page examples across ${OfflineDictionary.verbs.size} verbs and all six tense presentations; progress endpoints/rounding/clamping.\n")
    }

    @Test fun displayedProgressAndAllNextTenses() {
        val current=mutableStateOf(VerbState(loaded=true,session=List(132){LearningItem("regarder",0,0)}))
        val language=mutableStateOf(AppLanguage.ENGLISH_UK)
        compose.activityRule.scenario.onActivity{it.setContent{LocalizedContent(language.value){TempleTheme{
            LearnScreen(current.value,language.value,{},{})
        }}}}
        for((index,expected) in listOf(0 to "0%",1 to "5%",18 to "15%",130 to "95%",131 to "100%",18 to "15%")) {
            compose.runOnIdle{current.value=current.value.copy(index=index)}
            compose.onNodeWithTag("learn_progress").assertTextEquals(expected)
            compose.onNodeWithText("${index+1}/132").assertDoesNotExist()
        }
        for(lang in AppLanguage.entries) for(tense in 0..5) {
            compose.runOnIdle{language.value=lang;current.value=VerbState(loaded=true,session=listOf(LearningItem("regarder",0,0),LearningItem("aller",tense,0)))}
            val expected=if(lang==AppLanguage.ENGLISH_UK)"Next: aller in "+listOf("present","imperfect","simple future","perfect tense","past historic","pluperfect")[tense]
                else "Suivant : aller "+listOf("au présent","à l’imparfait","au futur simple","au passé composé","au passé simple","au plus-que-parfait")[tense]
            compose.onNodeWithTag("learn_next").assertTextEquals(expected)
            compose.onNodeWithTag("learn_progress").assertTextEquals("0%")
            compose.runOnIdle{current.value=current.value.copy(session=listOf(LearningItem("regarder",0,0)))}
            compose.onNodeWithTag("learn_progress").assertTextEquals("100%")
            compose.onNodeWithTag("learn_next").assertTextEquals(if(lang==AppLanguage.ENGLISH_UK)"Now repeat →" else "Maintenant, répète →")
        }
    }

    @Test fun darkThemeAndContrast() {
        val current=mutableStateOf(Case(412,915,1f,AppLanguage.ENGLISH_UK,"regarder",0,true))
        compose.activityRule.scenario.onActivity{it.setContent{Frame(current.value)}}
        for(l in AppLanguage.entries)for((w,h) in listOf(412 to 915,915 to 412))for((v,t)in items) {
            val c=Case(w,h,1f,l,v,t,true)
            compose.runOnIdle{current.value=c};compose.waitForIdle();check(c,true)
            if(v=="prendre")capture("dark-$w-$l")
        }
        for(dark in listOf(false,true)) {
            var colors:androidx.compose.material3.ColorScheme?=null
            compose.activityRule.scenario.onActivity{it.setContent{TempleTheme(darkTheme=dark){colors=androidx.compose.material3.MaterialTheme.colorScheme}}}
            compose.waitForIdle()
            val scheme=colors!!
            val primary=learnPanelColor(scheme,false);val comparison=learnPanelColor(scheme,true)
            assertNotEquals(primary,comparison)
            assertTrue("Comparison tint remains perceptible dark=$dark",comparison.blue>primary.blue+.02f)
            val green=if(dark)androidx.compose.ui.graphics.Color(0xFF87E3AD) else androidx.compose.ui.graphics.Color(0xFF17683D)
            fun ratio(a:androidx.compose.ui.graphics.Color,b:androidx.compose.ui.graphics.Color):Float {
                val x=a.luminance();val y=b.luminance();return (maxOf(x,y)+.05f)/(minOf(x,y)+.05f)
            }
            for(bg in listOf(primary,comparison))for(ink in listOf(scheme.onSurface,green,scheme.primary))
                assertTrue("Readable contrast dark=$dark ratio=${ratio(ink,bg)}",ratio(ink,bg)>=4.5f)
        }
    }

    @Test fun landscapePixelRoundingHasNoScroll() {
        val current=mutableStateOf(Case(915,412,1f,AppLanguage.ENGLISH_UK,"regarder",0))
        compose.activityRule.scenario.onActivity{it.setContent{Frame(current.value)}}
        for(l in AppLanguage.entries)for((v,t)in items) {
            val c=Case(915,412,1f,l,v,t)
            compose.runOnIdle{current.value=c};compose.waitForIdle();check(c,true)
            val range=compose.onNodeWithTag("learn_panels").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].maxValue()
            assertEquals("Exactly zero scroll $c",0f,range,0f)
        }
        File(out,"pixel-rounding.txt").writeText("PASS 12 cases at 915x412; exactly zero scroll pixels.\n")
    }

}
