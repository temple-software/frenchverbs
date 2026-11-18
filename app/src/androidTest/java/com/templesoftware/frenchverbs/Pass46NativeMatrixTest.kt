package com.templesoftware.frenchverbs

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.templesoftware.frenchverbs.data.preferences.AppLanguage
import com.templesoftware.frenchverbs.domain.verbs.*
import com.templesoftware.frenchverbs.domain.quiz.*
import com.templesoftware.frenchverbs.domain.use.*
import com.templesoftware.frenchverbs.domain.repeat.*
import com.templesoftware.frenchverbs.ui.app.*
import com.templesoftware.frenchverbs.ui.verbs.*
import com.templesoftware.frenchverbs.ui.quiz.*
import com.templesoftware.frenchverbs.ui.use.*
import com.templesoftware.frenchverbs.ui.about.AboutScreen
import com.templesoftware.frenchverbs.ui.theme.TempleTheme
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.random.Random
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextLayoutResult
import org.json.JSONArray
import org.json.JSONObject

/** Real emulator window/insets; fixtures never write the owner's preferences. */
class Pass46NativeMatrixTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun screenshotsOfEveryStage() {
        val inst=InstrumentationRegistry.getInstrumentation()
        val args=InstrumentationRegistry.getArguments()
        val configuration=args.getString("matrix")?:"native"
        val language=if(args.getString("language")=="en")AppLanguage.ENGLISH_UK else AppLanguage.FRENCH
        val out=File(inst.targetContext.getExternalFilesDir(null),"pass46/$configuration/${args.getString("language")}").apply{mkdirs()}
        var scene by mutableStateOf("tenses")
        val selection=Selection(tenses=Choices.tenses.toSet(),types=Choices.types.toSet(),important=setOf("être","avoir","aller"))
        val items=listOf(LearningItem("reconnaître",5,0),LearningItem("jouer",0,0))
        val q=QuizQuestion.from(items[0]);val timeline=RepeatTimeline()
        val questions=UseSessions.create(selection,Random(42))
        val allQuiz=OfflineDictionary.verbs.flatMap{v->(0..5).map{QuizQuestion.from(LearningItem(v.id,it,0))}}
        val extremes=((0..5).map{t->allQuiz.filter{it.tense==t}.maxBy{it.boxCount}} + allQuiz.groupBy{it.keys.size}.values.map{list->list.maxBy{it.boxCount}}).distinctBy{it.verb.id to it.tense}
        val longUse=listOf("reconnaître","aller","avoir","être","partir","jouer").flatMap{v->(0..5).flatMap{t->(0..5).flatMap{person->UseSentences.candidates(LearningItem(v,t,0),person,false,false)}}}.maxBy{it.prefix.length+it.form.length+it.suffix.length}
        val useExtremes=(0..5).map{t->
            val verb=OfflineDictionary.verbs.maxBy{v->(0..5).maxOf{person->UseSentences.fullForm(v,t,person,true).filter(Char::isLetter).length}}
            val person=(0..5).maxBy{UseSentences.fullForm(verb,t,it,true).length}
            UseSentences.candidates(LearningItem(verb.id,t,0),person,true,false).first()
        }
        File(out,"extremes.txt").writeText(extremes.mapIndexed{i,x->"$i: ${x.verb.id}, tense ${x.tense}, ${x.boxCount} boxes, ${x.keys.size} keys"}.joinToString("\n")+"\nLongest sentence: ${longUse.prefix}${longUse.form}${longUse.suffix}")
        compose.activityRule.scenario.onActivity{it.setContent{LocalizedContent(language){TempleTheme{key(scene){
            if(scene.startsWith("conjugate-long-")) {
                val question=extremes[scene.substringAfterLast('-').toInt()]
                var state=QuizState(session=listOf(LearningItem(question.verb.id,question.tense,0)))
                repeat(2){question.rows[state.active].answer.forEach{c->state=QuizActions.letter(state,question,c)};state=QuizActions.submit(state,question)}
                question.rows[2].answer.take(2).forEach{c->state=QuizActions.letter(state,question,c)}
                ConjugateScreen(state,question,language,{},{},{},{},{})
            } else if(scene.startsWith("use-extreme-")) {
                val question=useExtremes[scene.substringAfterLast('-').toInt()]
                var state=UseActions.start(List(10){question})
                question.answer.take(question.answer.length/2).forEach{c->state=UseActions.letter(state,c)}
                UseScreen(state,language,{},{},{},{},{})
            } else when(scene) {
                "tenses","types","important","er","families"->SelectionScreen(scene,VerbState(loaded=true,selection=selection),language,{},{},{},{},{})
                "welcome"->WelcomeScreen(selection,language,{},{})
                "activities"->ActivityScreen(language,{},{})
                "about"->AboutScreen(language,{},{})
                "learn-single"->LearnScreen(VerbState(loaded=true,session=listOf(items[1])),language,{},{})
                "learn-comparison","learn-examples"->LearnScreen(VerbState(loaded=true,session=items),language,{},{})
                "repeat-intro","repeat-partial","repeat-complete"->RepeatScreen(RepeatState(session=items,elapsed=if(scene=="repeat-intro")2000 else if(scene=="repeat-partial")timeline.phases.first{it.name=="rowHold" && it.row==2}.start+100 else timeline.waitingAt,playback=if(scene=="repeat-intro")RepeatPlayback.PLAYING else RepeatPlayback.WAITING),timeline,language,{},{})
                "conjugate-today","conjugate-tenses","conjugate-types"->ConjugateSelectionScreen(ConjugateSelection(original=ConjugateSelectionActions.candidates(LearningSequence.create(selection,random=Random(42)),selection),page=when(scene){"conjugate-today"->ConjugateChoicePage.TODAY;"conjugate-tenses"->ConjugateChoicePage.TENSE;else->ConjugateChoicePage.TYPE}),language,{},{},{},{},{},{},{})
                "conjugate-wrong","conjugate-complete"->{val item=LearningItem("prendre",0,0);val question=QuizQuestion.from(item)
                    var state=QuizState(session=listOf(item,items[1]))
                    if(scene.endsWith("wrong")){state=QuizActions.letter(state,question,'x');state=state.copy(entries=listOf("x","","","","",""),feedbackRemaining=650)}
                    else repeat(6){question.rows[state.active].answer.forEach{c->state=QuizActions.letter(state,question,c)};state=QuizActions.submit(state,question)}
                    ConjugateScreen(state,question,language,{},{},{},{},{})}
                "conjugate-collapsed"->{val result=results41Fixture(6);val data=ConjugateResults(result);ConjugateResultsScreen(result.copy(resultToggles=data.groups.keys.filter{data.initiallyExpanded(it)}.toSet()),language,{},{})}
                "conjugate","conjugate-short"->{val item=if(scene=="conjugate")items[0]else items[1];ConjugateScreen(QuizState(session=listOf(item)),QuizQuestion.from(item),language,{},{},{},{},{})}
                "conjugate-results","conjugate-results-expanded"->ConjugateResultsScreen(results41Fixture().let{if(scene.endsWith("expanded"))it.copy(resultToggles=setOf(1,3))else it},language,{},{})
                "use-long"->UseScreen(UseActions.start(List(10){longUse}),language,{},{},{},{},{})
                "use-long-typed"->{var state=UseActions.start(List(10){longUse});longUse.answer.forEach{c->state=UseActions.letter(state,c)};UseScreen(state,language,{},{},{},{},{})}
                "use-exit"->UseScreen(UseActions.start(questions),language,{},{},{},{},{})
                "use"->UseScreen(UseActions.start(questions),language,{},{},{},{},{})
                "use-typed"->UseScreen(UseActions.letter(UseActions.start(questions),questions.first().answer.first()),language,{},{},{},{},{})
                "use-results"->UseScreen(UseState(questions=questions,index=10,outcomes=listOf(1,-1,1,1,-1,1,1,-1,1,1)),language,{},{},{},{},{})
            }
        }}}}}
        val scenes=listOf("tenses","types","important","er","families","welcome","activities","about","learn-single","learn-comparison","learn-examples","repeat-intro","repeat-complete","conjugate-today","conjugate-tenses","conjugate-types","conjugate","conjugate-short","conjugate-results","conjugate-results-expanded","use","use-typed","use-results")
        val extended=scenes+listOf("repeat-partial","conjugate-wrong","conjugate-complete","conjugate-collapsed","use-long","use-long-typed","use-exit")+extremes.indices.map{"conjugate-long-$it"}+useExtremes.indices.map{"use-extreme-$it"}
        val reports=JSONArray()
        val chosen=args.getString("screens")?.split(",") ?: extended
        for(name in chosen) {
            compose.runOnIdle{scene=name};compose.waitForIdle()
            if(name=="use-exit")compose.onNodeWithTag("use_back").performClick()
            if(name=="learn-examples") {
                val toggles=compose.onAllNodes(hasTestTag("learn_examples_toggle"))
                if(toggles.fetchSemanticsNodes().isNotEmpty())toggles[0].performClick()
            }
            compose.waitForIdle()
            Thread.sleep(100)
            val roots=compose.onAllNodes(isRoot()).fetchSemanticsNodes()
            val rootBounds=roots.first().boundsInRoot
            val nodes=compose.onAllNodes(SemanticsMatcher("all"){true},useUnmergedTree=true).fetchSemanticsNodes().filter { node ->
                generateSequence(node){it.parent}.none{it.config.getOrNull(SemanticsProperties.TestTag)=="selection_measurement"}
            }
            val issues=JSONArray();val ranges=JSONArray()
            val geometry=JSONArray();val permanentlyOutside=JSONArray()
            nodes.forEach { node ->
                if(roots.size==1 && (node.config.contains(SemanticsProperties.Text) || node.config.contains(SemanticsActions.OnClick))) {
                    val pos=node.positionInRoot;val size=node.size
                    if(pos.x < rootBounds.left-2 || pos.y < rootBounds.top-2 || pos.x+size.width>rootBounds.right+2 || pos.y+size.height>rootBounds.bottom+2)
                    {
                        issues.put("out of window ${node.config.getOrNull(SemanticsProperties.TestTag)}")
                        if(generateSequence(node.parent){it.parent}.none{(it.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)?.maxValue()?:0f)>1f})permanentlyOutside.put(node.config.getOrNull(SemanticsProperties.TestTag).orEmpty())
                    }
                }
                val tag=node.config.getOrNull(SemanticsProperties.TestTag).orEmpty()
                if(tag.isNotEmpty())geometry.put(JSONObject().put("tag",tag).put("x",node.positionInRoot.x).put("y",node.positionInRoot.y).put("width",node.size.width).put("height",node.size.height))
                listOf(SemanticsProperties.VerticalScrollAxisRange,SemanticsProperties.HorizontalScrollAxisRange).forEach { key ->
                    node.config.getOrNull(key)?.let { range ->
                        if(range.maxValue()>1f) {
                            ranges.put(JSONObject().put("tag",tag).put("axis",key.name).put("maximum",range.maxValue()))
                            issues.put("scroll overflow $tag ${key.name}: ${range.maxValue()}")
                        }
                    }
                }
                val textResults=mutableListOf<TextLayoutResult>()
                node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(textResults)
                textResults.forEach { if(it.didOverflowHeight || it.multiParagraph.didExceedMaxLines || (0 until it.lineCount).any{line->it.getLineRight(line)-it.getLineLeft(line)>it.size.width+2f})issues.put("text overflow $tag: ${it.layoutInput.text}") }
            }
            // A real swipe verifies that nonzero ranges represent hidden content, not decoration.
            var swipeMoved=false
            val scrollNodes=nodes.filter{(it.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)?.maxValue()?:0f)>1f}
            if(scrollNodes.isNotEmpty()) {
                val before=scrollNodes.sumOf{(it.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)?.value()?:0f).toDouble()}
                val target=scrollNodes.maxBy{it.boundsInRoot.width*it.boundsInRoot.height}
                compose.onNode(SemanticsMatcher("scroll id"){it.id==target.id},useUnmergedTree=true).performTouchInput{swipeUp()}
                compose.waitForIdle()
                val after=scrollNodes.sumOf{(it.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)?.value()?:0f).toDouble()}
                swipeMoved=after>before+1
                // Restore the initial view for reproducible screenshots.
                scrollNodes.forEach{it.config.getOrNull(SemanticsActions.ScrollBy)?.action?.invoke(0f,-100000f)}
                compose.waitForIdle()
            }
            val accessIssues=JSONArray();val reached=JSONArray()
            if(args.getString("accessibility")=="true") {
                val controls=nodes.filter{node ->
                    val tag=node.config.getOrNull(SemanticsProperties.TestTag).orEmpty()
                    node.config.contains(SemanticsActions.OnClick) && tag.isNotEmpty() &&
                        (roots.size==1 || tag in listOf("use_exit_confirm","use_exit_stay")) &&
                        (!tag.startsWith("quiz_box_") || tag.endsWith("_0"))
                }
                for(node in controls) {
                    val tag=node.config.getOrNull(SemanticsProperties.TestTag).orEmpty()
                    try {
                        val target=compose.onNode(SemanticsMatcher("reachable id"){it.id==node.id},useUnmergedTree=true)
                        val ancestor=generateSequence(node.parent){it.parent}.any{(it.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)?.maxValue()?:0f)>0f}
                        if(ancestor)target.performScrollTo()
                        compose.waitForIdle()
                        target.assertIsDisplayed()
                        val fresh=target.fetchSemanticsNode()
                        if(fresh.boundsInRoot.width<minOf(fresh.size.width.toFloat(),40f) || fresh.boundsInRoot.height<minOf(fresh.size.height.toFloat(),40f))accessIssues.put("Insufficient visible target: $tag")
                        else reached.put(tag)
                    } catch(e:Throwable) {accessIssues.put("Unreachable $tag: ${e.message?.take(160)}")}
                }
                nodes.forEach{it.config.getOrNull(SemanticsActions.ScrollBy)?.action?.invoke(0f,-100000f)}
                compose.waitForIdle()
            }
            reports.put(JSONObject().put("screen",name).put("issues",issues).put("scrollRanges",ranges).put("swipeRevealedContent",swipeMoved).put("geometry",geometry).put("permanentlyOutside",permanentlyOutside).put("accessibilityChecked",args.getString("accessibility")=="true").put("accessibilityIssues",accessIssues).put("reachedControls",reached))
            inst.waitForIdleSync()
            Thread.sleep(350)
            val bitmap=inst.uiAutomation.takeScreenshot()
            File(out,"$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
            File(out,"$name-semantics.txt").writeText(compose.onAllNodes(isRoot(),useUnmergedTree=true).printToString())
        }
        File(out,"layout.json").writeText(reports.toString(2))
        File(out,"configuration.txt").writeText(compose.activity.resources.configuration.toString())
    }
}
