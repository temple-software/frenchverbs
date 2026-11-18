package com.templesoftware.frenchverbs.domain.verbs
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random
import java.io.File

class LearnStage3SequenceTest {
    private fun subset(values:List<String>,mask:Int)=values.filterIndexed{i,_->mask and (1 shl i)!=0}.toSet()
    @Test fun allSectionSubsetsAndTypeTenseCombinations() {
        val byId=OfflineDictionary.verbs.associateBy{it.id}
        var count=0
        fun verify(s:Selection) {
            val items=LearningSequence.create(s,random=Random(count++))
            val tenses=Choices.tenses.indices.filter{Choices.tenses[it] in s.tenses}
            if(tenses.isEmpty()){assertTrue(items.isEmpty());return}
            val groups=items.chunked(tenses.size)
            val expected=mutableListOf<String>()
            if("important" in s.types)expected+=Choices.important.filter{it in s.important}.map{"id:$it"}
            if("er" in s.types)expected+=Choices.er.filter{it in s.er && (it!="special" || "future" in s.tenses)}.map{"group:$it"}
            if("ir" in s.types)expected+="group:ir"
            if("irregular" in s.types)expected+=Choices.families.filter{it in s.families}.map{"group:$it"}
            assertEquals(expected.size,groups.size)
            assertEquals(groups.size,groups.map{it.first().verbId}.distinct().size)
            groups.zip(expected).forEach{(rows,key)->
                val id=rows.first().verbId
                assertEquals(tenses,rows.map{it.tense})
                assertTrue(rows.all{it.verbId==id})
                assertEquals(key,if(key.startsWith("id:"))"id:$id" else "group:${byId.getValue(id).group}")
            }
        }
        for(tm in 0..63) {
            val tenses=subset(Choices.tenses,tm)
            for(types in 0..15)verify(Selection(tenses,subset(Choices.types,types),Choices.important.toSet(),Choices.er.toSet(),Choices.families.toSet()))
            for((type,options)in listOf("important" to Choices.important,"er" to Choices.er,"irregular" to Choices.families))
                for(mask in 0 until (1 shl options.size)) {
                    val values=subset(options,mask)
                    verify(Selection(tenses,setOf(type),if(type=="important")values else emptySet(),if(type=="er")values else emptySet(),if(type=="irregular")values else emptySet()))
                }
        }
        // Mixed partial sections, in addition to exhaustive individual sections and type/tense products.
        val rng=Random(2501)
        repeat(2048){verify(Selection(subset(Choices.tenses,rng.nextInt(64)),subset(Choices.types,rng.nextInt(16)),subset(Choices.important,rng.nextInt(1024)),subset(Choices.er,rng.nextInt(256)),subset(Choices.families,rng.nextInt(512))))}
        File("build/reports/pass25-sequences.txt").apply{parentFile.mkdirs();writeText("PASS $count selections: all individual section subsets crossed with all 64 tense masks; all 16 type masks crossed with all tense masks; 2048 mixed partial selections. Ordering, exclusions, exact counts and uniqueness.\n")}
    }
    @Test fun freshRandomCategoryChoicesAcrossNewSequences() {
        val s=Selection(Choices.tenses.toSet(),Choices.types.toSet(),Choices.important.toSet(),Choices.er.toSet(),Choices.families.toSet())
        var previous=LearningSequence.create(s,random=Random(0))
        val seen=mutableSetOf<List<String>>()
        repeat(100){seed->
            val next=LearningSequence.create(s,previous,Random(seed))
            val a=previous.chunked(6).map{it.first().verbId};val b=next.chunked(6).map{it.first().verbId}
            assertEquals(Choices.important,b.take(10))
            a.drop(10).zip(b.drop(10)).forEach{(old,new)->assertNotEquals(old,new)}
            seen+=b;previous=next
        }
        assertTrue(seen.size>90)
    }
}
