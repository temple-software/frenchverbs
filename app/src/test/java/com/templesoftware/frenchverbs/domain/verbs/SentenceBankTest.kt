package com.templesoftware.frenchverbs.domain.verbs

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.random.Random

class SentenceBankTest {
    @Test fun everyVerbTensePersonAndAgreement() {
        assertEquals(OfflineDictionary.verbs.map{it.id}.toSet(),SentenceBank.complements.keys)
        var count=0;var combinations=0;var minimum=Int.MAX_VALUE
        val samples=mutableListOf<String>()
        for(v in OfflineDictionary.verbs)for(t in 0..5)for(p in 0..5) {
            val bank=SentenceBank.candidates(v,t,p);combinations++;minimum=minOf(minimum,bank.size)
            assertTrue("Variety ${v.id} $t $p",bank.map{it.text}.distinct().size>=3)
            val form=v.tenses[t].forms[p]
            for(s in bank) {
                count++;assertEquals(v.id,s.verbId);assertEquals(t,s.tense);assertEquals(p,s.person)
                assertEquals(form.marked,s.text.substring(s.highlightStart,s.highlightEnd))
                assertTrue(s.before.endsWith(form.before))
                assertFalse(s.text.contains('\uFFFD'));assertFalse(s.text.contains("("));assertFalse(s.text.contains("/"));assertFalse(s.text.contains("  "))
                assertTrue(s.text.endsWith("."))
                val expectedSubject=when(p){0->form.subject.trim();1->"tu";2->if(s.agreement.gender==SentenceBank.Gender.FEMININE)"elle" else "il";3->"nous";4->"vous";else->if(s.agreement.gender==SentenceBank.Gender.FEMININE)"elles" else "ils"}
                assertEquals(expectedSubject,s.subject)
                if(p!=4)assertEquals(p==3||p==5,s.agreement.plural)
                if(v.auxiliary=="être" && t in listOf(3,5)) {
                    val expected=SentenceBank.participles.getValue(v.id).getValue(s.agreement)
                    assertTrue(s.text.contains(form.before+expected+" "))
                } else assertTrue(s.after.startsWith(form.after+" "))
                val pattern=SentenceBank.patterns.getValue(v.id)[s.pattern]
                assertTrue(pattern.supports(t,p,s.agreement))
                if(v.id=="paraître" && pattern.complement in setOf("calme","pâle","triste"))assertEquals(s.agreement.plural,s.complement.endsWith("s"))
            }
            if(p==4 && v.auxiliary=="être" && t==3)assertEquals(setOf(false,true),bank.map{it.agreement.plural}.toSet())
            if(p in listOf(2,5))assertEquals(SentenceBank.Gender.entries.toSet(),bank.map{it.agreement.gender}.toSet())
            if(p==0 && v.id in listOf("être","aller","pouvoir","regarder","paraître","grandir","apparaître","prescrire"))samples+=bank.take(3).map{it.text}
        }
        File("build/reports/pass18-bank.txt").apply{parentFile.mkdirs();writeText("PASS verbs=95 combinations=$combinations candidates=$count minimum alternatives=$minimum\n"+samples.joinToString("\n"))}
    }
    @Test fun selectionAvoidsImmediateRepeatsAndUsesMeasuredWidth() {
        val v=OfflineDictionary.verbs.first{it.id=="aller"}
        val bank=SentenceBank.candidates(v,3,2)
        var previous:String?=null
        repeat(40) {
            val index=SentenceSelection.choose(bank,previous,{if(it.complement=="au parc")50f else 500f},100f,Random(it))
            assertEquals("au parc",bank[index].complement);assertNotEquals(previous,bank[index].text);previous=bank[index].text
        }
    }
}

