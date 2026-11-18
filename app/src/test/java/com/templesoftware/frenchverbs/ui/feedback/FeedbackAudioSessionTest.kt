package com.templesoftware.frenchverbs.ui.feedback

import org.junit.Assert.assertEquals
import org.junit.Test

class FeedbackAudioSessionTest {
    @Test
    fun incorrectAndCorrectFeedbackRequestMatchingSounds() {
        val audio = FakeAudioController()
        val session = FeedbackAudioSession(audio, initialFeedbackId = 0)

        session.onFeedback(FeedbackEvent.Incorrect(1))
        session.onFeedback(FeedbackEvent.Correct(2))

        assertEquals(listOf("stop", "failure", "stop", "success"), audio.calls)
    }

    @Test
    fun distinctFeedbackIdsRetriggerButRecompositionOfSameIdDoesNot() {
        val audio = FakeAudioController()
        val session = FeedbackAudioSession(audio, initialFeedbackId = 0)

        session.onFeedback(FeedbackEvent.Incorrect(1))
        session.onFeedback(FeedbackEvent.Incorrect(1))
        session.onFeedback(FeedbackEvent.Incorrect(2))

        assertEquals(listOf("stop", "failure", "stop", "failure"), audio.calls)
    }

    @Test
    fun currentFeedbackIsBaselineAndIsNotReplayedAfterRotation() {
        val audio = FakeAudioController()
        val session = FeedbackAudioSession(audio, initialFeedbackId = 7)

        session.onFeedback(FeedbackEvent.Correct(7))

        assertEquals(emptyList<String>(), audio.calls)
    }

    @Test
    fun clearProducesNoFeedbackEventAndThereforeNoAudioRequest() {
        val audio = FakeAudioController()
        val session = FeedbackAudioSession(audio, initialFeedbackId = 0)

        session.onFeedback(FeedbackEvent.None)

        assertEquals(emptyList<String>(), audio.calls)
    }

    @Test
    fun closingSessionReleasesAudioAndIgnoresStaleEvents() {
        val audio = FakeAudioController()
        val session = FeedbackAudioSession(audio, initialFeedbackId = 0)

        session.close()
        session.onFeedback(FeedbackEvent.Correct(1))
        session.close()

        assertEquals(listOf("release"), audio.calls)
    }

    @Test
    fun correctAfterIncorrectStopsFailureBeforeStartingSuccess() {
        val audio = FakeAudioController()
        val session = FeedbackAudioSession(audio, initialFeedbackId = 0)

        session.onFeedback(FeedbackEvent.Incorrect(1))
        session.onFeedback(FeedbackEvent.Correct(2))

        assertEquals(listOf("stop", "failure", "stop", "success"), audio.calls)
    }

    @Test
    fun incorrectAfterCorrectStopsSuccessBeforeStartingFailure() {
        val audio = FakeAudioController()
        val session = FeedbackAudioSession(audio, initialFeedbackId = 0)

        session.onFeedback(FeedbackEvent.Correct(1))
        session.onFeedback(FeedbackEvent.Incorrect(2))

        assertEquals(listOf("stop", "success", "stop", "failure"), audio.calls)
    }

    @Test fun olderConsumedEventCannotReplayAfterANewerEvent() {
        val audio = FakeAudioController()
        val session = FeedbackAudioSession(audio, initialFeedbackId = 0)
        session.onFeedback(FeedbackEvent.Correct(2))
        session.onFeedback(FeedbackEvent.Incorrect(1))
        assertEquals(listOf("stop", "success"), audio.calls)
    }

    private class FakeAudioController : FeedbackAudioController {
        val calls = mutableListOf<String>()
        override fun playSuccess() { calls += "success" }
        override fun playFailure() { calls += "failure" }
        override fun stopCurrent() { calls += "stop" }
        override fun release() { calls += "release" }
    }
}
