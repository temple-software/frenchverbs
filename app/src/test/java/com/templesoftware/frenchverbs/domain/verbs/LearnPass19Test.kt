package com.templesoftware.frenchverbs.domain.verbs

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.random.Random

class LearnPass19Test {
    private fun report(name:String,text:String)=File("build/reports/pass19/$name").apply{parentFile.mkdirs();writeText(text)}
    @Test fun exhaustiveStructuresAndBilingualPairs() {
        assertEquals(OfflineDictionary.verbs.map{it.id}.toSet(),SentenceBank.patterns.keys)
        var count=0
        val review=mutableListOf<String>()
        for(v in OfflineDictionary.verbs)for(t in 0..5) {
            for(p in 0..5) {
                val bank=SentenceBank.candidates(v,t,p)
                assertTrue(bank.size>=3)
                for(s in bank) {
                    count++
                    val form=v.tenses[t].forms[p];val pattern=SentenceBank.patterns.getValue(v.id)[s.pattern]
                    assertEquals(v.id,pattern.verbId);assertTrue(pattern.supports(t,p,s.agreement))
                    assertEquals(form.marked,s.text.substring(s.highlightStart,s.highlightEnd))
                    assertTrue(s.before.endsWith(form.before));assertTrue(s.text.endsWith("."))
                    assertFalse(s.text.contains(Regex("[()/\\uFFFD]|  ")))
                    assertTrue(s.before.startsWith(pattern.prefix))
                    if(p!=4)assertEquals(p==3||p==5,s.agreement.plural)
                    if(p==2||p==5)assertEquals(if(s.agreement.gender==SentenceBank.Gender.FEMININE)if(p==2)"elle" else "elles" else if(p==2)"il" else "ils",s.subject)
                    val ending=if(v.auxiliary=="être" && t in listOf(3,5))SentenceBank.participles.getValue(v.id).getValue(s.agreement).removePrefix(v.participle) else form.after
                    assertEquals(ending+" "+s.complement+".",s.after)
                    if(v.id=="paraître" && pattern.complement in setOf("calme","pâle","triste"))assertEquals(pattern.complement+if(s.agreement.plural)"s" else "",s.complement)
                }
            }
            for(paired in listOf(false,true)) {
                val fr=LearnExamples.forPanel(v,t,paired);val en=LearnExamples.forPanel(v,t,paired,true)
                assertEquals(2,fr.size);assertEquals(2,en.size);assertEquals(fr[0].marked,en[0].marked)
                assertNotEquals(fr[0].person,fr[1].person);assertNotEquals(fr[0].text,fr[1].text)
                assertTrue(en[1].translation);assertTrue(en[1].text.contains(Regex("\\bI[ ’]")));assertTrue(en[1].marked.isEmpty())
                assertFalse(en[1].text.contains("null"));assertFalse(en[1].text.contains("  "))
                for(f in fr)assertEquals(v.tenses[t].forms[f.person].marked,f.text.substring(f.highlightStart,f.highlightEnd))
                for(other in 0..5)assertEquals(fr[0].complement,LearnExamples.forPanel(v,other,paired)[0].complement)
            }
            val en=LearnExamples.forPanel(v,t,english=true)
            review+="${v.id} [$t] ${en[0].text} / ${en[1].text} / FR second: ${LearnExamples.forPanel(v,t)[1].text}"
            // Review one situation per verb/tense, alternating gender and singular/plural.
            val p=if(t%2==0)2 else 5
            val samples=SentenceBank.candidates(v,t,p)
            review+="Random: "+samples[(t*7+v.id.length)%samples.size].text
        }
        assertEquals("I am hungry.",LearnExamples.forPanel(OfflineDictionary.verbs.first{it.id=="avoir"},0,english=true)[1].text)
        val watch=OfflineDictionary.verbs.first{it.id=="regarder"}
        assertEquals(listOf("I watch the birds.","I was watching the birds.","I will watch the birds.","I’ve watched the birds before.","That day, I watched the birds.","I’d already watched the birds."),(0..5).map{LearnExamples.forPanel(watch,it,english=true)[1].text})
        report("editorial.txt","Exhaustive structural PASS candidates=$count combinations=3420 bilingual blocks=1140\n"+review.joinToString("\n"))
    }

