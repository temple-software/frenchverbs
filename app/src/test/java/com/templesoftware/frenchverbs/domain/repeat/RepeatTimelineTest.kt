package com.templesoftware.frenchverbs.domain.repeat

import org.junit.Assert.*
import org.junit.Test

class RepeatTimelineTest {
    @Test fun orderedPhasesAndDefaultTiming() {
        val t=RepeatTimeline()
        assertEquals(t.waitingAt+t.timing.completedHold+t.timing.finalFadeOut,t.duration)
        assertEquals(t.phases.last{it.name=="verb"}.end,t.waitingAt)
        assertEquals(t.phases.last{it.name=="verb"}.end,t.waitingAt)
        assertEquals(t.waitingAt+t.timing.completedHold,t.fadeOutAt)
        assertEquals("tenseHold",t.phase(t.timing.afterTense-1).name)
        assertEquals(0f,t.instructionAlpha(t.timing.afterTense-1))
        assertEquals(0f,t.subjectAlpha(0,t.phases.first{it.name=="subject"}.start))
        assertEquals(listOf(t.timing.firstInstructionHold,t.timing.secondInstructionHold,t.timing.thirdInstructionHold),t.phases.filter{it.name.startsWith("instructionHold")}.map{it.duration})
        assertEquals(t.timing.completedHold,t.phases.first{it.name=="completeHold"}.duration)
        for(row in 0..5)assertEquals(if(row==5)listOf("subject","subjectHold","verb") else listOf("subject","subjectHold","verb","rowHold"),t.phases.filter{it.row==row}.map{it.name})
        t.phases.zipWithNext().forEach{(a,b)->assertEquals(a.end,b.start)}
    }
    @Test fun subjectBeforeVerbAndCompletedRowsRemainVisible() {
        val t=RepeatTimeline()
        assertEquals(0f,t.instructionAlpha(0));assertEquals(0f,t.subjectAlpha(0,0))
        t.phases.filter{it.name=="subject"}.forEach { p ->
            assertEquals(0f,t.subjectAlpha(p.row,p.start));assertEquals(.5f,t.subjectAlpha(p.row,p.start+p.duration/2),.01f)
            assertEquals(1f,t.subjectAlpha(p.row,p.end));assertEquals(0f,t.verbAlpha(p.row,p.end+t.timing.subjectToVerb-1))
            for(i in 0 until p.row)assertEquals(1f,t.verbAlpha(i,p.start))
        }
        val hold=t.phases.first{it.name=="completeHold"}
        for(i in 0..5)assertEquals(1f,t.verbAlpha(i,hold.start))
        assertEquals(1f,t.contentAlpha(hold.end));assertEquals(0f,t.contentAlpha(t.duration))
    }
    @Test fun allTimingFieldsAreConfigurable() {
        val t=RepeatTimeline(RepeatTiming(10,20,30,40,50,60,70,80,90,100,11,13))
        assertEquals(3*10+20+2*30+40+13+6*(50+60+70)+5*80+90+100+11L,t.duration)
        assertEquals(90L,t.phases.first{it.name=="completeHold"}.duration)
        assertThrows(IllegalArgumentException::class.java){RepeatTiming(completedHold=0)}
    }
    @Test fun countdownAndHeadingFadesAreSynchronized() {
        val t=RepeatTimeline()
        for(p in t.phases) {
            val at=p.start+p.duration/2
            when(p.name) {
                "instructionHold1" -> assertEquals(3,t.countdown(at))
                "instructionHold2" -> assertEquals(2,t.countdown(at))
                "instructionHold3" -> assertEquals(1,t.countdown(at))
            }
            if(p.name in listOf("instructionOut1","instructionIn2")) {
                assertEquals(t.instructionAlpha(at),t.headingVerbAlpha(at))
                assertEquals(1f,t.headingTenseAlpha(at))
            }
            if(p.name in listOf("instructionOut2","instructionIn3")) {
                assertEquals(t.instructionAlpha(at),t.headingTenseAlpha(at))
                assertEquals(1f,t.headingVerbAlpha(at))
            }
        }
        val first=t.phases.first{it.name=="subject"}
        assertEquals(.1464466f,t.instructionAlpha(first.start+t.timing.subjectFadeIn/2),.001f)
        assertEquals(.5f,t.subjectAlpha(0,first.start+t.timing.subjectFadeIn/2),.001f)
        assertEquals(0f,t.instructionAlpha(first.end))
        assertEquals(0f,t.verbAlpha(0,first.end+t.timing.subjectToVerb-1))
        assertEquals(1f,t.headingVerbAlpha(t.waitingAt))
        assertEquals(1f,t.headingTenseAlpha(t.waitingAt))
    }
}
