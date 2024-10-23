package com.lovelumine.cmbuild.controller

import com.lovelumine.auth.model.User
import com.lovelumine.common.ResponseUtil
import com.lovelumine.cmbuild.model.CmbuildTask
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
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
@RequestMapping("/cmbuild")
class CmbuildController(
    @Autowired private val rabbitTemplate: RabbitTemplate,
    @Autowired private val messagingTemplate: SimpMessagingTemplate,
    @Autowired private val minioClient: MinioClient,
    @Value("\${minio.bucket-name}") private val bucketName: String,
    @Value("\${minio.url}") private val minioBaseUrl: String
) {

    private val logger = LoggerFactory.getLogger(CmbuildController::class.java)

    @Operation(summary = "提交 cmbuild 任务", description = "用户可以通过该接口提交 cmbuild 任务，上传 Stockholm 文件")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "任务已成功提交"),
            ApiResponse(responseCode = "401", description = "用户认证失败"),
            ApiResponse(responseCode = "400", description = "请求参数错误"),
            ApiResponse(responseCode = "500", description = "服务器内部错误")
        ]
    )
    @PostMapping("/process")
    fun processCmbuildTask(
        @RequestParam("stockholmFile") stockholmFile: MultipartFile
    ): ResponseEntity<Map<String, Any>> {
        logger.info("接收到任务提交请求")

        // 获取当前用户
        val user: User = try {
            SecurityContextHolder.getContext().authentication.principal as User
        } catch (e: Exception) {
            logger.error("用户认证失败: ${e.message}", e)
            val errorData = mapOf("message" to "无效的 Token 或用户未认证", "error" to e.localizedMessage)
            return ResponseEntity.status(401).body(ResponseUtil.formatResponse(401, errorData))
        }

        val userId = user.id

        // 上传文件到 MinIO
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
        val stockholmFileUrl = uploadToMinio(stockholmFile, "RF00005.stockholm", userId, timestamp)
        logger.info("文件已上传到 MinIO, 文件路径: $stockholmFileUrl")

        // 创建任务对象，包含 MinIO 文件的 URL
        val task = CmbuildTask(
            userId = userId,
            stockholmFileUrl = stockholmFileUrl
        )

        // 将任务发送到 RabbitMQ 队列
        logger.info("将任务发送到 RabbitMQ")
        rabbitTemplate.convertAndSend("cmbuildTasksExchange", "cmbuildTasks", task)

        // 通知用户任务已提交
        messagingTemplate.convertAndSend("/topic/progress/$userId", "任务已提交，正在排队处理")
        logger.info("通知用户任务已提交")

        // 返回结果，并告知如何通过 WebSocket 订阅任务进度
        val response = ResponseUtil.formatResponse(
            200, mapOf(
                "message" to "任务已提交，正在排队处理",
                "subscribeUrl" to "/topic/progress/$userId" // WebSocket 订阅 URL
            )
        )
        return ResponseEntity.ok(response)
    }

    // 上传文件到 MinIO
    private fun uploadToMinio(file: MultipartFile, fileName: String, userId: Long, timestamp: String): String {
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
        return "$minioBaseUrl/$bucketName/$objectName"
    }
}
