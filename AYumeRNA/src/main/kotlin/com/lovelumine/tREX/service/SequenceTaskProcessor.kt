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
        val queueKey = "sequenceTasksQueue"

        // 将任务添加到队列尾部
        redisTemplate.opsForList().rightPush(queueKey, task.userId.toString())

        // 获取整个队列的长度
        val queueLength = redisTemplate.opsForList().size(queueKey) ?: 0

        // 获取任务在队列中的位置（从0开始计数）
        val queuePosition = redisTemplate.opsForList().range(queueKey, 0, -1)?.indexOf(task.userId.toString()) ?: -1

        // 通知前端任务的队列位置
        messagingTemplate.convertAndSend("/topic/progress/${task.userId}", "任务已提交，排在第 ${queuePosition + 1} 位")

        // 尝试获取任务锁
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
                // 释放锁，并从Redis队列中移除任务
                redisTemplate.delete(lockKey)

                // 从队列头部移除第一个任务
                redisTemplate.opsForList().leftPop(queueKey)
                println("释放锁并移除队列中的任务：$lockKey")
            }
        } else {
            // 处理重试逻辑
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

                // 通知前端任务重新排队
                messagingTemplate.convertAndSend(
                    "/topic/progress/${task.userId}",
                    "任务重新排队，第 ${queuePosition + 1} 位"
                )
            }
        }
    }
}
