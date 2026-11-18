package com.templesoftware.frenchverbs

import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.platform.app.InstrumentationRegistry
import com.templesoftware.frenchverbs.data.preferences.AppLanguage
import com.templesoftware.frenchverbs.data.preferences.DataStoreUserPreferencesRepository
import com.templesoftware.frenchverbs.ui.app.LocalizedContent
import com.templesoftware.frenchverbs.ui.navigation.AppNavHost
import com.templesoftware.frenchverbs.ui.theme.TempleTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Stage 2 route boundaries only: no activity content or learning-function checks. */
class Stage2Pass12Test {
    val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val rotationRules=org.junit.rules.RuleChain.outerRule(PreserveRotationRule()).around(compose)
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private fun ready()=compose.waitUntil(15000){compose.onAllNodesWithTag("open_about").fetchSemanticsNodes().isNotEmpty()}
    private fun homepage() {
        repeat(6) {
            if(compose.onAllNodesWithTag("open_learn").fetchSemanticsNodes().isNotEmpty())return
            compose.onNodeWithTag("next").assertIsEnabled().performClick()
            compose.waitForIdle()
        }
        error("Homepage not reached using saved selections")
    }
    private fun systemBack()=inst.runOnMainSync{compose.activity.onBackPressedDispatcher.onBackPressed()}

    @Test fun stage2NavigationBothLanguages() {
        val language=mutableStateOf(AppLanguage.ENGLISH_UK)
        lateinit var nav:NavHostController
        compose.activityRule.scenario.onActivity{it.setContent{
            LocalizedContent(language.value){TempleTheme{
                nav=rememberNavController()
                AppNavHost(nav,language.value,{language.value=it})
            }}
        }}
        ready()
        for(lang in AppLanguage.entries) {
            compose.onNodeWithTag("language_selector").performClick()
            compose.onNodeWithText(if(lang==AppLanguage.FRENCH)"Français" else "English").performClick()
            compose.onNodeWithTag("open_about").assertIsDisplayed().performClick()
            compose.onNodeWithTag("about_title").assertTextEquals(if(lang==AppLanguage.FRENCH)"Verbes français" else "French Verbs")
            systemBack();ready()
            compose.onNodeWithTag("open_about").performClick()
            compose.onNode(hasContentDescription("Back") or hasContentDescription("Retour")).performClick()
            ready();homepage()
            val title=if(lang==AppLanguage.FRENCH)"Pratiquer le français" else "Practice French"
            compose.onNodeWithTag("screen_title").assertTextEquals(title)
            compose.onNodeWithTag("language_selector").assertDoesNotExist()
            compose.onNodeWithTag("open_about").assertDoesNotExist()
            for(route in listOf("learn","repeat","conjugate","use")) {
                // Verify destination identity, then both supported return paths.
                compose.onNodeWithTag("open_$route").performClick()
                compose.runOnIdle{assertEquals(route,nav.currentDestination!!.route)}
                compose.onNode(hasContentDescription("Home") or hasContentDescription("Accueil")).performClick()
                compose.runOnIdle{assertEquals("activities",nav.currentDestination!!.route)}
                compose.onNodeWithTag("screen_title").assertTextEquals(title)
                compose.onNodeWithTag("open_$route").performClick()
                compose.runOnIdle{assertEquals(route,nav.currentDestination!!.route)}
                systemBack()
                compose.runOnIdle{assertEquals("activities",nav.currentDestination!!.route)}
                compose.onNodeWithTag("screen_title").assertTextEquals(title)
            }
            compose.onNode(hasContentDescription("Home") or hasContentDescription("Accueil")).performClick()
            compose.runOnIdle{assertEquals("tenses",nav.currentDestination!!.route)}
            ready()
        }
    }

    @Test fun languageAndStage2RoutesSurviveRecreation() {
        val prefs=DataStoreUserPreferencesRepository(inst.targetContext)
        val original=runBlocking{prefs.preferences.first().language}
        try {
            ready()
            for(lang in AppLanguage.entries) {
                compose.onNodeWithTag("language_selector").performClick()
                compose.onNodeWithText(if(lang==AppLanguage.FRENCH)"Français" else "English").performClick()
                compose.waitUntil(15000){runBlocking{prefs.preferences.first().language}==lang}
                compose.onNodeWithTag("open_about").performClick()
                compose.activityRule.scenario.recreate()
                compose.onNodeWithTag("about_title").assertTextEquals(if(lang==AppLanguage.FRENCH)"Verbes français" else "French Verbs")
                systemBack();ready();homepage()
                compose.activityRule.scenario.recreate()
                compose.onNodeWithTag("screen_title").assertTextEquals(if(lang==AppLanguage.FRENCH)"Pratiquer le français" else "Practice French")
                compose.onNodeWithTag("open_learn").assertIsDisplayed()
                assertEquals(lang,runBlocking{prefs.preferences.first().language})
                compose.onNode(hasContentDescription("Home") or hasContentDescription("Accueil")).performClick()
                ready()
            }
        } finally {runBlocking{prefs.setLanguage(original)}}
    }
}
