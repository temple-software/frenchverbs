package com.templesoftware.frenchverbs

import androidx.activity.compose.setContent
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.templesoftware.frenchverbs.data.preferences.*
import com.templesoftware.frenchverbs.domain.verbs.*
import com.templesoftware.frenchverbs.ui.app.*
import com.templesoftware.frenchverbs.ui.navigation.AppNavHost
import com.templesoftware.frenchverbs.ui.theme.TempleTheme
import com.templesoftware.frenchverbs.ui.verbs.*
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RepeatLearnPreservationTest {
    val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val rules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    @Test fun pairedComparisonAndChosenExamplesSurviveSingleRepeatAndBack() {
        lateinit var nav:NavHostController
        lateinit var learn:VerbViewModel
        val session=listOf(LearningItem("aller",0,0),LearningItem("aller",5,1),LearningItem("venir",1,0))
        compose.activityRule.scenario.onActivity { activity ->
            activity.viewModelStore.clear()
            val factory=object:ViewModelProvider.Factory {
                override fun <T:ViewModel> create(modelClass:Class<T>):T {
                    val repo=object:SelectionRepository {
                        override val selections=flowOf(Selection())
                        override suspend fun save(selection:Selection) {error("No stored selection writes expected")}
                    }
                    @Suppress("UNCHECKED_CAST")
                    return VerbViewModel(repo,SavedStateHandle(mapOf("session" to ArrayList(session.map{it.encode()}),"index" to 1))) as T
                }
            }
            learn=ViewModelProvider(activity,factory)[VerbViewModel::class.java]
            activity.setContent {LocalizedContent(AppLanguage.ENGLISH_UK){TempleTheme{
                nav=rememberNavController();AppNavHost(nav,AppLanguage.ENGLISH_UK,{})
            }}}
        }
        compose.waitForIdle()
        compose.waitUntil(10000){learn.state.value.loaded && runCatching{nav.currentBackStackEntry!=null}.getOrDefault(false)}
        compose.runOnIdle{nav.navigate("activities");nav.navigate("learn")}
        compose.onNodeWithTag("learn_panel_comparison").assertExists()
        val origin=learn.state.value
        fun snapshot()=listOf("learn_heading_current","learn_heading_comparison","learn_progress").associateWith {
            compose.onNodeWithTag(it,true).fetchSemanticsNode().config[SemanticsProperties.Text].toString()
        }
        val panels=snapshot()
        lateinit var repeatModel:RepeatViewModel
        compose.activityRule.scenario.onActivity{repeatModel=ViewModelProvider(it)[RepeatViewModel::class.java]}
        compose.mainClock.autoAdvance=false
        compose.onNodeWithTag("learn_repeat").performSemanticsAction(SemanticsActions.OnClick){it()}
        compose.mainClock.advanceTimeBy(400)
        compose.runOnIdle{assertEquals(listOf(origin.current),repeatModel.state.value.session);repeatModel.tick(repeatModel.timeline.duration)}
        compose.mainClock.advanceTimeBy(400)
        assertEquals(origin,learn.state.value);assertSame(origin.session,learn.state.value.session)
        assertEquals(panels,snapshot())
        // The same-tense examples mode is a separate saved UI state from paired comparisons.
        compose.runOnIdle{learn.previous()};compose.mainClock.advanceTimeBy(400)
        compose.onNodeWithTag("learn_examples_toggle").performScrollTo().performSemanticsAction(SemanticsActions.OnClick){it()}
        compose.mainClock.advanceTimeBy(400)
        val examples=(0..5).map{compose.onNodeWithTag("learn_sentence_$it").fetchSemanticsNode().config[SemanticsProperties.Text]}
        val exampleOrigin=learn.state.value
        for(cancel in listOf(false,true)) {
            compose.onNodeWithTag("learn_repeat").performSemanticsAction(SemanticsActions.OnClick){it()}
            compose.mainClock.advanceTimeBy(400)
            compose.runOnIdle {
                if(cancel)compose.activity.onBackPressedDispatcher.onBackPressed()
                else repeatModel.tick(repeatModel.timeline.duration)
            }
            compose.mainClock.advanceTimeBy(400)
            assertEquals(exampleOrigin,learn.state.value);assertSame(exampleOrigin.session,learn.state.value.session)
            assertEquals(examples,(0..5).map{compose.onNodeWithTag("learn_sentence_$it").fetchSemanticsNode().config[SemanticsProperties.Text]})
            val stopped=repeatModel.state.value;compose.mainClock.advanceTimeBy(1000);assertEquals(stopped,repeatModel.state.value)
        }
        compose.mainClock.autoAdvance=true
    }
}
