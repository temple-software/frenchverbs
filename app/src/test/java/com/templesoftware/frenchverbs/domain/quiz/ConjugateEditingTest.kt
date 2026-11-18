package com.templesoftware.frenchverbs.domain.quiz

import com.templesoftware.frenchverbs.domain.verbs.LearningItem
import org.junit.Assert.*
import org.junit.Test

class ConjugateEditingTest {
    private val q=QuizQuestion.from(LearningItem("pouvoir",1,0))
    private fun state()=QuizState(session=listOf(LearningItem("pouvoir",1,0)),entries=listOf("pouvais","","","","",""))
    @Test fun firstMiddleAndLastLetterTruncateWithoutValidation() {
        for((position,wanted) in listOf(0 to "",3 to "pou",4 to "pouv",6 to "pouvai")) {
            val s=QuizActions.edit(state(),0,position)
            assertEquals(wanted,s.entries[0]);assertEquals(0L,s.feedbackRemaining);assertEquals(List(6){0},s.mistakes);assertTrue(s.locked.isEmpty());assertEquals(0,s.active)
        }
    }
    @Test fun currentOrLaterEmptyBoxesNeverCreateGaps() {
        val s=QuizActions.edit(state(),0,3)
        for(index in listOf(3,4,8,100,-1))assertEquals(s,QuizActions.edit(s,0,index))
        val next=QuizActions.letter(s,q,'v');assertEquals("pouv",next.entries[0])
    }
    @Test fun hintsAreProtectedAndEditingAfterHintsIsContiguous() {
        val s=state().copy(hints=listOf("pou","","","","",""),mistakes=listOf(2,0,0,0,0,0))
        for(index in 0..3) {
            val edited=QuizActions.edit(s,0,index);assertEquals("pou",edited.entries[0]);assertEquals(s.hints,edited.hints);assertEquals(s.mistakes,edited.mistakes)
        }
        assertEquals("pouv",QuizActions.edit(s,0,4).entries[0]);assertEquals("pou",QuizActions.clear(s).entries[0])
    }
    @Test fun redFeedbackBlocksAllEditingAndStillExpiresAt650ms() {
        val wrong=QuizActions.submit(QuizActions.letter(QuizActions.clear(state()),q,'p'),q)
        assertEquals(650L,wrong.feedbackRemaining)
        for(index in 0..8)assertEquals(wrong,QuizActions.edit(wrong,0,index))
        val almost=QuizActions.tick(wrong,649);assertEquals(almost,QuizActions.edit(almost,0,0))
        assertEquals("",QuizActions.tick(almost,1).entries[0])
    }
    @Test fun completedAndOtherRowsCannotBeEdited() {
        val locked=QuizActions.submit(state(),q);assertTrue(0 in locked.locked)
        assertEquals(locked,QuizActions.edit(locked,0,0))
        assertEquals(state(),QuizActions.edit(state(),1,0))
        assertEquals(state().copy(running=false),QuizActions.edit(state().copy(running=false),0,0))
    }
    @Test fun editedWordCanBeCompletedNormallyWithoutChangingOtherRows() {
        var s=QuizActions.edit(state(),0,3)
        "vais".forEach{s=QuizActions.letter(s,q,it)}
        s=QuizActions.submit(s,q);assertEquals("pouvais",s.entries[0]);assertTrue(0 in s.locked);assertEquals(1,s.active);assertEquals(List(6){0},s.mistakes)
    }
}
