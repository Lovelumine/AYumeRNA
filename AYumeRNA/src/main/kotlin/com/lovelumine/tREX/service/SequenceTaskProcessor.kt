package com.lovelumine.tREX.service

import com.lovelumine.tREX.model.SequenceTask
import org.springframework.amqp.core.MessagePostProcessor
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class SequenceTaskProcessor(
    @Autowired private val sequenceService: SequenceService,
    @Autowired private val redisTemplate: StringRedisTemplate,
    @Autowired private val rabbitTemplate: RabbitTemplate,
    @Autowired private val messagingTemplate: SimpMessagingTemplate // Ensure SimpMessagingTemplate is injected correctly
) {

    private val maxRetryCount = 5 // 设置最大重试次数

    @RabbitListener(queues = ["sequenceTasks"], containerFactory = "rabbitListenerContainerFactory")
    fun handleSequenceTask(task: SequenceTask) {
        val lockKey = "sequence_lock:${task.userId}"
        // Handling nullable queuePosition with default value
        val queuePosition = redisTemplate.opsForList().size("sequenceTasksQueue") ?: 0

        // Send queue position to WebSocket
        messagingTemplate.convertAndSend("/topic/progress/${task.userId}", "任务已提交，排在第 ${queuePosition + 1} 位")

        val acquiredLock = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", 1, TimeUnit.HOURS)
        if (acquiredLock == true) {
            println("成功获取到锁：$lockKey")
            try {
                // 处理任务
                sequenceService.processSequences(task)
            } catch (e: Exception) {
                println("处理任务时发生异常：${e.message}")
                e.printStackTrace()
            } finally {
                // 释放锁
                redisTemplate.delete(lockKey)
                println("释放锁：$lockKey")
            }
        } else {
            // Handle task retry logic
            if (task.retryCount >= maxRetryCount) {
                println("用户 ${task.userId} 的任务重试次数过多，放弃处理")
            } else {
                println("用户 ${task.userId} 已有任务在执行，任务将重新排队")
                task.retryCount += 1
                val delayInMilliseconds = 5000 * task.retryCount
                val messagePostProcessor = MessagePostProcessor { message ->
                    message.messageProperties.headers["x-delay"] = delayInMilliseconds
                    message
                }
                rabbitTemplate.convertAndSend("sequenceTasksExchange", "sequenceTasks", task, messagePostProcessor)

                // Notify WebSocket of re-queue
                messagingTemplate.convertAndSend("/topic/progress/${task.userId}", "任务重新排队，第 ${queuePosition + 1} 位")
            }
        }
    }
}
