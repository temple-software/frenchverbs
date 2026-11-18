package com.templesoftware.frenchverbs.ui.feedback

sealed interface FeedbackEvent {
    val id: Long

    data object None : FeedbackEvent { override val id: Long = 0 }
    data class Incorrect(override val id: Long) : FeedbackEvent
    data class Correct(override val id: Long) : FeedbackEvent
}
