package com.templesoftware.frenchverbs.domain.use

import com.templesoftware.frenchverbs.domain.verbs.*
import com.templesoftware.frenchverbs.domain.quiz.*
import kotlin.random.Random

/** Saved text and decisions, never regenerated while a session is running. */
data class UseQuestion(val verbId:String,val tense:Int,val person:Int,val feminine:Boolean,
    val prefix:String,val form:String,val suffix:String,val chosenBoxCount:Int=0) {
    val item get()=LearningItem(verbId,tense,0)
    val answer get()=form.filter(Char::isLetter)
    val sentence get()=prefix+form+suffix
    fun encode()=listOf(verbId,tense.toString(),person.toString(),feminine.toString(),prefix,form,suffix,chosenBoxCount.toString()).joinToString("\t")
    val quiz:QuizQuestion by lazy {
        val verb=OfflineDictionary.verbs.first{it.id==verbId}
        // Include both third-person genders in the union and longest-form count.
        val rows=(0..5).map { row -> QuizAnswer("","",if(row==person)answer else UseSentences.fullForm(verb,tense,row,false).filter(Char::isLetter),"") } +
            (0..5).map { row -> QuizAnswer("","",UseSentences.fullForm(verb,tense,row,true).filter(Char::isLetter),"") }
        QuizQuestion(verb,tense,rows)
    }
    val boxCount get()=if(chosenBoxCount==0)quiz.boxCount else chosenBoxCount.coerceIn(answer.length,quiz.boxCount)
    val conjugation get()=when(person){0->if(form.first().lowercaseChar() in "aeiouéèêàâîôûœ")"j’"else "je ";1->"tu ";2->if(feminine)"elle "else "il ";3->"nous ";4->"vous ";else->if(feminine)"elles "else "ils "}+form
    val keys get()=quiz.keys
}

object UseSentences {
    fun fullForm(v:Verb,t:Int,p:Int,female:Boolean):String {
        val f=v.tenses[t].forms[p]
        val ending=if(t in listOf(3,5) && v.auxiliary=="être") {
            val agreement=SentenceBank.Agreement(if(female)SentenceBank.Gender.FEMININE else SentenceBank.Gender.MASCULINE,p>=3)
            SentenceBank.participles.getValue(v.id).getValue(agreement).removePrefix(v.participle)
        } else f.after
        return f.before+f.marked+ending
    }
    /** Only existing complete verb-bound situations without a competing time prefix. */
    fun candidates(item:LearningItem,person:Int,female:Boolean,question:Boolean):List<UseQuestion> {
        val v=OfflineDictionary.verbs.first{it.id==item.verbId}
        val agreement=SentenceBank.Agreement(if(female)SentenceBank.Gender.FEMININE else SentenceBank.Gender.MASCULINE,person>=3)
        val form=fullForm(v,item.tense,person,female)
        val subject=when(person){0->if(form.first().lowercaseChar() in "aeiouéèêàâîôûœ")"j’"else "je ";1->"tu ";2->if(female)"elle "else "il ";3->"nous ";4->"vous ";else->if(female)"elles "else "ils "}
        val context=when(item.tense){0->"D’habitude, ";1->"À cette époque, ";2->"L’année prochaine, ";3->"La semaine dernière, ";4->"Dans ce récit, ";else->"Quand Léa est arrivée, "}
        // A masculine referent is explicit for first/second persons with être agreement.
        val identity=if(item.tense in listOf(3,5) && v.auxiliary=="être")when(person){0->if(female)"Emma raconte : « "else "Paul raconte : « ";1->if(female)"Emma, "else "Paul, ";3->if(female)"Entre filles, "else "Avec Paul, ";4->if(female)"Emma et Zoé, "else "Paul et Hugo, ";else->""}else ""
        val ask=question && item.tense!=4
        val connector=if(ask)"est-ce "+if(subject.first() in "aeiouéèêàâîôûœ")"qu’"else "que " else ""
        val start=identity+context.replaceFirstChar{if(identity.isEmpty()||identity.endsWith("« "))it.uppercase() else it.lowercase()}+connector+subject
        val end=if(ask)" ?" else "."
        return SentenceBank.patterns.getValue(v.id).filter{it.prefix.isEmpty() && it.supports(item.tense,person,agreement)}.map { pattern ->
            val complement=if(v.id=="paraître" && agreement.plural && pattern.complement in setOf("calme","pâle","triste"))pattern.complement+"s"else pattern.complement
            UseQuestion(v.id,item.tense,person,female,start,form," "+complement+end+if(identity.endsWith("« "))" »"else "")
        }
    }
}

