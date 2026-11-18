package com.templesoftware.frenchverbs

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.templesoftware.frenchverbs.ui.app.ConjugateViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class ConjugateRotationTest {
    private val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val rules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    @Test fun nativeRotationPreservesAnswersAndSingleFeedbackClock() {
        val out=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"pass32").apply{mkdirs()}
        val ready=File(out,"rotation-ready");ready.delete()
        compose.waitUntil(10000){compose.onAllNodesWithTag("next").fetchSemanticsNodes().isNotEmpty()}
        repeat(6) {
            if(compose.onAllNodesWithTag("open_conjugate").fetchSemanticsNodes().isEmpty())
                compose.onNodeWithTag("next").performSemanticsAction(SemanticsActions.OnClick){it()}
        }
        compose.mainClock.autoAdvance=false
        compose.onNodeWithTag("open_conjugate").performSemanticsAction(SemanticsActions.OnClick){it()}
        compose.mainClock.advanceTimeBy(400)
        lateinit var model:ConjugateViewModel
        compose.activityRule.scenario.onActivity{model=ViewModelProvider(it)[ConjugateViewModel::class.java]}
        compose.runOnIdle {
            model.question!!.rows[0].answer.forEach(model::letter);model.submit()
            val wrong=model.question!!.keys.first{it!=model.question!!.rows[1].answer.first()}
            model.letter(wrong);model.submit();model.tick(650)
            model.letter(wrong);model.submit()
        }
        val before=model.state.value
        val orientation=compose.activity.resources.configuration.orientation
        ready.writeText("ready")
        // The host invokes the emulator's native Rotate command while this countdown is paused.
        val deadline=System.currentTimeMillis()+30000
        var rotated=false
        while(!rotated && System.currentTimeMillis()<deadline) {
            Thread.sleep(100)
            compose.activityRule.scenario.onActivity{rotated=it.resources.configuration.orientation!=orientation}
        }
        assertTrue("Host native rotation",rotated)
        compose.activityRule.scenario.onActivity{assertSame(model,ViewModelProvider(it)[ConjugateViewModel::class.java])}
        assertEquals(before,model.state.value)
        compose.onNodeWithTag("quiz_enter").assertIsNotEnabled()
        assertEquals(setOf(0),model.state.value.locked)
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        val paused=model.state.value
        compose.mainClock.advanceTimeBy(1000)
        assertEquals(paused,model.state.value)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.mainClock.advanceTimeBy(32)
        val resumed=model.state.value.feedbackRemaining
        compose.mainClock.advanceTimeBy(160)
        val delta=resumed-model.state.value.feedbackRemaining
        assertTrue("exactly one clock after resume: $delta",delta in 144L..176L)
        assertEquals(before.item,model.state.value.item)
        assertTrue(model.state.value.feedbackRemaining<before.feedbackRemaining)
        compose.runOnIdle{model.tick(model.state.value.feedbackRemaining)}
        assertEquals(model.state.value.hints[1],model.state.value.entries[1]);assertTrue(model.state.value.hints[1].isNotEmpty());assertEquals(2,model.state.value.mistakes[1]);assertEquals(setOf(0),model.state.value.locked)
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("quiz_enter").assertIsEnabled()
        val afterHint=model.state.value
        ready.writeText("return")
        val returnDeadline=System.currentTimeMillis()+30000
        var returned=false
        while(!returned && System.currentTimeMillis()<returnDeadline) {
            Thread.sleep(100)
            compose.activityRule.scenario.onActivity{returned=it.resources.configuration.orientation==orientation}
        }
        assertTrue("Native return rotation",returned)
        assertEquals(afterHint,model.state.value)
        File(out,"lifecycle.txt").writeText("PASS native rotation retained session, correct row, wrong entry and feedback; background consumed no time; resume used one clock. feedbackBefore=${before.feedbackRemaining}, resumed=$resumed, subsequentDelta=$delta")
        ready.delete()
    }
}
