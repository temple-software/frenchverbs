package com.templesoftware.frenchverbs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.templesoftware.frenchverbs.data.preferences.AppLanguage
import com.templesoftware.frenchverbs.domain.quiz.*
import com.templesoftware.frenchverbs.domain.verbs.LearningItem
import com.templesoftware.frenchverbs.ui.app.*
import com.templesoftware.frenchverbs.ui.quiz.ConjugateActivityContent
import com.templesoftware.frenchverbs.ui.theme.TempleTheme

/** Debug-only completed session; never reads or changes owner selections/preferences. */
fun results40Fixture():QuizState {
    val items=listOf(LearningItem("jouer",0,0),LearningItem("prendre",1,0),LearningItem("aller",3,0))
    var s=QuizState(session=items)
    items.forEachIndexed { i,item ->
        val q=QuizQuestion.from(item)
        s=s.copy(index=i,entries=q.rows.map{it.answer},locked=(0..5).toSet(),active=-1,
            wrongLines=s.wrongLines+(0 until i+2).map{i*6+it})
        s=recordConjugateCompletion(s,q)
    }
    return s
}
fun results41Fixture(tenseCount:Int=4):QuizState {
    val items=(0 until tenseCount).map{LearningItem("aller",it,0)}
    var s=QuizState(session=items)
    items.forEachIndexed { i,item ->
        val q=QuizQuestion.from(item)
        val wrongCount=listOf(2,4,1,5,3,6)[i]
        s=s.copy(index=i,entries=q.rows.map{it.answer},locked=(0..5).toSet(),active=-1,
            wrongLines=s.wrongLines+(0 until wrongCount).map{i*6+it})
        s=recordConjugateCompletion(s,q)
    }
    return s
}
class ConjugateResultsPreviewActivity:ComponentActivity() {
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState);enableEdgeToEdge()
        val model=ViewModelProvider(this,viewModelFactory { initializer {
            ConjugateViewModel(createSavedStateHandle().apply {
                if(!contains("quizSession")) {
                    val s=if(intent.getBooleanExtra("grid",false))results41Fixture()else results40Fixture()
                    set("quizSession",ArrayList(s.session.map{it.encode()}));set("quizIndex",s.index)
                    set("quizEntries",ArrayList(s.entries));set("quizLocked",ArrayList(s.locked));set("quizActive",-1)
                    set("quizWrongLines",ArrayList(s.wrongLines));set("quizCompletedLines",ArrayList(s.completedLines.map{it.encode()}))
                }
            })
        } })[ConjugateViewModel::class.java]
        val language=if(intent.getStringExtra("language")=="en")AppLanguage.ENGLISH_UK else AppLanguage.FRENCH
        setContent { LocalizedContent(language){TempleTheme{ConjugateActivityContent(model,language,{finish()})}} }
    }
}
