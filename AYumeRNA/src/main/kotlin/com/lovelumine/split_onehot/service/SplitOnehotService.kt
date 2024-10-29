package com.lovelumine.split_onehot.service

import com.lovelumine.split_onehot.model.SplitOnehotTask
import com.lovelumine.split_onehot.utils.SplitOnehotUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
class SplitOnehotService(
    @Autowired private val messagingTemplate: SimpMessagingTemplate,
    @Value("\${minio.url}") private val minioBaseUrl: String
) {

    private val logger = LoggerFactory.getLogger(SplitOnehotService::class.java)

    fun processSplitOnehotTask(task: SplitOnehotTask) {
        val userId = task.userId

        try {
            // 通知进度
            messagingTemplate.convertAndSend("/topic/progress/$userId", "进度：5% - 准备处理任务...")

            // 调用 Flask API 处理任务，传递 MinIO 文件 URL 和参数
            val (resultUrls, progressMessages) = SplitOnehotUtils.processSplitOnehot(
                task.h5FileUrl,
                task.trainRatio,
                task.randomState,
                userId
            )

            // 发送进度消息
            for (message in progressMessages) {
                messagingTemplate.convertAndSend("/topic/progress/$userId", message)
            }

            // 通知用户任务完成，返回结果文件的链接
            messagingTemplate.convertAndSend(
                "/topic/progress/$userId",
                "任务完成，结果已上传：训练集 - ${resultUrls.trainUrl}, 验证集 - ${resultUrls.validUrl}, 测试集 - ${resultUrls.testUrl}"
            )

        } catch (e: Exception) {
            logger.error("任务处理失败：${e.message}", e)
            messagingTemplate.convertAndSend("/topic/progress/$userId", "任务失败：${e.message}")
        }
    }
}
