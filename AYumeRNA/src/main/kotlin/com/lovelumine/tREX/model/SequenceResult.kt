package com.lovelumine.tREX.model

data class SequenceResult(
    val testSequence: String,
    val score: Double
) {
    fun toMap(): Map<String, Any> = mapOf(
        "testSequence" to testSequence,
        "score" to score
    )
}