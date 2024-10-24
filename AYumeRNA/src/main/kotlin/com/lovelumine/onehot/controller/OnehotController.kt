package com.lovelumine.onehot.controller

import com.lovelumine.auth.model.User
import com.lovelumine.common.ResponseUtil
import com.lovelumine.onehot.model.OnehotTask
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
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
@RequestMapping("/onehot")
class OnehotController(
    @Autowired private val rabbitTemplate: RabbitTemplate,
    @Autowired private val messagingTemplate: SimpMessagingTemplate,
    @Autowired private val minioClient: MinioClient,
    @Value("\${minio.bucket-name}") private val bucketName: String,
    @Value("\${minio.url}") private val minioBaseUrl: String
) {

    @Operation(summary = "提交 onehot 任务", description = "用户可以通过该接口提交 onehot 任务，上传 FASTA 文件和 CM 文件")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "任务已成功提交"),
            ApiResponse(responseCode = "401", description = "用户认证失败"),
            ApiResponse(responseCode = "400", description = "请求参数错误"),
            ApiResponse(responseCode = "500", description = "服务器内部错误")
        ]
    )
    @PostMapping("/process")
    fun processOnehotTask(
        @RequestParam("fastaFile") fastaFile: MultipartFile,
        @RequestParam("cmFile") cmFile: MultipartFile
    ): ResponseEntity<Map<String, Any>> {
        val user: User = try {
            SecurityContextHolder.getContext().authentication.principal as User
        } catch (e: Exception) {
            val errorData = mapOf("message" to "无效的 Token 或用户未认证", "error" to e.localizedMessage)
            return ResponseEntity.status(401).body(ResponseUtil.formatResponse(401, errorData))
        }

        val userId = user.id

        // 上传文件到 MinIO
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
        val fastaFileUrl = uploadToMinio(fastaFile, "task_fasta.fasta", userId, timestamp)
        val cmFileUrl = uploadToMinio(cmFile, "task_cm.cm", userId, timestamp)

        // 创建任务对象，包含 MinIO 文件的 URL
        val task = OnehotTask(
            userId = userId,
            fastaFileUrl = fastaFileUrl,
            cmFileUrl = cmFileUrl
        )

        // 将任务发送到 RabbitMQ 队列
        rabbitTemplate.convertAndSend("onehotTasksExchange", "onehotTasks", task)

        // 通知用户任务已提交
        messagingTemplate.convertAndSend("/topic/progress/$userId", "任务已提交，正在排队处理")

        // 返回结果
        val response = ResponseUtil.formatResponse(
            200, mapOf(
                "message" to "任务已提交，正在排队处理",
                "subscribeUrl" to "/topic/progress/$userId"
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
