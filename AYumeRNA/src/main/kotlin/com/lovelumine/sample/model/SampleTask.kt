// SampleTask.kt
package com.lovelumine.sample.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

data class SampleTask @JsonCreator constructor(
    @JsonProperty("userId")
    val userId: Long,

    @JsonProperty("configFileUrl")
    val configFileUrl: String,

    @JsonProperty("ckptFileUrl")
    val ckptFileUrl: String,

    @JsonProperty("cmFileUrl")
    val cmFileUrl: String,

    @JsonProperty("n_samples")
    val n_samples: Int,  // 修改字段名为 n_samples

    @JsonProperty("retryCount")
    var retryCount: Int = 0
) : Serializable
