package com.lovelumine.split_onehot.model

import java.io.Serializable

data class SplitOnehotTask(
    val userId: Long,
    val h5FileUrl: String,   // MinIO 中 .h5 文件的 URL
    val trainRatio: Double = 0.7,  // 训练集比例
    val randomState: Int = 42,     // 随机种子
    var retryCount: Int = 0        // 记录重试次数
) : Serializable
