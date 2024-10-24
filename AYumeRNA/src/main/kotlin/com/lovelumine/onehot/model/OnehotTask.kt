package com.lovelumine.onehot.model

import java.io.Serializable

data class OnehotTask(
    val userId: Long,
    val fastaFileUrl: String, // MinIO 中 Fasta 文件的 URL
    val cmFileUrl: String,    // MinIO 中 CM 文件的 URL
    var retryCount: Int = 0   // 记录重试次数
) : Serializable
