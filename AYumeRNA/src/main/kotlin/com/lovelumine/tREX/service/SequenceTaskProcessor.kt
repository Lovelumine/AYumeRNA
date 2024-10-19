package com.lovelumine.tREX.service

import com.lovelumine.tREX.model.SequenceTask
import org.springframework.amqp.core.MessagePostProcessor
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class SequenceTaskProcessor(
    @Autowired private val sequenceService: SequenceService,
    @Autowired private val redisTemplate: StringRedisTemplate,
    @Autowired private val rabbitTemplate: RabbitTemplate
) {

    private val maxRetryCount = 5 // 设置最大重试次数

    @RabbitListener(queues = ["sequenceTasks"], containerFactory = "rabbitListenerContainerFactory")
    fun handleSequenceTask(task: SequenceTask) {
        val lockKey = "sequence_lock:${task.userId}"
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
            // 用户已有任务在执行，判断是否超过最大重试次数
            if (task.retryCount >= maxRetryCount) {
                println("用户 ${task.userId} 的任务重试次数过多，放弃处理")
                // 可以选择记录日志、发送通知或将任务存储到数据库以供后续分析
            } else {
                println("用户 ${task.userId} 已有任务在执行，任务将重新排队")
                // 增加重试次数
                task.retryCount += 1
                // 设置延迟（例如 5 秒）
                val delayInMilliseconds = 5000 * task.retryCount // 延迟时间递增
                val messagePostProcessor = MessagePostProcessor { message ->
                    message.messageProperties.headers["x-delay"] = delayInMilliseconds
                    message
                }
                rabbitTemplate.convertAndSend("sequenceTasksExchange", "sequenceTasks", task, messagePostProcessor)
            }
        }
    }
}
