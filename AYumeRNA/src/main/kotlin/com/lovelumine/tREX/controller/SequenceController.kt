package com.lovelumine.tREX.controller

import com.lovelumine.auth.model.User
import com.lovelumine.common.ResponseUtil
import com.lovelumine.tREX.model.SequenceTask
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/sequence")
class SequenceController(
    @Autowired private val rabbitTemplate: RabbitTemplate,
    @Autowired private val redisTemplate: StringRedisTemplate
) {

    @PostMapping("/process")
    fun processSequence(
        @RequestParam("templateFile") templateFile: MultipartFile,
        @RequestParam("testFile") testFile: MultipartFile
    ): ResponseEntity<Map<String, Any>> {
        // 捕获无效的用户认证信息
        val user: User
        try {
            user = SecurityContextHolder.getContext().authentication.principal as User
        } catch (e: Exception) {
            val errorData = mapOf("message" to "无效的 Token 或用户未认证", "error" to e.localizedMessage)
            return ResponseEntity.status(401).body(ResponseUtil.formatResponse(401, errorData))
        }

        val userId = user.id
        val lockKey = "sequence_lock:$userId"

        // 检查用户是否已有任务在执行
        val isLocked = redisTemplate.hasKey(lockKey)
        if (isLocked == true) {
            return ResponseEntity.status(429).body(
                ResponseUtil.formatResponse(429, "已有任务在执行，请稍后再试")
            )
        }

        // 创建任务对象
        val task = SequenceTask(
            userId = userId,
            username = user.username,
            templateFileData = templateFile.bytes,
            testFileData = testFile.bytes
        )

        // 将任务放入队列
        rabbitTemplate.convertAndSend("sequenceTasks", task)

        return ResponseEntity.ok(
            ResponseUtil.formatResponse(200, "任务已提交，正在排队处理")
        )
    }
}
