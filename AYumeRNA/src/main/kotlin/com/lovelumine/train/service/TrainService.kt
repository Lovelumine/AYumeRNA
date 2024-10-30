package com.lovelumine.train.service

import com.lovelumine.train.model.TrainTask
import com.lovelumine.train.utils.TrainUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
class TrainService(
    @Autowired private val messagingTemplate: SimpMessagingTemplate,
    @Value("\${minio.url}") private val minioBaseUrl: String
) {

    private val logger = LoggerFactory.getLogger(TrainService::class.java)

    fun processTrainTask(task: TrainTask) {
        val userId = task.userId

        try {
            // 通知进度
            messagingTemplate.convertAndSend("/topic/progress/$userId", "进度：5% - 准备处理训练任务...")

            // 调用训练工具处理任务
            val (outputFileUrl, progressMessages) = TrainUtils.processTrain(task)

            // 发送进度消息
            for (message in progressMessages) {
                messagingTemplate.convertAndSend("/topic/progress/$userId", message)
            }

            // 通知用户任务完成
            messagingTemplate.convertAndSend("/topic/progress/$userId", "训练任务完成，结果已上传：$outputFileUrl")

        } catch (e: Exception) {
            logger.error("训练任务处理失败：${e.message}", e)
            messagingTemplate.convertAndSend("/topic/progress/$userId", "训练任务失败：${e.message}")
        }
    }
}
