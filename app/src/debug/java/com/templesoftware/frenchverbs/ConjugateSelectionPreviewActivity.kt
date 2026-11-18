package com.templesoftware.frenchverbs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.templesoftware.frenchverbs.data.preferences.AppLanguage
import com.templesoftware.frenchverbs.domain.verbs.*
import com.templesoftware.frenchverbs.ui.app.*
import com.templesoftware.frenchverbs.ui.quiz.*
import com.templesoftware.frenchverbs.ui.theme.TempleTheme

/** Temporary review fixture, excluded from release. No preferences repository or writes. */
class ConjugateSelectionPreviewActivity:ComponentActivity() {
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val model=ViewModelProvider(this)[ConjugateViewModel::class.java]
        if(savedInstanceState==null)model.start(Selection(Choices.tenses.toSet(),Choices.types.toSet(),Choices.important.toSet(),Choices.er.toSet(),Choices.families.toSet()))
        val language=if(intent.getStringExtra("language")=="en")AppLanguage.ENGLISH_UK else AppLanguage.FRENCH
        setContent { LocalizedContent(language) { TempleTheme {
            val selection by model.selection.collectAsStateWithLifecycle()
            val quiz by model.state.collectAsStateWithLifecycle()
            fun back(){if(!model.selectionBack())finish()}
            BackHandler{if(selection.done)finish()else back()}
            if(!selection.done)ConjugateSelectionScreen(selection,language,{finish()},::back,model::chooseTenses,model::chooseTypes,model::chooseRandom,model::chooseTense,model::chooseCategory)
            else { QuizFeedbackClock(model);model.question?.let { ConjugateScreen(quiz,it,language,{finish()},model::letter,model::clear,model::submit,model::nextQuestion,model::edit) } }
        } } }
    }
}
