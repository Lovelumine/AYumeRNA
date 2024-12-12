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
    @Autowired private val messagingTemplate: SimpMessagingTemplate
) {

    private val maxRetryCount = 5

    @RabbitListener(queues = ["sequenceTasks"], containerFactory = "rabbitListenerContainerFactory")
    fun handleSequenceTask(task: SequenceTask) {
        val lockKey = "sequence_lock:${task.userId}"
        val queueKey = "sequenceTasksQueue"
        val userIdStr = task.userId.toString()

        val currentQueue = redisTemplate.opsForList().range(queueKey, 0, -1) ?: emptyList()
        if (!currentQueue.contains(userIdStr)) {
            redisTemplate.opsForList().rightPush(queueKey, userIdStr)
        }

        val updatedQueue = redisTemplate.opsForList().range(queueKey, 0, -1) ?: emptyList()
        val queuePosition = updatedQueue.indexOf(userIdStr)
        if (queuePosition >= 0) {
            messagingTemplate.convertAndSend("/topic/progress/${task.userId}", "任务已提交，排在第 ${queuePosition + 1} 位")
        }

        val acquiredLock = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", 1, TimeUnit.HOURS)
        if (acquiredLock == true) {
            println("成功获取到锁：$lockKey")
            try {
                sequenceService.processSequences(task)
            } catch (e: Exception) {
                println("处理任务时发生异常：${e.message}")
                e.printStackTrace()
            } finally {
                redisTemplate.delete(lockKey)
                redisTemplate.opsForList().leftPop(queueKey)
                println("释放锁并移除队列中的任务：$lockKey")
            }
        } else {
            if (task.retryCount >= maxRetryCount) {
                println("用户 ${task.userId} 的任务重试次数过多，放弃处理")
                redisTemplate.opsForList().remove(queueKey, 1, userIdStr)
                messagingTemplate.convertAndSend("/topic/progress/${task.userId}", "用户 ${task.userId} 的任务重试次数过多，已放弃任务")
            } else {
                println("用户 ${task.userId} 已有任务在执行，任务将重新排队")
                task.retryCount += 1
                val delayInMilliseconds = 5000 * task.retryCount
                val messagePostProcessor = MessagePostProcessor { message ->
                    message.messageProperties.headers["x-delay"] = delayInMilliseconds
                    message
                }
                rabbitTemplate.convertAndSend("sequenceTasksExchange", "sequenceTasks", task, messagePostProcessor)

                if (queuePosition >= 0) {
                    messagingTemplate.convertAndSend(
                        "/topic/progress/${task.userId}",
                        "任务重新排队，第 ${queuePosition + 1} 位"
                    )
                }
            }
        }
    }
}