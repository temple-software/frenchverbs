package com.templesoftware.frenchverbs.domain.quiz

import com.templesoftware.frenchverbs.domain.verbs.*

/** Every fixed/entered segment comes directly from a verified dictionary Form. */
data class QuizAnswer(val subject:String,val prefix:String,val answer:String,val suffix:String,val displaySuffix:String=suffix) {
    val text get()=subject+prefix+answer+suffix
    val displaySubject get()=when(subject.trim()){ "il/elle"->"il / elle ";"ils"->"ils / elles ";else->subject }
}
data class QuizQuestion(val verb:Verb,val tense:Int,val rows:List<QuizAnswer>) {
    val boxCount=rows.maxOf{it.answer.length}
    val commonStem=rows.map{it.answer}.reduce{a,b->a.commonPrefixWith(b)}
    val keys:List<Char> = buildList {
        rows.forEach { row -> row.answer.forEach { if(it !in this)add(it) } }
        for(c in "seéiozaabcdefghijklmnopqrstuvwxyz")if(size<9 && c !in this)add(c)
    }
    companion object {
        fun from(item:LearningItem):QuizQuestion {
            val verb=OfflineDictionary.verbs.first{it.id==item.verbId}
            val compound=item.tense in listOf(3,5)
            val ending=verb.group in Choices.er || verb.group=="ir"
            return QuizQuestion(verb,item.tense,verb.tenses[item.tense].forms.mapIndexed { row,f ->
                // Compound auxiliaries and agreement notation stay fixed; practise the participle.
                // Simple regular forms use the dictionary's verified stem/ending segmentation.
                val split=compound || ending
                val answer=if(split)f.marked else f.before+f.marked+f.after
                require(answer.isNotEmpty() && answer.all(Char::isLetter)) { "Non-letter quiz segment: ${item.encode()}" }
                val suffix=if(split)f.after else ""
                // The sixth row names both genders. Reuse this same conjugation's verified
                // plural agreement notation (nous), without inventing an inflection.
                val displaySuffix=if(compound && verb.auxiliary=="être" && row==5)
                    verb.tenses[item.tense].forms[3].after else suffix
                QuizAnswer(f.subject,if(split)f.before else "",answer,suffix,displaySuffix)
            })
        }
    }
}

data class QuizState(
    val session:List<LearningItem> = emptyList(), val index:Int=0,
    val entries:List<String> = List(6){""}, val locked:Set<Int> = emptySet(),
    val active:Int=0, val feedbackRemaining:Long=0, val running:Boolean=true,
    val hints:List<String> = List(6){""}, val mistakes:List<Int> = List(6){0},
    val pendingHint:String="", val pendingComplete:Boolean=false,
    val wrongLines:List<Int> = emptyList(),
    val completedLines:List<ConjugateResultLine> = emptyList(),
    val resultToggles:Set<Int> = emptySet(),
) {
    val item get()=session.getOrNull(index)
    val done get()=locked.size==6
    val last get()=index==session.lastIndex
    val canEdit get()=running && item!=null && !done && feedbackRemaining==0L
    val progress get()=if(session.isEmpty())0 else ((index*6+locked.size)*100/(session.size*6))
}

/** Pure transitions shared by the UI and focused deterministic tests. No automatic checking on input. */
object QuizActions {
    const val ERROR_MILLIS=650L
    fun letter(s:QuizState,q:QuizQuestion,c:Char):QuizState {
        if(!s.canEdit || c !in q.keys || s.entries[s.active].length>=q.boxCount)return s
        return s.copy(entries=s.entries.mapIndexed{i,v->if(i==s.active)v+c else v})
    }
    fun clear(s:QuizState)=if(!s.canEdit)s else s.copy(entries=s.entries.mapIndexed{i,v->if(i==s.active)s.hints[i] else v})
    /** Editing truncates the one authoritative entry; its length is also the cursor position. */
    fun edit(s:QuizState,row:Int,index:Int):QuizState {
        if(!s.canEdit || row!=s.active || row in s.locked || index<0)return s
        val entry=s.entries[row]
        if(index>=entry.length)return s
        val keep=index.coerceAtLeast(s.hints[row].length)
        return s.copy(entries=s.entries.mapIndexed{i,v->if(i==row)v.take(keep) else v})
    }
    fun submit(s:QuizState,q:QuizQuestion,target:Int?=null):QuizState {
        if(!s.canEdit || (target!=null && (target !in 0..5 || target in s.locked || target==s.active)))return s
        val entry=s.entries[s.active]
        val hasTyped=entry.length>s.hints[s.active].length
        if(hasTyped && entry!=q.rows[s.active].answer) {
            val attempts=s.mistakes[s.active]+1
            val answer=q.rows[s.active].answer
            val length=when {attempts<2->s.hints[s.active].length;attempts==2->q.commonStem.length+1;else->s.hints[s.active].length+1}.coerceAtMost(answer.length)
            return s.copy(feedbackRemaining=ERROR_MILLIS,
                wrongLines=if(s.index*6+s.active in s.wrongLines)s.wrongLines else s.wrongLines+(s.index*6+s.active),
                mistakes=s.mistakes.mapIndexed{i,n->if(i==s.active)attempts else n},
                pendingHint=answer.take(length),pendingComplete=length==answer.length)
        }
        val locked=if(!hasTyped)s.locked else s.locked+s.active
        val next=if(locked.size==6)-1 else target ?: (1..6).map{(s.active+it)%6}.first{it !in locked}
        return s.copy(locked=locked,active=next)
    }
    fun tick(s:QuizState,delta:Long):QuizState {
        if(!s.running || s.feedbackRemaining==0L || delta<=0)return s
        val remaining=(s.feedbackRemaining-delta).coerceAtLeast(0)
        if(remaining>0)return s.copy(feedbackRemaining=remaining)
        val locked=if(s.pendingComplete)s.locked+s.active else s.locked
        val active=if(s.pendingComplete){if(locked.size==6)-1 else (1..6).map{(s.active+it)%6}.first{it !in locked}}else s.active
        return s.copy(feedbackRemaining=0,
            entries=s.entries.mapIndexed{i,v->if(i==s.active)s.pendingHint else v},
            hints=s.hints.mapIndexed{i,v->if(i==s.active)s.pendingHint else v},
            locked=locked,active=active,pendingHint="",pendingComplete=false)
    }
    fun nextQuestion(s:QuizState)=if(!s.running || !s.done || s.last)s else QuizState(session=s.session,index=s.index+1,wrongLines=s.wrongLines,completedLines=recordConjugateCompletion(s,QuizQuestion.from(s.item!!)).completedLines)
}
