package com.templesoftware.frenchverbs.ui.motion

import org.junit.Assert.*
import org.junit.Test

class LinearMotionTest {
    @Test fun frameRateAndDroppedFramesDoNotChangeElapsedPosition() {
        val patterns = listOf(30, 60, 90, 120).map { hz -> (0..hz).map { it * 1000.0 / hz } } +
            listOf(listOf(0.0, 16.7, 300.25, 950.0, 1000.0))
        patterns.forEach { frames ->
            frames.forEach { time -> assertEquals(2.5 + time * .04, linearPosition(2.5, 40.0, time), 1e-10) }
            assertEquals(42.5, linearPosition(2.5, 40.0, frames.last()), 1e-10)
        }
        assertTrue(linearPosition(0.0, 1.0, .25) > 0)
        assertEquals(4.0, linearPosition(0.0, 40.0, 200.0) - linearPosition(0.0, 40.0, 100.0), 1e-10)
    }
}
