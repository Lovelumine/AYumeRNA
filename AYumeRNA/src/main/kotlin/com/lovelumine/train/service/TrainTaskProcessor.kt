// TrainTaskProcessor.kt
package com.lovelumine.train.service

import com.lovelumine.train.model.TrainTask
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.support.converter.MessageConversionException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
class TrainTaskProcessor(
    @Autowired private val trainService: TrainService,
    @Autowired private val messagingTemplate: SimpMessagingTemplate
) {

    private val logger = LoggerFactory.getLogger(TrainTaskProcessor::class.java)

    @RabbitListener(queues = ["trainTasks"], containerFactory = "rabbitListenerContainerFactory")
    fun handleTrainTask(task: TrainTask) {
        try {
            logger.info("接收到训练任务: ${task.userId}")
            trainService.processTrainTask(task)
        } catch (e: MessageConversionException) {
            // 捕获并处理反序列化异常
            logger.error("处理训练任务时出错：消息转换失败，可能缺少必要的字段", e)
            messagingTemplate.convertAndSend("/topic/progress/${task.userId}", "训练任务处理失败：缺少必要的字段")
        } catch (e: Exception) {
            // 捕获其他异常
            logger.error("处理训练任务时出错：${e.message}", e)
            messagingTemplate.convertAndSend("/topic/progress/${task.userId}", "训练任务处理失败：${e.message}")
        }
    }

}
