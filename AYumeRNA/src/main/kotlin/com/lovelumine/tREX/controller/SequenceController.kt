package com.lovelumine.tREX.controller

import com.lovelumine.auth.model.User
import com.lovelumine.common.ResponseUtil
import com.lovelumine.tREX.service.SequenceService
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.io.ByteArrayInputStream

data class SequenceRequest(
    val templateContent: String,
    val testContent: String
)

@RestController
@RequestMapping("/sequence")
class SequenceController(
    @Autowired private val sequenceService: SequenceService,
    @Autowired private val minioClient: MinioClient
) {

    @Operation(
        summary = "处理序列内容",
        description = "用户可以直接上传模板序列和测试序列的内容，服务器将计算得分并返回结果文件的路径"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "序列处理成功"),
        ApiResponse(responseCode = "400", description = "请求参数错误"),
        ApiResponse(responseCode = "500", description = "服务器内部错误")
    ])
    @PostMapping("/process", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun processSequence(
        @RequestBody request: SequenceRequest
    ): ResponseEntity<Map<String, Any>> {
        val user = SecurityContextHolder.getContext().authentication.principal as User
        val username = user.username
        val bucketName = "ayumerna"

        return try {
            // 1. 读取模板序列内容
            val templateSequences = sequenceService.parseSequences(request.templateContent)

            // 2. 读取测试序列内容并添加 CCA
            val testSequences = sequenceService.parseSequencesWithCCA(request.testContent)

            // 3. 计算测试序列的得分
            val results = sequenceService.calculateScores(templateSequences, testSequences)

            // 4. 将结果保存为 CSV
            val csvContent = buildString {
                append("Test_Sequence,Score,Sequence\n")
                for (result in results) {
                    append("${result["Test_Sequence"]},${result["Score"]},${result["Sequence"]}\n")
                }
            }

            // 5. 上传结果文件到 MinIO
            val objectName = "$username/results.csv"
            val inputStream = ByteArrayInputStream(csvContent.toByteArray())
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(objectName)
                    .stream(inputStream, csvContent.toByteArray().size.toLong(), -1)
                    .contentType("text/csv")
                    .build()
            )

            ResponseEntity.ok(ResponseUtil.formatResponse(200, mapOf(
                "message" to "序列处理成功，结果已上传",
                "path" to objectName
            )))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(400).body(ResponseUtil.formatResponse(400, e.message ?: "请求参数错误"))
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity.status(500).body(ResponseUtil.formatResponse(500, "序列处理失败：${e.message}"))
        }
    }
}
