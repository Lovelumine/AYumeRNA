package com.lovelumine.onehot.utils

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object OnehotUtils {

    // 调整 OkHttpClient 的超时时间设置
    private val client = OkHttpClient.Builder()
        .connectTimeout(600, TimeUnit.SECONDS)  // 连接超时
        .writeTimeout(600, TimeUnit.SECONDS)    // 写入超时
        .readTimeout(3000, TimeUnit.SECONDS)    // 读取超时，设置为50分钟
        .build()

    @Throws(IOException::class)
    fun processOnehot(fastaFileUrl: String, cmFileUrl: String, userId: Long): Pair<String, List<String>> {
        // 创建 JSON 请求体，传递 MinIO 文件 URL 和用户 ID
        val json = JSONObject()
        json.put("traceback", fastaFileUrl)
        json.put("cmfile", cmFileUrl)
        json.put("user_id", userId)

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)

        // 创建请求
        val request = Request.Builder()
            .url("http://223.82.75.76:2002/process_traceback")
            .post(body)
            .build()

        // 执行请求并处理响应
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected code $response")
            }

            val responseBody = response.body?.string() ?: throw IOException("Empty response body")
            val jsonResponse = JSONObject(responseBody)

            // 处理错误
            if (jsonResponse.has("error")) {
                throw RuntimeException("Error from Flask API: ${jsonResponse.getString("error")}")
            }

            val outputFileUrl = jsonResponse.getString("output_file")
            val progressMessagesJsonArray = jsonResponse.getJSONArray("progress_messages")
            val progressMessages = mutableListOf<String>()
            for (i in 0 until progressMessagesJsonArray.length()) {
                progressMessages.add(progressMessagesJsonArray.getString(i))
            }

            return Pair(outputFileUrl, progressMessages)
        }
    }
}
