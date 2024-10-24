package com.lovelumine.onehot.service

import com.lovelumine.onehot.model.OnehotTask
import com.lovelumine.onehot.utils.OnehotUtils
import io.minio.MinioClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class OnehotService(
    @Autowired private val messagingTemplate: SimpMessagingTemplate,
    @Autowired private val minioClient: MinioClient,
    @Value("\${minio.url}") private val minioBaseUrl: String,
    @Value("\${minio.bucket-name}") private val bucketName: String
) {

    private val logger = LoggerFactory.getLogger(OnehotService::class.java)

    fun processOnehotTask(task: OnehotTask) {
        val userId = task.userId

        try {
            // 通知进度
            messagingTemplate.convertAndSend("/topic/progress/$userId", "进度：5% - 准备处理任务...")

            // 调用 Flask API 处理任务，传递 MinIO 文件 URL
            val outputFileUrl = OnehotUtils.processOnehot(task.fastaFileUrl, task.cmFileUrl)

            // 下载并上传处理结果到 MinIO
            val resultBytes = Files.readAllBytes(Paths.get(outputFileUrl))
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            val objectName = "$userId-$timestamp-onehot.h5"

            minioClient.putObject(
                io.minio.PutObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(objectName)
                    .stream(resultBytes.inputStream(), resultBytes.size.toLong(), -1)
                    .contentType("application/octet-stream")
                    .build()
            )

            val fileUrl = "$minioBaseUrl/$bucketName/$objectName"
            messagingTemplate.convertAndSend("/topic/progress/$userId", "任务完成，结果已上传：$fileUrl")

        } catch (e: Exception) {
            logger.error("任务处理失败：${e.message}", e)
            messagingTemplate.convertAndSend("/topic/progress/$userId", "任务失败：${e.message}")
        }
    }
}
