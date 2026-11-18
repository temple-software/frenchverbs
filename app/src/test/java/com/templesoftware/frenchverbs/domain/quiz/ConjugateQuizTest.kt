package com.templesoftware.frenchverbs.domain.quiz

import com.templesoftware.frenchverbs.domain.verbs.*
import org.junit.Assert.*
import org.junit.Test

class ConjugateQuizTest {
    private fun question(id:String="parler",tense:Int=0)=QuizQuestion.from(LearningItem(id,tense,0))
    private fun state(id:String="parler",tense:Int=0)=QuizState(session=listOf(LearningItem(id,tense,0),LearningItem("être",0,0)))
    private fun enter(s:QuizState,q:QuizQuestion,text:String)=text.fold(s){a,c->QuizActions.letter(a,q,c)}
    @Test fun verifiedRegularIrregularAndCompoundAnswers() {
        val parler=question();assertEquals(listOf("e","es","e","ons","ez","ent"),parler.rows.map{it.answer})
        assertEquals(3,parler.boxCount);assertTrue(parler.rows.all{it.prefix=="parl"})
        assertEquals(listOf("suis","es","est","sommes","êtes","sont"),question("être").rows.map{it.answer})
        assertEquals(6,question("être").boxCount)
        for(id in listOf("parler","manger","commencer","préférer","envoyer","finir","être","avoir","aller","recevoir","connaître","peindre"))for(tense in 0..5) {
            val q=question(id,tense);val forms=q.verb.tenses[tense].forms
            assertEquals(forms.map{it.text},q.rows.map{it.text})
            assertEquals(q.rows.maxOf{it.answer.length},q.boxCount)
            assertTrue(q.rows.all{it.answer.isNotEmpty() && it.answer.all(Char::isLetter) && it.answer.all{c->c in q.keys}})
            assertEquals(q.keys.size,q.keys.distinct().size)
        }
        val compound=question("aller",5)
        assertEquals("étais ",compound.rows[0].prefix);assertEquals("allé",compound.rows[0].answer);assertEquals("(e)",compound.rows[0].suffix);assertEquals("ils / elles ",compound.rows[5].displaySubject);assertEquals("(e)s",compound.rows[5].displaySuffix)
    }
    @Test fun keypadFillersAndExtendedRequiredLetters() {
        assertEquals("esonz téia".replace(" ",""),question().keys.joinToString(""))
        val q=question("être")
        assertTrue(q.keys.contains('ê'));assertEquals(9,q.keys.size)
        val synthetic=QuizQuestion(q.verb,0,List(6){QuizAnswer("je ","","abcdefghijk","")})
        assertEquals("abcdefghijk",synthetic.keys.joinToString(""))
    }
    @Test fun noCheckingUntilSubmitCorrectLocksAndClearLeavesCompletedRows() {
        val q=question();var s=enter(state(),q,"e")
        assertTrue(s.locked.isEmpty());assertEquals(0L,s.feedbackRemaining)
        s=QuizActions.submit(s,q);assertEquals(setOf(0),s.locked);assertEquals(1,s.active)
        s=enter(s,q,"e");s=QuizActions.clear(s)
        assertEquals("e",s.entries[0]);assertEquals("",s.entries[1]);assertEquals(s,QuizActions.submit(s,q,0))
        s=enter(s,q,"es");s=QuizActions.submit(s,q,4);assertEquals(4,s.active);assertEquals(setOf(0,1),s.locked)
    }
    @Test fun emptySkipWrapAndSelectingRowsNeverErrors() {
        val q=question();var s=state()
        repeat(6){s=QuizActions.submit(s,q);assertEquals(0L,s.feedbackRemaining);assertTrue(s.locked.isEmpty())}
        assertEquals(0,s.active);s=QuizActions.submit(s,q,4);assertEquals(4,s.active)
        assertEquals(s,QuizActions.submit(s,q,4))
    }
    @Test fun wrongPartialAndRapidActionsFreezeForExactly650ms() {
        val q=question();val typed=enter(state(),q,"s")
        val wrong=QuizActions.submit(typed,q,3)
        assertEquals(650L,wrong.feedbackRemaining);assertEquals(0,wrong.active)
        assertEquals(wrong,QuizActions.letter(wrong,q,'e'));assertEquals(wrong,QuizActions.clear(wrong))
        assertEquals(wrong,QuizActions.submit(wrong,q));assertEquals(wrong,QuizActions.submit(wrong,q,4))
        val almost=QuizActions.tick(wrong,649);assertEquals(1L,almost.feedbackRemaining);assertEquals("s",almost.entries[0]);assertFalse(almost.canEdit)
        val cleared=QuizActions.tick(almost,1);assertEquals("",cleared.entries[0]);assertEquals(0,cleared.active);assertTrue(cleared.canEdit)
        val second=QuizActions.submit(enter(state(),q,"e"),q)
        val partial=QuizActions.submit(enter(second,q,"e"),q)
        assertEquals(650L,partial.feedbackRemaining);assertEquals(setOf(0),QuizActions.tick(partial,650).locked)
    }
    @Test fun sixCorrectAnswersCompletionAndNextKeepTheSameSequence() {
        val q=question();var s=state();val session=s.session
        assertEquals(s,QuizActions.nextQuestion(s))
        repeat(6){s=QuizActions.submit(enter(s,q,q.rows[s.active].answer),q)}
        assertTrue(s.done);assertEquals(-1,s.active);assertEquals(50,s.progress)
        assertEquals(s,QuizActions.clear(s));assertEquals(s,QuizActions.letter(s,q,'e'))
        s=QuizActions.nextQuestion(s);assertSame(session,s.session);assertEquals(1,s.index);assertTrue(s.entries.all{it.isEmpty()})
        val irregular=question("être")
        repeat(6){s=QuizActions.submit(enter(s,irregular,irregular.rows[s.active].answer),irregular)}
        assertEquals(100,s.progress);assertEquals(s,QuizActions.nextQuestion(s))
    }
    @Test fun dictionaryAdapterNeverCreatesAnImpossibleQuestion() {
        val questions=OfflineDictionary.verbs.flatMap{verb->(0..5).map{QuizQuestion.from(LearningItem(verb.id,it,0))}}
        for(q in questions) {
            assertEquals(q.verb.tenses[q.tense].forms.map{it.text},q.rows.map{it.text})
            assertEquals(q.keys.distinct().size,q.keys.size)
            assertTrue(q.rows.all{it.answer.all{c->c in q.keys}})
        }
        java.io.File("build/reports/pass31-answer-audit.txt").apply{parentFile.mkdirs();writeText(
            "PASS ${questions.size} quiz adapters reconstruct exact dictionary forms. Max boxes=${questions.maxOf{it.boxCount}}; max keys=${questions.maxOf{it.keys.size}}\n"+
            questions.filter{it.keys.size>9}.joinToString("\n"){"${it.verb.id} / ${Choices.tenses[it.tense]}: ${it.keys.size} keys, ${it.boxCount} boxes"})}
    }

}
