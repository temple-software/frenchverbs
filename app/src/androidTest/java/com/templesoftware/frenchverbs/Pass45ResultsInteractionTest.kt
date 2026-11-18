package com.templesoftware.frenchverbs

import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.templesoftware.frenchverbs.data.preferences.AppLanguage
import com.templesoftware.frenchverbs.domain.verbs.*
import com.templesoftware.frenchverbs.domain.quiz.*
import com.templesoftware.frenchverbs.domain.use.*
import com.templesoftware.frenchverbs.ui.app.LocalizedContent
import com.templesoftware.frenchverbs.ui.quiz.ConjugateResultsScreen
import com.templesoftware.frenchverbs.ui.use.UseScreen
import com.templesoftware.frenchverbs.ui.theme.TempleTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import kotlin.random.Random

class Pass45ResultsInteractionTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun useReviewExpandsCollapsesAndHomeRemainsVisible() {
        val qs=UseSessions.create(Selection(tenses=Choices.tenses.toSet()),Random(45))
        val state=UseState(questions=qs,index=10,outcomes=List(10){-1})
        var home=0
        compose.activityRule.scenario.onActivity{it.setContent{LocalizedContent(AppLanguage.FRENCH){TempleTheme{UseScreen(state,AppLanguage.FRENCH,{home++},{},{},{},{})}}}}
        compose.onNodeWithTag("use_review_toggle").assertIsDisplayed().performClick()
        compose.onAllNodesWithTag("use_review_0").assertCountEquals(1)
        compose.onNodeWithTag("use_home").assertIsDisplayed()
        compose.onNodeWithTag("use_review_toggle").performClick()
        compose.onAllNodesWithTag("use_review_0").assertCountEquals(0)
        compose.onNodeWithTag("use_home").performClick()
        assertEquals(1,home)
        assertEquals(0,state.score)
    }
    @Test fun conjugateCollapsedGroupsExpandWithoutChangingScore() {
        val initial=results41Fixture(6)
        val model=ConjugateResults(initial)
        var state by mutableStateOf(initial.copy(resultToggles=model.groups.keys.filter{model.initiallyExpanded(it)}.toSet()))
        val before=ConjugateResults(state).score
        var home=0
        compose.activityRule.scenario.onActivity{it.setContent{LocalizedContent(AppLanguage.ENGLISH_UK){TempleTheme{ConjugateResultsScreen(state,AppLanguage.ENGLISH_UK,{t->state=state.copy(resultToggles=if(t in state.resultToggles)state.resultToggles-t else state.resultToggles+t)},{home++})}}}}
        compose.onNodeWithTag("conj_result_group_0").assertIsDisplayed().performClick()
        compose.onNodeWithTag("conj_result_line_0",true).assertExists()
        assertEquals(before,ConjugateResults(state).score)
        compose.onNodeWithTag("conj_result_group_0").performClick()
        compose.onNodeWithTag("conj_results_back").assertIsDisplayed().performClick()
        assertEquals(1,home)
    }
}
