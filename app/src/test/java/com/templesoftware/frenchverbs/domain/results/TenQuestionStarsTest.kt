package com.templesoftware.frenchverbs.domain.results
import org.junit.Assert.*
import org.junit.Test
class TenQuestionStarsTest {
    @Test fun everyBoundary() { assertEquals(listOf(1,1,1,1,1,1,2,3,4,4,5), (0..10).map(::tenQuestionStars)) }
    @Test(expected = IllegalArgumentException::class) fun rejectsInvalidCount() { tenQuestionStars(11) }
}
