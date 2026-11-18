package com.templesoftware.frenchverbs.domain.quiz

import com.templesoftware.frenchverbs.domain.verbs.*
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class ConjugateSelectionTest {
    private val all=Selection(Choices.tenses.toSet(),Choices.types.toSet(),Choices.important.toSet(),Choices.er.toSet(),Choices.families.toSet())
    private fun candidates(s:Selection=all)=ConjugateSelectionActions.candidates(LearningSequence.create(s,random=Random(7)),s)
    @Test fun zeroOneSixSevenAndLargeRouting() {
        assertEquals(ConjugateChoicePage.EMPTY,ConjugateSelectionActions.begin(emptyList()).page)
        val source=candidates()
        for(n in listOf(1,6)) {
            val s=ConjugateSelectionActions.begin(source.take(n));assertTrue(s.done);assertEquals(source.take(n).map{it.item},s.accepted)
        }
        for(n in listOf(7,source.size))assertEquals(ConjugateChoicePage.TODAY,ConjugateSelectionActions.begin(source.take(n)).page)
    }
    @Test fun singleAndMultipleTensesAndCategories() {
        val one=all.copy(tenses=setOf("present"),types=setOf("important"))
        val direct=ConjugateSelectionActions.begin(candidates(one),Random(1));assertTrue(direct.done);assertEquals(6,direct.current.size)
        assertEquals(ConjugateChoicePage.TYPE,ConjugateSelectionActions.begin(candidates(all.copy(tenses=setOf("present")))).page)
        val onlyCategory=ConjugateSelectionActions.begin(candidates(all.copy(types=setOf("important"))))
        assertEquals(ConjugateChoicePage.TODAY,onlyCategory.page)
        assertTrue(ConjugateSelectionActions.openTypes(onlyCategory).done)
        assertEquals(ConjugateChoicePage.TODAY,ConjugateSelectionActions.begin(candidates()).page)
    }
    @Test fun samplingWithoutReplacementPreservesOrderAndNeverResamplesDone() {
        val source=candidates();val s=ConjugateSelectionActions.begin(source)
        val result=ConjugateSelectionActions.random(s,Random(4))
        assertEquals(6,result.accepted.size);assertEquals(6,result.accepted.distinctBy{it.verbId to it.tense}.size)
        val positions=result.accepted.map{source.map{c->c.item}.indexOf(it)};assertEquals(positions.sorted(),positions)
        assertSame(result,ConjugateSelectionActions.random(result));assertSame(result,ConjugateSelectionActions.chooseCategory(result,"er"))
        assertEquals(source,result.original)
    }
    @Test fun tenseFilteringDirectCapOrMoreCategories() {
        val full=ConjugateSelectionActions.openTenses(ConjugateSelectionActions.begin(candidates()))
        assertEquals((0..5).toList(),full.originalTenses)
        val type=ConjugateSelectionActions.chooseTense(full,2);assertEquals(ConjugateChoicePage.TYPE,type.page);assertTrue(type.current.all{it.tense==2})
        val few=all.copy(types=setOf("important"),important=Choices.important.take(3).toSet())
        val direct=ConjugateSelectionActions.chooseTense(ConjugateSelectionActions.openTenses(ConjugateSelectionActions.begin(candidates(few))),1)
        assertTrue(direct.done);assertEquals(3,direct.current.size)
        val capped=ConjugateSelectionActions.chooseTense(ConjugateSelectionActions.openTenses(ConjugateSelectionActions.begin(candidates(all.copy(types=setOf("important"))))),3)
        assertTrue(capped.done);assertEquals(6,capped.current.size);assertTrue(capped.current.all{it.tense==3})
    }
    @Test fun categoryMetadataIncludesImportantIrregularsAndNoSubcategories() {
        val source=candidates();assertEquals(Choices.types.toSet(),source.map{it.category}.toSet())
        for(id in Choices.important)assertTrue(source.filter{it.item.verbId==id}.all{it.category=="important"})
        for(c in Choices.types) {
            val s=ConjugateSelectionActions.openTypes(ConjugateSelectionActions.begin(source))
            val done=ConjugateSelectionActions.chooseCategory(s,c)
            assertTrue(done.done);assertTrue(done.current.size in 1..6)
            assertTrue(done.current.all{item->source.first{it.item==item}.category==c})
        }
        val selected=all.copy(types=setOf("er","ir"))
        assertEquals(listOf("er","ir"),ConjugateSelectionActions.openTypes(ConjugateSelectionActions.begin(candidates(selected))).categories)
    }
    @Test fun changedTenseAndBackRestoreAllOriginalOptions() {
        val s=ConjugateSelectionActions.begin(candidates());val source=s.original
        val first=ConjugateSelectionActions.chooseTense(ConjugateSelectionActions.openTenses(s),0)
        assertTrue(first.current.all{it.tense==0})
        val returned=ConjugateSelectionActions.openTenses(first)
        assertEquals(source.map{it.item},returned.current);assertEquals((0..5).toList(),returned.originalTenses)
        val second=ConjugateSelectionActions.chooseTense(returned,5)
        assertTrue(second.current.all{it.tense==5});assertEquals(source,second.original)
        assertEquals(ConjugateChoicePage.TENSE,ConjugateSelectionActions.back(second)!!.page)
        assertEquals(ConjugateChoicePage.TODAY,ConjugateSelectionActions.back(returned)!!.page)
        assertNull(ConjugateSelectionActions.back(s))
    }
    @Test fun uniqueVerbCountsAndNumberWords() {
        val s=ConjugateSelectionActions.openTenses(ConjugateSelectionActions.begin(candidates(all.copy(types=setOf("important")))))
        assertEquals(60,s.current.size);assertEquals(10,s.verbs.size)
        assertEquals("eight",verbCountWord(8,false));assertEquals("huit",verbCountWord(8,true))
        assertEquals("one",verbCountWord(1,false));assertEquals("un",verbCountWord(1,true))
        assertEquals("twenty-eight",verbCountWord(28,false));assertEquals("vingt et un",verbCountWord(21,true))
        assertEquals("soixante et onze",verbCountWord(71,true));assertEquals("quatre-vingts",verbCountWord(80,true))
    }
    @Test fun duplicateConjugationsAreRemovedAndSerializationRoundTrips() {
        val source=candidates();val duplicated=source+source.map{it.copy(item=it.item.copy(example=1-it.item.example))}
        val s=ConjugateSelectionActions.begin(duplicated)
        assertEquals(source,s.original)
        source.forEach{assertEquals(it,ConjugateCandidate.decode(it.encode()))}
        assertNull(ConjugateCandidate.decode("broken"))
    }
}
