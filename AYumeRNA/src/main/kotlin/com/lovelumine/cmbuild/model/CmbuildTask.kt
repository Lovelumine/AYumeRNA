package com.lovelumine.cmbuild.model

import java.io.Serializable

data class CmbuildTask(
    val userId: Long,
    val stockholmFileUrl: String, // MinIO 中 Stockholm 文件的 URL
    var retryCount: Int = 0 // 记录重试次数
) : Serializable
