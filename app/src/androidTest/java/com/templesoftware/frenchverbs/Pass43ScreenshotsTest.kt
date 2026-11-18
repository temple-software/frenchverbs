package com.templesoftware.frenchverbs

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
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
import org.junit.Assert.*
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import kotlin.random.Random

/** Publication fixtures: production renderers/data/actions, no release hooks or owner writes. */
class Pass43ScreenshotsTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun quiz(item:LearningItem):QuizState {
        val q=QuizQuestion.from(item);var s=QuizState(session=listOf(item,LearningItem("finir",2,0)))
        repeat(2){ q.rows[s.active].answer.forEach{c->s=QuizActions.letter(s,q,c)};s=QuizActions.submit(s,q) }
        q.rows[2].answer.take((q.rows[2].answer.length/2).coerceAtLeast(2).coerceAtMost(q.rows[2].answer.length-1)).forEach{s=QuizActions.letter(s,q,it)}
        check(s.locked==setOf(0,1) && s.entries[2].isNotEmpty() && s.entries[2]!=q.rows[2].answer)
        return s
    }
    private fun results():QuizState {
        var s=QuizState(session=listOf(LearningItem("jouer",0,0),LearningItem("finir",2,0)))
        repeat(2){ question ->
            val q=QuizQuestion.from(s.item!!)
            repeat(6){row->
                if((question==0 && row==3)||(question==1 && row==4)) {
                    s=QuizActions.letter(s,q,q.keys.first{it!=q.rows[row].answer.first()})
                    s=QuizActions.submit(s,q);s=QuizActions.tick(s,650)
                }
                q.rows[row].answer.forEach{s=QuizActions.letter(s,q,it)};s=QuizActions.submit(s,q)
            }
            s=recordConjugateCompletion(s,q)
            if(!s.last)s=QuizActions.nextQuestion(s)
        }
        check(ConjugateResults(s).score.correctLines==10 && ConjugateResults(s).score.stars==4)
        return s
    }
    private fun questions():List<UseQuestion> {
        val pairs=listOf("jouer" to 0,"finir" to 2,"avoir" to 0,"aller" to 2,"partir" to 2,"jouer" to 1,"finir" to 0,"avoir" to 3,"partir" to 0,"jouer" to 2)
        return pairs.mapIndexed{i,(v,t)->
            val person=listOf(0,1,2,4,3,0,4,2,5,3)[i]
            val options=UseSentences.candidates(LearningItem(v,t,0),person,false,false)
            val q=if(i==4)options.firstOrNull{"train" in it.suffix}?:options.first() else options.first()
            q.copy(chosenBoxCount=useBoxCapacity(q.answer.length,q.quiz.boxCount,Random(430+i)))
        }
    }
    private fun advance(s0:UseState,wrong:Boolean):UseState {
        var s=s0;val q=s.current!!
        if(wrong){s=UseActions.letter(s,q.keys.first{it!=q.answer.first()});s=UseActions.submit(s);s=UseActions.tick(s,650)}
        q.answer.forEach{s=UseActions.letter(s,it)};s=UseActions.submit(s);return UseActions.tick(s,800)
    }
    private fun partial(qs:List<UseQuestion>,index:Int):UseState {
        var s=UseActions.start(qs)
        repeat(index){s=advance(s,it==1)}
        s.current!!.answer.take(if(index==4)4 else 3).forEach{s=UseActions.letter(s,it)}
        check(s.index==index && s.input.entries[s.current!!.person].isNotEmpty())
        return s
    }
    @Test fun capturePublicationCollection() {
        val inst=InstrumentationRegistry.getInstrumentation()
        check(inst.targetContext.packageName.endsWith(".audit43"))
        val out=File(inst.targetContext.getExternalFilesDir(null),"pass43").apply{mkdirs()}
        val selection=Selection(tenses=setOf("present","imperfect","future"),types=setOf("important","er","irregular"),important=setOf("être","avoir","aller"),er=setOf("standard","ger","cer"),families=setOf("partir","mettre"))
        val single=listOf(LearningItem("jouer",0,0),LearningItem("aller",0,0))
        val comparison=listOf(LearningItem("jouer",1,0),LearningItem("jouer",0,0))
        val q1=quiz(LearningItem("prendre",0,0));val q2=quiz(LearningItem("jouer",3,0))
        val result=results();val qs=questions();val u1=partial(qs,4);val u2=partial(qs,5)
        var ur=UseActions.start(qs);repeat(10){ur=advance(ur,it==1||it==7)}
        check(ur.complete && ur.score==8 && useStars(ur.score)==4)
        val timeline=RepeatTimeline()
        var scene by mutableStateOf("")
        var language by mutableStateOf(AppLanguage.ENGLISH_UK)
        val names=listOf("01-select-tenses","02-verb-types","03-important-verbs","04-regular-er","05-irregular-families","06-welcome-back","07-practice-french","08-about","09-learn-conjugation","10-compare-tenses","11-learn-examples","12-repeat-introduction","13-repeat-progress","14-repeat-complete","15-concentrate-on","16-quiz-tense-selection","17-quiz-verb-type","18-conjuguer-quiz","19-conjuguer-perfect","20-utiliser-future","21-utiliser-imperfect","22-conjuguer-results","23-utiliser-results","24-leave-quiz")
        // Restore supported Learn saved state for deterministic bank choices in both languages.
        val example=LearnExampleState().apply {
            itemKey="${single.hashCode()}:0:${single[0].encode()}";active=true
            val v=OfflineDictionary.verbs.first{it.id=="jouer"}
            choices=(0..5).map{p->p%SentenceBank.candidates(v,0,p).size}
        }.encode()
        compose.activityRule.scenario.onActivity{it.setContent{LocalizedContent(language){TempleTheme{key(scene){
            when(scene.substringBefore('-').toIntOrNull()) {
                in 1..5->{val section=listOf("tenses","types","important","er","families")[scene.take(2).toInt()-1];SelectionScreen(section,VerbState(loaded=true,selection=selection),language,{},{},{},{},{})}
                6->WelcomeScreen(selection,language,{},{})
                7->ActivityScreen(language,{},{})
                8->AboutScreen(language,{},{})
                9->LearnScreen(VerbState(loaded=true,session=single),language,{},{},onRepeat={})
                10->LearnScreen(VerbState(loaded=true,session=comparison),language,{},{},onRepeat={})
                11->{val base=remember{SaveableStateRegistry(null){true}};val registry=remember{object:SaveableStateRegistry by base {var supplied=false;override fun consumeRestored(key:String):Any?=if(!supplied){supplied=true;example}else base.consumeRestored(key)}}
                    CompositionLocalProvider(LocalSaveableStateRegistry provides registry){LearnScreen(VerbState(loaded=true,session=single),language,{},{},onRepeat={})}}
                in 12..14->{val n=scene.take(2).toInt();val elapsed=when(n){12->2000L;13->timeline.phases.first{it.name=="rowHold"&&it.row==2}.start+100;else->timeline.waitingAt}
                    RepeatScreen(RepeatState(session=single,elapsed=elapsed,playback=if(n==14)RepeatPlayback.WAITING else RepeatPlayback.PLAYING),timeline,language,{},{})}
                in 15..17->ConjugateSelectionScreen(ConjugateSelection(original=ConjugateSelectionActions.candidates(LearningSequence.create(selection,random=Random(43)),selection),page=when(scene.take(2).toInt()){15->ConjugateChoicePage.TODAY;16->ConjugateChoicePage.TENSE;else->ConjugateChoicePage.TYPE}),language,{},{},{},{},{},{},{})
                18,19->{val s=if(scene.startsWith("18"))q1 else q2;ConjugateScreen(s,QuizQuestion.from(s.item!!),language,{},{},{},{},{})}
                20,21,24->UseScreen(if(scene.startsWith("21"))u2 else u1,language,{},{},{},{},{})
                22->ConjugateResultsScreen(result,language,{},{})
                23->UseScreen(ur,language,{},{},{},{},{})
            }
        }}}}}
        compose.mainClock.autoAdvance=false
        val playOnly=InstrumentationRegistry.getArguments().getString("playOnly")=="true"
        val captureNames=if(playOnly)names.filter{it.take(2) in setOf("01","07","09","10","14","18","20","22")}else names
        for(name in captureNames)for(lang in AppLanguage.entries) {
            compose.runOnIdle{scene=name;language=lang}
            compose.mainClock.advanceTimeBy(64);compose.waitForIdle()
            if(name.startsWith("24") && lang==AppLanguage.entries.first())compose.onNodeWithTag("use_back").performClick()
            compose.mainClock.advanceTimeBy(64);compose.waitForIdle()
            Thread.sleep(350)
            val folder=File(out,if(lang==AppLanguage.FRENCH)"fr-FR"else "en-GB").apply{mkdirs()}
            val bitmap=inst.uiAutomation.takeScreenshot()
            check(bitmap.width==1080 && bitmap.height==1920)
            File(folder,"$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
            File(folder,"$name.txt").writeText(compose.onAllNodes(isRoot(),useUnmergedTree=true).printToString())
        }
        File(out,"scenario-evidence.json").writeText(JSONObject().put("useQuestions",JSONArray(qs.map{it.encode()})).put("useProgress",JSONArray(u1.outcomes)).put("useResults",JSONArray(ur.outcomes)).put("useScore",ur.score).put("conjugateCorrect",ConjugateResults(result).score.correctLines).put("conjugateTotal",12).put("conjugateStars",4).put("learnSavedState",example).toString(2))
    }
}
