package com.example.autosrt

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File

class SrtPreviewActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "SrtPreviewActivity"
        const val EXTRA_SRT_FILE_PATH = "srt_file_path"
    }

    private lateinit var tvFileName: TextView
    private lateinit var tvFileSize: TextView
    private lateinit var tvSubtitleCount: TextView
    private lateinit var tvSrtContent: TextView
    private lateinit var btnBack: Button
    private lateinit var btnShare: Button
    private lateinit var btnCopyContent: Button
    private lateinit var btnOpenFile: Button

    private var srtFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_srt_preview)

        // 初始化视图
        tvFileName = findViewById(R.id.tvFileName)
        tvFileSize = findViewById(R.id.tvFileSize)
        tvSubtitleCount = findViewById(R.id.tvSubtitleCount)
        tvSrtContent = findViewById(R.id.tvSrtContent)
        btnBack = findViewById(R.id.btnBack)
        btnShare = findViewById(R.id.btnShare)
        btnCopyContent = findViewById(R.id.btnCopyContent)
        btnOpenFile = findViewById(R.id.btnOpenFile)

        // 获取传递的文件路径
        val filePath = intent.getStringExtra(EXTRA_SRT_FILE_PATH)
        if (filePath != null) {
            srtFile = File(filePath)
            loadSrtFile(srtFile!!)
        } else {
            Toast.makeText(this, "无法获取SRT文件路径", Toast.LENGTH_SHORT).show()
            finish()
        }

        // 设置按钮点击事件
        btnBack.setOnClickListener {
            finish()
        }

        btnShare.setOnClickListener {
            shareSrtFile()
        }

        btnCopyContent.setOnClickListener {
            copySrtContent()
        }

        btnOpenFile.setOnClickListener {
            openFileLocation()
        }
    }

    private fun loadSrtFile(file: File) {
        try {
            if (!file.exists()) {
                tvSrtContent.text = "文件不存在: ${file.absolutePath}"
                return
            }

            // 读取文件内容
            val content = file.readText(Charsets.UTF_8)
            
            // 显示文件信息
            tvFileName.text = "文件名: ${file.name}"
            tvFileSize.text = "文件大小: ${file.length()} 字节"
            
            // 统计字幕条数
            val subtitleCount = countSubtitles(content)
            tvSubtitleCount.text = "字幕条数: $subtitleCount"
            
            // 显示内容
            tvSrtContent.text = content

            Log.d(TAG, "SRT文件加载成功: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "读取SRT文件失败", e)
            tvSrtContent.text = "读取文件失败: ${e.message}"
            Toast.makeText(this, "读取文件失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun countSubtitles(content: String): Int {
        // 统计字幕序号来计算条数
        return content.split("\n")
            .filter { it.trim().matches(Regex("^\\d+$")) }
            .size
    }

    private fun copySrtContent() {
        try {
            val content = tvSrtContent.text.toString()
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("SRT内容", content)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "内容已复制到剪贴板", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "复制内容失败", e)
            Toast.makeText(this, "复制失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareSrtFile() {
        try {
            if (srtFile == null || !srtFile!!.exists()) {
                Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show()
                return
            }

            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                srtFile!!
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "分享SRT字幕文件")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, "分享SRT文件"))
        } catch (e: Exception) {
            Log.e(TAG, "分享文件失败", e)
            Toast.makeText(this, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openFileLocation() {
        try {
            if (srtFile == null || !srtFile!!.exists()) {
                Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show()
                return
            }

            // 尝试打开文件所在目录
            val intent = Intent(Intent.ACTION_VIEW).apply {
                val uri = FileProvider.getUriForFile(
                    this@SrtPreviewActivity,
                    "${applicationContext.packageName}.fileprovider",
                    srtFile!!.parentFile!!
                )
                setDataAndType(uri, "resource/folder")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            try {
                startActivity(intent)
            } catch (e: Exception) {
                // 如果无法打开文件夹，显示文件路径
                Toast.makeText(
                    this,
                    "文件位置: ${srtFile!!.absolutePath}",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "打开文件位置失败", e)
            Toast.makeText(this, "无法打开文件位置", Toast.LENGTH_SHORT).show()
        }
    }
}
