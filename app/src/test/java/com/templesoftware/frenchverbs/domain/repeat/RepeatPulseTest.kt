package com.templesoftware.frenchverbs.domain.repeat

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class RepeatPulseTest {
    @Test fun everyOpacityIsContinuousAndBounded() {
        val t=RepeatTimeline()
        for(alpha in listOf(t::instructionAlpha,t::headingVerbAlpha,t::headingTenseAlpha)) {
            var previous=alpha(0)
            for(ms in 1L..t.phases.first{it.name=="subject"}.end) {
                val current=alpha(ms)
                assertTrue("bounds at $ms",current in 0f..1f)
                assertTrue("jump at $ms: $previous -> $current",abs(current-previous)<.004f)
                previous=current
            }
            for(p in t.phases.filter{it.name.startsWith("instruction")}) {
                assertTrue("boundary velocity ${p.name}",abs(alpha(p.start+1)-alpha(p.start))<.0001f)
            }
        }
    }
    @Test fun numbersChangeAtTheSharedPulseMinimum() {
        val t=RepeatTimeline()
        for((name,number) in listOf("instructionIn2" to 2,"instructionIn3" to 1)) {
            val p=t.phases.first{it.name==name}
            assertEquals(number+1,t.countdown(p.start-1));assertEquals(number,t.countdown(p.start))
            assertEquals(t.timing.pulseMinimumAlpha,t.instructionAlpha(p.start),.00001f)
            val heading=if(number==2)t::headingVerbAlpha else t::headingTenseAlpha
            assertEquals(t.instructionAlpha(p.start),heading(p.start))
            assertEquals(1f,t.instructionAlpha(p.end),.00001f)
        }
    }
    @Test fun dimAndBrightenMonotonicallyWithConfigurableDepth() {
        val t=RepeatTimeline(RepeatTiming(pulseMinimumAlpha=.3f))
        for(p in t.phases.filter{it.name in listOf("instructionOut1","instructionOut2","instructionIn2","instructionIn3")}) {
            val values=(p.start..p.end).map(t::instructionAlpha)
            values.zipWithNext().forEach{(a,b)->if(p.name.startsWith("instructionOut"))assertTrue(b<=a+.00001f) else assertTrue(b>=a-.00001f)}
            assertEquals(.3f,values.minOrNull()!!,.00001f)
        }
        assertThrows(IllegalArgumentException::class.java){RepeatTiming(pulseMinimumAlpha=1.1f)}
    }
    @Test fun finalExitOverlapsJeWithoutChangingRowDurations() {
        val t=RepeatTimeline();val first=t.phases.first{it.name=="subject"}
        assertEquals(t.timing.afterTense+3*t.timing.instructionFadeIn+t.timing.firstInstructionHold+t.timing.secondInstructionHold+t.timing.thirdInstructionHold+2*t.timing.instructionFadeOut+(t.timing.instructionFadeOut-t.timing.subjectFadeIn).coerceAtLeast(0),first.start)
        assertEquals(.5f,t.instructionAlpha(first.start),.00001f)
        assertEquals(0f,t.instructionAlpha(first.end),.00001f)
        for(ms in first.start+1 until first.end) {
            assertTrue(t.instructionAlpha(ms)>0f);assertTrue(t.subjectAlpha(0,ms)>0f)
        }
        for(row in 0..5) {
            val subject=t.phases.first{it.name=="subject" && it.row==row}
            val verb=t.phases.first{it.name=="verb" && it.row==row}
            assertEquals(t.timing.subjectFadeIn,subject.duration);assertEquals(t.timing.subjectToVerb,verb.start-subject.end)
            assertEquals(t.timing.verbFadeIn,verb.duration)
            if(row<5)assertEquals(t.timing.nextSubjectDelay,t.phases.first{it.name=="subject" && it.row==row+1}.start-verb.end)
        }
        assertEquals(t.phases.last{it.name=="verb"}.end,t.waitingAt)
    }
}
