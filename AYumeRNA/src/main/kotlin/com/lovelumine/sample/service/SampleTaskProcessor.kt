// SampleTaskProcessor.kt
package com.lovelumine.sample.service

import com.lovelumine.sample.model.SampleTask
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.support.converter.MessageConversionException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
class SampleTaskProcessor(
    @Autowired private val sampleService: SampleService,
    @Autowired private val messagingTemplate: SimpMessagingTemplate
) {

    private val logger = LoggerFactory.getLogger(SampleTaskProcessor::class.java)

    @RabbitListener(queues = ["sampleTasks"], containerFactory = "rabbitListenerContainerFactory")
    fun handleSampleTask(task: SampleTask) {
        try {
            logger.info("接收到采样任务: ${task.userId}")
            sampleService.processSampleTask(task)
        } catch (e: MessageConversionException) {
            // 捕获并处理反序列化异常
            logger.error("处理采样任务时出错：消息转换失败，可能缺少必要的字段", e)
            messagingTemplate.convertAndSend("/topic/progress/${task.userId}", "采样任务处理失败：缺少必要的字段")
        } catch (e: Exception) {
            // 捕获其他异常
            logger.error("处理采样任务时出错：${e.message}", e)
            messagingTemplate.convertAndSend("/topic/progress/${task.userId}", "采样任务处理失败：${e.message}")
        }
    }

}
