package com.lovelumine.cmbuild.service

import com.lovelumine.cmbuild.model.CmbuildTask
import com.lovelumine.cmbuild.utils.CmbuildUtils
import io.minio.GetObjectArgs
import io.minio.MinioClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class CmbuildService(
    @Autowired private val messagingTemplate: SimpMessagingTemplate,
    @Autowired private val minioClient: MinioClient,
    @Value("\${minio.url}") private val minioBaseUrl: String,
    @Value("\${minio.bucket-name}") private val bucketName: String
) {

    private val logger = LoggerFactory.getLogger(CmbuildService::class.java)

    fun processCmbuildTask(task: CmbuildTask) {
        val userId = task.userId
        logger.info("开始处理任务，用户ID：$userId")

        try {
            // 创建临时目录
            val tempDir = Files.createTempDirectory("cmbuild_task_$userId").toString()
            val stockholmFilePath = Paths.get(tempDir, "RF00005.stockholm")
            logger.info("临时目录创建成功：$tempDir")

            // 向用户发送消息，告知正在下载文件
            messagingTemplate.convertAndSend("/topic/progress/$userId", "进度：5% - 正在下载文件...")
            logger.info("通知用户文件正在下载...")

            // 从 MinIO 下载文件
            downloadFromMinio(task.stockholmFileUrl, stockholmFilePath.toString())
            logger.info("文件从 MinIO 下载完成，路径：$stockholmFilePath")

            // 通知文件下载完成
            messagingTemplate.convertAndSend("/topic/progress/$userId", "进度：10% - 文件下载完成，准备处理...")
            logger.info("通知用户文件下载完成，准备处理任务...")

            // 调用 Cmbuild 工具类的方法处理任务，并记录详细信息
            val resultPath = CmbuildUtils.runCmbuild(
                stockholmFilePath.toString()
            ) { details ->
                logger.info("处理详情：$details")
                messagingTemplate.convertAndSend("/topic/progress/$userId", "处理详情：$details")
            }

            // 通知用户任务处理完成
            messagingTemplate.convertAndSend("/topic/progress/$userId", "进度：90% - 处理完成，正在生成结果...")
            logger.info("任务处理完成，正在生成结果文件...")

            // 上传结果到 MinIO
            val resultBytes = Files.readAllBytes(Paths.get(resultPath))
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            val objectName = "$userId-$timestamp-result.cm"

            minioClient.putObject(
                io.minio.PutObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(objectName)
                    .stream(resultBytes.inputStream(), resultBytes.size.toLong(), -1)
                    .contentType("text/plain")
                    .build()
            )
            logger.info("结果文件上传到 MinIO，URL: $minioBaseUrl/$bucketName/$objectName")

            val fileUrl = "$minioBaseUrl/$bucketName/$objectName"
            messagingTemplate.convertAndSend("/topic/progress/$userId", "任务完成，结果已上传：$fileUrl")

            // 通知用户任务完成
            messagingTemplate.convertAndSend("/topic/progress/$userId", "任务已成功完成，结果已上传到 MinIO")
            logger.info("通知用户任务成功完成")

            // 删除临时文件和目录
            Files.deleteIfExists(stockholmFilePath)
            Files.deleteIfExists(Paths.get(resultPath))
            logger.info("删除临时文件和目录")

        } catch (e: Exception) {
            logger.error("任务处理失败：${e.message}", e)
            messagingTemplate.convertAndSend("/topic/progress/$userId", "任务失败：${e.message}")
        }
    }

    // 从 MinIO 下载文件
    private fun downloadFromMinio(fileUrl: String, outputPath: String) {
        val objectName = fileUrl.substringAfterLast("/")
        logger.info("开始从 MinIO 下载文件，文件名：$objectName，目标路径：$outputPath")
        val inputStream = minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(bucketName)
                .`object`(objectName)
                .build()
        )
        FileOutputStream(outputPath).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        logger.info("文件下载成功，已保存到：$outputPath")
    }
}
