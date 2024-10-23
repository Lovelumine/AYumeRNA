package com.lovelumine.Rfam.service

import com.lovelumine.Rfam.model.RfamTask
import com.lovelumine.Rfam.utils.RfamUtils
import io.minio.MinioClient
import io.minio.PutObjectArgs
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class RfamService(
    @Autowired private val messagingTemplate: SimpMessagingTemplate,
    @Autowired private val minioClient: MinioClient,
    @Value("\${minio.url}") private val minioBaseUrl: String,
    @Value("\${minio.bucket-name}") private val bucketName: String
) {

    private val logger = LoggerFactory.getLogger(RfamService::class.java)

    fun processRfamTask(task: RfamTask) {
        val userId = task.userId
        val rfamAcc = task.rfamAcc
        val seedFileData = task.seedFileData
        val originalFileData = task.originalFileData

        try {
            // 创建临时目录
            val tempDir = Files.createTempDirectory("rfam_task_$userId").toString()
            val seedFilePath = Paths.get(tempDir, "Rfam.seed")
            val originalFilePath = Paths.get(tempDir, "original.fa")

            // 将 seedFileData 和 originalFileData 写入文件
            Files.write(seedFilePath, seedFileData.toByteArray())
            Files.write(originalFilePath, originalFileData.toByteArray())

            // 通知进度
            messagingTemplate.convertAndSend("/topic/progress/$userId", "进度：10% - 开始处理任务...")

            // 调用工具类的方法处理任务
            val resultPath = RfamUtils.processRfamTask(rfamAcc, originalFilePath.toString(), seedFilePath.toString())

            // 上传结果到 MinIO
            val resultBytes = Files.readAllBytes(Paths.get(resultPath))
            val inputStream = ByteArrayInputStream(resultBytes)
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            val objectName = "$userId-$rfamAcc-$timestamp-result.fa"

            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(objectName)
                    .stream(inputStream, resultBytes.size.toLong(), -1)
                    .contentType("text/fasta")
                    .build()
            )

            val fileUrl = "$minioBaseUrl/$bucketName/$objectName"
            messagingTemplate.convertAndSend("/topic/progress/$userId", "任务完成，结果已上传：$fileUrl")

            // 删除临时文件和目录
            Files.deleteIfExists(seedFilePath)
            Files.deleteIfExists(originalFilePath)
            Files.deleteIfExists(Paths.get(resultPath))

        } catch (e: Exception) {
            e.printStackTrace()
            messagingTemplate.convertAndSend("/topic/progress/$userId", "任务失败：${e.message}")
        }
    }
}
