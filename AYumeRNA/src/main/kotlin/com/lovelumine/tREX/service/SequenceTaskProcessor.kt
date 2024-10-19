package com.lovelumine.tREX.service

import com.lovelumine.tREX.model.SequenceTask
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class SequenceTaskProcessor(
    @Autowired private val sequenceService: SequenceService,
    @Autowired private val redisTemplate: StringRedisTemplate
) {

    @RabbitListener(queues = ["sequenceTasks"])
    fun handleSequenceTask(task: SequenceTask) {
        val lockKey = "sequence_lock:${task.userId}"
        val isLocked = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", 1, TimeUnit.HOURS)
        if (isLocked == true) {
            try {
                // 调用修改后的 processSequences 方法
                sequenceService.processSequences(task)
            } finally {
                redisTemplate.delete(lockKey)
            }
        } else {
            // 用户已有任务在执行，将任务重新放入队列末尾或进行其他处理
            println("用户 ${task.userId} 已有任务在执行，跳过当前任务")
        }
    }
}
