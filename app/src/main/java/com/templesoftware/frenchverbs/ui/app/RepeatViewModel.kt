package com.templesoftware.frenchverbs.ui.app

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.templesoftware.frenchverbs.domain.repeat.*
import com.templesoftware.frenchverbs.domain.verbs.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

enum class RepeatPlayback { PLAYING, WAITING, TRANSITION }

data class RepeatState(val session: List<LearningItem> = emptyList(), val index: Int = 0,
    val elapsed: Long = 0, val single: Boolean = false, val complete: Boolean = false, val playback: RepeatPlayback = RepeatPlayback.PLAYING) {
    val next get() = session.getOrNull(index+1)
    val current get() = session.getOrNull(index)
}
/** Separate from Learn: no Learn item, selection, or example state is ever mutated here. */
class RepeatViewModel(private val saved: SavedStateHandle, val timeline: RepeatTimeline = RepeatTimeline()) : ViewModel() {
    private val mutable=MutableStateFlow(RepeatState(
        saved.get<ArrayList<String>>("repeatSession")?.mapNotNull(LearningItem::decode).orEmpty(),
        saved["repeatIndex"] ?: 0, saved["repeatElapsed"] ?: 0L,
        saved["repeatSingle"] ?: false, saved["repeatComplete"] ?: false,
        saved.get<String>("repeatPlayback")?.let{runCatching{RepeatPlayback.valueOf(it)}.getOrNull()} ?: RepeatPlayback.PLAYING))
    val state=mutable.asStateFlow()
    private fun set(value: RepeatState) {
        saved["repeatPlayback"]=value.playback.name
        saved["repeatIndex"]=value.index; saved["repeatElapsed"]=value.elapsed
        saved["repeatSingle"]=value.single; saved["repeatComplete"]=value.complete
        if(value.session!=mutable.value.session) saved["repeatSession"]=ArrayList(value.session.map{it.encode()})
        mutable.value=value
    }
    fun startFull(selection: Selection, random: Random = Random.Default): Boolean {
        val previous=saved.get<ArrayList<String>>("repeatPrevious")?.mapNotNull(LearningItem::decode).orEmpty()
        val session=LearningSequence.create(selection,previous,random)
        if(session.isEmpty())return false
        saved["repeatPrevious"]=ArrayList(session.map{it.encode()})
        set(RepeatState(session=session));return true
    }
    fun startSingle(item: LearningItem) { set(RepeatState(session=listOf(item),single=true)) }
    fun cancel() { set(state.value.copy(complete=true, single=false)) }
    fun next(): Boolean {
        val s=state.value
        if(s.single || s.complete || s.playback!=RepeatPlayback.WAITING || s.next==null)return false
        set(s.copy(playback=RepeatPlayback.TRANSITION,elapsed=timeline.fadeOutAt))
        return true
    }
    fun tick(milliseconds: Long) {
        val s=state.value
        if(s.complete || s.playback==RepeatPlayback.WAITING || s.current==null || milliseconds<=0)return
        val elapsed=s.elapsed+milliseconds
        if(!s.single && s.playback==RepeatPlayback.PLAYING && elapsed>=timeline.waitingAt)
            set(s.copy(elapsed=timeline.waitingAt,playback=RepeatPlayback.WAITING))
        else if(elapsed < timeline.duration) set(s.copy(elapsed=elapsed))
        else if(s.single || s.index==s.session.lastIndex) set(s.copy(elapsed=timeline.duration,complete=true))
        else set(s.copy(index=s.index+1,elapsed=0,playback=RepeatPlayback.PLAYING))
    }
}
