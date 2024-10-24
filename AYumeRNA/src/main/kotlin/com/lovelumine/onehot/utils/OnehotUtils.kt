package com.lovelumine.onehot.utils

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object OnehotUtils {

    private val client = OkHttpClient()

    fun processOnehot(fastaFileUrl: String, cmFileUrl: String): String {
        // 创建 JSON 请求体，传递 MinIO 文件 URL
        val json = JSONObject()
        json.put("traceback", fastaFileUrl)
        json.put("cmfile", cmFileUrl)

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)

        // 创建请求
        val request = Request.Builder()
            .url("http://localhost:2002/process_traceback")
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
            return jsonResponse.getString("output_file")
        }
    }
}
