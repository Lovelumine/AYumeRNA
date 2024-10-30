// SampleController.kt
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
        @RequestParam("config_file") configFile: MultipartFile,
        @RequestParam("ckpt_file") ckptFile: MultipartFile,
        @RequestParam("cm_file") cmFile: MultipartFile,
        @RequestParam("n_samples") nSamples: Int
    ): ResponseEntity<Map<String, Any>> {
        val user: User = try {
            SecurityContextHolder.getContext().authentication.principal as User
        } catch (e: Exception) {
            val errorData = mapOf("message" to "无效的 Token 或用户未认证", "error" to e.localizedMessage)
            return ResponseEntity.status(401).body(ResponseUtil.formatResponse(401, errorData))
        }

        val userId = user.id

        // 检查所有文件是否上传成功
        if (configFile.isEmpty || ckptFile.isEmpty || cmFile.isEmpty) {
            logger.error("必需的文件未上传或为空")
            val errorData = mapOf("message" to "所有文件（config_file, ckpt_file, cm_file）必须上传且不能为空")
            return ResponseEntity.status(400).body(ResponseUtil.formatResponse(400, errorData))
        }

        // 上传文件到 MinIO
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))

        val configFileUrl = uploadToMinio(configFile, "config.yaml", userId, timestamp)
        val ckptFileUrl = uploadToMinio(ckptFile, "model.pt", userId, timestamp)
        val cmFileUrl = uploadToMinio(cmFile, "model.cm", userId, timestamp)

        // 创建任务对象，包含 MinIO 文件的 URL 和其他参数
        val task = SampleTask(
            userId = userId,
            configFileUrl = configFileUrl,
            ckptFileUrl = ckptFileUrl,
            cmFileUrl = cmFileUrl,
            n_samples = nSamples
        )

        // 打印任务对象
        logger.info("即将发送的采样任务: $task")

        // 序列化任务对象为 JSON，检查是否包含所有必要的字段
        val taskJson = objectMapper.writeValueAsString(task)
        logger.info("采样任务的 JSON 表示: $taskJson")

        // 检查是否有空字段
        if (configFileUrl.isBlank() || ckptFileUrl.isBlank() || cmFileUrl.isBlank()) {
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

}
