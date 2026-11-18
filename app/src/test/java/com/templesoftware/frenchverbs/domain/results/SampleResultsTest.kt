package com.templesoftware.frenchverbs.domain.results

import org.junit.Assert.*
import org.junit.Test

class SampleResultsTest {
    @Test fun integerBoundariesAndEmptyInput() {
        assertEquals(0, accuracyStars(emptyList()))
        for ((count, stars) in listOf(49 to 1, 50 to 2, 69 to 2, 70 to 3, 84 to 3, 85 to 4, 94 to 4, 95 to 5, 100 to 5)) {
            assertEquals(stars, accuracyStars(List(100) { SampleResult("question-$it", it < count) }))
        }
        assertEquals(5, accuracyStars(List(60) { SampleResult("$it", it < 57) }))
    }
    @Test(expected = IllegalArgumentException::class) fun duplicateIdentityRejected() {
        accuracyStars(listOf(SampleResult("same", true), SampleResult("same", false)))
    }
}
