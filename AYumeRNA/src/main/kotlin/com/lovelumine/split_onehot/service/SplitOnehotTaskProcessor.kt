package com.lovelumine.split_onehot.service

import com.lovelumine.split_onehot.model.SplitOnehotTask
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
class SplitOnehotTaskProcessor(
    @Autowired private val splitOnehotService: SplitOnehotService,
    @Autowired private val messagingTemplate: SimpMessagingTemplate
) {

    private val logger = LoggerFactory.getLogger(SplitOnehotTaskProcessor::class.java)

    @RabbitListener(queues = ["splitOnehotTasks"], containerFactory = "rabbitListenerContainerFactory")
    fun handleSplitOnehotTask(task: SplitOnehotTask) {
        logger.info("接收到 split_onehot 任务: ${task.userId}")
        try {
            splitOnehotService.processSplitOnehotTask(task)
        } catch (e: Exception) {
            messagingTemplate.convertAndSend("/topic/progress/${task.userId}", "任务处理失败：${e.message}")
        }
    }
}
