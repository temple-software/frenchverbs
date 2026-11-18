package com.templesoftware.frenchverbs.domain.verbs

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class LearnPass20Test {
    @Test fun contextualPairsAndPreservedFrench() {
        assertEquals(OfflineDictionary.verbs.map{it.id}.toSet(),EnglishLearnContexts.verbIds)
        val lines=mutableListOf<String>();val readings=mutableListOf<String>();val overlaps=mutableListOf<String>()
        var frenchBlocks=0
        for(v in OfflineDictionary.verbs)for(t in 0..5) {
            val context=EnglishLearnContexts.forVerb(v.id,t)
            for(paired in listOf(false,true)) {
                val fr=LearnExamples.forPanel(v,t,paired,false)
                assertEquals(Pass19LearnExamples.forPanel(v,t,paired,false),fr);frenchBlocks++
                val en=LearnExamples.forPanel(v,t,paired,true)
                assertEquals(2,en.size);assertEquals(context.english,en[1].text)
                assertTrue(en[1].translation);assertTrue(en[1].marked.isEmpty())
                assertEquals(v.tenses[t].forms[0].marked,en[0].text.substring(en[0].highlightStart,en[0].highlightEnd))
                assertEquals(context.complement,en[0].complement)
                assertTrue(en[0].text.startsWith(context.prefix));assertFalse(en[0].text.contains("  "))
                assertFalse(en[0].text.contains(Regex("[()/\\uFFFD]")))
                assertTrue(en[0].text.endsWith("."));assertTrue(en[1].text.endsWith("."))
                if(t in listOf(3,5))assertTrue("${v.id} $t",en[0].after.startsWith((if(v.auxiliary=="être")"" else v.tenses[t].forms[0].after)+" "))
                for(other in 0..5)assertEquals(en[0].complement,LearnExamples.forPanel(v,other,paired,true)[0].complement)
                // Switching back is pure selection, with no mutation of the French baseline.
                assertEquals(fr,LearnExamples.forPanel(v,t,paired,false))
            }
            val en=LearnExamples.forPanel(v,t,english=true)
            readings+="$t:${context.reading}"
            lines+="${v.id} [$t] ${en[0].text} / ${en[1].text} (${context.reading})"
            if(t==3)for(other in listOf(1,4,5))if(en[1].text==LearnExamples.forPanel(v,other,english=true)[1].text)overlaps+="${v.id}: $t / $other -> ${en[1].text}"
        }
        assertTrue(overlaps.any{it.startsWith("grandir: 3 / 4")})
        val out=File("build/reports/pass20/linguistic-audit.txt");out.parentFile!!.mkdirs()
        out.writeText("PASS 570 contextual pairs, $frenchBlocks French blocks byte-for-byte equivalent in text/metadata to Pass 19.\nReadings: "+readings.groupingBy{it}.eachCount()+"\nNatural overlaps: "+overlaps.joinToString()+"\n"+lines.joinToString("\n"))
    }
    @Test fun contextualContrastGoldenExamples() {
        fun pair(v:String,t:Int)=LearnExamples.forPanel(OfflineDictionary.verbs.first{it.id==v},t,english=true).map{it.text}
        assertEquals(listOf("Je suis déjà allé en ville.","I’ve been to town before."),pair("aller",3))
        assertEquals(listOf("Ce jour-là, j’allai en ville.","That day, I went to town."),pair("aller",4))
        assertEquals(listOf("J’allais souvent en ville.","I often went to town."),pair("aller",1))
        assertEquals(listOf("J’avais souvent faim.","I was often hungry."),pair("avoir",1))
        assertEquals(listOf("Alors, j’eus faim.","Then, I felt hungry."),pair("avoir",4))
        assertEquals(listOf("J’ai eu faim hier.","I was hungry yesterday."),pair("avoir",3))
        assertEquals(listOf("À midi, j’avais fini un dessin.","By noon, I’d finished a drawing."),pair("finir",5))
        assertEquals(listOf("Je regardais les oiseaux.","I was watching the birds."),pair("regarder",1))
        assertEquals(listOf("Je voyais la mer.","I could see the sea."),pair("voir",1))
        assertEquals(listOf("Je connaissais déjà Léa.","I already knew Léa."),pair("connaître",1))
        assertEquals(listOf("Alors, je connus Léa.","Then, I got to know Léa."),pair("connaître",4))
        assertEquals(pair("grandir",3)[1],pair("grandir",4)[1])
    }
    @Test fun unchangedPersonAndAgreementExamples() {
        for(id in listOf("aller","venir","sortir","paraître","prendre","être"))for(t in 0..5)for(p in 0..5) {
            val v=OfflineDictionary.verbs.first{it.id==id};val f=v.tenses[t].forms[p]
            for(s in SentenceBank.candidates(v,t,p)) {
                assertEquals(f.marked,s.text.substring(s.highlightStart,s.highlightEnd))
                if(p==2 || p==5)assertTrue(s.subject in setOf("il","elle","ils","elles"))
                if(v.auxiliary=="être" && t in listOf(3,5))assertTrue(s.text.contains(f.before+SentenceBank.participles.getValue(id).getValue(s.agreement)))
            }
        }
    }
}
