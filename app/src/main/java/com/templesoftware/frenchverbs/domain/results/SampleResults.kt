package com.templesoftware.frenchverbs.domain.results

/** Neutral example only: products define their own question/answer records. */
data class SampleResult(val questionId: String, val correct: Boolean)
fun accuracyStars(records: List<SampleResult>): Int {
    require(records.map { it.questionId }.distinct().size == records.size)
    require(records.all { it.questionId.isNotBlank() })
    if (records.isEmpty()) return 0
    val numerator = records.count { it.correct }.toLong() * 100
    val denominator = records.size.toLong()
    return when {
        numerator >= 95 * denominator -> 5
        numerator >= 85 * denominator -> 4
        numerator >= 70 * denominator -> 3
        numerator >= 50 * denominator -> 2
        else -> 1
    }
}
