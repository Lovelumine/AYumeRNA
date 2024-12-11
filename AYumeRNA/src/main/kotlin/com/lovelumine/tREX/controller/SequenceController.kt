package com.lovelumine.tREX.controller

import com.lovelumine.common.ResponseUtil
import com.lovelumine.tREX.model.SequenceTask
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/sequence")
class SequenceController(
    @Autowired private val rabbitTemplate: RabbitTemplate
) {

    // 文件路径映射表
    private val filePaths = mapOf(
        "TAA" to "https://minio.lumoxuan.cn/ayumerna/model/Thr序列.csv",
        "TAG" to "https://minio.lumoxuan.cn/ayumerna/model/Thr序列.csv",
        "TGA" to "https://minio.lumoxuan.cn/ayumerna/model/Thr序列.csv",
    )

    private val httpClient = OkHttpClient() // 创建 HTTP 客户端

    @PostMapping("/process")
    fun processSequence(
        @RequestParam("aminoAcid") aminoAcid: String,
        @RequestParam("domain") domain: String,
        @RequestParam("reverseCodon") reverseCodon: String,
        @RequestParam("testFile") testFile: MultipartFile
    ): ResponseEntity<Map<String, Any>> {
        // 模拟用户信息
        val userId: Long = 1
        val username: String = "lovelumine"

        // 构造映射键
        val key = reverseCodon
        val templateFileUrl = filePaths[key] ?: run {
            // 如果未找到映射，返回 400 错误
            val errorMessage = "未找到对应的模板文件映射，请检查输入参数"
            return ResponseEntity.status(400).body(ResponseUtil.formatResponse(400, mapOf("message" to errorMessage)))
        }

        // 下载模板文件内容
        val templateFileContent = try {
            downloadFileContent(templateFileUrl)
        } catch (e: Exception) {
            val errorMessage = "无法下载模板文件，请检查文件路径：$templateFileUrl"
            return ResponseEntity.status(500).body(ResponseUtil.formatResponse(500, mapOf("message" to errorMessage)))
        }

        // 校验上传文件是否为空
        if (testFile.isEmpty) {
            val errorMessage = "上传的测试文件不能为空，请上传有效文件"
            return ResponseEntity.status(400).body(ResponseUtil.formatResponse(400, mapOf("message" to errorMessage)))
        }

        // 创建任务对象
        val task = SequenceTask(
            userId = userId,
            username = username,
            templateFileData = templateFileContent,
            testFileData = testFile.bytes
        )

        // 将任务放入队列
        rabbitTemplate.convertAndSend("sequenceTasks", task)

        return ResponseEntity.ok(
            ResponseUtil.formatResponse(200, "任务已提交，正在排队处理")
        )
    }

    // 使用 OkHttp 从远程 URL 下载文件内容
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
