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
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.nio.file.Files

@RestController
@RequestMapping("/sequence")
class SequenceController(
    @Autowired private val sequenceService: SequenceService,
    @Autowired private val minioClient: MinioClient,
    @Value("\${minio.url}") private val minioBaseUrl: String // 注入 MinIO 基础 URL
) {

    @Operation(
        summary = "处理序列文件",
        description = "用户可以上传模板序列文件和测试序列文件，服务器将计算得分并返回结果文件的路径"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "序列处理成功"),
        ApiResponse(responseCode = "400", description = "请求参数错误"),
        ApiResponse(responseCode = "500", description = "服务器内部错误")
    ])
    @PostMapping("/process")
    fun processSequence(
        @RequestParam("templateFile") templateFile: MultipartFile,
        @RequestParam("testFile") testFile: MultipartFile
    ): ResponseEntity<Map<String, Any>> {
        val user = SecurityContextHolder.getContext().authentication.principal as User
        val username = user.username
        val bucketName = "ayumerna"

        // 保存上传的文件到临时目录
        val tempDir = Files.createTempDirectory("sequence_upload")
        val templateFilePath = tempDir.resolve(templateFile.originalFilename)
        val testFilePath = tempDir.resolve(testFile.originalFilename)
        templateFile.transferTo(templateFilePath)
        testFile.transferTo(testFilePath)

        return try {
            // 调用服务层处理逻辑
            val resultsCsvPath = sequenceService.processSequences(
                templateFilePath.toString(),
                testFilePath.toString(),
                tempDir.toString()
            )

            // 上传结果文件到 MinIO
            val objectName = "$username/results.csv"
            val resultsBytes = Files.readAllBytes(resultsCsvPath)
            val inputStream = ByteArrayInputStream(resultsBytes)
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(objectName)
                    .stream(inputStream, resultsBytes.size.toLong(), -1)
                    .contentType("text/csv")
                    .build()
            )

            // 构建完整的 URL
            val fileUrl = "$minioBaseUrl/$bucketName/$objectName"

            ResponseEntity.ok(ResponseUtil.formatResponse(200, mapOf(
                "message" to "序列处理成功，结果已上传",
                "path" to fileUrl
            )))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(400).body(ResponseUtil.formatResponse(400, e.message ?: "请求参数错误"))
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity.status(500).body(ResponseUtil.formatResponse(500, "序列处理失败：${e.message}"))
        } finally {
            // 清理临时文件
            Files.deleteIfExists(templateFilePath)
            Files.deleteIfExists(testFilePath)
            Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .forEach(Files::deleteIfExists)
        }
    }
}
