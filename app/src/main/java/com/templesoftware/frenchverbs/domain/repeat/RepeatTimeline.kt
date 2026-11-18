package com.templesoftware.frenchverbs.domain.repeat

/** All durations are milliseconds; change defaults here for owner visual tuning. */
data class RepeatTiming(
    val instructionFadeIn: Long = 500,
    val firstInstructionHold: Long = 750,
    val instructionFadeOut: Long = 500,
    val secondInstructionHold: Long = 750,
    val subjectFadeIn: Long = 250,
    val subjectToVerb: Long = 1000,
    val verbFadeIn: Long = 250,
    val nextSubjectDelay: Long = 1000,
    val completedHold: Long = 2000,
    val finalFadeOut: Long = 350,
    val afterTense: Long = 1000,
    val thirdInstructionHold: Long = 750,
    val pulseMinimumAlpha: Float = .25f,
) {
    init { require(listOf(instructionFadeIn,firstInstructionHold,instructionFadeOut,secondInstructionHold,
        subjectFadeIn,subjectToVerb,verbFadeIn,nextSubjectDelay,completedHold,finalFadeOut,afterTense,thirdInstructionHold).all { it > 0 }); require(pulseMinimumAlpha in 0f..1f) }
}
data class RepeatPhase(val name: String, val start: Long, val duration: Long, val row: Int = -1) {
    val end get() = start + duration
}
class RepeatTimeline(val timing: RepeatTiming = RepeatTiming()) {
    val phases: List<RepeatPhase> = buildList {
        var cursor = 0L
        fun phase(name: String, duration: Long, row: Int = -1) {
            add(RepeatPhase(name,cursor,duration,row)); cursor += duration
        }
        with(timing) {
            phase("tenseHold",afterTense)
            phase("instructionIn1",instructionFadeIn); phase("instructionHold1",firstInstructionHold)
            phase("instructionOut1",instructionFadeOut); phase("instructionIn2",instructionFadeIn)
            phase("instructionHold2",secondInstructionHold); phase("instructionOut2",instructionFadeOut)
            phase("instructionIn3",instructionFadeIn); phase("instructionHold3",thirdInstructionHold)
            // Start the final dim before je, then overlap its tail with the unchanged subject fade.
            if(instructionFadeOut>subjectFadeIn) phase("instructionOut3",instructionFadeOut-subjectFadeIn)
            repeat(6) { row ->
                phase("subject",subjectFadeIn,row); phase("subjectHold",subjectToVerb,row)
                phase("verb",verbFadeIn,row)
                if(row < 5) phase("rowHold",nextSubjectDelay,row)
            }
            phase("completeHold",completedHold); phase("fadeOut",finalFadeOut)
        }
    }
    val waitingAt = phases.first{it.name=="completeHold"}.start
    val fadeOutAt = phases.last().start
    val duration = phases.last().end
    fun phase(elapsed: Long) = phases.firstOrNull { elapsed < it.end } ?: phases.last()
    private fun fade(elapsed: Long, phase: RepeatPhase): Float {
        val x=((elapsed-phase.start).toFloat()/phase.duration).coerceIn(0f,1f)
        return x*x*(3-2*x)
    }
    // Raised-cosine easing has zero velocity at both ends, including the dim turnaround.
    private fun introductionFade(elapsed: Long, phase: RepeatPhase): Float {
        val x=((elapsed-phase.start).toDouble()/phase.duration).coerceIn(0.0,1.0)
        return ((1-kotlin.math.cos(kotlin.math.PI*x))/2).toFloat()
    }
    private fun pulse(elapsed: Long, phase: RepeatPhase, brightening: Boolean): Float {
        val eased=introductionFade(elapsed,phase)
        return timing.pulseMinimumAlpha+(1-timing.pulseMinimumAlpha)*(if(brightening)eased else 1-eased)
    }
    private val instructionExit by lazy {
        val start=phases.firstOrNull{it.name=="instructionOut3"}?.start
            ?: phases.first{it.name=="subject"}.start
        RepeatPhase("instructionExit",start,timing.instructionFadeOut)
    }
    fun instructionAlpha(elapsed: Long): Float {
        val p=phase(elapsed)
        return when(p.name) {
            "instructionIn1" -> introductionFade(elapsed,p)
            "instructionIn2","instructionIn3" -> pulse(elapsed,p,true)
            "instructionHold1","instructionHold2","instructionHold3" -> 1f
            "instructionOut1","instructionOut2" -> pulse(elapsed,p,false)
            "instructionOut3","subject" -> if(elapsed<=instructionExit.end)1f-introductionFade(elapsed,instructionExit) else 0f
            else -> 0f
        }
    }
    fun countdown(elapsed: Long): Int = when(phase(elapsed).name) {
        "tenseHold","instructionIn1","instructionHold1","instructionOut1" -> 3
        "instructionIn2","instructionHold2","instructionOut2" -> 2
        else -> 1
    }
    private fun headingAlpha(elapsed: Long, out: String, into: String): Float {
        val p=phase(elapsed)
        return when(p.name) { out -> pulse(elapsed,p,false); into -> pulse(elapsed,p,true); else -> 1f }
    }
    fun headingVerbAlpha(elapsed: Long)=headingAlpha(elapsed,"instructionOut1","instructionIn2")
    fun headingTenseAlpha(elapsed: Long)=headingAlpha(elapsed,"instructionOut2","instructionIn3")
    fun subjectAlpha(row: Int, elapsed: Long) = fade(elapsed,phases.first{it.name=="subject" && it.row==row}.copy(duration=timing.subjectFadeIn))
    fun verbAlpha(row: Int, elapsed: Long) = fade(elapsed,phases.first{it.name=="verb" && it.row==row})
    fun contentAlpha(elapsed: Long) = 1f-fade(elapsed,phases.last())
}
