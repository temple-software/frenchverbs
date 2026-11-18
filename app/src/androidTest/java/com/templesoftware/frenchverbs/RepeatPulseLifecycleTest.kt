package com.templesoftware.frenchverbs

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.templesoftware.frenchverbs.ui.app.RepeatViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class RepeatPulseLifecycleTest {
    private val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val rules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    @Test fun nativeRotationAndBackgroundPreserveCountdown() {
        val out=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"pass29").apply{mkdirs()}
        val ready=File(out,"rotation-ready");ready.delete()
        compose.waitUntil(10000){compose.onAllNodesWithTag("next").fetchSemanticsNodes().isNotEmpty()}
        repeat(6) {
            if(compose.onAllNodesWithTag("open_repeat").fetchSemanticsNodes().isEmpty())
                compose.onNodeWithTag("next").performSemanticsAction(SemanticsActions.OnClick){it()}
        }
        compose.mainClock.autoAdvance=false
        compose.onNodeWithTag("open_repeat").performSemanticsAction(SemanticsActions.OnClick){it()}
        compose.mainClock.advanceTimeBy(400)
        lateinit var model:RepeatViewModel
        compose.activityRule.scenario.onActivity{model=ViewModelProvider(it)[RepeatViewModel::class.java]}
        val target=model.timeline.phases.first{it.name=="instructionIn2"}.start+200
        compose.runOnIdle{model.tick(target-model.state.value.elapsed)}
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
        compose.activityRule.scenario.onActivity{assertSame(model,ViewModelProvider(it)[RepeatViewModel::class.java])}
        assertEquals(before,model.state.value)
        compose.onNodeWithTag("repeat_instruction",true).assertTextContains("... 2",substring=true)
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        val paused=model.state.value
        compose.mainClock.advanceTimeBy(1000)
        assertEquals(paused,model.state.value)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.mainClock.advanceTimeBy(32)
        val resumed=model.state.value.elapsed
        compose.mainClock.advanceTimeBy(160)
        val delta=model.state.value.elapsed-resumed
        assertTrue("exactly one clock after resume: $delta",delta in 144L..176L)
        assertEquals(before.current,model.state.value.current)
        assertTrue(model.state.value.elapsed>=before.elapsed)
        File(out,"lifecycle.txt").writeText("PASS native rotation during countdown retained model/item/elapsed; background consumed no time; resume used one clock. elapsedBefore=${before.elapsed}, resumed=$resumed, subsequentDelta=$delta")
        ready.delete()
    }
}
