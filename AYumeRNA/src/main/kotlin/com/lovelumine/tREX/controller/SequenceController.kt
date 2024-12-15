package com.lovelumine.tREX.controller

import com.lovelumine.common.ResponseUtil
import com.lovelumine.tREX.model.SequenceTask
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/sequence")
class SequenceController(
    private val rabbitTemplate: RabbitTemplate
) {

    private val filePaths = mapOf(
        "CUA" to "https://minio.lumoxuan.cn/ayumerna/model/CTA.csv",
        "UUA" to "https://minio.lumoxuan.cn/ayumerna/model/TTA.csv"
    )

    private val httpClient = OkHttpClient()

    @PostMapping("/process")
    fun processSequence(
        @RequestParam("aminoAcid") aminoAcid: String,
        @RequestParam("domain") domain: String,
        @RequestParam("reverseCodon") reverseCodon: String,
        @RequestParam("testFile") testFile: MultipartFile
    ): ResponseEntity<Map<String, Any>> {
        val userId: Long = 1
        val username: String = "lovelumine"

        val key = reverseCodon
        val templateFileUrl = filePaths[key] ?: run {
            val errorMessage = "未找到对应的模板文件映射，请检查输入参数"
            return ResponseEntity.status(400).body(ResponseUtil.formatResponse(400, mapOf("message" to errorMessage)))
        }

        val templateFileContent = try {
            downloadFileContent(templateFileUrl)
        } catch (e: Exception) {
            val errorMessage = "无法下载模板文件，请检查文件路径：$templateFileUrl"
            return ResponseEntity.status(500).body(ResponseUtil.formatResponse(500, mapOf("message" to errorMessage)))
        }

        if (testFile.isEmpty) {
            val errorMessage = "上传的测试文件不能为空，请上传有效文件"
            return ResponseEntity.status(400).body(ResponseUtil.formatResponse(400, mapOf("message" to errorMessage)))
        }

        val task = SequenceTask(
            userId = userId,
            username = username,
            templateFileData = templateFileContent,
            testFileData = testFile.bytes
        )

        rabbitTemplate.convertAndSend("sequenceTasksExchange", "sequenceTasks", task)

        return ResponseEntity.ok(
            ResponseUtil.formatResponse(200, "任务已提交，正在排队处理")
        )
    }

    private fun downloadFileContent(fileUrl: String): ByteArray {
        val request = Request.Builder()
            .url(fileUrl)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Failed to download file: ${response.message}")
            }
            return response.body?.bytes() ?: throw RuntimeException("File content is empty")
        }
    }
}