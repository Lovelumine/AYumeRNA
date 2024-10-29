package com.lovelumine.generate_weight.model

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

data class GenerateWeightTask(
    val userId: Long,
    val h5FileUrl: String,
    val mode: String = "cm",
    val threshold: Double = 0.1,

    @JsonProperty("n_samples")
    @JsonAlias("nsamples", "nSamples")
    val nSamples: Double = 10000.0,

    val cpu: Int = 4,
    val printEvery: Int = 500,
    var retryCount: Int = 0
) : Serializable {

    @JsonIgnore
    fun getNSamplesAsInt(): Int {
        return if (nSamples == Double.POSITIVE_INFINITY) {
            Integer.MAX_VALUE
        } else {
            nSamples.toInt()
        }
    }
}
