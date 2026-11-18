package com.templesoftware.frenchverbs

import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.templesoftware.frenchverbs.data.preferences.AppLanguage
import com.templesoftware.frenchverbs.domain.verbs.*
import com.templesoftware.frenchverbs.ui.app.*
import com.templesoftware.frenchverbs.ui.theme.TempleTheme
import com.templesoftware.frenchverbs.ui.verbs.SelectionScreen
import org.junit.*
import org.junit.Assert.*

class Pass48SelectionInteractionTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 @Test fun everyCategoryToggleAllAndLanguages() {
  check(InstrumentationRegistry.getInstrumentation().targetContext.packageName.endsWith(".audit43"))
  var selection by mutableStateOf(Selection(tenses=Choices.tenses.toSet(),types=setOf("er"),er=emptySet()))
  var language by mutableStateOf(AppLanguage.ENGLISH_UK)
  var forwards=0
  compose.activityRule.scenario.onActivity { activity -> activity.setContent {
   LocalizedContent(language) { TempleTheme {
    SelectionScreen("er",VerbState(loaded=true,selection=selection),language,{},
     {id->selection=if(id==null)selection.toggleAll("er")else selection.toggle("er",id)},{forwards++},{},{})
   }}
  }}
  for(lang in listOf(AppLanguage.ENGLISH_UK,AppLanguage.FRENCH)) {
   compose.runOnIdle{language=lang;selection=selection.copy(er=emptySet())}
   compose.onNodeWithTag("next").assertIsNotEnabled()
   for(id in Choices.er) {
    val card=compose.onNodeWithTag("choice_$id")
    card.performScrollTo().performClick().assertIsOn()
    compose.onNodeWithTag("next").assertIsEnabled()
    card.performClick().assertIsOff()
   }
   compose.onNodeWithTag("choice_all").performScrollTo().performClick()
   Choices.er.forEach{compose.onNodeWithTag("choice_$it").assertIsOn()}
   compose.onNodeWithTag("choice_all").assertDoesNotExist()
   compose.onNodeWithTag("choice_ger").performScrollTo().performClick().assertIsOff()
   compose.onNodeWithTag("choice_all").assertExists()
   compose.onNodeWithTag("next").performClick()
   compose.runOnIdle{assertEquals(Choices.er.toSet()-"ger",selection.er)}
  }
  assertEquals(2,forwards)
  compose.runOnIdle{selection=selection.copy(tenses=setOf("present"))}
  compose.onNodeWithTag("choice_special").assertDoesNotExist()
  compose.runOnIdle{selection=selection.copy(tenses=setOf("future"))}
  compose.onNodeWithTag("choice_special").assertExists().assertIsOn()
 }
}
