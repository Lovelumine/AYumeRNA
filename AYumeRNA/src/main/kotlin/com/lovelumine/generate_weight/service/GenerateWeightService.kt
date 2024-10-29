package com.lovelumine.generate_weight.service

import com.lovelumine.generate_weight.model.GenerateWeightTask
import com.lovelumine.generate_weight.utils.GenerateWeightUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
class GenerateWeightService(
    @Autowired private val messagingTemplate: SimpMessagingTemplate,
    @Value("\${minio.url}") private val minioBaseUrl: String
) {

    private val logger = LoggerFactory.getLogger(GenerateWeightService::class.java)

    fun processGenerateWeightTask(task: GenerateWeightTask) {
        val userId = task.userId

        try {
            // 通知进度
            messagingTemplate.convertAndSend("/topic/progress/$userId", "进度：5% - 准备处理任务...")

            // 调用 Flask API 处理任务，传递 MinIO 文件 URL 和参数
            val (outputUrl, progressMessages) = GenerateWeightUtils.processGenerateWeight(task)

            // 发送进度消息
            for (message in progressMessages) {
                messagingTemplate.convertAndSend("/topic/progress/$userId", message)
            }

            // 通知用户任务完成，返回结果文件的链接
            messagingTemplate.convertAndSend(
                "/topic/progress/$userId",
                "任务完成，结果已上传：权重文件 - $outputUrl"
            )

        } catch (e: Exception) {
            logger.error("任务处理失败：${e.message}", e)
            messagingTemplate.convertAndSend("/topic/progress/$userId", "任务失败：${e.message}")
        }
    }
}
