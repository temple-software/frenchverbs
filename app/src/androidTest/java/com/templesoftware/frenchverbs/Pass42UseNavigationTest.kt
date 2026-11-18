package com.templesoftware.frenchverbs

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.templesoftware.frenchverbs.data.preferences.DataStoreSelectionRepository
import com.templesoftware.frenchverbs.domain.verbs.Selection
import com.templesoftware.frenchverbs.ui.app.UseViewModel
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.*

class Pass42UseNavigationTest {
    private val compose=createAndroidComposeRule<MainActivity>()
    private val seed=object:ExternalResource(){override fun before(){
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".audit42"))
        runBlocking{DataStoreSelectionRepository(context).complete(Selection())}
    }}
    @get:Rule val rules=RuleChain.outerRule(seed).around(PreserveRotationRule()).around(compose)
    private fun visible(tag:String){compose.waitUntil(15000){compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()}}
    @Test fun welcomeActivitiesUseSystemBackStayAndLeave() {
        visible("welcome_again");compose.onNodeWithTag("welcome_again").performClick()
        visible("open_use");compose.onNodeWithTag("open_use").performClick();visible("use_title")
        lateinit var model:UseViewModel
        compose.activityRule.scenario.onActivity{model=ViewModelProvider(it)[UseViewModel::class.java]}
        assertEquals(10,model.state.value.questions.size)
        val questions=model.state.value.questions
        compose.activityRule.scenario.onActivity{it.onBackPressedDispatcher.onBackPressed()}
        visible("use_exit_stay");compose.onNodeWithTag("use_exit_stay").performClick();visible("use_title")
        assertEquals(questions,model.state.value.questions)
        compose.onNodeWithTag("use_back").performClick();visible("use_exit_confirm")
        compose.onNodeWithTag("use_exit_confirm").performClick();visible("open_use")
        assertFalse(model.state.value.running)
    }
}
