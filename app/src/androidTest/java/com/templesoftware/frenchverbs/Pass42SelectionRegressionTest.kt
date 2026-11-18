package com.templesoftware.frenchverbs
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.compose.rememberNavController
import com.templesoftware.frenchverbs.data.preferences.*
import com.templesoftware.frenchverbs.domain.verbs.*
import com.templesoftware.frenchverbs.ui.app.*
import com.templesoftware.frenchverbs.ui.navigation.AppNavHost
import com.templesoftware.frenchverbs.ui.theme.TempleTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*

class Pass42SelectionRegressionTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 private class Memory(s:Selection):SelectionRepository {
  override val selections=MutableStateFlow(s)
  override val completedSelections=MutableStateFlow<Selection?>(null)
  override suspend fun save(selection:Selection){selections.value=selection}
  override suspend fun complete(selection:Selection){save(selection);completedSelections.value=selection}
 }
 @Test fun allCategoryRoutesLanguageAndCommitBoundary() {
  var generation by mutableIntStateOf(0);var lang by mutableStateOf(AppLanguage.ENGLISH_UK)
  var repository=Memory(Selection());var model by mutableStateOf(VerbViewModel(repository,SavedStateHandle()))
  compose.activityRule.scenario.onActivity{it.setContent{key(generation){LocalizedContent(lang){TempleTheme{AppNavHost(rememberNavController(),lang,{lang=it},model)}}}}}
  fun next(){compose.onNodeWithTag("next").assertIsEnabled().performClick()}
  for(mask in 1..15) {
   val types=Choices.types.filterIndexed{i,_->mask and (1 shl i)!=0}.toSet()
   compose.runOnIdle{repository=Memory(Selection(types=types));model=VerbViewModel(repository,SavedStateHandle());generation++}
   compose.waitUntil(10000){model.state.value.loaded};compose.onNodeWithTag("choice_present").assertIsOn()
   if(mask==1) {
    compose.onNodeWithTag("choice_present").performClick();compose.onNodeWithTag("next").assertIsNotEnabled()
    compose.onNodeWithTag("choice_present").performClick()
    compose.onNodeWithTag("language_selector").performClick();compose.onNodeWithText("Français").performClick()
    compose.onNodeWithText("Verbes français").assertExists();compose.onNodeWithTag("choice_present").assertIsOn()
    compose.onNodeWithTag("language_selector").performClick();compose.onNodeWithText("English").performClick()
   }
   next();Choices.types.forEach{if(it in types)compose.onNodeWithTag("choice_$it").assertIsOn()else compose.onNodeWithTag("choice_$it").assertIsOff()}
   next()
   if("important" in types){compose.onNodeWithTag("choice_être").assertExists();assertNull(repository.completedSelections.value);next()}
   if("er" in types){compose.onNodeWithTag("choice_standard").assertExists();compose.onNodeWithTag("choice_special").assertDoesNotExist();assertNull(repository.completedSelections.value);next()}
   if("irregular" in types){compose.onNodeWithTag("choice_partir").assertExists();assertNull(repository.completedSelections.value);next()}
   compose.onNodeWithTag("open_learn").assertExists();assertEquals(types,repository.completedSelections.value!!.types)
  }
 }
}
