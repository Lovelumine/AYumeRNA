// SampleService.kt
package com.lovelumine.sample.service

import com.lovelumine.sample.model.SampleTask
import com.lovelumine.sample.utils.SampleUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
class SampleService(
    @Autowired private val messagingTemplate: SimpMessagingTemplate,
    @Value("\${flask.api.url}") private val flaskApiUrl: String
) {

    private val logger = LoggerFactory.getLogger(SampleService::class.java)

    fun processSampleTask(task: SampleTask) {
        val userId = task.userId

        try {
            // 通知进度
            messagingTemplate.convertAndSend("/topic/progress/$userId", "进度：5% - 准备处理采样任务...")

            // 调用工具处理任务
            val (outputFileUrl, progressMessages) = SampleUtils.processSample(task, flaskApiUrl)

            // 发送进度消息
            for (message in progressMessages) {
                messagingTemplate.convertAndSend("/topic/progress/$userId", message)
            }

            // 通知用户任务完成
            messagingTemplate.convertAndSend("/topic/progress/$userId", "采样任务完成，结果已上传：$outputFileUrl")

        } catch (e: Exception) {
            logger.error("采样任务处理失败：${e.message}", e)
            messagingTemplate.convertAndSend("/topic/progress/$userId", "采样任务失败：${e.message}")
        }
    }
}
