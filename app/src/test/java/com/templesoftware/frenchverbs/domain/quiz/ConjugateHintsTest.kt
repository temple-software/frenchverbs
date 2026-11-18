package com.templesoftware.frenchverbs.domain.quiz

import com.templesoftware.frenchverbs.domain.verbs.LearningItem
import org.junit.Assert.*
import org.junit.Test

class ConjugateHintsTest {
    private fun q(id:String="être",tense:Int=0)=QuizQuestion.from(LearningItem(id,tense,0))
    private fun state(id:String="être",tense:Int=0)=QuizState(session=listOf(LearningItem(id,tense,0)))
    private fun wrong(s:QuizState,q:QuizQuestion):QuizState {
        val expected=q.rows[s.active].answer.getOrNull(s.hints[s.active].length)
        val c=q.keys.first{it!=expected}
        return QuizActions.submit(QuizActions.letter(QuizActions.clear(s),q,c),q)
    }
    @Test fun secondMistakeRevealsFirstLetterAndLaterMistakesAddOne() {
        val q=q();var s=state()
        s=QuizActions.tick(wrong(s,q),650);assertEquals("",s.hints[0]);assertEquals(1,s.mistakes[0])
        s=wrong(s,q);assertEquals("",s.hints[0]);s=QuizActions.tick(s,649);assertEquals("",s.hints[0])
        s=QuizActions.tick(s,1);assertEquals("s",s.hints[0]);assertEquals("s",s.entries[0]);assertEquals(0,s.active)
        s=QuizActions.tick(wrong(s,q),650);assertEquals("su",s.hints[0])
        s=QuizActions.tick(wrong(s,q),650);assertEquals("sui",s.hints[0])
        s=QuizActions.tick(wrong(s,q),650);assertEquals("suis",s.hints[0]);assertEquals(setOf(0),s.locked);assertEquals(1,s.active)
    }
    @Test fun commonStemPlusOneAndExistingRegularStem() {
        val q=q("prendre",1);var s=state("prendre",1)
        assertEquals("pren",q.commonStem)
        repeat(2){s=QuizActions.tick(wrong(s,q),650)}
        assertEquals("pren",s.hints[0].take(4));assertEquals(5,s.hints[0].length)
        val regular=q("parler",0);var r=state("parler",0)
        repeat(2){r=QuizActions.tick(wrong(r,regular),650)}
        assertEquals("e",r.entries[0]);assertTrue(0 in r.locked)
    }
    @Test fun clearAndEmptySkipPreserveHintsWithoutCountingAnotherMistake() {
        val q=q();var s=state();repeat(2){s=QuizActions.tick(wrong(s,q),650)}
        s=QuizActions.letter(s,q,'u');s=QuizActions.clear(s);assertEquals("s",s.entries[0])
        val skipped=QuizActions.submit(s,q);assertEquals(1,skipped.active);assertEquals(2,skipped.mistakes[0]);assertEquals(0L,skipped.feedbackRemaining)
        s=QuizActions.submit(skipped,q,0);assertEquals("s",s.entries[0]);assertEquals(0,s.active)
        "uis".forEach{s=QuizActions.letter(s,q,it)};s=QuizActions.submit(s,q)
        assertTrue(0 in s.locked);assertEquals("s",s.hints[0]);assertEquals("suis",s.entries[0])
    }
    @Test fun fullyHintedLastRowCompletesQuestion() {
        val q=q();var s=state().copy(locked=setOf(0,1,2,3,4),active=5)
        while(!s.done)s=QuizActions.tick(wrong(s,q),650)
        assertEquals(-1,s.active);assertEquals(100,s.progress);assertEquals("sont",s.entries[5])
        assertEquals(s,QuizActions.clear(s));assertEquals(s,QuizActions.nextQuestion(s))
    }
}
