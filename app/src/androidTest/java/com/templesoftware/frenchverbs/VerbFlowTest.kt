package com.templesoftware.frenchverbs

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.platform.app.InstrumentationRegistry
import com.templesoftware.frenchverbs.data.preferences.*
import com.templesoftware.frenchverbs.domain.verbs.*
import com.templesoftware.frenchverbs.ui.app.VerbViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class VerbFlowTest {
    val compose = createAndroidComposeRule<MainActivity>()
    @get:Rule val rotationRules = org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private fun ready() { compose.waitUntil(15000) { compose.onAllNodesWithTag("next").fetchSemanticsNodes().isNotEmpty() } }
    private fun reset() {
        // Recreate retains ViewModels: cancel old loads/writes before seeding defaults.
        compose.activityRule.scenario.onActivity { it.viewModelStore.clear() }
        runBlocking { DataStoreSelectionRepository(inst.targetContext).save(Selection()) }
        compose.activityRule.scenario.recreate(); ready()
    }
    @Test fun defaultsNavigationLanguagePersistenceRotationAndFinalRepeat() {
        reset()
        compose.onNodeWithTag("choice_present").assertIsOn()
        compose.onNodeWithTag("choice_future").assertIsOff()
        compose.onNodeWithTag("choice_present").performClick(); compose.onNodeWithTag("next").assertIsNotEnabled()
        compose.onNodeWithTag("choice_present").performClick()
        compose.onNodeWithTag("language_selector").performClick(); compose.onNodeWithText("Français").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("Verbes français").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("next").performClick(); compose.onNodeWithTag("language_selector").assertDoesNotExist(); compose.onNodeWithTag("open_about").assertDoesNotExist()
        compose.onNodeWithTag("next").performClick(); compose.onNodeWithTag("choice_être").assertIsOn(); compose.onNodeWithTag("choice_all").assertIsOff()
        compose.onNodeWithTag("next").performClick(); compose.onNodeWithTag("open_learn").performClick()
        compose.onNodeWithTag("screen_title").assertTextEquals("être")
        compose.onNodeWithTag("learn_next").performClick(); compose.onNodeWithTag("screen_title").assertTextEquals("avoir")
        compose.activityRule.scenario.recreate(); compose.onNodeWithTag("screen_title").assertTextEquals("avoir")
        compose.activityRule.scenario.onActivity { it.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        compose.waitUntil(10000) { compose.activity.resources.configuration.orientation==2 }
        compose.onNodeWithTag("screen_title").assertTextEquals("avoir")
        compose.onNodeWithTag("learn_next").performClick(); compose.onNodeWithTag("learn_next").performClick()
        compose.onNodeWithText("Maintenant, répète →").assertIsDisplayed().performClick()
        compose.onNodeWithTag("screen_title").assertTextEquals("Répéter")
        compose.onNodeWithContentDescription("Accueil").performClick(); compose.onNodeWithTag("open_conjugate").performClick()
        compose.onNodeWithTag("screen_title").assertTextEquals("Conjuguer")
        compose.onNodeWithContentDescription("Accueil").performClick(); compose.onNodeWithTag("open_use").performClick()
        compose.onNodeWithTag("screen_title").assertTextEquals("Utiliser")
        compose.onNodeWithContentDescription("Accueil").performClick(); compose.onNodeWithContentDescription("Accueil").performClick()
        compose.onNodeWithTag("language_selector").assertIsDisplayed(); compose.onNodeWithTag("choice_present").assertIsOn()
        compose.onNodeWithTag("language_selector").performClick(); compose.onNodeWithText("English").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("French Verbs").fetchSemanticsNodes().isNotEmpty() }
    }
    @Test fun allVisibilitySurvivesRotationAndRecreation() {
        reset()
        compose.onNodeWithTag("next").performClick()
        compose.onNodeWithTag("choice_er").performScrollTo().performClick()
        compose.onNodeWithTag("next").performClick()
        compose.onNodeWithTag("choice_all").performScrollTo().performClick()
        compose.onNodeWithTag("choice_all").assertDoesNotExist()
        compose.activityRule.scenario.onActivity { it.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        compose.waitUntil(10000) { compose.activity.resources.configuration.orientation==2 }
        compose.onNodeWithTag("choice_all").assertDoesNotExist()
        compose.onNodeWithTag("choice_être").performScrollTo().performClick()
        compose.onNodeWithTag("choice_all").assertIsOff()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("choice_être").assertIsOff()
        compose.onNodeWithTag("choice_all").assertIsOff()
        compose.onNodeWithTag("choice_all").performScrollTo().performClick()
        compose.onNodeWithTag("next").performClick()
        compose.onNodeWithTag("choice_all").performScrollTo().performClick()
        compose.onNodeWithTag("choice_all").assertDoesNotExist()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("choice_all").assertDoesNotExist()
        compose.onNodeWithTag("choice_cer").performScrollTo().performClick()
        compose.onNodeWithTag("choice_all").assertIsOff()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("choice_cer").assertIsOff()
        compose.onNodeWithTag("choice_all").assertIsOff()
    }
    @Test fun savedSessionSnapshotRestoresAndAdvanceCannotOverrun() {
        val handle=SavedStateHandle(); lateinit var model:VerbViewModel
        inst.runOnMainSync { model=VerbViewModel(DataStoreSelectionRepository(inst.targetContext),handle) }
        compose.waitUntil(10000){model.state.value.loaded}
        inst.runOnMainSync { assertTrue(model.startLearn()); model.advance() }
        val before=model.state.value
        lateinit var restored:VerbViewModel
        inst.runOnMainSync { restored=VerbViewModel(DataStoreSelectionRepository(inst.targetContext),SavedStateHandle(handle.keys().associateWith { handle.get<Any>(it) })) }
        assertEquals(before.session,restored.state.value.session); assertEquals(before.index,restored.state.value.index)
        inst.runOnMainSync { while(restored.advance()) { }; val index=restored.state.value.index; assertFalse(restored.advance()); assertEquals(index,restored.state.value.index) }
    }
}
