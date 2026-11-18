package com.templesoftware.frenchverbs.domain.verbs

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random
import java.text.Normalizer

class VerbDomainTest {
    @Test fun defaultsAndAllAndEmptyAreExact() {
        var s=Selection()
        assertEquals(setOf("present"),s.tenses); assertEquals(setOf("important"),s.types)
        assertEquals(Choices.important.take(4).toSet(),s.important)
        assertEquals(Choices.er.take(4).toSet(),s.er); assertEquals(Choices.families.take(3).toSet(),s.families)
        for(section in listOf("important","er","families")) {
            assertFalse(s.all(section)); s=s.toggleAll(section); assertTrue(s.all(section))
            val first=s.visible(section).first(); s=s.toggle(section,first); assertFalse(s.all(section))
            s=s.toggle(section,first); assertTrue(s.all(section)); s=s.toggleAll(section)
            assertFalse(s.canContinue(section))
        }
        assertTrue(LearningSequence.create(s).isEmpty())
        assertTrue(LearningSequence.create(Selection(tenses=emptySet())).isEmpty())
        assertTrue(LearningSequence.create(Selection(types=emptySet())).isEmpty())
    }
    @Test fun specialFutureIsHiddenButRememberedAndAllOnlyAffectsVisibleChoices() {
        var s=Selection(types=setOf("er"),er=setOf("special"),tenses=setOf("present","future"))
        assertTrue("special" in s.visible("er")); assertTrue(s.canContinue("er"))
        s=s.toggle("tenses","future"); assertFalse(s.canContinue("er")); assertFalse("special" in s.visible("er"))
        assertTrue("special" in s.er); assertTrue(LearningSequence.create(s).isEmpty())
        s=s.toggleAll("er").toggleAll("er"); assertEquals(setOf("special"),s.er)
        s=s.toggle("tenses","future"); assertTrue(s.canContinue("er"))
        assertEquals(2,LearningSequence.create(s,random=Random(2)).size)
    }
    @Test fun conditionalScreenOrderForAllTypeCombinations() {
        for(mask in 0..15) {
            val types=Choices.types.filterIndexed { i,_ -> mask and (1 shl i)!=0 }.toSet()
            val routes=Selection(types=types).setupRoutes()
            assertEquals("tenses",routes.first()); assertEquals("activities",routes.last())
            assertEquals("important" in types,"important" in routes)
            assertEquals("er" in types,"er" in routes); assertEquals("irregular" in types,"families" in routes)
            assertFalse("ir" in routes)
            routes.zipWithNext().forEach { (a,b)->assertEquals(b,Selection(types=types).next(a)) }
        }
    }
    @Test fun orderingUniquenessAndReuseAcrossTenses() {
        val s=Selection(tenses=Choices.tenses.toSet(),types=Choices.types.toSet(),important=Choices.important.toSet(),er=Choices.er.toSet(),families=Choices.families.toSet())
        val sequence=LearningSequence.create(s,random=Random(42))
        val groups=sequence.chunked(6)
        assertEquals(28,groups.size); assertEquals(28,groups.map { it.first().verbId }.distinct().size)
        assertEquals(Choices.important,groups.take(10).map { it.first().verbId })
        groups.forEach { assertEquals(1,it.map { i->i.verbId }.distinct().size); assertEquals((0..5).toList(),it.map { i->i.tense }) }
        val verbs=OfflineDictionary.verbs.associateBy { it.id }
        assertEquals(Choices.er,groups.drop(10).take(8).map { verbs.getValue(it.first().verbId).group })
        assertEquals("ir",verbs.getValue(groups[18].first().verbId).group)
        assertEquals(Choices.families,groups.drop(19).map { verbs.getValue(it.first().verbId).group })
        val next=LearningSequence.create(s,sequence,Random(42)).chunked(6)
        groups.drop(10).zip(next.drop(10)).forEach { (a,b)->assertNotEquals(a.first().verbId,b.first().verbId) }
        assertEquals(sequence,sequence.map { LearningItem.decode(it.encode()) })
        assertNull(LearningItem.decode("unknown:0:0")); assertNull(LearningItem.decode("être:6:0"))
    }
    @Test fun specifiedExampleSequence() {
        val s=Selection(tenses=setOf("present","imperfect"),types=setOf("important","er"),important=setOf("être","avoir"),er=setOf("ger"))
        val items=LearningSequence.create(s,random=Random(1))
        assertEquals(listOf("être","être","avoir","avoir"),items.take(4).map { it.verbId })
        assertEquals(items[4].verbId,items[5].verbId); assertEquals(listOf(0,1,0,1,0,1),items.map { it.tense })
    }
    @Test fun allOfflineEntriesAreCompleteAnnotatedNormalizedAndClassified() {
        assertEquals(95,OfflineDictionary.verbs.size)
        assertEquals(95,OfflineDictionary.verbs.map { it.id }.toSet().size)
        for(v in OfflineDictionary.verbs) {
            assertEquals(6,v.tenses.size); assertTrue(v.auxiliary in listOf("avoir","être")); assertTrue(v.participle.isNotBlank())
            assertTrue(v.source.startsWith("https://www.larousse.fr/conjugaison/francais/")); assertTrue(v.meaning.startsWith("to "))
            assertTrue(v.group in Choices.er+Choices.families+listOf("ir","important"))
            for(t in v.tenses) {
                assertEquals(6,t.forms.size); assertEquals(2,t.examples.size)
                t.examples.forEach { assertTrue(it.endsWith(".")); assertTrue(it.contains("nous ")) }
                for(f in t.forms) {
                    assertTrue(f.text.isNotBlank()); assertTrue(f.marked.isNotEmpty())
                    assertTrue(f.highlightStart>0); assertTrue(f.highlightEnd<=f.text.length)
                    assertEquals(f.marked,f.text.substring(f.highlightStart,f.highlightEnd))
                    assertTrue(Normalizer.isNormalized(f.text,Normalizer.Form.NFC)); assertFalse(f.text.contains('\uFFFD'))
                }
            }
            if(v.group=="ir") assertTrue(v.tenses[0].forms[3].text.endsWith("issons"))
        }
        listOf("partir","dormir","sortir","venir").forEach { id->assertNotEquals("ir",OfflineDictionary.verbs.first { it.id==id }.group) }
    }
    @Test fun independentlyRecordedReferenceForms() {
        // Factual reference fixtures independently read from Larousse / Académie, not the generator.
        val fixtures=listOf(
            Triple("être",0,"je suis"),Triple("avoir",0,"j’ai"),Triple("aller",0,"je vais"),Triple("faire",0,"je fais"),
            Triple("prendre",4,"je pris"),Triple("finir",0,"je finis"),Triple("manger",0,"je mange"),Triple("commencer",0,"je commence"),
            Triple("nettoyer",0,"je nettoie"),Triple("lever",0,"je lève"),Triple("préférer",0,"je préfère"),Triple("appeler",0,"j’appelle"),
            Triple("envoyer",2,"j’enverrai"),Triple("partir",3,"je suis parti(e)"),Triple("sortir",5,"j’étais sorti(e)"),
            Triple("mettre",4,"je mis"),Triple("connaître",0,"je connais"),Triple("écrire",4,"j’écrivis"),Triple("recevoir",0,"je reçois"),
            Triple("conduire",4,"je conduisis"),Triple("peindre",0,"je peins")
        )
        fixtures.forEach { (v,t,expected)->assertEquals(v,expected,OfflineDictionary.verbs.first { it.id==v }.tenses[t].forms[0].text) }
        fun row(v:String,t:Int,n:Int)=OfflineDictionary.verbs.first { it.id==v }.tenses[t].forms[n].text
        assertEquals("nous mangeons",row("manger",0,3)); assertEquals("nous commençons",row("commencer",0,3))
        assertEquals("nous vînmes",row("venir",4,3)); assertEquals("nous sommes parti(e)s",row("partir",3,3))
        assertEquals("vous êtes sorti(e)(s)",row("sortir",3,4)); assertEquals("ils sont venus",row("venir",3,5))
        assertEquals("nous cueillerons",row("cueillir",2,3)); assertEquals("nous protégeâmes",row("protéger",4,3))
        assertEquals("nous connaissions",row("connaître",1,3)); assertEquals("nous avons relu",row("relire",3,3))
    }
}
