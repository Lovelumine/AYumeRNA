package com.lovelumine.cmbuild.service

import com.lovelumine.cmbuild.model.CmbuildTask
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
class CmbuildTaskProcessor(
    @Autowired private val cmbuildService: CmbuildService,
    @Autowired private val messagingTemplate: SimpMessagingTemplate
) {

    private val logger = LoggerFactory.getLogger(CmbuildTaskProcessor::class.java)

    @RabbitListener(queues = ["cmbuildTasks"], containerFactory = "rabbitListenerContainerFactory")
    fun handleCmbuildTask(task: CmbuildTask) {
        logger.info("接收到任务: ${task.userId}")
        try {
            cmbuildService.processCmbuildTask(task)
        } catch (e: Exception) {
            logger.error("任务处理失败: ${e.message}", e)
            messagingTemplate.convertAndSend("/topic/progress/${task.userId}", "任务处理失败：${e.message}")
        }
    }
}
