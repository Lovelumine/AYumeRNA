package com.lovelumine.generate_weight.utils

import com.lovelumine.generate_weight.model.GenerateWeightTask
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object GenerateWeightUtils {

    private val client = OkHttpClient.Builder()
        .connectTimeout(600, TimeUnit.SECONDS)
        .writeTimeout(600, TimeUnit.SECONDS)
        .readTimeout(3000, TimeUnit.SECONDS)
        .build()

    @Throws(IOException::class)
    fun processGenerateWeight(task: GenerateWeightTask): Pair<String, List<String>> {
        val userId = task.userId

        // 创建 JSON 请求体，传递 MinIO 文件 URL 和参数
        val json = JSONObject()
        json.put("file_url", task.h5FileUrl)
        json.put("mode", task.mode)
        json.put("threshold", task.threshold)
        json.put("n_samples", task.getNSamplesAsInt())
        json.put("cpu", task.cpu)
        json.put("print_every", task.printEvery)
        json.put("user_id", userId)

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)

        // 创建请求，URL 指向 Flask API 的 /generate_weight 端点
        val request = Request.Builder()
            .url("http://223.82.75.76:2002/generate_weight")
            .post(body)
            .build()

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

            val outputUrl = jsonResponse.getString("output_url")

            val progressMessagesJsonArray = jsonResponse.optJSONArray("progress_messages") ?: JSONArray()
            val progressMessages = mutableListOf<String>()
            for (i in 0 until progressMessagesJsonArray.length()) {
                progressMessages.add(progressMessagesJsonArray.getString(i))
            }

            return Pair(outputUrl, progressMessages)
        }
    }
}
