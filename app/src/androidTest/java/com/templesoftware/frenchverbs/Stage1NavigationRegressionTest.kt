package com.templesoftware.frenchverbs

import androidx.activity.compose.setContent
import androidx.compose.runtime.key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.templesoftware.frenchverbs.data.preferences.*
import com.templesoftware.frenchverbs.domain.verbs.*
import com.templesoftware.frenchverbs.ui.app.TempleEducationApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

/** Real navigation/persistence integration. Stop at arrival on activities. */
class Stage1NavigationRegressionTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private val repo get()=DataStoreSelectionRepository(context)
    private lateinit var original:Selection
    private lateinit var originalLanguage:AppLanguage
    private var generation=0
    @Before fun preserveOwnerState() {
        original=runBlocking{repo.selections.first()}
        originalLanguage=runBlocking{DataStoreUserPreferencesRepository(context).preferences.first().language}
        runBlocking{DataStoreUserPreferencesRepository(context).setLanguage(AppLanguage.ENGLISH_UK)}
    }
    @After fun restoreOwnerState() {
        compose.activityRule.scenario.onActivity{it.viewModelStore.clear()}
        runBlocking {repo.save(original);DataStoreUserPreferencesRepository(context).setLanguage(originalLanguage)}
    }
    private fun ready()=compose.waitUntil(15000){compose.onAllNodesWithTag("next").fetchSemanticsNodes().isNotEmpty()}
    private fun seed(s:Selection=Selection()) {
        compose.waitUntil(15000){compose.onAllNodesWithTag("next").fetchSemanticsNodes().isNotEmpty() || compose.onAllNodesWithTag("open_learn").fetchSemanticsNodes().isNotEmpty()}
        repeat(5) {
            if(compose.onAllNodesWithTag("choice_present").fetchSemanticsNodes().isEmpty())back()
        }
        card("present").assertExists()
        // Production loads preferences once per ViewModel. Start the test fixture
        // with a fresh ViewModel at Tenses rather than writing beneath a live model.
        compose.activityRule.scenario.onActivity{it.viewModelStore.clear()}
        runBlocking{repo.save(s)}
        compose.activityRule.scenario.recreate()
        ready()
    }
    private fun card(id:String)=compose.onNodeWithTag("choice_$id")
    private fun click(id:String)=card(id).performScrollTo().performClick()
    private fun next(){compose.onNodeWithTag("next").assertIsEnabled().performClick();compose.waitForIdle()}
    private fun back(){compose.activityRule.scenario.onActivity{it.onBackPressedDispatcher.onBackPressed()};compose.waitForIdle()}
    private fun defaults(section:String,ids:List<String>,count:Int) {
        ids.forEachIndexed{i,id->val node=card(id).performScrollTo();if(i<count)node.assertIsOn()else node.assertIsOff()}
        assertEquals(ids.size,compose.onAllNodes(isToggleable()).fetchSemanticsNodes().size)
    }
    @Test fun firstUseLanguageMenusBackPersistenceAndActivityArrival() {
        seed()
        defaults("tenses",Choices.tenses,1)
        click("present");compose.onNodeWithTag("next").assertIsNotEnabled();click("present");click("future")
        compose.onNodeWithTag("language_selector").performClick();compose.onNodeWithText("Français").performClick()
        compose.waitUntil(15000){compose.onAllNodesWithText("Verbes français").fetchSemanticsNodes().isNotEmpty()}
        card("present").assertIsOn();card("future").assertIsOn()
        compose.onNodeWithTag("language_selector").performClick();compose.onNodeWithText("English").performClick()
        compose.waitUntil(15000){compose.onAllNodesWithText("French Verbs").fetchSemanticsNodes().isNotEmpty()}
        next();defaults("types",Choices.types,1)
        click("important");compose.onNodeWithTag("next").assertIsNotEnabled();click("important")
        listOf("er","ir","irregular").forEach(::click)
        next();defaults("important",Choices.important,4);compose.onNodeWithTag("choice_all").assertDoesNotExist()
        click("être");click("vouloir");back();next();card("être").assertIsOff();card("vouloir").assertIsOn()
        next();Choices.er.forEachIndexed{i,id->val n=card(id).performScrollTo();if(i<4)n.assertIsOn()else n.assertIsOff()}
        click("all");compose.onNodeWithTag("choice_all").assertDoesNotExist();click("cer")
        back();next();card("cer").assertIsOff();compose.onNodeWithTag("choice_all").assertExists()
        next();defaults("families",Choices.families,3);compose.onNodeWithTag("choice_all").assertDoesNotExist()
        Choices.families.take(3).forEach(::click);compose.onNodeWithTag("next").assertIsNotEnabled()
        click("peindre");back();next();card("peindre").assertIsOn()
        compose.activityRule.scenario.recreate();ready();card("peindre").assertIsOn();card("partir").assertIsOff()
        assertEquals(setOf("peindre"),runBlocking{repo.selections.first()}.families)
        next();compose.onNodeWithTag("open_learn").assertIsDisplayed() // Arrival only; do not enter Stage 2.
    }
    @Test fun allFifteenNonemptyTypeCombinationsUseCorrectConditionalRoutes() {
        for(mask in 1..15) {
            val selected=Choices.types.filterIndexed{i,_->mask and (1 shl i)!=0}.toSet()
            seed(Selection(types=selected));next()
            Choices.types.forEach{if(it in selected)card(it).assertIsOn()else card(it).assertIsOff()}
            next()
            if("important" in selected){card("être").assertExists();next()}
            if("er" in selected){card("standard").assertExists();card("special").assertDoesNotExist();next()}
            if("irregular" in selected){card("partir").assertExists();next()}
            compose.onNodeWithTag("open_learn").assertIsDisplayed()
        }
    }
    @Test fun changingTensesAndTypesOnBackNavigationRecomputesVisibleRoutes() {
        seed(Selection(types=setOf("er","irregular"),tenses=setOf("present","future")))
        next();next();card("special").assertExists();click("special")
        back();back();click("future");next();next();card("special").assertDoesNotExist()
        click("all");compose.onNodeWithTag("choice_all").assertDoesNotExist()
        back();back();click("future");next();next();card("special").assertIsOn()
        back();click("er");next();card("partir").assertExists()
        back();click("irregular");compose.onNodeWithTag("next").assertIsNotEnabled()
        click("ir");next();compose.onNodeWithTag("open_learn").assertIsDisplayed()
    }
}
