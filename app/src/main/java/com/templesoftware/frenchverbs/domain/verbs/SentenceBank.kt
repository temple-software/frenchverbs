package com.templesoftware.frenchverbs.domain.verbs

/** Only complete, verb-bound situations are admitted. No independent phrase cross-products. */
object SentenceBank {
    enum class Gender { MASCULINE, FEMININE }
    data class Agreement(val gender:Gender,val plural:Boolean)
    data class Pattern(
        val id:String,val verbId:String,val complement:String,val theme:String,val structure:String,
        val prefix:String="",
        // All current situations admit human speakers in all six persons and all six indicative tenses.
        // Narrow these explicit masks when adding a situation with restricted meaning or chronology.
        val tenses:Set<Int> = (0..5).toSet(),val persons:Set<Int> = (0..5).toSet(),
        val genders:Set<Gender> = Gender.entries.toSet(),val numbers:Set<Boolean> = setOf(false,true)
    ) {
        val meaning get()="$verbId $complement"
        fun supports(tense:Int,person:Int,agreement:Agreement)=tense in tenses && person in persons &&
            agreement.gender in genders && agreement.plural in numbers
    }
    val patterns get()=SentenceSituations.patterns
    val complements get()=patterns.mapValues{(_,v)->v.map{it.complement}.distinct()}
    data class Sentence(val verbId:String,val tense:Int,val person:Int,val agreement:Agreement,
        val pattern:Int,val complement:String,val subject:String,val before:String,val marked:String,val after:String,
        val contextId:String,val theme:String,val structure:String,val introduction:String,val semanticKey:String) {
        val text get()=before+marked+after
        val highlightStart get()=before.length
        val highlightEnd get()=before.length+marked.length
    }
    fun agreements(person:Int):List<Agreement> = Gender.entries.flatMap { g ->
        (if(person==4)listOf(false,true) else listOf(person==3||person==5)).map{Agreement(g,it)}
    }
    fun candidates(verb:Verb,tense:Int,person:Int):List<Sentence> {
        require(tense in 0..5 && person in 0..5)
        val form=verb.tenses[tense].forms[person]
        return agreements(person).flatMap { agreement ->
            patterns.getValue(verb.id).mapIndexedNotNull { index,pattern ->
                if(!pattern.supports(tense,person,agreement))return@mapIndexedNotNull null
                val subject=when(person) {
                    2 -> if(agreement.gender==Gender.FEMININE)"elle " else "il "
                    5 -> if(agreement.gender==Gender.FEMININE)"elles " else "ils "
                    else -> form.subject
                }
                val ending=if(tense in listOf(3,5) && verb.auxiliary=="être")
                    participles.getValue(verb.id).getValue(agreement).removePrefix(verb.participle) else form.after
                val complement=if(verb.id=="paraître" && agreement.plural && pattern.complement in setOf("calme","pâle","triste"))
                    pattern.complement+"s" else pattern.complement
                val written=if(pattern.prefix.isEmpty())subject.replaceFirstChar{it.uppercaseChar()} else subject
                Sentence(verb.id,tense,person,agreement,index,complement,subject.trim(),
                    pattern.prefix+written+form.before,form.marked,ending+" "+complement+".",
                    pattern.id,pattern.theme,pattern.structure,pattern.prefix,pattern.complement)
            }
        }.distinctBy{it.text}
    }
    private fun paradigm(ms:String,fs:String,mp:String,fp:String)=mapOf(
        Agreement(Gender.MASCULINE,false) to ms,Agreement(Gender.FEMININE,false) to fs,
        Agreement(Gender.MASCULINE,true) to mp,Agreement(Gender.FEMININE,true) to fp)
    val participles=mapOf(
        "aller" to paradigm("allé","allée","allés","allées"),
        "venir" to paradigm("venu","venue","venus","venues"),
        "partir" to paradigm("parti","partie","partis","parties"),
        "sortir" to paradigm("sorti","sortie","sortis","sorties"),
        "apparaître" to paradigm("apparu","apparue","apparus","apparues"))
}


/** Persist semantic history, including vocabulary shared by different verbs. Selection runs only at UI events. */
object SentenceSelection {
    data class Recent(val text:String,val context:String,val complement:String,val theme:String,val structure:String,val introduction:String)
    fun remembered(s:SentenceBank.Sentence)=Recent(s.text,s.contextId,s.semanticKey,s.theme,s.structure,s.introduction)
    private val wordPattern=Regex("[\\p{L}’]+")
    private val vocabulary by lazy {
        SentenceBank.complements.values.flatten().associateWith { text ->
            wordPattern.findAll(text.lowercase()).map{it.value}.filter{it.length>3}.toSet()
        }
    }
    private fun words(s:String)=vocabulary.getValue(s)
    fun choose(candidates:List<SentenceBank.Sentence>, previous:String?, width:(SentenceBank.Sentence)->Float,
        available:Float, random:kotlin.random.Random=kotlin.random.Random.Default, history:List<Recent> = emptyList()):Int {
        val widths=candidates.map(width)
        val fitting=candidates.indices.filter{widths[it]<=available}
        // Full-text wrapping/scrolling is the fallback; never clip or shrink the approved typography.
        val pool=if(fitting.size>=2)fitting else candidates.indices.sortedBy{widths[it]}.take(3)
        val fresh=pool.filter{candidates[it].text!=previous}.ifEmpty{pool}
        fun score(i:Int):Int {
            val s=candidates[i];val nouns=words(s.semanticKey)
            return history.takeLast(18).reversed().mapIndexed{age,h ->
                val weight=18-age
                weight*((if(h.context==s.contextId)12 else 0)+(if(h.complement==s.semanticKey)10 else 0)+
                    nouns.intersect(words(h.complement)).size*4+(if(h.theme==s.theme)2 else 0)+
                    (if(h.structure==s.structure)1 else 0)+(if(s.introduction.isNotEmpty() && h.introduction==s.introduction)3 else 0))
            }.sum()
        }
        val scores=fresh.associateWith{score(it)};val best=scores.values.min()
        return fresh.filter{scores[it]==best}.random(random)
    }
}
