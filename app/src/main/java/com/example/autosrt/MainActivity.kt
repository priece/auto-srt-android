package com.example.autosrt

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_VIDEO_PICK = 1001
    }

    private lateinit var btnSelectVideo: Button
    private lateinit var tvSelectedVideo: TextView
    private lateinit var btnConvert: Button
    private lateinit var btnSettings: Button
    private lateinit var btnClearLog: Button
    private lateinit var btnPreviewSrt: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var tvResult: TextView
    private lateinit var tvLog: TextView
    private lateinit var scrollView: ScrollView

    private var selectedVideoUri: Uri? = null
    private var generatedSrtFile: File? = null
    private val logBuilder = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        btnSelectVideo = findViewById(R.id.btnSelectVideo)
        tvSelectedVideo = findViewById(R.id.tvSelectedVideo)
        btnConvert = findViewById(R.id.btnConvert)
        btnSettings = findViewById(R.id.btnSettings)
        btnClearLog = findViewById(R.id.btnClearLog)
        btnPreviewSrt = findViewById(R.id.btnPreviewSrt)
        progressBar = findViewById(R.id.progressBar)
        tvStatus = findViewById(R.id.tvStatus)
        tvResult = findViewById(R.id.tvResult)
        tvLog = findViewById(R.id.tvLog)
        val logParent = tvLog.parent
        scrollView = logParent as ScrollView
        
        // 初始时隐藏预览按钮
        btnPreviewSrt.visibility = Button.GONE
    }

    private fun setupClickListeners() {
        btnSelectVideo.setOnClickListener {
            selectVideoFile()
        }

        btnConvert.setOnClickListener {
            convertVideoToSRT()
        }

        btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        btnClearLog.setOnClickListener {
            clearLog()
        }
        
        btnPreviewSrt.setOnClickListener {
            previewSrtFile()
        }
    }

    private fun addLog(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val logMessage = "[$timestamp] $message\n"
        logBuilder.append(logMessage)
        
        runOnUiThread {
            tvLog.text = logBuilder.toString()
            // 自动滚动到底部
            scrollView.post {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
        
        // 同时输出到Logcat
        Log.d(TAG, message)
    }

    private fun clearLog() {
        logBuilder.clear()
        tvLog.text = "等待操作..."
        Toast.makeText(this, "日志已清除", Toast.LENGTH_SHORT).show()
    }
    
    private fun previewSrtFile() {
        if (generatedSrtFile == null || !generatedSrtFile!!.exists()) {
            Toast.makeText(this, "SRT文件不存在", Toast.LENGTH_SHORT).show()
            return
        }
        
        val intent = Intent(this, SrtPreviewActivity::class.java)
        intent.putExtra(SrtPreviewActivity.EXTRA_SRT_FILE_PATH, generatedSrtFile!!.absolutePath)
        startActivity(intent)
    }

    private fun selectVideoFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "video/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, REQUEST_VIDEO_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_VIDEO_PICK && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                selectedVideoUri = uri
                val fileName = getFileName(uri)
                tvSelectedVideo.text = "已选择: $fileName"
                btnConvert.isEnabled = true
                tvResult.text = ""
                addLog("已选择视频文件: $fileName")
            }
        }
    }

    private fun copyUriToFile(uri: Uri): File? {
        try {
            addLog("开始复制视频文件...")
            val fileName = getFileName(uri)
            val extension = if (fileName.contains('.')) {
                fileName.substring(fileName.lastIndexOf('.'))
            } else {
                ".mp4"
            }
            val file = File(getExternalFilesDir(null), System.currentTimeMillis().toString() + extension)
            
            contentResolver.openInputStream(uri)?.use { inputStream ->
                file.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            addLog("视频文件复制成功: ${file.absolutePath}")
            addLog("文件大小: ${file.length() / 1024 / 1024} MB")
            return file
        } catch (e: Exception) {
            val errorMsg = "复制文件失败: ${e.message}"
            addLog("[ERROR] $errorMsg")
            Log.e(TAG, errorMsg, e)
            return null
        }
    }

    private fun getFileName(uri: Uri): String {
        var fileName = "未知文件"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0) {
                cursor.moveToFirst()
                fileName = cursor.getString(nameIndex)
            }
        }
        return fileName
    }

    private fun convertVideoToSRT() {
        if (selectedVideoUri == null) {
            Toast.makeText(this, "请先选择视频文件", Toast.LENGTH_SHORT).show()
            addLog("[ERROR] 未选择视频文件")
            return
        }

        // 检查API密钥是否已配置
        val apiKey = getSharedPreferences("api_config", MODE_PRIVATE).getString("api_key", "")
        val accessKey = getSharedPreferences("api_config", MODE_PRIVATE).getString("access_key", "")

        if (apiKey.isNullOrEmpty() || accessKey.isNullOrEmpty()) {
            Toast.makeText(this, "请先在API配置中设置API密钥和Access密钥", Toast.LENGTH_LONG).show()
            addLog("[ERROR] API密钥未配置")
            return
        }

        addLog("开始转换进程...")
        addLog("API Key: ${apiKey.take(10)}...")
        // 开始转换过程
        startConversionProcess()
    }

    private fun startConversionProcess() {
        // 显示进度条
        progressBar.visibility = ProgressBar.VISIBLE
        tvStatus.text = "正在处理视频..."
        btnConvert.isEnabled = false

        // 使用协程或其他异步方式处理转换
        Thread {
            try {
                // 1. 提取音频
                runOnUiThread { tvStatus.text = "正在提取音频..." }
                addLog("\n=== 步骤1: 提取音频 ===")
                val audioFile = extractAudioFromVideo()

                if (audioFile != null) {
                    addLog("音频提取成功")
                    addLog("音频文件: ${audioFile.absolutePath}")
                    addLog("音频大小: ${audioFile.length() / 1024} KB")
                    
                    // 2. 调用火山引擎API
                    runOnUiThread { tvStatus.text = "正在调用API识别..." }
                    addLog("\n=== 步骤2: 调用火山引擎API ===")
                    val result = callVolcEngineAPI(audioFile)

                    if (result != null) {
                        addLog("音频识别完成")
                        addLog("返回数据长度: ${result.length} 字符")
                        
                        // 3. 生成SRT文件
                        runOnUiThread { tvStatus.text = "正在生成字幕文件..." }
                        addLog("\n=== 步骤3: 生成SRT字幕文件 ===")
                        val srtFile = generateSRTFile(result, audioFile)

                        if (srtFile != null) {
                            addLog("字幕文件生成成功!")
                            addLog("文件路径: ${srtFile.absolutePath}")
                            generatedSrtFile = srtFile
                            runOnUiThread {
                                tvStatus.text = "转换完成!"
                                tvResult.text = "字幕文件已保存: ${srtFile.absolutePath}"
                                progressBar.visibility = ProgressBar.GONE
                                btnConvert.isEnabled = true
                                btnPreviewSrt.visibility = Button.VISIBLE
                            }
                        } else {
                            addLog("[ERROR] 生成SRT文件失败")
                            runOnUiThread {
                                tvStatus.text = "生成SRT文件失败"
                                progressBar.visibility = ProgressBar.GONE
                                btnConvert.isEnabled = true
                            }
                        }
                    } else {
                        addLog("[ERROR] API调用失败")
                        runOnUiThread {
                            tvStatus.text = "API调用失败"
                            progressBar.visibility = ProgressBar.GONE
                            btnConvert.isEnabled = true
                        }
                    }
                } else {
                    addLog("[ERROR] 提取音频失败")
                    runOnUiThread {
                        tvStatus.text = "提取音频失败"
                        progressBar.visibility = ProgressBar.GONE
                        btnConvert.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "转换过程中发生错误: ${e.message}"
                addLog("[ERROR] $errorMsg")
                addLog("[ERROR] 堆栈跟踪: ${e.stackTraceToString()}")
                Log.e(TAG, errorMsg, e)
                runOnUiThread {
                    tvStatus.text = "转换失败: ${e.message}"
                    progressBar.visibility = ProgressBar.GONE
                    btnConvert.isEnabled = true
                }
            }
        }.start()
    }

    private fun extractAudioFromVideo(): File? {
        // 将URI转换为文件
        val videoFile = selectedVideoUri?.let { copyUriToFile(it) }
        if (videoFile == null) {
            addLog("[ERROR] 无法复制视频文件")
            Log.e(TAG, "无法复制视频文件")
            return null
        }

        // 创建输出音频文件
        val audioFile = File(getExternalFilesDir(null), 
            videoFile.nameWithoutExtension + ".mp3")
        addLog("开始提取音频轨道...")

        // 使用AudioExtractor提取音频
        val extractor = AudioExtractor()
        val success = extractor.extractAudioFromVideo(videoFile, audioFile)

        // 清理临时视频文件
        if (videoFile.absolutePath != selectedVideoUri?.path) {
            videoFile.delete()
            addLog("已清理临时视频文件")
        }

        if (!success) {
            addLog("[ERROR] AudioExtractor返回失败")
        }

        return if (success) audioFile else null
    }

    private fun callVolcEngineAPI(audioFile: File): String? {
        // 从SharedPreferences获取API密钥
        val apiKey = getSharedPreferences("api_config", MODE_PRIVATE).getString("api_key", "") ?: ""
        val accessKey = getSharedPreferences("api_config", MODE_PRIVATE).getString("access_key", "") ?: ""

        if (apiKey.isEmpty() || accessKey.isEmpty()) {
            addLog("[ERROR] API密钥未配置")
            Log.e(TAG, "API密钥未配置")
            return null
        }

        val api = VolcEngineAPI()
        
        // 提交音频文件进行转录
        addLog("正在提交音频转录任务...")
        val taskInfo = api.submitAudioForTranscription(audioFile, apiKey, accessKey)
        if (taskInfo == null) {
            addLog("[ERROR] 提交音频转录任务失败")
            Log.e(TAG, "提交音频转录任务失败")
            return null
        }
        addLog("任务提交成功, 任务ID: ${taskInfo.split("|")[0]}")

        // 轮询查询任务状态，直到完成
        var attempt = 0
        val maxAttempts = 300 // 最多等待5分钟 (300秒)
        addLog("开始轮询查询任务状态...")
        while (attempt < maxAttempts) {
            Thread.sleep(2000) // 等待2秒再查询
            attempt++

            val response = api.queryTaskStatus(taskInfo, apiKey, accessKey)
            if (response != null) {
                when (response.statusCode) {
                    "20000000" -> {
                        // 任务完成
                        addLog("音频转录完成!")
                        Log.d(TAG, "音频转录完成")
                        return response.body
                    }
                    "20000001", "20000002" -> {
                        // 任务仍在处理中，继续轮询
                        if (attempt % 5 == 0) { // 每10秒输出一次
                            addLog("音频转录进行中... 已等待${attempt * 2}秒")
                        }
                        Log.d(TAG, "音频转录进行中... (${attempt * 2}s)")
                        continue
                    }
                    else -> {
                        // 任务失败
                        addLog("[ERROR] 音频转录失败")
                        addLog("[ERROR] 状态码: ${response.statusCode}")
                        addLog("[ERROR] 响应: ${response.body}")
                        Log.e(TAG, "音频转录失败: ${response.body}")
                        return null
                    }
                }
            } else {
                addLog("[ERROR] 查询响应为空")
            }
        }

        addLog("[ERROR] 音频转录超时 (超过${maxAttempts * 2}秒)")
        Log.e(TAG, "音频转录超时")
        return null
    }

    private fun generateSRTFile(apiResult: String, sourceFile: File): File? {
        try {
            addLog("开始解析API响应...")
            addLog("响应数据预览: ${apiResult.take(200)}...")
            
            // 尝试解析JSON结构
            val jsonObj = org.json.JSONObject(apiResult)
            addLog("JSON解析成功")
            
            // 输出JSON的主要键
            val keys = jsonObj.keys()
            val keyList = mutableListOf<String>()
            while (keys.hasNext()) {
                keyList.add(keys.next())
            }
            addLog("JSON主要键: ${keyList.joinToString(", ")}")
            
            // 检查status字段
            val status = jsonObj.optString("status", "")
            addLog("检查status字段: '${status}'")
            
            // 检查result字段
            if (jsonObj.has("result")) {
                val result = jsonObj.getJSONObject("result")
                val resultKeys = result.keys().asSequence().toList()
                addLog("result字段包含的键: ${resultKeys.joinToString(", ")}")
                
                if (result.has("text")) {
                    val textPreview = result.getString("text").take(50)
                    addLog("result.text预览: $textPreview...")
                }
            }
            
            // 检查audio_info字段
            if (jsonObj.has("audio_info")) {
                val audioInfo = jsonObj.getJSONObject("audio_info")
                val duration = audioInfo.optLong("duration", 0)
                addLog("audio_info.duration: $duration 毫秒")
            }
            
            // 获取公共目录用于保存SRT，方便剪映读取
            val publicSrtDir = getPublicSrtDirectory()
            addLog("使用公共目录: ${publicSrtDir.absolutePath}")
            
            addLog("调用SRTGenerator.generateSRT()...")
            val generator = SRTGenerator()
            val result = generator.generateSRT(apiResult, sourceFile, publicSrtDir)
            
            if (result != null) {
                addLog("字幕文件生成成功!")
                addLog("字幕文件大小: ${result.length()} 字节")
            } else {
                addLog("[ERROR] SRTGenerator.generateSRT()返回null")
                addLog("[ERROR] 请查看Logcat中的SRTGenerator错误日志")
            }
            return result
        } catch (e: org.json.JSONException) {
            addLog("[ERROR] JSON解析异常: ${e.message}")
            addLog("[ERROR] 响应数据: $apiResult")
            Log.e(TAG, "JSON解析失败", e)
            return null
        } catch (e: Exception) {
            addLog("[ERROR] 生成SRT文件异常: ${e.message}")
            addLog("[ERROR] 堆栈跟踪: ${e.stackTraceToString()}")
            Log.e(TAG, "SRT生成失败", e)
            return null
        }
    }
    
    /**
     * 获取公共目录用于保存SRT文件
     * 使用Documents目录，方便剪映等视频编辑软件读取
     */
    private fun getPublicSrtDirectory(): File {
        val publicDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+，使用Documents公共目录
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "AutoSRT")
        } else {
            // Android 9及以下
            File(Environment.getExternalStorageDirectory(), "Documents/AutoSRT")
        }
        
        if (!publicDir.exists()) {
            publicDir.mkdirs()
            Log.d(TAG, "创建公共目录: ${publicDir.absolutePath}")
        }
        
        return publicDir
    }
}