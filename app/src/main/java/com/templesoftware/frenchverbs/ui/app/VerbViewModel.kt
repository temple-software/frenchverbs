package com.templesoftware.frenchverbs.ui.app

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.templesoftware.frenchverbs.data.preferences.SelectionRepository
import com.templesoftware.frenchverbs.domain.verbs.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class VerbState(val loaded: Boolean = false, val selection: Selection = Selection(),
    val session: List<LearningItem> = emptyList(), val index: Int = 0, val storageError: Boolean = false,
    val completedSelection: Selection? = null, val savingSelection: Boolean = false) {
    val current get() = session.getOrNull(index)
    val next get() = session.getOrNull(index + 1)
}
class VerbViewModel(private val repository: SelectionRepository, private val saved: SavedStateHandle) : ViewModel() {
    private val restored = saved.get<ArrayList<String>>("session")?.mapNotNull(LearningItem::decode).orEmpty()
    private val mutable = MutableStateFlow(VerbState(session = restored,
        index = (saved.get<Int>("index") ?: 0).coerceIn(0, (restored.size - 1).coerceAtLeast(0))))
    val state = mutable.asStateFlow()
    init {
        viewModelScope.launch {
            try {
                val persisted = repository.selections.first()
                val completed = repository.completedSelections.first()
                val draft = com.templesoftware.frenchverbs.data.preferences.DataStoreSelectionRepository.sections.fold(completed ?: persisted) { s, section ->
                    saved.get<ArrayList<String>>("draft_$section")?.let { s.with(section,it.toSet()) } ?: s
                }
                mutable.update { it.copy(selection=draft,completedSelection=completed,loaded=true) }
            } catch (_: java.io.IOException) { mutable.update { it.copy(loaded=true,storageError=true) } }
        }
    }
    private fun draft(selection:Selection) {
        com.templesoftware.frenchverbs.data.preferences.DataStoreSelectionRepository.sections.forEach {
            saved["draft_$it"]=ArrayList(selection.selected(it))
        }
        mutable.update { it.copy(selection=selection) }
    }
    fun toggle(section: String, id: String? = null) {
        if (!state.value.loaded || state.value.savingSelection) return
        draft(if (id == null) state.value.selection.toggleAll(section) else state.value.selection.toggle(section,id))
    }
    fun useCompletedSelection() { state.value.completedSelection?.let(::draft) }
    /** Save atomically before navigating, so reaching Activities means the snapshot is durable. */
    fun completeSelection(onSaved:()->Unit) {
        val selection=state.value.selection
        if(!state.value.loaded || state.value.savingSelection || !selection.readyForActivities())return
        mutable.update { it.copy(savingSelection=true,storageError=false) }
        viewModelScope.launch {
            try {
                repository.complete(selection)
                mutable.update { it.copy(completedSelection=selection,savingSelection=false) }
                onSaved()
            } catch (_:java.io.IOException) {
                mutable.update { it.copy(savingSelection=false,storageError=true) }
            }
        }
    }
    fun startLearn(): Boolean {
        val session = LearningSequence.create(state.value.selection, state.value.session)
        if (session.isEmpty()) return false
        saved["session"] = ArrayList(session.map { it.encode() }); saved["index"] = 0
        mutable.update { it.copy(session = session, index = 0) }; return true
    }
    fun previous(): Boolean {
        if (state.value.index <= 0 || state.value.current == null) return false
        val index = state.value.index - 1
        saved["index"] = index
        mutable.update { it.copy(index = index) }
        return true
    }
    fun advance(): Boolean {
        if (state.value.next == null) return false
        val index = state.value.index + 1; saved["index"] = index
        mutable.update { it.copy(index = index) }; return true
    }
}
