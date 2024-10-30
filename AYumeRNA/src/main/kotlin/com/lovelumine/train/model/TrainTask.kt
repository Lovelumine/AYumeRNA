// TrainTask.kt
package com.lovelumine.train.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

data class TrainTask @JsonCreator constructor(
    @JsonProperty("userId")
    val userId: Long,

    @JsonProperty("XTrainFileUrl")
    val XTrainFileUrl: String,

    @JsonProperty("WTrainFileUrl")
    val WTrainFileUrl: String,

    @JsonProperty("XValidFileUrl")
    val XValidFileUrl: String,

    @JsonProperty("WValidFileUrl")
    val WValidFileUrl: String,

    @JsonProperty("retryCount")
    var retryCount: Int = 0
) : Serializable
