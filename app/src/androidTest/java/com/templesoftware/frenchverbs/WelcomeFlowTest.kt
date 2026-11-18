package com.templesoftware.frenchverbs

import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.templesoftware.frenchverbs.data.preferences.*
import com.templesoftware.frenchverbs.domain.verbs.*
import com.templesoftware.frenchverbs.ui.app.*
import com.templesoftware.frenchverbs.ui.navigation.AppNavHost
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class WelcomeFlowTest {
    private val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val rules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    private class MemoryRepository(initial:Selection,val savedInitially:Boolean=false):SelectionRepository {
        override val selections=MutableStateFlow(initial)
        override val completedSelections=MutableStateFlow(initial.takeIf{savedInitially})
        var saves=0;var fail=false
        override suspend fun save(selection:Selection){selections.value=selection}
        override suspend fun complete(selection:Selection){if(fail)throw java.io.IOException("test save failure");saves++;selections.value=selection;completedSelections.value=selection}
    }
    @Test fun draftCommitRestartResumeChangeAndSaveFailure() {
        val original=Selection(important=setOf("être","avoir"))
        val repository=MemoryRepository(original)
        var model by mutableStateOf<VerbViewModel?>(null)
        var generation by mutableIntStateOf(0)
        lateinit var nav:NavHostController
        compose.runOnIdle{model=VerbViewModel(repository,SavedStateHandle())}
        compose.activityRule.scenario.onActivity{it.setContent{key(generation){LocalizedContent(AppLanguage.ENGLISH_UK){com.templesoftware.frenchverbs.ui.theme.TempleTheme{
            nav=rememberNavController();AppNavHost(nav,AppLanguage.ENGLISH_UK,{},model)
        }}}}}
        compose.waitForIdle();compose.waitUntil(10000){model!!.state.value.loaded}
        compose.onNodeWithTag("next").performClick() // tenses -> types
        compose.onNodeWithTag("next").performClick() // types -> important
        compose.runOnIdle{model!!.toggle("important","aller")}
        assertEquals(0,repository.saves);assertNull(repository.completedSelections.value)
        repository.fail=true
        compose.onNodeWithTag("next").performClick();compose.waitForIdle()
        assertEquals("important",nav.currentDestination?.route)
        assertTrue(model!!.state.value.storageError)
        repository.fail=false
        compose.onNodeWithTag("next").performClick()
        compose.onNodeWithTag("open_conjugate").assertExists()
        val completed=model!!.state.value.selection
        assertEquals(1,repository.saves);assertEquals(completed,repository.completedSelections.value)
        fun restart() {
            compose.runOnIdle{model=VerbViewModel(repository,SavedStateHandle());generation++}
            compose.waitForIdle();compose.waitUntil(10000){model!!.state.value.loaded}
            compose.onNodeWithTag("welcome_again").assertExists()
        }
        restart()
        compose.onNodeWithTag("welcome_category_important",true).assertTextContains("être, avoir, aller",substring=true)
        compose.onNodeWithTag("welcome_again").performClick()
        compose.onNodeWithTag("open_learn").assertExists();assertEquals("activities",nav.currentDestination?.route)
        // Activity Home must still reach tenses even when it was skipped by Go again.
        compose.onNodeWithContentDescription("Home").performClick()
        assertEquals("tenses",nav.currentDestination?.route)
        restart()
        compose.onNodeWithTag("welcome_change").performClick()
        assertEquals("tenses",nav.currentDestination?.route);assertEquals(completed,model!!.state.value.selection)
        compose.runOnIdle{model!!.toggle("tenses","future")}
        assertEquals(completed,repository.completedSelections.value)
        restart() // Abandon draft; durable snapshot is still the previous completed choice.
        assertEquals(completed,model!!.state.value.selection)
        compose.onNodeWithTag("welcome_change").performClick()
        compose.runOnIdle{model!!.toggle("tenses","future")}
        compose.onNodeWithTag("next").performClick();compose.onNodeWithTag("next").performClick();compose.onNodeWithTag("next").performClick()
        compose.onNodeWithTag("open_use").assertExists()
        assertEquals(2,repository.saves);assertTrue("future" in repository.completedSelections.value!!.tenses)
    }
    @Test fun draftRestorationDoesNotOverwriteCompletedSnapshot() {
        val original=Selection()
        val repository=MemoryRepository(original,true)
        val handle=SavedStateHandle()
        lateinit var model:VerbViewModel
        compose.runOnIdle{model=VerbViewModel(repository,handle)}
        compose.waitUntil(10000){model.state.value.loaded}
        compose.runOnIdle{model.toggle("tenses","future")}
        val draft=model.state.value.selection
        compose.runOnIdle{model=VerbViewModel(repository,SavedStateHandle(handle.keys().associateWith{handle.get<Any?>(it)}))}
        compose.waitUntil(10000){model.state.value.loaded}
        assertEquals(draft,model.state.value.selection);assertEquals(original,model.state.value.completedSelection);assertEquals(0,repository.saves)
    }
}