object UseSessions {
    fun available(selection:Selection,random:Random):List<LearningItem> {
        // Reuse the established eligibility/tense sequence, expanding selected families to
        // their complete verified membership instead of choosing one representative verb.
        val seed=LearningSequence.create(selection,random=random)
        val groups=seed.map{it.verbId}.map{id->OfflineDictionary.verbs.first{it.id==id}.group}.toSet()
        val verbs=OfflineDictionary.verbs.filter { v ->
            ("important" in selection.types && v.id in selection.important && v.id in Choices.important) ||
            (v.group in groups && (("er" in selection.types && v.group in selection.er && v.group in selection.visible("er")) ||
                ("ir" in selection.types && v.group=="ir") || ("irregular" in selection.types && v.group in selection.families)))
        }
        return verbs.flatMap{v->Choices.tenses.mapIndexedNotNull{t,id->if(id in selection.tenses)LearningItem(v.id,t,0)else null}}
    }
    fun create(selection:Selection,random:Random=Random.Default)=from(available(selection,random),random)
    fun from(pool:List<LearningItem>,r:Random):List<UseQuestion> {
        val distinct=pool.distinctBy{it.verbId to it.tense};if(distinct.isEmpty())return emptyList()
        val combos=if(distinct.size>=10)distinct.shuffled(r).take(10) else buildList {while(size<10)addAll(distinct.shuffled(r).take(10-size))}
        val persons=((0..5).toList()+(0..5).shuffled(r).take(4)).shuffled(r)
        val used=mutableSetOf<String>();val usedComplements=mutableSetOf<String>()
        return combos.mapIndexed { i,item ->
            val p=persons[i];val female=p in listOf(2,5)&&r.nextBoolean()
            val candidates=UseSentences.candidates(item,p,female,i%3==1)
            val fresh=candidates.filter{it.sentence !in used}.ifEmpty{candidates}
            val varied=fresh.filter{it.suffix !in usedComplements && listOf("parc","livre","vélo","billes").none{word->word in it.suffix}}.ifEmpty{fresh}
            varied.random(r).let{it.copy(chosenBoxCount=useBoxCapacity(it.answer.length,it.quiz.boxCount,r))}.also{used+=it.sentence;usedComplements+=it.suffix}
        }.shuffled(r)
    }
    fun decode(s:String):UseQuestion {val a=s.split('\t');return UseQuestion(a[0],a[1].toInt(),a[2].toInt(),a[3].toBoolean(),a[4],a[5],a[6],a.getOrNull(7)?.toIntOrNull()?:0)}
}

data class UseState(val questions:List<UseQuestion> = emptyList(),val index:Int=0,
    val input:QuizState=QuizState(),val outcomes:List<Int> = List(10){0},
    val successRemaining:Long=0,val running:Boolean=true,
    val attempts:List<Int> = List(10){0},val revealed:List<String> = List(10){""},val selectedTenseCount:Int=questions.map{it.tense}.distinct().size) {
    val current get()=questions.getOrNull(index)
    val complete get()=questions.isNotEmpty() && index==questions.size
    val canEdit get()=running && current!=null && successRemaining==0L && input.canEdit
    val score get()=outcomes.count{it==1}
    val pending get()=input.feedbackRemaining>0 || successRemaining>0
}
object UseActions {
    const val SUCCESS_MILLIS=800L
    fun input(q:UseQuestion)=QuizState(session=listOf(q.item),active=q.person,locked=(0..5).filter{it!=q.person}.toSet())
    fun start(questions:List<UseQuestion>)=UseState(questions=questions,input=questions.firstOrNull()?.let(::input)?:QuizState())
    fun letter(s:UseState,c:Char):UseState {
        val q=s.current ?: return s
        if(!s.canEdit || c !in q.keys || s.input.entries[q.person].length>=q.boxCount)return s
        return s.copy(input=QuizActions.letter(s.input,q.quiz,c))
    }
    fun clear(s:UseState)=if(s.canEdit)s.copy(input=QuizActions.clear(s.input))else s
    fun edit(s:UseState,i:Int)=if(s.canEdit)s.copy(input=QuizActions.edit(s.input,s.current!!.person,i))else s
    fun submit(s:UseState,successMillis:Long=SUCCESS_MILLIS):UseState {
        val q=s.current ?: return s
        if(!s.canEdit || s.input.entries[q.person].length==s.input.hints[q.person].length)return s
        val next=QuizActions.submit(s.input,q.quiz)
        val wrong=next.feedbackRemaining>0
        val outcomes=s.outcomes.mapIndexed{i,n->if(i!=s.index)n else if(wrong||n==-1)-1 else 1}
        return s.copy(input=next,outcomes=outcomes,attempts=s.attempts.mapIndexed{i,n->if(i==s.index)next.mistakes[q.person]else n},successRemaining=if(next.done)successMillis else 0)
    }
    fun tick(s:UseState,delta:Long,successMillis:Long=SUCCESS_MILLIS):UseState {
        if(!s.running || delta<=0)return s
        if(s.successRemaining>0) {
            val left=(s.successRemaining-delta).coerceAtLeast(0)
            if(left>0)return s.copy(successRemaining=left)
            val i=s.index+1
            return s.copy(index=i,input=s.questions.getOrNull(i)?.let(::input)?:s.input,successRemaining=0)
        }
        val next=QuizActions.tick(s.input,delta)
        return s.copy(input=next,revealed=s.revealed.mapIndexed{i,h->if(i==s.index)s.current?.let{next.hints[it.person]}?:h else h},successRemaining=if(next.done&&!s.input.done)successMillis else 0)
    }
}

/** Same ten-question accuracy policy as EducationTemple and TellingTime. */
fun useStars(correct:Int):Int {require(correct in 0..10);return when(correct){10->5;8,9->4;7->3;6->2;else->1}}

/** New sessions only: existing encoded capacities remain readable without truncating entries. */
internal fun useBoxCapacity(answerLength:Int,longest:Int,random:Random):Int =
    random.nextInt(answerLength,(if(answerLength<=10)minOf(10,longest)else longest)+1)
