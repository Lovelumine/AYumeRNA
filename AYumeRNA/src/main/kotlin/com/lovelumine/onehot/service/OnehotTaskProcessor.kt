package com.lovelumine.onehot.service

import com.lovelumine.onehot.model.OnehotTask
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
class OnehotTaskProcessor(
    @Autowired private val onehotService: OnehotService,
    @Autowired private val messagingTemplate: SimpMessagingTemplate
) {

    private val logger = LoggerFactory.getLogger(OnehotTaskProcessor::class.java)

    @RabbitListener(queues = ["onehotTasks"], containerFactory = "rabbitListenerContainerFactory")
    fun handleOnehotTask(task: OnehotTask) {
        logger.info("接收到 onehot 任务: ${task.userId}")
        try {
            onehotService.processOnehotTask(task)
        } catch (e: Exception) {
            messagingTemplate.convertAndSend("/topic/progress/${task.userId}", "任务处理失败：${e.message}")
        }
    }
}
