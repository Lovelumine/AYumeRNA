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
            // Notify progress
            messagingTemplate.convertAndSend("/topic/progress/$userId", "Progress: 5% - Preparing to process sampling task...")

            // Call utility to process the task
            val (outputFileUrl, progressMessages) = SampleUtils.processSample(task, flaskApiUrl)

            // Send progress messages
            for (message in progressMessages) {
                messagingTemplate.convertAndSend("/topic/progress/$userId", message)
            }

            // Notify user that the task is complete
            messagingTemplate.convertAndSend("/topic/progress/$userId", "Sampling task completed, result uploaded: $outputFileUrl")

        } catch (e: Exception) {
            logger.error("Sampling task processing failed: ${e.message}", e)
            messagingTemplate.convertAndSend("/topic/progress/$userId", "Sampling task failed: ${e.message}")
        }
    }
}
