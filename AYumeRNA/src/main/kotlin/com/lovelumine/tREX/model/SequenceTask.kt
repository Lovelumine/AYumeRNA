package com.lovelumine.tREX.model

import java.io.Serializable

data class SequenceTask(
    val userId: Long,
    val username: String,
    val templateFileData: ByteArray,
    val testFileData: ByteArray,
    var retryCount: Int = 0 // 新增字段，记录重试次数
)
