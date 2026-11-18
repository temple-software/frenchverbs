package com.templesoftware.frenchverbs.domain.use
import com.templesoftware.frenchverbs.domain.verbs.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class UseSessionTest {
    @Test fun randomCapacityAndResultsStars() {
        assertEquals(listOf(1,1,1,1,1,1,2,3,4,4,5),(0..10).map(::useStars))
        val seen=mutableSetOf<Int>()
        repeat(40){seed->UseSessions.from(listOf(LearningItem("pouvoir",1,0)),Random(seed)).forEach{q->
            assertTrue(q.boxCount in q.answer.length..q.quiz.boxCount);assertEquals(q.boxCount,UseSessions.decode(q.encode()).boxCount)
            if(q.person==0)seen+=q.boxCount
        }}
        assertTrue(seen.size>1)
    }

    @Test fun tenQuestionsDistributionAndUniqueness() {
        val all=OfflineDictionary.verbs.take(12).map{LearningItem(it.id,0,0)}
        for(count in listOf(1,4,10,12))for(seed in 0..25) {
            val q=UseSessions.from(all.take(count),Random(seed));assertEquals(10,q.size)
            val persons=q.groupingBy{it.person}.eachCount();assertEquals(6,persons.size);assertEquals(4,persons.values.count{it==2});assertEquals(2,persons.values.count{it==1})
            if(count>=10)assertEquals(10,q.map{it.verbId to it.tense}.distinct().size)
            assertEquals(10,q.map{it.sentence}.distinct().size)
            assertEquals(q,q.map{UseSessions.decode(it.encode())})
            assertEquals(q,UseSessions.from(all.take(count),Random(seed)))
        }
        val genders=(0..60).flatMap{UseSessions.from(all,Random(it))}.filter{it.person in listOf(2,5)}.groupBy{it.person}
        for(group in genders.values)assertEquals(setOf(false,true),group.map{it.feminine}.toSet())
    }
    @Test fun selectedPoolIncludesCompleteFamilies() {
        val selection=Selection(tenses=setOf("present","perfect"),types=setOf("er","irregular"),er=setOf("standard"),families=setOf("partir"))
        val pool=UseSessions.available(selection,Random(1))
        val expected=OfflineDictionary.verbs.filter{it.group in setOf("standard","partir")}
        assertEquals(expected.size*2,pool.size);assertEquals(expected.map{it.id}.toSet(),pool.map{it.verbId}.toSet());assertEquals(setOf(0,3),pool.map{it.tense}.toSet())
    }
    @Test fun exhaustiveDictionarySentenceAndKeyCoverage() {
        var total=0
        for(v in OfflineDictionary.verbs)for(t in 0..5)for(p in 0..5)for(f in listOf(false,true))for(ask in listOf(false,true)) {
            val candidates=UseSentences.candidates(LearningItem(v.id,t,0),p,f,ask)
            assertTrue("${v.id}/$t/$p/$f",candidates.isNotEmpty())
            for(q in candidates) {
                assertEquals(v.id,q.verbId);assertEquals(t,q.tense);assertEquals(p,q.person)
                assertFalse(q.sentence.contains("("));assertFalse(q.sentence.contains("il/elle"))
                assertEquals(q.answer,q.form.filter(Char::isLetter));assertTrue(q.answer.all{it in q.keys});assertEquals(q.keys.size,q.keys.distinct().size)
                assertTrue(q.boxCount>=q.answer.length);assertEquals(q.boxCount,q.quiz.boxCount)
                assertTrue(q.keys.size>=9);assertEquals(q.keys.toSet(),q.quiz.keys.toSet())
                if(t in listOf(3,5)&&v.auxiliary=="être") {
                    val a=SentenceBank.Agreement(if(f)SentenceBank.Gender.FEMININE else SentenceBank.Gender.MASCULINE,p>=3)
                    assertTrue(q.form.endsWith(SentenceBank.participles.getValue(v.id).getValue(a)))
                }
                if(p==0)assertTrue(if(q.form.first().lowercaseChar() in "aeiouéèêàâîôûœ")q.prefix.endsWith("j’")else q.prefix.endsWith("je "))
                total++
            }
        }
        println("Audited $total verb-bound sentence realizations")
    }
    private fun first(id:String="parler",tense:Int=0)=UseSentences.candidates(LearningItem(id,tense,0),0,false,false).first()
    private fun wrong(s:UseState):UseState {val q=s.current!!;val c=q.keys.first{it!=q.answer.getOrNull(s.input.hints[q.person].length)};return UseActions.submit(UseActions.letter(UseActions.clear(s),c))}
    @Test fun feedbackHintsEditingAndPersistentRedScore() {
        val q=first();var s=UseActions.start(List(10){q})
        assertEquals(s,UseActions.submit(s));s=wrong(s);assertEquals(650L,s.input.feedbackRemaining);assertEquals(-1,s.outcomes[0])
        assertEquals(s,UseActions.submit(s));assertEquals(s,UseActions.letter(s,'a'));assertEquals(s,UseActions.clear(s));assertEquals(s,UseActions.edit(s,0))
        s=UseActions.tick(s,649);assertEquals(1L,s.input.feedbackRemaining);s=UseActions.tick(s,1);assertEquals("",s.input.entries[0]);assertEquals(0,s.index)
        s=UseActions.tick(wrong(s),650);assertEquals(q.answer.take(q.quiz.commonStem.length+1),s.input.hints[0])
        val hint=s.input.hints[0];s=UseActions.letter(s,'s');s=UseActions.edit(s,0);assertEquals(hint,s.input.entries[0]);s=UseActions.clear(s);assertEquals(hint,s.input.entries[0])
        while(s.successRemaining==0L)s=UseActions.tick(wrong(s),650)
        assertEquals(-1,s.outcomes[0]);assertEquals(0,s.score);assertEquals(800L,s.successRemaining)
        s=UseActions.tick(s,799);assertEquals(0,s.index);s=UseActions.tick(s,1);assertEquals(1,s.index);assertEquals(-1,s.outcomes[0]);assertTrue(s.attempts[0]>=2);assertEquals(q.answer,s.revealed[0])
    }
    @Test fun fullFormsGreenAdvanceAndTenQuestionCompletion() {
        val q=first("aller",3);var s=UseActions.start(List(10){q})
        repeat(10) { i ->
            q.answer.forEach{s=UseActions.letter(s,it)};assertEquals(q.answer,s.input.entries[0]);s=UseActions.submit(s)
            assertEquals(800L,s.successRemaining);assertEquals(1,s.outcomes[i]);assertEquals(s,UseActions.submit(s));assertEquals(s,UseActions.clear(s))
            s=UseActions.tick(s,800);assertEquals(i+1,s.index)
        }
        assertTrue(s.complete);assertEquals(10,s.score);assertEquals(s,UseActions.tick(s,800))
        var edit=UseActions.start(List(10){first("pouvoir",1)})
        "pouvai".forEach{edit=UseActions.letter(edit,it)};edit=UseActions.edit(edit,3);assertEquals("pou",edit.input.entries[0]);assertEquals(edit,UseActions.edit(edit,6))
    }
}
