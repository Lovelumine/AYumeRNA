package com.lovelumine.Rfam.controller

import com.lovelumine.auth.model.User
import com.lovelumine.common.ResponseUtil
import com.lovelumine.Rfam.model.RfamTask
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/rfam")
class RfamController(
    @Autowired private val rabbitTemplate: RabbitTemplate,
    @Autowired private val messagingTemplate: SimpMessagingTemplate
) {

    @Operation(summary = "提交 Rfam 任务", description = "用户可以通过该接口提交 Rfam 任务，上传种子文件和原始序列文件")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "任务已成功提交"),
            ApiResponse(responseCode = "401", description = "用户认证失败"),
            ApiResponse(responseCode = "400", description = "请求参数错误"),
            ApiResponse(responseCode = "500", description = "服务器内部错误")
        ]
    )
    @PostMapping("/process")
    fun processRfamTask(
        @RequestParam("rfamAcc") rfamAcc: String,
        @RequestParam("seedFile") seedFile: MultipartFile,
        @RequestParam("originalFile") originalFile: MultipartFile
    ): ResponseEntity<Map<String, Any>> {
        // 获取当前用户
        val user: User = try {
            SecurityContextHolder.getContext().authentication.principal as User
        } catch (e: Exception) {
            val errorData = mapOf("message" to "无效的 Token 或用户未认证", "error" to e.localizedMessage)
            return ResponseEntity.status(401).body(ResponseUtil.formatResponse(401, errorData))
        }

        val userId = user.id

        // 创建任务对象
        val seedFileData: String = try {
            String(seedFile.bytes)
        } catch (e: Exception) {
            return ResponseEntity.badRequest().body(mapOf("message" to "无法读取种子文件", "error" to e.localizedMessage))
        }

        val originalFileData: String = try {
            String(originalFile.bytes)
        } catch (e: Exception) {
            return ResponseEntity.badRequest().body(mapOf("message" to "无法读取原始序列文件", "error" to e.localizedMessage))
        }

        val task = RfamTask(
            userId = userId,
            rfamAcc = rfamAcc,
            seedFileData = seedFileData,
            originalFileData = originalFileData // 原始序列文件数据
        )

        // 将任务发送到 RabbitMQ 队列
        rabbitTemplate.convertAndSend("rfamTasksExchange", "rfamTasks", task)

        // 通知用户任务已提交
        messagingTemplate.convertAndSend("/topic/progress/$userId", "任务已提交，正在排队处理")

        return ResponseEntity.ok(ResponseUtil.formatResponse(200, "任务已提交，正在排队处理"))
    }
}
