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

/** Real emulator window/insets; fixtures never write the owner's preferences. */
class Pass42NativeMatrixTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun screenshotsOfEveryStage() {
        val inst=InstrumentationRegistry.getInstrumentation()
        val args=InstrumentationRegistry.getArguments()
        val configuration=args.getString("matrix")?:"native"
        val language=if(args.getString("language")=="en")AppLanguage.ENGLISH_UK else AppLanguage.FRENCH
        val out=File(inst.targetContext.getExternalFilesDir(null),"pass42/$configuration/${args.getString("language")}").apply{mkdirs()}
        var scene by mutableStateOf("tenses")
        val selection=Selection(tenses=Choices.tenses.toSet(),types=Choices.types.toSet(),important=setOf("être","avoir","aller"))
        val items=listOf(LearningItem("reconnaître",5,0),LearningItem("jouer",0,0))
        val q=QuizQuestion.from(items[0]);val timeline=RepeatTimeline()
        val questions=UseSessions.create(selection,Random(42))
        compose.activityRule.scenario.onActivity{it.setContent{LocalizedContent(language){TempleTheme{key(scene){
            when(scene) {
                "tenses","types","important","er","families"->SelectionScreen(scene,VerbState(loaded=true,selection=selection),language,{},{},{},{},{})
                "welcome"->WelcomeScreen(selection,language,{},{})
                "activities"->ActivityScreen(language,{},{})
                "about"->AboutScreen(language,{},{})
                "learn-single"->LearnScreen(VerbState(loaded=true,session=listOf(items[1])),language,{},{})
                "learn-comparison","learn-examples"->LearnScreen(VerbState(loaded=true,session=items),language,{},{})
                "repeat-intro","repeat-complete"->RepeatScreen(RepeatState(session=items,elapsed=if(scene=="repeat-intro")2000 else timeline.waitingAt,playback=if(scene=="repeat-intro")RepeatPlayback.PLAYING else RepeatPlayback.WAITING),timeline,language,{},{})
                "conjugate-today","conjugate-tenses","conjugate-types"->ConjugateSelectionScreen(ConjugateSelection(original=ConjugateSelectionActions.candidates(LearningSequence.create(selection,random=Random(42)),selection),page=when(scene){"conjugate-today"->ConjugateChoicePage.TODAY;"conjugate-tenses"->ConjugateChoicePage.TENSE;else->ConjugateChoicePage.TYPE}),language,{},{},{},{},{},{},{})
                "conjugate","conjugate-short"->{val item=if(scene=="conjugate")items[0]else items[1];ConjugateScreen(QuizState(session=listOf(item)),QuizQuestion.from(item),language,{},{},{},{},{})}
                "conjugate-results","conjugate-results-expanded"->ConjugateResultsScreen(results41Fixture().let{if(scene.endsWith("expanded"))it.copy(resultToggles=setOf(1,3))else it},language,{},{})
                "use"->UseScreen(UseActions.start(questions),language,{},{},{},{},{})
                "use-typed"->UseScreen(UseActions.letter(UseActions.start(questions),questions.first().answer.first()),language,{},{},{},{},{})
                "use-results"->UseScreen(UseState(questions=questions,index=10,outcomes=listOf(1,-1,1,1,-1,1,1,-1,1,1)),language,{},{},{},{},{})
            }
        }}}}}
        val scenes=listOf("tenses","types","important","er","families","welcome","activities","about","learn-single","learn-comparison","learn-examples","repeat-intro","repeat-complete","conjugate-today","conjugate-tenses","conjugate-types","conjugate","conjugate-short","conjugate-results","conjugate-results-expanded","use","use-typed","use-results")
        for(name in scenes) {
            compose.runOnIdle{scene=name};compose.waitForIdle()
            if(name=="learn-examples") {
                val toggles=compose.onAllNodes(hasTestTag("learn_examples_toggle"))
                if(toggles.fetchSemanticsNodes().isNotEmpty())toggles[0].performClick()
            }
            compose.waitForIdle()
            Thread.sleep(500)
            val bitmap=compose.onRoot().captureToImage().asAndroidBitmap()
            File(out,"$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
            File(out,"$name-semantics.txt").writeText(compose.onRoot(useUnmergedTree=true).printToString())
        }
        File(out,"configuration.txt").writeText(compose.activity.resources.configuration.toString())
    }
}
