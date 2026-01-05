package com.example.autosrt

import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

class VolcEngineAPI {
    companion object {
        private const val TAG = "VolcEngineAPI"
        private const val SUBMIT_URL = "https://openspeech-direct.zijieapi.com/api/v3/auc/bigmodel/submit"
        private const val QUERY_URL = "https://openspeech-direct.zijieapi.com/api/v3/auc/bigmodel/query"
        // 豆包录音文件识别模型2.0
        private const val RESOURCE_ID = "volc.seedasr.auc"
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    fun submitAudioForTranscription(
        audioFile: File,
        apiKey: String,
        accessKey: String
    ): String? {
        return try {
            // 将音频文件转换为Base64
            val audioBase64 = fileToBase64(audioFile)

            // 创建请求体
            val requestBody = createSubmitRequestBody(audioBase64)
            val taskId = UUID.randomUUID().toString()

            val request = Request.Builder()
                .url(SUBMIT_URL)
                .header("X-Api-App-Key", apiKey)
                .header("X-Api-Access-Key", accessKey)
                .header("X-Api-Resource-Id", RESOURCE_ID)
                .header("X-Api-Request-Id", taskId)
                .header("X-Api-Sequence", "-1")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            val responseCode = response.header("X-Api-Status-Code")

            Log.d(TAG, "Submit response: $responseBody, Code: $responseCode")

            if ("20000000" == responseCode && responseBody != null) {
                // 提取任务ID
                val xTtLogid = response.header("X-Tt-Logid", "")
                "$taskId|$xTtLogid" // 返回任务ID和日志ID，用|分隔
            } else {
                Log.e(TAG, "提交任务失败: $responseBody")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "提交任务时发生错误", e)
            null
        }
    }

    fun queryTaskStatus(taskInfo: String, apiKey: String, accessKey: String): APIResponse? {
        val parts = taskInfo.split("|")
        if (parts.size < 2) {
            Log.e(TAG, "任务信息格式错误: $taskInfo")
            return null
        }

        val taskId = parts[0]
        val xTtLogid = parts[1]

        return try {
            val requestBody = "{}".toRequestBody(JSON)

            val request = Request.Builder()
                .url(QUERY_URL)
                .header("X-Api-App-Key", apiKey)
                .header("X-Api-Access-Key", accessKey)
                .header("X-Api-Resource-Id", RESOURCE_ID)
                .header("X-Api-Request-Id", taskId)
                .header("X-Tt-Logid", xTtLogid)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            val responseCode = response.header("X-Api-Status-Code")

            Log.d(TAG, "Query response: $responseBody, Code: $responseCode")

            if (responseCode != null) {
                APIResponse(responseCode, responseBody)
            } else {
                Log.e(TAG, "查询任务失败: $responseBody")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "查询任务时发生错误", e)
            null
        }
    }

    private fun fileToBase64(audioFile: File): String {
        return Base64.encodeToString(audioFile.readBytes(), Base64.NO_WRAP)
    }

    private fun createSubmitRequestBody(audioBase64: String): RequestBody {
        val gson = Gson()

        // 创建 user 部分
        val user = mapOf("uid" to "fake_uid")

        // 创建 audio 部分，使用base64编码的音频数据
        val audio = mapOf("data" to audioBase64)

        // 创建 corpus 部分
        val corpus = mapOf(
            "correct_table_name" to "",
            "context" to ""
        )

        // 创建 request 部分
        val innerRequest = mapOf(
            "model_name" to "bigmodel",
            "enable_channel_split" to true,
            "enable_ddc" to true,
            "enable_speaker_info" to true,
            "enable_punc" to true,
            "enable_itn" to true,
            "corpus" to corpus
        )

        // 创建主 request 对象
        val mainRequest = mapOf(
            "user" to user,
            "audio" to audio,
            "request" to innerRequest
        )

        val jsonString = gson.toJson(mainRequest)
        Log.d(TAG, "Submit request: $jsonString")
        return jsonString.toRequestBody(JSON)
    }

    data class APIResponse(
        val statusCode: String,
        val body: String?
    )
}