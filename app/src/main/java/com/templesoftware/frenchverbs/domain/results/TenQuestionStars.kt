package com.templesoftware.frenchverbs.domain.results

/** Optional accuracy-first ten-question policy; choose explicitly for a product. */
fun tenQuestionStars(correct: Int): Int {
    require(correct in 0..10)
    return when (correct) { 10 -> 5; 8, 9 -> 4; 7 -> 3; 6 -> 2; else -> 1 }
}
