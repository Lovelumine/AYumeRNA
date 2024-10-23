package com.lovelumine.Rfam.model

import java.io.Serializable

data class RfamTask(
    val userId: Long,
    val rfamAcc: String,
    val seedFileUrl: String, // MinIO 中种子文件的 URL
    val originalFileUrl: String, // MinIO 中原始序列文件的 URL
    var retryCount: Int = 0 // 记录重试次数
) : Serializable
