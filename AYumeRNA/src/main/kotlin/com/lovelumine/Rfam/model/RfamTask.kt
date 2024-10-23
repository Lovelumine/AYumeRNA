package com.lovelumine.Rfam.model

import java.io.Serializable

data class RfamTask(
    val userId: Long,
    val rfamAcc: String,
    val seedFileData: String, // Rfam.seed 文件的内容
    val originalFileData: String, // 原始序列文件的内容
    var retryCount: Int = 0 // 记录重试次数
) : Serializable
