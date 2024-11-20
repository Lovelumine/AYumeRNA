package com.lovelumine.sample.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.lovelumine.auth.model.User
import com.lovelumine.common.ResponseUtil
import com.lovelumine.sample.model.SampleTask
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.net.URI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/sample")
class SampleController(
    @Autowired
    @Qualifier("rabbitTemplate")
    private val rabbitTemplate: RabbitTemplate,
    @Autowired private val messagingTemplate: SimpMessagingTemplate,
    @Autowired private val minioClient: MinioClient,
    @Value("\${minio.bucket-name}") private val bucketName: String,
    @Value("\${minio.url}") private val minioBaseUrl: String,
    @Autowired private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(SampleController::class.java)

    @Operation(summary = "提交采样任务", description = "用户可以通过该接口提交采样任务，上传必要的参数")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "任务已成功提交"),
            ApiResponse(responseCode = "401", description = "用户认证失败"),
            ApiResponse(responseCode = "400", description = "请求参数错误"),
            ApiResponse(responseCode = "500", description = "服务器内部错误")
        ]
    )
    @PostMapping("/process")
    fun processSampleTask(
        @RequestParam("config_file") configFile: MultipartFile?,
        @RequestParam("ckpt_file") ckptFile: MultipartFile?,
        @RequestParam("cm_file") cmFile: MultipartFile?,
        @RequestParam("config_file_url") configFileUrl: String?,
        @RequestParam("ckpt_file_url") ckptFileUrl: String?,
        @RequestParam("cm_file_url") cmFileUrl: String?,
        @RequestParam("n_samples") nSamples: Int
    ): ResponseEntity<Map<String, Any>> {
        val user: User = try {
            SecurityContextHolder.getContext().authentication.principal as User
        } catch (e: Exception) {
            val errorData = mapOf("message" to "无效的 Token 或用户未认证", "error" to e.localizedMessage)
            return ResponseEntity.status(401).body(ResponseUtil.formatResponse(401, errorData))
        }

        val userId = user.id

        // 检查文件上传或文件 URL 提供的有效性
        if ((configFile == null && configFileUrl.isNullOrBlank()) ||
            (ckptFile == null && ckptFileUrl.isNullOrBlank()) ||
            (cmFile == null && cmFileUrl.isNullOrBlank())) {
            logger.error("必须提供至少一个文件或文件的 URL")
            val errorData = mapOf("message" to "至少需要提供一个文件或文件的 URL")
            return ResponseEntity.status(400).body(ResponseUtil.formatResponse(400, errorData))
        }

        // 上传文件到 MinIO 或使用 URL
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))

        val configFileUrlFinal = if (configFile != null) {
            uploadToMinio(configFile, "config.yaml", userId, timestamp)
        } else {
            verifyAndUseFileUrl(configFileUrl!!, "config.yaml")
        }

        val ckptFileUrlFinal = if (ckptFile != null) {
            uploadToMinio(ckptFile, "model.pt", userId, timestamp)
        } else {
            verifyAndUseFileUrl(ckptFileUrl!!, "model.pt")
        }

        val cmFileUrlFinal = if (cmFile != null) {
            uploadToMinio(cmFile, "model.cm", userId, timestamp)
        } else {
            verifyAndUseFileUrl(cmFileUrl!!, "model.cm")
        }

        // 创建任务对象，包含 MinIO 文件的 URL 和其他参数
        val task = SampleTask(
            userId = userId,
            configFileUrl = configFileUrlFinal,
            ckptFileUrl = ckptFileUrlFinal,
            cmFileUrl = cmFileUrlFinal,
            n_samples = nSamples
        )

        // 打印任务对象
        logger.info("即将发送的采样任务: $task")

        // 序列化任务对象为 JSON，检查是否包含所有必要的字段
        val taskJson = objectMapper.writeValueAsString(task)
        logger.info("采样任务的 JSON 表示: $taskJson")

        // 检查是否有空字段
        if (configFileUrlFinal.isBlank() || ckptFileUrlFinal.isBlank() || cmFileUrlFinal.isBlank()) {
            logger.error("采样任务发送失败：必填字段为空")
            messagingTemplate.convertAndSend("/topic/progress/$userId", "采样任务发送失败：请检查文件上传是否完整")
            return ResponseEntity.status(400).body(ResponseUtil.formatResponse(400, mapOf("message" to "必填字段为空，采样任务未发送")))

        }

        // 将任务发送到 RabbitMQ 队列
        rabbitTemplate.convertAndSend("sampleTasksExchange", "sampleTasks", task) { message ->
            message.messageProperties.contentType = "application/json"
            message.messageProperties.headers["__TypeId__"] = "com.lovelumine.sample.model.SampleTask"
            message
        }

        // 通知用户任务已提交
        messagingTemplate.convertAndSend("/topic/progress/$userId", "采样任务已提交，正在排队处理")

        // 返回结果
        val response = ResponseUtil.formatResponse(
            200, mapOf(
                "message" to "采样任务已提交，正在排队处理",
                "subscribeUrl" to "/topic/progress/$userId"
            )
        )
        return ResponseEntity.ok(response)
    }

    // 上传文件到 MinIO，增加异常处理并打印日志
    private fun uploadToMinio(file: MultipartFile, fileName: String, userId: Long, timestamp: String): String {
        return try {
            val objectName = "$userId-$timestamp-$fileName"
            val inputStream = ByteArrayInputStream(file.bytes)
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(objectName)
                    .stream(inputStream, file.size, -1)
                    .contentType(file.contentType ?: "application/octet-stream")
                    .build()
            )
            val fileUrl = "$minioBaseUrl/$bucketName/$objectName"
            logger.info("文件上传成功：$fileName -> $fileUrl")
            fileUrl
        } catch (e: Exception) {
            logger.error("文件上传失败: $fileName", e)
            throw RuntimeException("文件上传失败，请稍后重试")
        }
    }

    // 检查并使用文件 URL（必须是有效的 MinIO 链接）
    private fun verifyAndUseFileUrl(fileUrl: String, fileName: String): String {
        return try {
            // 替换 URL 构造方法，避免使用已弃用的构造函数
            val url = URI(fileUrl).toURL()
            if (url.protocol != "http" && url.protocol != "https") {
                throw IllegalArgumentException("无效的 URL 协议")
            }
            val fileUrlFinal = "$minioBaseUrl/$bucketName/$fileName"
            logger.info("文件链接有效：$fileName -> $fileUrlFinal")
            fileUrlFinal
        } catch (e: Exception) {
            logger.error("无效的文件 URL: $fileUrl", e)
            throw IllegalArgumentException("无效的文件 URL，请检查链接")
        }
    }
}
