package com.lovelumine.sample.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.lovelumine.auth.model.User
import com.lovelumine.common.ResponseUtil
import com.lovelumine.sample.model.SampleTask
import io.minio.MinioClient
import io.minio.GetObjectArgs
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

    // 文件路径映射
    private val filePaths = mapOf(
        "Phenylalanine_Bacteria.pt" to "https://minio.lumoxuan.cn/ayumerna/1-20241030205748-model.pt",
        "1-20241030211218-model.cm" to "https://minio.lumoxuan.cn/ayumerna/1-20241030211218-model.cm",
        "config.yaml" to "https://minio.lumoxuan.cn/ayumerna/1-20241030214357-config.yaml"
    )

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
        @RequestParam("model") model: String,
        @RequestParam("reverseCodon") reverseCodon: String,
        @RequestParam("sequenceCount") sequenceCount: Int
    ): ResponseEntity<Map<String, Any>> {
//        val user: User = try {
//            SecurityContextHolder.getContext().authentication.principal as User
//        } catch (e: Exception) {
//            val errorData = mapOf("message" to "无效的 Token 或用户未认证", "error" to e.localizedMessage)
//            return ResponseEntity.status(401).body(ResponseUtil.formatResponse(401, errorData))
//        }

        val userId: Long = 1

        // 根据模型名称查找文件路径
        val configFileUrl = getFileUrl("config.yaml")
        val ckptFileUrl = getFileUrl(model)
        val cmFileUrl = getFileUrl("1-20241030211218-model.cm")

        // 检查是否找到了文件路径
        if (configFileUrl.isBlank() || ckptFileUrl.isBlank()) {
            logger.error("未找到所请求的文件：model=$model")
            val errorData = mapOf("message" to "无法找到模型文件或配置文件")
            return ResponseEntity.status(400).body(ResponseUtil.formatResponse(400, errorData))
        }

        // 创建任务对象，包含 MinIO 文件的 URL 和其他参数
        val task = SampleTask(
            userId = userId,
            configFileUrl = configFileUrl,
            ckptFileUrl = ckptFileUrl,
            n_samples = sequenceCount,
            cmFileUrl = cmFileUrl,
        )

        // 打印任务对象
        logger.info("即将发送的采样任务: $task")

        // 序列化任务对象为 JSON，检查是否包含所有必要的字段
        val taskJson = objectMapper.writeValueAsString(task)
        logger.info("采样任务的 JSON 表示: $taskJson")

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

    // 自动获取文件的 MinIO 存储路径
    private fun getFileUrl(fileName: String): String {
        return filePaths[fileName] ?: ""
    }
}
