package com.templesoftware.frenchverbs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.templesoftware.frenchverbs.data.preferences.AppLanguage
import com.templesoftware.frenchverbs.domain.verbs.LearningItem
import com.templesoftware.frenchverbs.ui.app.*
import com.templesoftware.frenchverbs.ui.quiz.*
import com.templesoftware.frenchverbs.ui.theme.TempleTheme

/** Debug-only exact question for visual review, without touching persistent selections. */
class ConjugateInputPreviewActivity:ComponentActivity() {
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState);enableEdgeToEdge()
        val model=ViewModelProvider(this,viewModelFactory { initializer {
            ConjugateViewModel(createSavedStateHandle().apply {
                if(!contains("quizSession")) {
                    val alignment=intent.getBooleanExtra("alignment",false)
                    set("quizSession",arrayListOf(LearningItem(if(alignment)"jouer"else "pouvoir",if(alignment)0 else 1,0).encode()))
                    set("quizEntries",arrayListOf(if(alignment)"e"else "pouvais","","","","",""))
                    if(intent.getBooleanExtra("completion",false)) {
                        set("quizSession",arrayListOf(LearningItem("pouvoir",1,0).encode(),LearningItem("être",0,0).encode()))
                        set("quizEntries",ArrayList(com.templesoftware.frenchverbs.domain.quiz.QuizQuestion.from(LearningItem("pouvoir",1,0)).rows.map{it.answer}))
                        set("quizLocked",arrayListOf(0,1,2,3,4,5));set("quizActive",-1)
                    }
                }
            })
        } })[ConjugateViewModel::class.java]
        val language=if(intent.getStringExtra("language")=="en")AppLanguage.ENGLISH_UK else AppLanguage.FRENCH
        setContent { LocalizedContent(language){TempleTheme {
            val state by model.state.collectAsStateWithLifecycle()
            BackHandler{finish()};QuizFeedbackClock(model)
            model.question?.let{ConjugateScreen(state,it,language,{finish()},model::letter,model::clear,model::submit,model::nextQuestion,model::edit)}
        }} }
    }
}
