package com.templesoftware.frenchverbs

import android.content.res.Configuration
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.platform.app.InstrumentationRegistry
import com.templesoftware.frenchverbs.data.preferences.*
import com.templesoftware.frenchverbs.domain.repeat.*
import com.templesoftware.frenchverbs.domain.verbs.*
import com.templesoftware.frenchverbs.ui.app.*
import com.templesoftware.frenchverbs.ui.navigation.AppNavHost
import com.templesoftware.frenchverbs.ui.theme.TempleTheme
import com.templesoftware.frenchverbs.ui.verbs.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.random.Random
import kotlin.math.roundToInt

class RepeatCountdownTest {
    val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val rules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    private val out get()=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"repeat-countdown").apply{mkdirs()}
    private fun capture(name:String) {
        val b=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(out,"$name.png").outputStream().use{b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};b.recycle()
    }
    private data class Case(val w:Int,val h:Int,val lang:AppLanguage,val font:Float=1f)
    @Composable private fun Frame(c:Case, s:RepeatState, timeline:RepeatTimeline) {
        BoxWithConstraints(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            val host=LocalDensity.current
            val d=minOf(with(host){maxWidth.toPx()}/c.w,with(host){maxHeight.toPx()}/c.h)
            val config=Configuration(LocalConfiguration.current).apply{screenWidthDp=c.w;screenHeightDp=c.h;fontScale=c.font;densityDpi=(d*160).roundToInt();orientation=if(c.w>c.h)2 else 1}
            CompositionLocalProvider(LocalDensity provides Density(d,c.font),LocalConfiguration provides config) {
                Box(Modifier.requiredSize(c.w.dp,(c.h-76).dp)){LocalizedContent(c.lang){TempleTheme{RepeatScreen(s,timeline,c.lang,{},{})}}}
            }
        }
    }
    @Test fun renderedSpacingTypographyAndStablePhases() {
        val timeline=RepeatTimeline()
        val c=mutableStateOf(Case(412,915,AppLanguage.ENGLISH_UK))
        val s=mutableStateOf(RepeatState(listOf(LearningItem("aller",5,0),LearningItem("connaître",5,0)),elapsed=timeline.waitingAt,playback=RepeatPlayback.WAITING))
        compose.activityRule.scenario.onActivity{it.setContent{Frame(c.value,s.value,timeline)}}
        val cases=AppLanguage.entries.flatMap { lang -> listOf(412 to 915,915 to 412,800 to 360,800 to 1280,1280 to 800).map{Case(it.first,it.second,lang)}+listOf(Case(412,915,lang,1.3f),Case(800,360,lang,1.3f)) }
        fun node(tag:String)=compose.onNodeWithTag(tag,true).fetchSemanticsNode()
        fun layout(tag:String):androidx.compose.ui.text.TextLayoutResult {
            val r=mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            compose.onNodeWithTag(tag,true).performSemanticsAction(SemanticsActions.GetTextLayoutResult){it(r)}
            return r.single()
        }
        val log=File(out,"geometry.txt");log.writeText("")
        for(case in cases)for(tense in listOf(2,5)) {
            compose.runOnIdle{c.value=case;s.value=s.value.copy(session=listOf(LearningItem("aller",tense,0),LearningItem("connaître",5,0)),elapsed=timeline.waitingAt,playback=RepeatPlayback.WAITING)}
            compose.waitForIdle()
            val viewport=node("repeat_viewport");val heading=node("repeat_tense");val group=node("repeat_rows");val nav=node("repeat_navigation")
            val d=viewport.size.width.toFloat()/case.w
            val rows=(0..5).map{node("repeat_row_$it")}
            assertEquals(viewport.positionInRoot.x+viewport.size.width/2f,group.positionInRoot.x+group.size.width/2f,2f)
            rows.zipWithNext().forEach{(a,b)->assertTrue(a.positionInRoot.y+a.size.height<=b.positionInRoot.y+1)}
            for(i in 0..5){assertEquals(OfflineDictionary.verbs.first{it.id=="aller"}.tenses[tense].forms[i].text,layout("repeat_row_$i").layoutInput.text.text);assertFalse("overflow $case tense=$tense row=$i size=${layout("repeat_row_$i").size} paragraph=${layout("repeat_row_$i").multiParagraph.height} width=${layout("repeat_row_$i").multiParagraph.width}",layout("repeat_row_$i").hasVisualOverflow)}
            assertFalse("heading $case tense=$tense size=${layout("repeat_tense").size} paragraph=${layout("repeat_tense").multiParagraph.height} width=${layout("repeat_tense").multiParagraph.width}",layout("repeat_tense").hasVisualOverflow)
            assertTrue(layout("repeat_tense").layoutInput.text.text.contains(if(tense==2)"Le futur simple" else "Le plus-que-parfait"))
            val range=node("repeat_scroll").config[SemanticsProperties.VerticalScrollAxisRange].maxValue()
            if(case.font==1f && case.h!=360)assertEquals("scroll $case tense=$tense",0f,range,1f)
            val font=layout("repeat_row_0").layoutInput.style.fontSize.value
            val headingFont=layout("repeat_tense").layoutInput.style.fontSize.value
            if(case.w>case.h) {
                val clearance=if(viewport.size.height/d<240f)8f else minOf(viewport.size.height/d*.04f,24f)+4f
                assertEquals(clearance*d,node("repeat_verb").positionInRoot.y-viewport.positionInRoot.y,2f)
                assertTrue(headingFont>=18f)
                assertTrue(font>=19f)
                val prompt=node("repeat_instruction")
                val regionTop=heading.positionInRoot.y+heading.size.height+prompt.size.height+8*d
                val contentBottom=viewport.positionInRoot.y+viewport.size.height+range-4*d
                assertEquals("centred $case",(regionTop+contentBottom)/2,group.positionInRoot.y+group.size.height/2f,2f)
            } else {
                assertEquals(if(minOf(case.w,case.h)>=600)30f else 25f,font)
                assertEquals(22f,headingFont)
                val prompt=node("repeat_instruction")
                assertEquals("portrait +12dp",heading.positionInRoot.y+heading.size.height+prompt.size.height+42*d,group.positionInRoot.y,3f)
            }
            if(range==0f)assertTrue(group.positionInRoot.y+group.size.height<=nav.positionInRoot.y+1)
            val positions=rows.map{it.positionInRoot};val button=nav.positionInRoot
            for(elapsed in timeline.phases.filter{it.name.startsWith("instruction") || (it.name=="subject" && it.row==0)}.map{it.start+it.duration/2}+listOf(timeline.waitingAt)) {
                compose.runOnIdle{s.value=s.value.copy(elapsed=elapsed,playback=if(elapsed==timeline.waitingAt)RepeatPlayback.WAITING else RepeatPlayback.PLAYING)}
                assertEquals(positions,(0..5).map{node("repeat_row_$it").positionInRoot})
                assertEquals(button,node("repeat_navigation").positionInRoot)
                val prompt=node("repeat_instruction")
                val third=node("repeat_row_2")
                assertEquals(third.positionInRoot.y+third.size.height/2f,prompt.positionInRoot.y+prompt.size.height/2f,2f)
                compose.onNodeWithTag("repeat_title",true).assertTextEquals(if(case.lang==AppLanguage.ENGLISH_UK)"Repeat" else "Répéter")
                if(timeline.instructionAlpha(elapsed)>0f)compose.onNodeWithTag("repeat_instruction",true).assertTextContains("... ${timeline.countdown(elapsed)}",substring=true)
            }
            compose.waitForIdle();capture("${case.w}-${case.h}-${case.lang}-${case.font}-$tense")
            log.appendText("PASS $case tense=$tense headingSp=$headingFont rowsSp=$font headingY=${heading.positionInRoot.y} groupY=${group.positionInRoot.y} groupHeight=${group.size.height} buttonY=${nav.positionInRoot.y} scroll=$range stable countdown and reveal phases\n")
        }
    }
    @Test fun accessibilityScrollReachesLastRow() {
        val timeline=RepeatTimeline()
        val language=mutableStateOf(AppLanguage.ENGLISH_UK)
        val state=RepeatState(listOf(LearningItem("aller",5,0),LearningItem("connaître",5,0)),elapsed=timeline.waitingAt,playback=RepeatPlayback.WAITING)
        compose.activityRule.scenario.onActivity { it.setContent { Frame(Case(800,360,language.value,1.3f),state,timeline) } }
        for(lang in AppLanguage.entries) {
            compose.runOnIdle{language.value=lang}
            compose.onNodeWithTag("repeat_row_5",true).performScrollTo().assertIsDisplayed()
            val last=compose.onNodeWithTag("repeat_row_5",true).fetchSemanticsNode()
            val nav=compose.onNodeWithTag("repeat_navigation",true).fetchSemanticsNode()
            assertTrue(last.positionInRoot.y+last.size.height<=nav.positionInRoot.y)
            compose.onNodeWithTag("repeat_navigation",true).assertIsDisplayed()
            capture("accessibility-scrolled-$lang")
        }
    }
    @Test fun countdownPresentation() {
        val timeline=RepeatTimeline()
        val case=mutableStateOf(Case(412,915,AppLanguage.ENGLISH_UK))
        val state=mutableStateOf(RepeatState(listOf(LearningItem("aller",2,0),LearningItem("aller",5,0))))
        compose.activityRule.scenario.onActivity { it.setContent { Frame(case.value,state.value,timeline) } }
        for(lang in AppLanguage.entries)for(landscape in listOf(false,true)) {
            compose.runOnIdle{case.value=if(landscape)Case(915,412,lang) else Case(412,915,lang)}
            for(phase in timeline.phases.filter{it.name.startsWith("instructionHold") || it.name in listOf("instructionOut1","instructionIn2","instructionOut2","instructionIn3") || (it.name=="subject" && it.row==0)}) {
                compose.runOnIdle{state.value=state.value.copy(elapsed=phase.start+phase.duration/2)}
                val layouts=mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                compose.onNodeWithTag("repeat_verb",true).performSemanticsAction(SemanticsActions.GetTextLayoutResult){it(layouts)}
                assertFalse(layouts.single().hasVisualOverflow)
                assertEquals(if(lang==AppLanguage.ENGLISH_UK)"aller / to go" else "aller",layouts.single().layoutInput.text.text)
                assertTrue(layouts.single().layoutInput.style.fontSize.value>=24f)
                val verb=compose.onNodeWithTag("repeat_verb",true).fetchSemanticsNode()
                val tense=compose.onNodeWithTag("repeat_tense",true).fetchSemanticsNode()
                val prompt=compose.onNodeWithTag("repeat_instruction",true).fetchSemanticsNode()
                assertTrue(verb.positionInRoot.y+verb.size.height<=tense.positionInRoot.y)
                assertTrue(tense.positionInRoot.y+tense.size.height<=prompt.positionInRoot.y)
                capture("intro-$lang-$landscape-${phase.name}")
            }
        }
    }
    @Test fun buttonAppearsImmediatelyAfterLastVerb() {
        val timeline=RepeatTimeline()
        val model=RepeatViewModel(SavedStateHandle(),timeline)
        model.startSingle(LearningItem("aller",2,0))
        val saved=SavedStateHandle(mapOf("repeatSession" to arrayListOf(LearningItem("aller",2,0).encode(),LearningItem("aller",5,0).encode())))
        val full=RepeatViewModel(saved,timeline)
        compose.activityRule.scenario.onActivity { it.setContent {
            val state by full.state.collectAsState()
            Frame(Case(412,915,AppLanguage.ENGLISH_UK),state,timeline)
        } }
        val end=timeline.phases.last{it.name=="verb"}.end
        compose.runOnIdle{full.tick(end-1)}
        assertEquals(RepeatPlayback.PLAYING,full.state.value.playback)
        compose.onNodeWithTag("repeat_next").assertDoesNotExist()
        compose.runOnIdle{full.tick(1)}
        assertEquals(RepeatPlayback.WAITING,full.state.value.playback)
        compose.onNodeWithTag("repeat_next").assertIsDisplayed().assertIsEnabled()
        compose.runOnIdle{assertTrue(full.next());full.tick(timeline.timing.finalFadeOut)}
        assertEquals(1,full.state.value.index)
        model.tick(end)
        assertFalse(model.state.value.complete)
        model.tick(timeline.duration-end)
        assertTrue(model.state.value.complete)
    }
}



