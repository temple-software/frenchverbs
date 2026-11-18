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
import com.templesoftware.frenchverbs.ui.quiz.ConjugateScreen
import com.templesoftware.frenchverbs.ui.use.UseScreen
import com.templesoftware.frenchverbs.ui.theme.TempleTheme
import org.junit.*
import org.junit.Assert.*
import kotlin.random.Random
class Pass46AccessibilityInputTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 private fun tap(tag:String) {compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed().performClick()}
 @Test fun conjugateEntryClearValidateAndRowSelectionAt200Percent() {
  assertTrue(compose.activity.resources.configuration.fontScale>=1.9f)
  val item=LearningItem("reconnaître",1,0);val q=QuizQuestion.from(item)
  var s by mutableStateOf(QuizState(session=listOf(item)))
  compose.activityRule.scenario.onActivity{it.setContent{LocalizedContent(AppLanguage.FRENCH){TempleTheme{ConjugateScreen(s,q,AppLanguage.FRENCH,{}, {s=QuizActions.letter(s,q,it)},{s=QuizActions.clear(s)},{s=QuizActions.submit(s,q,it)},{},{r,i->s=QuizActions.edit(s,r,i)})}}}}
  tap("quiz_key_${q.rows[0].answer.first()}");assertTrue(s.entries[0].isNotEmpty())
  tap("quiz_clear");assertEquals("",s.entries[0])
  q.rows[0].answer.forEach{tap("quiz_key_$it")}
  tap("quiz_enter");assertTrue(0 in s.locked)
  tap("quiz_row_3");assertEquals(3,s.active)
 }
 @Test fun useEntryClearAndValidateAt200Percent() {
  assertTrue(compose.activity.resources.configuration.fontScale>=1.9f)
  val questions=UseSessions.create(Selection(tenses=Choices.tenses.toSet()),Random(46))
  var s by mutableStateOf(UseActions.start(questions));val q=s.current!!
  compose.activityRule.scenario.onActivity{it.setContent{LocalizedContent(AppLanguage.FRENCH){TempleTheme{UseScreen(s,AppLanguage.FRENCH,{}, {s=UseActions.letter(s,it)},{s=UseActions.clear(s)},{s=UseActions.submit(s)},{s=UseActions.edit(s,it)})}}}}
  tap("quiz_key_${q.answer.first()}");assertTrue(s.input.entries[q.person].isNotEmpty())
  tap("quiz_clear");assertEquals("",s.input.entries[q.person])
  q.answer.forEach{tap("quiz_key_$it")}
  tap("quiz_enter");assertTrue(s.successRemaining>0)
 }
}