    @Test fun diversityAuditAndRepeatAvoidance() {
        val before=mutableListOf<String>();val after=mutableListOf<SentenceBank.Sentence>()
        var oldRepeats=0;var newRepeats=0;var semanticRepeats=0;var oldSemanticRepeats=0
        val rng=Random(1901);val history=mutableListOf<SentenceSelection.Recent>()
        for(v in OfflineDictionary.verbs)for(t in 0..5) {
            val oldPrevious=mutableMapOf<Int,String>();val oldContexts=mutableMapOf<Int,String>();val previous=mutableMapOf<Int,SentenceBank.Sentence>()
            repeat(12) { for(p in 0..5) {
                val a=BaselineSentenceBank.candidates(v,t,p)
                val old=a[BaselineSentenceSelection.choose(a,oldPrevious[p],{0f},1000f,rng)]
                if(old.text==oldPrevious[p])oldRepeats++;oldPrevious[p]=old.text;before+=old.text
                if(old.complement==oldContexts[p])oldSemanticRepeats++;oldContexts[p]=old.complement
                val b=SentenceBank.candidates(v,t,p)
                val selected=b[SentenceSelection.choose(b,previous[p]?.text,{0f},1000f,rng,history)]
                if(selected.text==previous[p]?.text)newRepeats++
                if(selected.semanticKey==previous[p]?.semanticKey)semanticRepeats++
                previous[p]=selected;after+=selected;history+=SentenceSelection.remembered(selected)
                if(history.size>36)history.removeAt(0)
            }}
        }
        assertEquals(0,newRepeats)
        fun top(xs:List<String>)=xs.groupingBy{it}.eachCount().entries.sortedByDescending{it.value}.take(20).joinToString{ "${it.key}: ${it.value}" }
        fun nouns(xs:List<String>)=xs.flatMap{Regex("[\\p{L}’]+").findAll(it.lowercase()).map{it.value}.filter{it.length>3}.toList()}
        val flagged=listOf("parc","billes","livre","vélo")
        val audit=buildString {
            appendLine("Seed 1901; ${after.size} selections per version: 95 verbs x 6 tenses x 6 persons x 12 activations.")
            appendLine("Unconstrained semantic audit; real measured-width coverage is tested separately on Android.")
            appendLine("Before complements=295; after distinct verb/complement situations="+SentenceBank.complements.values.sumOf{it.size})
            appendLine("Before immediate text repeats=$oldRepeats; after=$newRepeats; same-row consecutive semantic repeats before=$oldSemanticRepeats after=$semanticRepeats")
            for(word in flagged) {
                fun frequency(xs:List<String>)=xs.count{Regex("\\b${word}s?\\b").containsMatchIn(it)}
                val b=frequency(before);val a=frequency(after.map{it.text})
                appendLine("$word: before=$b (${100.0*b/before.size}%), after=$a (${100.0*a/after.size}%)")
                assertTrue("Reduced $word",a<b)
            }
            appendLine("After complete objects/complements: "+top(after.map{it.semanticKey}))
            appendLine("After content tokens (not POS-tagged nouns): "+top(nouns(after.map{it.semanticKey})))
            val head=Regex("(?:un|une|le|la|les|des|du|ce|cet|cette) ([\\p{L}’]+)")
            appendLine("After noun heads after determiners (surface forms, heuristic): "+top(after.flatMap{s->head.findAll(s.semanticKey).map{it.groupValues[1]}.toList()}))
            appendLine("Introductions: "+top(after.map{it.introduction.ifEmpty{"[none]"}}))
            appendLine("Structures: "+top(after.map{it.structure}))
            appendLine("Themes: "+top(after.map{it.theme}))
            for(id in listOf("être","avoir","aller","faire","prendre","mettre","conduire","regarder","grandir","prescrire")) {
                appendLine("$id: before="+BaselineSentenceBank.complements.getValue(id).distinct().size+"; after="+SentenceBank.complements.getValue(id).size+"; contexts="+SentenceBank.patterns.getValue(id).map{it.meaning}.distinct().joinToString())
            }
        }
        report("diversity.txt",audit)
    }
}
