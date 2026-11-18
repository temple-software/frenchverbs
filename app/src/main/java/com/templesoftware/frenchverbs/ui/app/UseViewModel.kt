package com.templesoftware.frenchverbs.ui.app

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.templesoftware.frenchverbs.domain.use.*
import com.templesoftware.frenchverbs.domain.quiz.*
import com.templesoftware.frenchverbs.domain.verbs.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class UseViewModel(private val saved:SavedStateHandle,private val successMillis:Long=UseActions.SUCCESS_MILLIS):ViewModel() {
    private val questions=saved.get<ArrayList<String>>("useQuestions")?.map(UseSessions::decode).orEmpty()
    private val index=saved.get<Int>("useIndex")?:0
    private val initial=questions.getOrNull(index)?.let(UseActions::input)?:QuizState()
    private val mutable=MutableStateFlow(UseState(questions,index,initial.copy(
        entries=saved.get<ArrayList<String>>("useEntries")?:initial.entries,
        hints=saved.get<ArrayList<String>>("useHints")?:initial.hints,
        mistakes=saved.get<ArrayList<Int>>("useMistakes")?:initial.mistakes,
        wrongLines=saved.get<ArrayList<Int>>("useWrongLines")?:initial.wrongLines,
        locked=saved.get<ArrayList<Int>>("useLocked")?.toSet()?:initial.locked,
        active=saved["useActive"]?:initial.active,feedbackRemaining=saved["useRed"]?:0L,
        pendingHint=saved["usePendingHint"]?:"",pendingComplete=saved["usePendingComplete"]?:false),
        outcomes=saved.get<ArrayList<Int>>("useOutcomes")?:List(10){0},successRemaining=saved["useGreen"]?:0L,running=saved["useRunning"]?:true,attempts=saved.get<ArrayList<Int>>("useAttempts")?:List(10){0},revealed=saved.get<ArrayList<String>>("useRevealed")?:List(10){""},selectedTenseCount=saved["useTenseCount"]?:questions.map{it.tense}.distinct().size))
    val state=mutable.asStateFlow()
    private fun set(s:UseState) {
        if(s==state.value)return
        if(s.questions!=state.value.questions)saved["useQuestions"]=ArrayList(s.questions.map{it.encode()})
        saved["useTenseCount"]=s.selectedTenseCount
        saved["useIndex"]=s.index;saved["useEntries"]=ArrayList(s.input.entries);saved["useHints"]=ArrayList(s.input.hints)
        saved["useMistakes"]=ArrayList(s.input.mistakes);saved["useLocked"]=ArrayList(s.input.locked)
        saved["useWrongLines"]=ArrayList(s.input.wrongLines)
        saved["useActive"]=s.input.active;saved["useRed"]=s.input.feedbackRemaining
        saved["usePendingHint"]=s.input.pendingHint;saved["usePendingComplete"]=s.input.pendingComplete
        saved["useAttempts"]=ArrayList(s.attempts);saved["useRevealed"]=ArrayList(s.revealed)
        saved["useOutcomes"]=ArrayList(s.outcomes);saved["useGreen"]=s.successRemaining;saved["useRunning"]=s.running
        mutable.value=s
    }
    fun start(selection:Selection,r:Random=Random.Default):Boolean {val q=UseSessions.create(selection,r);set(UseActions.start(q).copy(selectedTenseCount=selection.tenses.intersect(Choices.tenses.toSet()).size));return q.isNotEmpty()}
    fun letter(c:Char)=set(UseActions.letter(state.value,c))
    /** Emit only at a successful input event, never when observing/restoring state. */
    fun letterWithClick(c:Char,click:()->Unit) {
        val before=state.value.input.entries
        letter(c)
        if(state.value.input.entries!=before)click()
    }
    fun clear()=set(UseActions.clear(state.value))
    fun edit(i:Int)=set(UseActions.edit(state.value,i))
    fun submit()=set(UseActions.submit(state.value,successMillis))
    fun tick(delta:Long)=set(UseActions.tick(state.value,delta,successMillis))
    fun pause(paused:Boolean)=set(state.value.copy(running=!paused))
    fun cancel()=set(state.value.copy(running=false))
}
