package com.lovelumine.onehot.service

import com.lovelumine.onehot.model.OnehotTask
import com.lovelumine.onehot.utils.OnehotUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
class OnehotService(
    @Autowired private val messagingTemplate: SimpMessagingTemplate,
    @Value("\${minio.url}") private val minioBaseUrl: String
) {

    private val logger = LoggerFactory.getLogger(OnehotService::class.java)

    fun processOnehotTask(task: OnehotTask) {
        val userId = task.userId

        try {
            // 通知进度
            messagingTemplate.convertAndSend("/topic/progress/$userId", "进度：5% - 准备处理任务...")

            // 调用 Flask API 处理任务，传递 MinIO 文件 URL
            val (outputFileUrl, progressMessages) = OnehotUtils.processOnehot(task.fastaFileUrl, task.cmFileUrl, userId)

            // 发送进度消息
            for (message in progressMessages) {
                messagingTemplate.convertAndSend("/topic/progress/$userId", message)
            }

            // 通知用户任务完成
            messagingTemplate.convertAndSend("/topic/progress/$userId", "任务完成，结果已上传：$outputFileUrl")

        } catch (e: Exception) {
            logger.error("任务处理失败：${e.message}", e)
            messagingTemplate.convertAndSend("/topic/progress/$userId", "任务失败：${e.message}")
        }
    }
}
