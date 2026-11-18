package com.templesoftware.frenchverbs

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.templesoftware.frenchverbs.ui.app.VerbViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class WelcomeRotationTest {
    private val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val rules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    @Test fun realRelaunchAndNativeRotation() {
        compose.waitUntil(15000){compose.onAllNodesWithTag("welcome_again").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("welcome_again").assertExists()
        lateinit var model:VerbViewModel
        compose.activityRule.scenario.onActivity{model=ViewModelProvider(it)[VerbViewModel::class.java]}
        val before=model.state.value.selection
        assertEquals(before,model.state.value.completedSelection)
        val out=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"welcome").apply{mkdirs()}
        val ready=File(out,"rotation-ready");ready.delete()
        for(step in listOf("out","return")) {
            val orientation=compose.activity.resources.configuration.orientation
            ready.writeText(step)
            val deadline=System.currentTimeMillis()+30000;var changed=false
            while(!changed && System.currentTimeMillis()<deadline) {
                Thread.sleep(100);compose.activityRule.scenario.onActivity{changed=it.resources.configuration.orientation!=orientation}
            }
            assertTrue("native rotation $step",changed)
            compose.waitUntil(15000){compose.onAllNodesWithTag("welcome_again").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("welcome_again").assertExists()
            assertEquals(before,model.state.value.selection)
        }
        ready.delete()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("welcome_change").assertExists()
        assertEquals(before,model.state.value.completedSelection)
    }
}

