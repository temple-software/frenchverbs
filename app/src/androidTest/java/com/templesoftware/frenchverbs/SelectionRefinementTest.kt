package com.templesoftware.frenchverbs

import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.templesoftware.frenchverbs.data.preferences.AppLanguage
import com.templesoftware.frenchverbs.domain.verbs.*
import com.templesoftware.frenchverbs.ui.app.*
import com.templesoftware.frenchverbs.ui.theme.TempleTheme
import com.templesoftware.frenchverbs.ui.verbs.SelectionScreen
import org.junit.Rule
import org.junit.Test

class SelectionRefinementTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val selection=mutableStateOf(Selection())
    private val route=mutableStateOf("important")
    private fun show(section:String) {
        route.value=section
        compose.activityRule.scenario.onActivity { it.setContent {
            LocalizedContent(AppLanguage.ENGLISH_UK) { TempleTheme {
                SelectionScreen(route.value,VerbState(loaded=true,selection=selection.value),AppLanguage.ENGLISH_UK,{},
                    { id->selection.value=if(id==null) selection.value.toggleAll(route.value) else selection.value.toggle(route.value,id) },{},{},{})
            } }
        } }
    }
    private fun click(id:String)=compose.onNodeWithTag("choice_$id").performScrollTo().performClick()
    private fun allCycle(section:String) {
        show(section)
        compose.onNodeWithTag("choice_all").assertIsOff()
        click("all");compose.onNodeWithTag("choice_all").assertDoesNotExist()
        click(selection.value.visible(section).first());compose.onNodeWithTag("choice_all").assertIsOff()
        click("all");compose.onNodeWithTag("choice_all").assertDoesNotExist()
        click(selection.value.visible(section).last());compose.onNodeWithTag("choice_all").assertIsOff()
        click(selection.value.visible(section).last());compose.onNodeWithTag("choice_all").assertDoesNotExist()
    }
    @Test fun importantAllTracksIndividualSelection() { allCycle("important") }
    @Test fun erAllTracksOnlyAvailableCategories() {
        allCycle("er")
        compose.onNodeWithTag("choice_special").assertDoesNotExist()
        compose.runOnIdle { selection.value=selection.value.toggle("tenses","future") }
        compose.onNodeWithTag("choice_all").assertIsOff();compose.onNodeWithTag("choice_special").assertIsOff()
        click("special");compose.onNodeWithTag("choice_all").assertDoesNotExist()
        compose.runOnIdle { selection.value=selection.value.toggle("tenses","future") }
        compose.onNodeWithTag("choice_all").assertDoesNotExist();compose.onNodeWithTag("choice_special").assertDoesNotExist()
        compose.runOnIdle { selection.value=selection.value.toggle("tenses","future") }
        compose.onNodeWithTag("choice_special").assertIsOn();compose.onNodeWithTag("choice_all").assertDoesNotExist()
    }
    @Test fun nineFamiliesIndividualSelectionWithoutAll() {
        show("families");compose.onNodeWithTag("choice_all").assertDoesNotExist()
        Choices.families.forEachIndexed { i,id->
            val node=compose.onNodeWithTag("choice_$id").performScrollTo()
            if(i<3) node.assertIsOn() else node.assertIsOff()
        }
        Choices.families.take(3).forEach(::click)
        compose.onNodeWithTag("next").assertIsNotEnabled()
        Choices.families.forEach { id->click(id);compose.onNodeWithTag("choice_$id").assertIsOn() }
        compose.onNodeWithTag("next").assertIsEnabled()
        compose.onNodeWithText("je pars / nous partons").assertDoesNotExist()
    }
}
