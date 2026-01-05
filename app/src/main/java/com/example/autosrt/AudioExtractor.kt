package com.example.autosrt

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

class AudioExtractor {
    companion object {
        private const val TAG = "AudioExtractor"
    }

    fun extractAudioFromVideo(videoFile: File, outputAudioFile: File): Boolean {
        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null

        try {
            extractor = MediaExtractor()
            extractor.setDataSource(videoFile.absolutePath)
            return extractAudioInternal(extractor, outputAudioFile)
        } catch (e: Exception) {
            Log.e(TAG, "提取音频时发生错误", e)
            return false
        }
    }

    // 新增方法：直接从FileDescriptor读取，避免复制文件
    fun extractAudioFromVideo(context: android.content.Context, videoUri: android.net.Uri, outputAudioFile: File): Boolean {
        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        var inputStream: java.io.FileInputStream? = null

        try {
            extractor = MediaExtractor()
            val fd = context.contentResolver.openFileDescriptor(videoUri, "r")
            fd?.let {
                extractor.setDataSource(it.fileDescriptor)
                return extractAudioInternal(extractor, outputAudioFile)
            } ?: run {
                Log.e(TAG, "无法打开文件描述符")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "提取音频时发生错误", e)
            return false
        } finally {
            try {
                inputStream?.close()
                extractor?.release()
                muxer?.stop()
                muxer?.release()
            } catch (e: Exception) {
                Log.e(TAG, "释放资源时发生错误", e)
            }
        }
    }

    // 内部方法：实际的音频提取逻辑
    private fun extractAudioInternal(extractor: MediaExtractor, outputAudioFile: File): Boolean {
        var muxer: MediaMuxer? = null

        try {
            val audioTrackIndex = selectTrackByMimeType(extractor, "audio/")
            if (audioTrackIndex < 0) {
                Log.e(TAG, "视频中未找到音频轨道")
                return false
            }

            extractor.selectTrack(audioTrackIndex)
            val format = extractor.getTrackFormat(audioTrackIndex)

            muxer = MediaMuxer(outputAudioFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val destAudioTrackIndex = muxer.addTrack(format)
            muxer.start()

            val buffer = ByteBuffer.allocate(1024 * 1024) // 1MB buffer
            val bufferInfo = android.media.MediaCodec.BufferInfo()
            extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            var sampleSize: Int
            var presentationTimeUs: Long
            var copiedSampleCount = 0

            while (true) {
                sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) {
                    break
                }

                presentationTimeUs = extractor.sampleTime
                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = presentationTimeUs
                // 转换MediaExtractor的sampleFlags到MediaCodec的buffer flags
                bufferInfo.flags = if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
                    MediaCodec.BUFFER_FLAG_SYNC_FRAME
                } else {
                    0
                }

                muxer.writeSampleData(destAudioTrackIndex, buffer, bufferInfo)

                extractor.advance()
                copiedSampleCount++
            }

            Log.d(TAG, "提取完成，共复制了 $copiedSampleCount 个样本")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "提取音频时发生错误", e)
            return false
        } finally {
            try {
                muxer?.stop()
                muxer?.release()
            } catch (e: Exception) {
                Log.e(TAG, "释放资源时发生错误", e)
            }
        }
    }

    private fun selectTrackByMimeType(extractor: MediaExtractor, mimeTypePrefix: String): Int {
        val trackCount = extractor.trackCount
        for (i in 0 until trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue

            if (mime.startsWith(mimeTypePrefix)) {
                return i
            }
        }
        return -1
    }
}