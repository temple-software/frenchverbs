package com.templesoftware.frenchverbs.domain.quiz

import com.templesoftware.frenchverbs.domain.verbs.*
import kotlin.random.Random

const val TOTALCONJ = 6
enum class ConjugateChoicePage { TODAY, TENSE, TYPE, DONE, EMPTY }
data class ConjugateCandidate(val item:LearningItem,val category:String) {
    fun encode()="${item.encode()}|$category"
    companion object {
        fun decode(value:String):ConjugateCandidate? {
            val parts=value.split('|')
            return if(parts.size==2 && parts[1] in Choices.types)
                LearningItem.decode(parts[0])?.let{ConjugateCandidate(it,parts[1])} else null
        }
    }
}

/** CONJSTARTLIST is immutable; CURRENTCONJ is derived, never destructively filtered. */
data class ConjugateSelection(
    val original:List<ConjugateCandidate> = emptyList(),
    val tense:Int?=null,
    val category:String?=null,
    val page:ConjugateChoicePage=ConjugateChoicePage.EMPTY,
    val accepted:List<LearningItem> = emptyList(),
) {
    val eligible get()=original.filter{(tense==null || it.item.tense==tense) && (category==null || it.category==category)}
    val current get()=if(done)accepted else eligible.map{it.item}
    val acceptable get()=current.size<=TOTALCONJ // zero is handled separately before CONJGO
    val done get()=page==ConjugateChoicePage.DONE
    val originalTenses get()=Choices.tenses.indices.filter{t->original.any{it.item.tense==t}}
    val currentTenses get()=Choices.tenses.indices.filter{t->current.any{it.tense==t}}
    val categories get()=Choices.types.filter{c->eligible.any{it.category==c}}
    val verbs get()=current.map{it.verbId}.distinct()
}

object ConjugateSelectionActions {
    /** Use selection origin/verified group metadata, never infinitive spelling. */
    fun candidates(items:List<LearningItem>,selection:Selection):List<ConjugateCandidate> =
        items.distinctBy{it.verbId to it.tense}.map { item ->
            val group=OfflineDictionary.verbs.first{it.id==item.verbId}.group
            val category=when {
                "important" in selection.types && item.verbId in selection.important && item.verbId in Choices.important->"important"
                group in Choices.er->"er"
                group=="ir"->"ir"
                else->"irregular"
            }
            ConjugateCandidate(item,category)
        }
    fun begin(original:List<ConjugateCandidate>,random:Random=Random.Default):ConjugateSelection {
        val s=ConjugateSelection(original=original.distinctBy{it.item.verbId to it.item.tense}.toList())
        return when {
            s.original.isEmpty()->s
            s.acceptable->finish(s,random)
            s.originalTenses.size>1->s.copy(page=ConjugateChoicePage.TODAY)
            s.categories.size>1->s.copy(page=ConjugateChoicePage.TYPE)
            else->finish(s,random)
        }
    }
    private fun finish(s:ConjugateSelection,random:Random):ConjugateSelection {
        if(s.done)return s
        val list=s.eligible.map{it.item}
        if(list.isEmpty())return s.copy(page=ConjugateChoicePage.EMPTY)
        // Sample indices without replacement, then retain the original relative sequence order.
        val indices=list.indices.shuffled(random).take(TOTALCONJ).toSet()
        val accepted=if(list.size<=TOTALCONJ)list else list.filterIndexed{i,_->i in indices}
        return s.copy(page=ConjugateChoicePage.DONE,accepted=accepted)
    }
    fun random(s:ConjugateSelection,r:Random=Random.Default)=if(s.page==ConjugateChoicePage.TODAY)finish(s,r)else s
    fun openTenses(s:ConjugateSelection)=if(!s.done && s.originalTenses.size>1)
        s.copy(tense=null,category=null,page=ConjugateChoicePage.TENSE) else s
    fun openTypes(s:ConjugateSelection,r:Random=Random.Default):ConjugateSelection {
        if(s.page!=ConjugateChoicePage.TODAY)return s
        val next=s.copy(category=null,page=ConjugateChoicePage.TYPE)
        return if(next.categories.size<=1)finish(next,r) else next
    }
    fun chooseTense(s:ConjugateSelection,tense:Int,r:Random=Random.Default):ConjugateSelection {
        if(s.page!=ConjugateChoicePage.TENSE || tense !in s.originalTenses)return s
        val next=s.copy(tense=tense,category=null)
        return if(next.acceptable || next.categories.size<=1)finish(next,r)else next.copy(page=ConjugateChoicePage.TYPE)
    }
    fun chooseCategory(s:ConjugateSelection,category:String,r:Random=Random.Default):ConjugateSelection {
        if(s.page!=ConjugateChoicePage.TYPE || category !in s.categories)return s
        return finish(s.copy(category=category),r)
    }
    /** Null means return to Activity Homepage. Earlier pages always regain original options. */
    fun back(s:ConjugateSelection):ConjugateSelection?=when(s.page) {
        ConjugateChoicePage.TENSE->s.copy(tense=null,category=null,page=ConjugateChoicePage.TODAY)
        ConjugateChoicePage.TYPE->when {
            s.tense!=null->openTenses(s)
            s.originalTenses.size>1->s.copy(tense=null,category=null,page=ConjugateChoicePage.TODAY)
            else->null
        }
        else->null
    }
}

/** Current dictionary selection generates at most 28 distinct verbs. Covers 0–99 for growth. */
fun verbCountWord(n:Int,french:Boolean):String {
    require(n in 0..99)
    val small=if(french)listOf("zéro","un","deux","trois","quatre","cinq","six","sept","huit","neuf","dix","onze","douze","treize","quatorze","quinze","seize","dix-sept","dix-huit","dix-neuf")
        else listOf("zero","one","two","three","four","five","six","seven","eight","nine","ten","eleven","twelve","thirteen","fourteen","fifteen","sixteen","seventeen","eighteen","nineteen")
    if(n<20)return small[n]
    if(french && n>=80)return if(n==80)"quatre-vingts" else "quatre-vingt-${verbCountWord(n-80,true)}"
    if(french && n>=70)return "soixante"+(if(n==71)" et " else "-")+verbCountWord(n-60,true)
    val tens=if(french)listOf("","","vingt","trente","quarante","cinquante","soixante") else listOf("","","twenty","thirty","forty","fifty","sixty","seventy","eighty","ninety")
    return tens[n/10]+if(n%10==0)"" else (if(french && n%10==1)" et " else "-")+small[n%10]
}
