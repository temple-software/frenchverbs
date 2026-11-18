package com.templesoftware.frenchverbs.domain.quiz

import com.templesoftware.frenchverbs.domain.use.useStars
import kotlin.math.roundToInt

/** Snapshot the verified complete form, never the child's typed letters or a guessed suffix. */
data class ConjugateResultLine(val question:Int,val person:Int,val tense:Int,val conjugation:String) {
    val id get()=question*6+person
    fun encode()="$question\t$person\t$tense\t$conjugation"
    companion object {
        fun decode(value:String):ConjugateResultLine {
            val parts=value.split('\t',limit=4)
            return ConjugateResultLine(parts[0].toInt(),parts[1].toInt(),parts[2].toInt(),parts[3])
        }
    }
}

fun recordConjugateCompletion(s:QuizState,q:QuizQuestion):QuizState {
    if(!s.done || s.completedLines.any{it.question==s.index})return s
    return s.copy(completedLines=s.completedLines+q.rows.mapIndexed { person,answer ->
        ConjugateResultLine(s.index,person,q.tense,answer.text)
    })
}

data class ConjugateScore(val totalLines:Int,val wrongLines:Int) {
    init {require(totalLines>0 && wrongLines in 0..totalLines)}
    val correctLines=totalLines-wrongLines
    val scaledScore=if(wrongLines==0)10 else (10.0*correctLines/totalLines).roundToInt().coerceAtMost(9)
    // Same policy as Utiliser, EducationTemple and TellingTime; do not duplicate thresholds.
    val stars=useStars(scaledScore)
}

class ConjugateResults(s:QuizState) {
    val completedLines=s.completedLines
    private val byId=completedLines.associateBy{it.id}
    // First incorrect-submission order, independent of completion/attempt order.
    val wrongLines=s.wrongLines.distinct().mapNotNull{byId[it]}
    val score=ConjugateScore(completedLines.size,wrongLines.size)
    val groups=wrongLines.groupBy{it.tense}.toSortedMap()
    fun initiallyExpanded(tense:Int)=groups.getValue(tense).size<=3
}
