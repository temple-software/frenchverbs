package com.templesoftware.frenchverbs
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.templesoftware.frenchverbs.ui.app.UseViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
class UseRotationTest {
    private val compose=createAndroidComposeRule<UsePreviewActivity>()
    @get:Rule val rules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    @Test fun nativeRotationRetainsRedGreenAndSession() {
        compose.mainClock.autoAdvance=false
        lateinit var m:UseViewModel
        compose.activityRule.scenario.onActivity{m=ViewModelProvider(it)[UseViewModel::class.java]}
        val out=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"pass36").apply{mkdirs()}
        val ready=File(out,"rotation-ready");ready.delete()
        fun rotate(step:String) {
            val state=m.state.value;val orientation=compose.activity.resources.configuration.orientation
            ready.writeText(step)
            val deadline=System.currentTimeMillis()+30000;var changed=false
            while(!changed&&System.currentTimeMillis()<deadline){Thread.sleep(100);compose.activityRule.scenario.onActivity{changed=it.resources.configuration.orientation!=orientation}}
            assertTrue("native rotation $step",changed);assertEquals(state,m.state.value)
        }
        val q=m.state.value.current!!
        compose.runOnIdle{m.letter(q.keys.first{it!=q.answer.first()});m.submit();m.tick(200)}
        rotate("red");assertEquals(450L,m.state.value.input.feedbackRemaining)
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        val background=m.state.value;compose.mainClock.advanceTimeBy(1000);assertEquals(background,m.state.value)
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        compose.runOnIdle{m.tick(450);q.answer.forEach(m::letter);m.submit();m.tick(300)}
        rotate("green");assertEquals(500L,m.state.value.successRemaining)
        compose.runOnIdle{m.tick(500)};assertEquals(1,m.state.value.index);assertEquals(-1,m.state.value.outcomes[0])
        ready.delete()
    }
}
