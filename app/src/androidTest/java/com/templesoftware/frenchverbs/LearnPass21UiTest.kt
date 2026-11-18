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

class LearnPass21UiTest {
    val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val rotationRules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    private var nextCalls=0
    private var backCalls=0
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private val out get()=File(inst.targetContext.getExternalFilesDir(null),"pass21").apply{mkdirs()}
    private data class Case(val w:Int,val h:Int,val font:Float,val lang:AppLanguage,val verb:String,val tense:Int,val dark:Boolean=false)
    private fun state(c:Case)=VerbState(loaded=true,session=listOf(LearningItem("être",0,1),LearningItem(c.verb,c.tense,1)),index=0)
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
                Box(Modifier.requiredSize(c.w.dp,(c.h-76).dp)){LocalizedContent(c.lang){TempleTheme(darkTheme=c.dark){LearnScreen(state(c),c.lang,{}, { nextCalls++ }, { backCalls++ })}}}
            }
        }
    }
    @Test fun navigationWidthsLanguagesAndDisabledRepeat() {
        val current=mutableStateOf(Case(412,915,1f,AppLanguage.ENGLISH_UK,"connaître",5))
        compose.activityRule.scenario.onActivity{it.setContent{Frame(current.value)}}
        val log=File(out,"navigation-matrix.txt");log.writeText("")
        var prefixes=0;var omitted=0
        for((w,h) in listOf(320 to 640,360 to 800,412 to 915,640 to 320,915 to 412,800 to 1280,1280 to 800))
        for(lang in listOf(AppLanguage.ENGLISH_UK,AppLanguage.FRENCH))for(tense in listOf(0,1,5)) {
            val c=Case(w,h,1f,lang,"connaître",tense)
            compose.runOnIdle{current.value=c};compose.waitForIdle()
            for(tag in listOf("learn_previous","learn_repeat","learn_next","learn_next_arrow"))compose.onNodeWithTag(tag,useUnmergedTree=true).assertIsDisplayed()
            compose.onNodeWithTag("learn_previous").assertIsNotEnabled()
            compose.onNodeWithTag("learn_repeat").assertIsNotEnabled().assertTextEquals(if(lang==AppLanguage.ENGLISH_UK)"Repeat" else "Répéter")
            val before=nextCalls
            compose.onNodeWithTag("learn_repeat").performTouchInput{click()}
            compose.runOnIdle{assertEquals(before,nextCalls);assertEquals(0,backCalls)}
            val result=layouts("learn_next_label").single()
            assertFalse("$c overflow text=${result.layoutInput.text} size=${result.size} constraints=${result.layoutInput.constraints} lines=${result.lineCount} height=${result.didOverflowHeight} width=${result.didOverflowWidth}",result.hasVisualOverflow)
            val label=rect("learn_next_label");val arrow=rect("learn_next_arrow")
            val back=rect("learn_previous");val repeat=rect("learn_repeat");val next=rect("learn_next")
            assertTrue("$c arrow overlap",label.right<arrow.left)
            assertTrue(arrow.right<=next.right);assertTrue(back.right<repeat.left);assertTrue(repeat.right<next.left)
            assertEquals(back.height,repeat.height,1f);assertEquals(back.height,next.height,1f)
            val text=result.layoutInput.text.text
            if(text.startsWith("Next:")||text.startsWith("Suivant"))prefixes++ else omitted++
            compose.onNodeWithTag("learn_next").performClick()
            compose.runOnIdle{assertEquals(before+1,nextCalls)}
            log.appendText("PASS $c label=${text.replace('\n','|')} lines=${result.lineCount}; disabled Repeat touch inactive; Next callback; equal heights, visible arrow and no overlap/overflow.\n")
        }
        assertTrue(prefixes>0);assertTrue(omitted>0)
    }

    @Test fun backAndFinalNextKeepCallbacks() {
        val index=mutableStateOf(1);var completed=0
        compose.activityRule.scenario.onActivity{it.setContent{LocalizedContent(AppLanguage.ENGLISH_UK){TempleTheme{
            LearnNavigationBar(VerbState(loaded=true,session=listOf(LearningItem("être",0,1),LearningItem("avoir",1,1)),index=index.value),true,false,
                {index.value--},{completed++})
        }}}}
        compose.onNodeWithTag("learn_previous").assertIsEnabled()
        compose.onNodeWithTag("learn_next_label",true).assertTextEquals("Now repeat")
        compose.onNodeWithTag("learn_repeat").assertIsNotEnabled().performTouchInput{click()}
        compose.runOnIdle{assertEquals(1,index.value);assertEquals(0,completed)}
        compose.onNodeWithTag("learn_next").performClick()
        compose.runOnIdle{assertEquals(1,completed)}
        compose.onNodeWithTag("learn_previous").performClick()
        compose.runOnIdle{assertEquals(0,index.value)}
        compose.onNodeWithTag("learn_previous").assertIsNotEnabled()
    }
    @Test fun measuredPrefixAndAccessibility() {
        compose.waitUntil(10000){OfflineDictionary.verbs.isNotEmpty()}
        val current=mutableStateOf(Case(412,915,1.3f,AppLanguage.FRENCH,"connaître",5))
        compose.activityRule.scenario.onActivity{it.setContent{Frame(current.value)}}
        val log=File(out,"prefix-accessibility.txt");log.writeText("")
        for(lang in listOf(AppLanguage.ENGLISH_UK,AppLanguage.FRENCH))for(w in listOf(360,412,600,800)) {
            compose.runOnIdle{current.value=Case(w,1280,1.3f,lang,"connaître",5)}
            val r=layouts("learn_next_label").single()
            assertFalse(r.hasVisualOverflow)
            val prefix=if(lang==AppLanguage.ENGLISH_UK)"Next: " else "Suivant : "
            val shown=r.layoutInput.text
            val prefixed=shown.text.startsWith(prefix)
            val full=if(prefixed)shown else androidx.compose.ui.text.AnnotatedString(
                prefix+shown.text.replace('\n',' '),shown.spanStyles.map{it.copy(start=it.start+prefix.length,end=it.end+prefix.length)})
            val fullWidth=androidx.compose.ui.text.TextMeasurer(r.layoutInput.fontFamilyResolver,r.layoutInput.density,r.layoutInput.layoutDirection)
                .measure(full,r.layoutInput.style,softWrap=false).size.width
            assertTrue("actual measured width $w $lang full=$fullWidth available=${r.layoutInput.constraints.maxWidth} prefix=$prefixed", if(prefixed) fullWidth<=r.layoutInput.constraints.maxWidth+2 else fullWidth>r.layoutInput.constraints.maxWidth-2)
            assertTrue(rect("learn_next_label").right<rect("learn_next_arrow").left)
            compose.onNodeWithTag("learn_repeat").assertIsDisplayed().assertIsNotEnabled()
            log.appendText("PASS width=$w portrait font=1.3 language=$lang fullWidth=$fullWidth available=${r.layoutInput.constraints.maxWidth} prefix=$prefixed lines=${r.lineCount} no overflow.\n")
        }
    }

}

