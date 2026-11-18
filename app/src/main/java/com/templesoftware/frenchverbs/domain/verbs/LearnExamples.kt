package com.templesoftware.frenchverbs.domain.verbs

/** Highlight offsets follow the verified French form; translations deliberately have no marked part. */
data class LearnExample(val tense:Int,val person:Int,val subject:String,val complement:String,
    val before:String,val marked:String,val after:String,val translation:Boolean=false) {
    val text get()=before+marked+after
    val highlightStart get()=before.length
    val highlightEnd get()=before.length+marked.length
}

object LearnExamples {
    /** Both panels use the same underlying pair, with independently correct tense readings. */
    fun forPanel(verb:Verb,tense:Int,paired:Boolean=false,english:Boolean=false):List<LearnExample> {
        val pair=BilingualExamples.pairs.getValue(verb.id)
        val context=if(english)EnglishLearnContexts.forVerb(verb.id,tense) else null
        fun french(person:Int,complement:String):LearnExample {
            val form=verb.tenses[tense].forms[person]
            val ending=if(verb.auxiliary=="être" && tense in listOf(3,5))
                SentenceBank.participles.getValue(verb.id).getValue(SentenceBank.Agreement(SentenceBank.Gender.MASCULINE,person==3))
                    .removePrefix(verb.participle) else form.after
            val prefix=context?.prefix.orEmpty()
            val subject=if(prefix.isEmpty())form.subject.replaceFirstChar{it.uppercaseChar()} else form.subject
            return LearnExample(tense,person,form.subject.trim(),complement,
                prefix+subject+form.before+context?.compoundAdverb.orEmpty(),form.marked,
                ending+context?.afterVerb.orEmpty()+" "+complement+context?.suffix.orEmpty()+".")
        }
        val first=french(0,context?.complement ?: pair.french)
        val second=if(english)LearnExample(tense,0,"I",first.complement,context!!.english,"","",true)
            else french(if(paired)1 else 3,pair.otherFrench)
        return listOf(first,second)
    }
}
