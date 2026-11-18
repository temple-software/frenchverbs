package com.templesoftware.frenchverbs.domain.quiz

import com.templesoftware.frenchverbs.domain.verbs.LearningItem
import org.junit.Assert.*
import org.junit.Test

class ConjugateResults40Test {
    private val item=LearningItem("être",0,0)
    private val q=QuizQuestion.from(item)
    private fun wrong(s:QuizState):QuizState {
        val c=q.keys.first{it!=q.rows[s.active].answer.getOrNull(s.hints[s.active].length)}
        return QuizActions.tick(QuizActions.submit(QuizActions.letter(QuizActions.clear(s),q,c),q),650)
    }
    @Test fun distinctMistakesAndHints() {
        var s=QuizState(session=listOf(item))
        repeat(3){s=wrong(s)}
        assertEquals(listOf(0),s.wrongLines)
        while(0 !in s.locked)s=wrong(s)
        assertEquals(listOf(0),s.wrongLines)
        assertEquals(q.rows[0].answer,s.entries[0])
        s=wrong(s);assertEquals(listOf(0,1),s.wrongLines)
    }
    @Test fun emptyClearEditingAndCorrectAreNotMistakes() {
        var s=QuizState(session=listOf(item))
        s=QuizActions.submit(s,q);s=QuizActions.submit(s,q,3)
        s=QuizActions.clear(QuizActions.letter(s,q,q.keys.first()))
        s=QuizActions.edit(QuizActions.letter(s,q,q.keys.first()),3,0)
        assertTrue(s.wrongLines.isEmpty())
        s=QuizActions.clear(s)
        q.rows[3].answer.forEach{s=QuizActions.letter(s,q,it)}
        s=QuizActions.submit(s,q);assertTrue(3 in s.locked);assertTrue(s.wrongLines.isEmpty())
    }
    @Test fun normalizationAndPerfectOnlyFiveStars() {
        for(total in listOf(6,12,18,36)) {
            assertEquals(5,ConjugateScore(total,0).stars)
            assertTrue(ConjugateScore(total,1).stars<5)
            for(wrong in 0..total)assertEquals(total-wrong,ConjugateScore(total,wrong).correctLines)
        }
        assertEquals(9,ConjugateScore(36,1).scaledScore)
        assertEquals(4,ConjugateScore(36,6).stars)
        assertEquals(3,ConjugateScore(36,11).stars)
        for((correct,stars) in listOf(10 to 5,9 to 4,8 to 4,7 to 3,6 to 2,5 to 1,0 to 1))assertEquals(stars,ConjugateScore(10,10-correct).stars)
    }
    @Test fun historyFullFormsOrderingGroupsAndSerialization() {
        val items=listOf(LearningItem("jouer",0,0),LearningItem("aller",3,0),LearningItem("jouer",0,0))
        var s=QuizState(session=items)
        items.forEachIndexed { i,it ->
            val question=QuizQuestion.from(it)
            s=s.copy(locked=(0..5).toSet(),entries=question.rows.map{it.answer},active=-1,wrongLines=s.wrongLines+listOf(i*6+4,i*6))
            s=recordConjugateCompletion(s,question)
            assertEquals(s,recordConjugateCompletion(s,question))
            if(!s.last)s=QuizActions.nextQuestion(s)
        }
        val restored=s.copy(completedLines=s.completedLines.map{ConjugateResultLine.decode(it.encode())})
        assertEquals(s,restored)
        val r=ConjugateResults(restored)
        assertEquals(18,r.score.totalLines);assertEquals(6,r.score.wrongLines)
        assertEquals(listOf(0,3),r.groups.keys.toList())
        assertEquals(listOf(4,0,16,12),r.groups.getValue(0).map{it.id})
        assertFalse(r.initiallyExpanded(0));assertTrue(r.initiallyExpanded(3))
        assertEquals("je joue",r.groups.getValue(0)[1].conjugation)
        assertEquals(QuizQuestion.from(items[1]).rows[0].text,r.groups.getValue(3)[1].conjugation)
        assertEquals(6,ConjugateResults(restored.copy(wrongLines=s.wrongLines+s.wrongLines)).score.wrongLines)
    }
    @Test fun defaultExpansionAndFullyHintedCompletion() {
        var s=QuizState(session=listOf(item))
        while(!s.done)s=wrong(s)
        s=recordConjugateCompletion(s,q)
        assertEquals(6,ConjugateResults(s).score.wrongLines)
        assertEquals(1,ConjugateResults(s).score.stars)
        assertEquals(q.rows.map{it.text},s.completedLines.map{it.conjugation})
        for(count in 1..4) {
            val r=ConjugateResults(s.copy(wrongLines=(0 until count).toList()))
            assertEquals(count<=3,r.initiallyExpanded(0))
        }
    }

}
