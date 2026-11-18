package com.templesoftware.frenchverbs

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.templesoftware.frenchverbs.ui.app.VerbViewModel
import com.templesoftware.frenchverbs.domain.verbs.readyForActivities
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Exercise the owner's existing choices unchanged, then deliberately complete their selection. */
class WelcomeHandoffTest {
    private val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val rules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    @Test fun completeExistingChoicesWithoutChangingThem() {
        lateinit var model:VerbViewModel
        compose.waitUntil(15000){compose.onAllNodesWithTag("next").fetchSemanticsNodes().isNotEmpty() || compose.onAllNodesWithTag("welcome_again").fetchSemanticsNodes().isNotEmpty()}
        compose.activityRule.scenario.onActivity{model=ViewModelProvider(it)[VerbViewModel::class.java]}
        val original=model.state.value.selection
        assertTrue("Existing choices must already be complete; never substitute a different selection",original.readyForActivities())
        if(compose.onAllNodesWithTag("welcome_again").fetchSemanticsNodes().isNotEmpty())compose.onNodeWithTag("welcome_again").performClick()
        else {
            repeat(original.setupRoutes().size-1){compose.onNodeWithTag("next").performClick()}
        }
        compose.waitUntil(15000){compose.onAllNodesWithTag("open_learn").fetchSemanticsNodes().isNotEmpty()}
        assertEquals(original,model.state.value.selection)
        assertEquals(original,model.state.value.completedSelection)
        val out=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"welcome").apply{mkdirs()}
        File(out,"owner-selection.txt").writeText("PASS existing selection unchanged and now saved as completed:\n$original")
    }
}
