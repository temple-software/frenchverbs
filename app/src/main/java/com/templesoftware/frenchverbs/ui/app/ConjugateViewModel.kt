package com.templesoftware.frenchverbs.ui.app

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.templesoftware.frenchverbs.domain.quiz.*
import com.templesoftware.frenchverbs.domain.verbs.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class ConjugateViewModel(private val saved:SavedStateHandle):ViewModel() {
    private val mutable=MutableStateFlow(QuizState(
        session=saved.get<ArrayList<String>>("quizSession")?.mapNotNull(LearningItem::decode).orEmpty(),
        index=saved["quizIndex"] ?: 0,
        entries=saved.get<ArrayList<String>>("quizEntries")?.toList() ?: List(6){""},
        locked=saved.get<ArrayList<Int>>("quizLocked")?.toSet().orEmpty(),
        active=saved["quizActive"] ?: 0,feedbackRemaining=saved["quizFeedback"] ?: 0L,
        running=saved["quizRunning"] ?: true,
        hints=saved.get<ArrayList<String>>("quizHints")?.toList() ?: List(6){""},
        mistakes=saved.get<ArrayList<Int>>("quizMistakes")?.toList() ?: List(6){0},
        wrongLines=saved.get<ArrayList<Int>>("quizWrongLines")?.toList().orEmpty(),
        completedLines=saved.get<ArrayList<String>>("quizCompletedLines")?.map(ConjugateResultLine::decode).orEmpty(),
        resultToggles=saved.get<ArrayList<Int>>("quizResultToggles")?.toSet().orEmpty(),
        pendingHint=saved["quizPendingHint"] ?: "",pendingComplete=saved["quizPendingComplete"] ?: false))
    val state=mutable.asStateFlow()
    private val selectionMutable=MutableStateFlow(ConjugateSelection(
        original=saved.get<ArrayList<String>>("conjOriginal")?.mapNotNull(ConjugateCandidate::decode).orEmpty(),
        tense=saved.get<Int>("conjTense")?.takeIf{it>=0},category=saved.get<String>("conjCategory")?.takeIf{it.isNotEmpty()},
        page=saved.get<String>("conjPage")?.let{ConjugateChoicePage.valueOf(it)} ?: ConjugateChoicePage.DONE,
        accepted=saved.get<ArrayList<String>>("conjAccepted")?.mapNotNull(LearningItem::decode).orEmpty()))
    val selection=selectionMutable.asStateFlow()
    private fun select(next:ConjugateSelection) {
        if(next==selectionMutable.value)return
        val wasDone=selectionMutable.value.done
        saved["conjOriginal"]=ArrayList(next.original.map{it.encode()})
        saved["conjTense"]=next.tense ?: -1;saved["conjCategory"]=next.category.orEmpty()
        saved["conjPage"]=next.page.name;saved["conjAccepted"]=ArrayList(next.accepted.map{it.encode()})
        selectionMutable.value=next
        // CONJGO occurs only on a transition into DONE, never in a composable/effect.
        if(next.done && !wasDone)set(QuizState(session=next.accepted))
    }
    fun chooseTenses(){select(ConjugateSelectionActions.openTenses(selection.value))}
    fun chooseTypes(){select(ConjugateSelectionActions.openTypes(selection.value))}
    fun chooseRandom(){select(ConjugateSelectionActions.random(selection.value))}
    fun chooseTense(tense:Int){select(ConjugateSelectionActions.chooseTense(selection.value,tense))}
    fun chooseCategory(category:String){select(ConjugateSelectionActions.chooseCategory(selection.value,category))}
    fun selectionBack():Boolean {
        val previous=ConjugateSelectionActions.back(selection.value) ?: return false
        select(previous);return true
    }
    private var cachedItem:LearningItem?=null
    private var cachedQuestion:QuizQuestion?=null
    val question:QuizQuestion? get() {
        val item=state.value.item ?: return null
        if(item!=cachedItem){cachedQuestion=QuizQuestion.from(item);cachedItem=item}
        return cachedQuestion
    }
    private fun set(next:QuizState) {
        val s=if(next.done && next.item!=null)recordConjugateCompletion(next,QuizQuestion.from(next.item!!))else next
        if(s==state.value)return
        if(s.session!=state.value.session)saved["quizSession"]=ArrayList(s.session.map{it.encode()})
        saved["quizIndex"]=s.index;saved["quizEntries"]=ArrayList(s.entries)
        saved["quizLocked"]=ArrayList(s.locked);saved["quizActive"]=s.active
        saved["quizFeedback"]=s.feedbackRemaining;saved["quizRunning"]=s.running
        saved["quizHints"]=ArrayList(s.hints);saved["quizMistakes"]=ArrayList(s.mistakes)
        saved["quizPendingHint"]=s.pendingHint;saved["quizPendingComplete"]=s.pendingComplete
        saved["quizWrongLines"]=ArrayList(s.wrongLines)
        saved["quizCompletedLines"]=ArrayList(s.completedLines.map{it.encode()})
        saved["quizResultToggles"]=ArrayList(s.resultToggles)
        mutable.value=s
    }
    fun start(selection:Selection,random:Random=Random.Default):Boolean {
        val previous=saved.get<ArrayList<String>>("quizPrevious")?.mapNotNull(LearningItem::decode).orEmpty()
        val sequence=LearningSequence.create(selection,previous,random)
        saved["quizPrevious"]=ArrayList(sequence.map{it.encode()})
        // One fresh source sequence per activity entry; all later decisions only filter this snapshot.
        val original=ConjugateSelectionActions.candidates(sequence,selection)
        select(ConjugateSelection(original=original))
        select(ConjugateSelectionActions.begin(original,random))
        return true
    }
    fun letter(c:Char){question?.let{set(QuizActions.letter(state.value,it,c))}}
    fun edit(row:Int,index:Int){set(QuizActions.edit(state.value,row,index))}
    fun clear(){set(QuizActions.clear(state.value))}
    fun submit(target:Int?=null){question?.let{set(QuizActions.submit(state.value,it,target))}}
    fun tick(delta:Long){set(QuizActions.tick(state.value,delta))}
    fun nextQuestion(){set(QuizActions.nextQuestion(state.value))}
    fun toggleResult(tense:Int){val s=state.value;set(s.copy(resultToggles=if(tense in s.resultToggles)s.resultToggles-tense else s.resultToggles+tense))}
    fun cancel(){set(state.value.copy(running=false,feedbackRemaining=0))}
}
