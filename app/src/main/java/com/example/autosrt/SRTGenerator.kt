package com.example.autosrt

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SRTGenerator {
    companion object {
        private const val TAG = "SRTGenerator"
    }

    fun generateSRT(apiResult: String, sourceAudioFile: File, outputDir: File? = null): File? {
        try {
            val resultJson = JSONObject(apiResult)
            val status = resultJson.optString("status", "")
            
            Log.d(TAG, "API返回的status字段: '$status'")
            Log.d(TAG, "JSON包含的所有键: ${resultJson.keys().asSequence().toList()}")
            
            // 检查是否有result字段，如果有则认为是有效响应
            val hasResult = resultJson.has("result")
            val hasAudioInfo = resultJson.has("audio_info")
            
            if (status.isNotEmpty() && status != "20000000") {
                Log.e(TAG, "API返回状态错误: $status")
                return null
            }
            
            // 如果没有status字段，但有result和audio_info，认为是成功的响应
            if (status.isEmpty() && !(hasResult || hasAudioInfo)) {
                Log.e(TAG, "响应格式不正确，既没有status也没有result/audio_info")
                return null
            }

            // 使用指定的输出目录，或者默认使用音频文件所在目录
            val targetDir = outputDir ?: sourceAudioFile.parentFile
            if (!targetDir!!.exists()) {
                targetDir.mkdirs()
                Log.d(TAG, "创建输出目录: ${targetDir.absolutePath}")
            }
            
            val srtFileName = sourceAudioFile.nameWithoutExtension + ".srt"
            val srtFile = File(targetDir, srtFileName)

            Log.d(TAG, "开始解析API结果...")
            // 解析API结果并生成SRT内容
            val srtContent = parseResultToSRT(resultJson)
            
            if (srtContent.isEmpty()) {
                Log.e(TAG, "解析后的SRT内容为空")
                return null
            }
            
            Log.d(TAG, "SRT内容长度: ${srtContent.length} 字符")
            Log.d(TAG, "将写入文件: ${srtFile.absolutePath}")
            
            // 写入SRT文件
            try {
                // 确保文件可以被覆盖
                if (srtFile.exists()) {
                    Log.d(TAG, "SRT文件已存在，将覆盖: ${srtFile.absolutePath}")
                    // 尝试删除旧文件
                    if (!srtFile.delete()) {
                        Log.e(TAG, "无法删除旧的SRT文件")
                        // 尝试直接写入，有些系统允许覆盖
                    }
                }
                
                srtFile.writeText(srtContent, Charsets.UTF_8)
                
                Log.i(TAG, "SRT文件生成成功: ${srtFile.absolutePath}")
                return srtFile
            } catch (e: Exception) {
                Log.e(TAG, "写入SRT文件失败: ${e.message}", e)
                
                // 如果是权限错误，尝试使用应用程序的私有目录
                if (e.message?.contains("EACCES") == true) {
                    Log.d(TAG, "检测到权限错误，尝试使用应用程序私有目录")
                    
                    // 使用应用程序的缓存目录作为备选
                    val privateDir = File(sourceAudioFile.parentFile, "srt")
                    if (!privateDir.exists()) {
                        privateDir.mkdirs()
                        Log.d(TAG, "创建私有SRT目录: ${privateDir.absolutePath}")
                    }
                    
                    val privateSrtFile = File(privateDir, srtFileName)
                    Log.d(TAG, "尝试在私有目录写入SRT文件: ${privateSrtFile.absolutePath}")
                    
                    try {
                        privateSrtFile.writeText(srtContent, Charsets.UTF_8)
                        Log.i(TAG, "SRT文件在私有目录生成成功: ${privateSrtFile.absolutePath}")
                        return privateSrtFile
                    } catch (privateE: Exception) {
                        Log.e(TAG, "在私有目录写入SRT文件也失败了: ${privateE.message}", privateE)
                        return null
                    }
                }
                
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "生成SRT文件时发生错误", e)
            return null
        }
    }

    private fun parseResultToSRT(resultJson: JSONObject): String {
        val sb = StringBuilder()
        var index = 1

        try {
            // 根据火山引擎API响应格式解析结果
            val response = resultJson.optJSONObject("response")
            if (response != null) {
                val sentenceList = response.optJSONArray("sentence_list")
                if (sentenceList != null) {
                    for (i in 0 until sentenceList.length()) {
                        val sentence = sentenceList.getJSONObject(i)
                        val text = sentence.optString("text", "")
                        val startTime = sentence.optLong("st", 0) // 开始时间，毫秒
                        val endTime = sentence.optLong("et", 0)   // 结束时间，毫秒
                        
                        if (text.isNotEmpty()) {
                            sb.append("$index\n")
                            sb.append("${formatTime(startTime)} --> ${formatTime(endTime)}\n")
                            sb.append("$text\n\n")
                            index++
                        }
                    }
                }
            }
            
            // 如果没有找到sentence_list，尝试其他可能的字段
            if (sb.isEmpty()) {
                // 尝试解析其他可能的字段
                val result = resultJson.optJSONObject("result")
                if (result != null) {
                    // 尝试解析带时间戳的sentences数组
                    val sentences = result.optJSONArray("sentences")
                    if (sentences != null && sentences.length() > 0) {
                        for (i in 0 until sentences.length()) {
                            val sentence = sentences.getJSONObject(i)
                            val text = sentence.optString("text", "")
                            val startTime = (sentence.optDouble("start_time", 0.0) * 1000).toLong() // 转换为毫秒
                            val endTime = (sentence.optDouble("end_time", 0.0) * 1000).toLong()   // 转换为毫秒
                            
                            if (text.isNotEmpty()) {
                                sb.append("$index\n")
                                sb.append("${formatTime(startTime)} --> ${formatTime(endTime)}\n")
                                sb.append("$text\n\n")
                                index++
                            }
                        }
                    } else {
                        // 如果只有text字段（没有时间戳），尝试处理
                        val text = result.optString("text", "")
                        if (text.isNotEmpty()) {
                            Log.i(TAG, "API返回的是纯文本，没有时间戳信息，将按句子分割")
                            // 获取音频时长用于估算时间
                            val audioInfo = resultJson.optJSONObject("audio_info")
                            val duration = audioInfo?.optLong("duration", 0) ?: 0
                            
                            // 按句号、问号、感叹号分割文本
                            val sentences = text.split("[。！？.!?]".toRegex())
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                            
                            if (sentences.isNotEmpty() && duration > 0) {
                                // 平均分配时间
                                val timePerSentence = duration / sentences.size
                                for ((i, sentence) in sentences.withIndex()) {
                                    val startTime = i * timePerSentence
                                    val endTime = (i + 1) * timePerSentence
                                    sb.append("${index}\n")
                                    sb.append("${formatTime(startTime)} --> ${formatTime(endTime)}\n")
                                    sb.append("$sentence\n\n")
                                    index++
                                }
                            } else {
                                // 兜底方案：创建单个字幕条目
                                sb.append("1\n")
                                sb.append("00:00:00,000 --> ${formatTime(duration)}\n")
                                sb.append("$text\n\n")
                            }
                        }
                    }
                }
            }
            
            // 如果仍然没有内容，尝试解析words数组
            if (sb.isEmpty()) {
                val words = resultJson.optJSONArray("words")
                if (words != null) {
                    // 按时间顺序分组单词以形成句子
                    val sentenceMap = mutableMapOf<Long, Pair<Long, StringBuilder>>()
                    
                    for (i in 0 until words.length()) {
                        val word = words.getJSONObject(i)
                        val text = word.optString("text", "")
                        val startTime = word.optLong("st", 0)
                        val endTime = word.optLong("et", 0)
                        
                        // 简单地按每秒分组单词形成句子
                        val second = startTime / 1000
                        val current = sentenceMap[second]
                        
                        if (current != null) {
                            sentenceMap[second] = Pair(current.first.coerceAtLeast(endTime), 
                                StringBuilder("${current.second} $text"))
                        } else {
                            sentenceMap[second] = Pair(endTime, StringBuilder(text))
                        }
                    }
                    
                    // 按时间顺序输出句子
                    val sortedEntries = sentenceMap.entries.sortedBy { it.key }
                    for (entry in sortedEntries) {
                        val text = entry.value.second.toString().trim()
                        val endTime = entry.value.first
                        
                        if (text.isNotEmpty()) {
                            sb.append("$index\n")
                            val startTime = entry.key * 1000
                            sb.append("${formatTime(startTime)} --> ${formatTime(endTime)}\n")
                            sb.append("$text\n\n")
                            index++
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析API结果时发生错误", e)
        }

        return if (sb.isNotEmpty()) sb.toString() else {
            // 如果解析失败，创建一个简单的占位符SRT
            "1\n00:00:00,000 --> 00:00:05,000\n无法解析字幕数据\n\n"
        }
    }

    private fun formatTime(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val hours = (totalSeconds / 3600).toInt()
        val minutes = ((totalSeconds % 3600) / 60).toInt()
        val seconds = (totalSeconds % 60).toInt()
        val ms = (milliseconds % 1000).toInt()
        
        return String.format(Locale.US, "%02d:%02d:%02d,%03d", hours, minutes, seconds, ms)
    }
}