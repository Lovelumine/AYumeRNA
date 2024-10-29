package com.lovelumine.generate_weight.service

import com.lovelumine.generate_weight.model.GenerateWeightTask
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
class GenerateWeightTaskProcessor(
    @Autowired private val generateWeightService: GenerateWeightService,
    @Autowired private val messagingTemplate: SimpMessagingTemplate
) {

    private val logger = LoggerFactory.getLogger(GenerateWeightTaskProcessor::class.java)

    @RabbitListener(queues = ["generateWeightTasks"], containerFactory = "rabbitListenerContainerFactory")
    fun handleGenerateWeightTask(task: GenerateWeightTask) {
        logger.info("接收到 generate_weight 任务: ${task.userId}")
        try {
            generateWeightService.processGenerateWeightTask(task)
        } catch (e: Exception) {
            messagingTemplate.convertAndSend("/topic/progress/${task.userId}", "任务处理失败：${e.message}")
        }
    }
}
