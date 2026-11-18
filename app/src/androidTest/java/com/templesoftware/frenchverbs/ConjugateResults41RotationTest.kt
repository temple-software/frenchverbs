package com.templesoftware.frenchverbs

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.templesoftware.frenchverbs.ui.app.ConjugateViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class ConjugateResults41RotationTest {
    private val compose=createAndroidComposeRule<ConjugateResultsPreviewActivity>()
    @get:Rule val rules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    @Test fun resultsSurviveNativeRotationRecreationAndResume() {
        val out=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"pass41").apply{mkdirs()}
        val ready=File(out,"rotation-ready");ready.delete()
        lateinit var model:ConjugateViewModel
        compose.activityRule.scenario.onActivity{model=ViewModelProvider(it)[ConjugateViewModel::class.java]}
        compose.onNodeWithTag("conj_result_heading_0",true).performScrollTo().performTouchInput{click()}
        val before=model.state.value
        fun rotate(step:String) {
            val orientation=compose.activity.resources.configuration.orientation
            ready.writeText(step)
            val deadline=System.currentTimeMillis()+30000;var changed=false
            while(!changed && System.currentTimeMillis()<deadline) {
                Thread.sleep(100);compose.activityRule.scenario.onActivity{changed=it.resources.configuration.orientation!=orientation}
            }
            assertTrue("native rotation $step",changed);assertEquals(before,model.state.value)
        }
        compose.onNodeWithTag("conj_results_grid").assertDoesNotExist()
        rotate("out")
        compose.onNodeWithTag("conj_results_grid").assertExists()
        rotate("return")
        compose.onNodeWithTag("conj_results_grid").assertDoesNotExist()
        ready.delete()
        compose.activityRule.scenario.recreate()
        compose.activityRule.scenario.onActivity{model=ViewModelProvider(it)[ConjugateViewModel::class.java]}
        assertEquals(before,model.state.value)
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        compose.onNodeWithTag("conj_result_summary_0",true).performScrollTo().assertExists()
        assertEquals(before,model.state.value)
        File(out,"rotation-result.txt").writeText("PASS native rotation both directions, activity recreation and background/resume preserve score, records and toggles.")
    }
}
