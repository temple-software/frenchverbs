package com.templesoftware.frenchverbs
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.*
import com.templesoftware.frenchverbs.ui.app.*
import com.templesoftware.frenchverbs.ui.use.*
import com.templesoftware.frenchverbs.ui.theme.TempleTheme
import com.templesoftware.frenchverbs.domain.verbs.*
import com.templesoftware.frenchverbs.data.preferences.AppLanguage
import kotlin.random.Random
class UsePreviewActivity:ComponentActivity() {
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState);enableEdgeToEdge()
        val model=ViewModelProvider(this,viewModelFactory{initializer{UseViewModel(createSavedStateHandle())}})[UseViewModel::class.java]
        if(model.state.value.questions.isEmpty())model.start(Selection(tenses=setOf("imperfect"),types=setOf("important"),important=setOf("pouvoir")),Random(36))
        val language=if(intent.getStringExtra("language")=="en")AppLanguage.ENGLISH_UK else AppLanguage.FRENCH
        setContent{LocalizedContent(language){TempleTheme{val s by model.state.collectAsState();val click=rememberUseKeyClick();UseFeedbackClock(model);UseScreen(s,language,{finish()},{model.letterWithClick(it,click)},model::clear,model::submit,model::edit,onPause=model::pause,letterReady=rememberUseAudio().ready)}}}
    }
}
