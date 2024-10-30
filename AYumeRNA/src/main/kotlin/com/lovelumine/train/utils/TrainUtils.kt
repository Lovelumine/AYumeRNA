package com.lovelumine.train.utils

import com.lovelumine.train.model.TrainTask
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object TrainUtils {

    private val client = OkHttpClient.Builder()
        .connectTimeout(600, TimeUnit.SECONDS)  // 连接超时
        .writeTimeout(600, TimeUnit.SECONDS)    // 写入超时
        .readTimeout(3000, TimeUnit.SECONDS)    // 读取超时
        .build()

    @Throws(IOException::class)
    fun processTrain(task: TrainTask): Pair<String, List<String>> {
        val userId = task.userId

        // 创建 JSON 请求体，传递 MinIO 文件 URL 和其他参数
        val json = JSONObject()
        json.put("user_id", userId)
        json.put("X_train_url", task.XTrainFileUrl)
        json.put("w_train_url", task.WTrainFileUrl)
        json.put("X_valid_url", task.XValidFileUrl)
        json.put("w_valid_url", task.WValidFileUrl)

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)

        // 创建请求
        val request = Request.Builder()
            .url("http://223.82.75.76:2002/train")
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