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

    // 新增方法：将音频提取为 PCM/WAV 格式，单声道
    fun extractAudioFromVideoToWav(videoFile: File, outputWavFile: File): Boolean {
        var extractor: MediaExtractor? = null

        try {
            extractor = MediaExtractor()
            extractor.setDataSource(videoFile.absolutePath)
            return extractAudioToWavInternal(extractor, outputWavFile)
        } catch (e: Exception) {
            Log.e(TAG, "提取 WAV 音频时发生错误", e)
            return false
        } finally {
            extractor?.release()
        }
    }

    // 新增方法：直接从 FileDescriptor 读取并提取为 WAV 格式，单声道
    fun extractAudioFromVideoToWav(context: android.content.Context, videoUri: android.net.Uri, outputWavFile: File): Boolean {
        var extractor: MediaExtractor? = null
        var parcelFd: android.os.ParcelFileDescriptor? = null

        try {
            extractor = MediaExtractor()
            parcelFd = context.contentResolver.openFileDescriptor(videoUri, "r")
            if (parcelFd != null) {
                extractor.setDataSource(parcelFd.fileDescriptor)
                return extractAudioToWavInternal(extractor, outputWavFile)
            } else {
                Log.e(TAG, "无法打开文件描述符")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "提取 WAV 音频时发生错误", e)
            return false
        } finally {
            extractor?.release()
            parcelFd?.close()
        }
    }

    // 内部方法：实际的 WAV 音频提取逻辑
    private fun extractAudioToWavInternal(extractor: MediaExtractor, outputWavFile: File): Boolean {
        try {
            // 选择音频轨道
            val audioTrackIndex = selectTrackByMimeType(extractor, "audio/")
            if (audioTrackIndex < 0) {
                Log.e(TAG, "视频中未找到音频轨道")
                return false
            }

            extractor.selectTrack(audioTrackIndex)
            val format = extractor.getTrackFormat(audioTrackIndex)

            // 获取音频参数
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val bitDepth = 16 // 固定为 16 位

            // 配置 MediaCodec 进行解码
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return false
            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val randomAccessFile = java.io.RandomAccessFile(outputWavFile, "rw")
            val buffer = ByteBuffer.allocate(1024 * 1024) // 1MB 缓冲区
            val bufferInfo = MediaCodec.BufferInfo()

            // 先写入 WAV 文件头占位符
            val wavHeader = ByteArray(44)
            randomAccessFile.write(wavHeader)

            var totalAudioLength: Long = 0
            extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            // 解码和处理音频数据
            while (true) {
                // 将样本数据送入解码器
                val inputBufferIndex = codec.dequeueInputBuffer(1000)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputBufferIndex) ?: continue
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        break
                    } else {
                        codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }

                // 获取解码后的音频数据
                val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 1000)
                if (outputBufferIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputBufferIndex) ?: continue
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                        // 保存解码前的位置和限制
                        val position = outputBuffer.position()
                        val limit = outputBuffer.limit()

                        // 处理 PCM 数据：转换为单声道
                        val pcmData = ByteArray(bufferInfo.size)
                        outputBuffer.get(pcmData)

                        val monoData = convertToMono(pcmData, channelCount)
                        randomAccessFile.write(monoData)
                        totalAudioLength += monoData.size

                        // 恢复缓冲区位置和限制
                        outputBuffer.position(position)
                        outputBuffer.limit(limit)
                    }
                    codec.releaseOutputBuffer(outputBufferIndex, false)
                } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // 格式变化时不需要额外处理
                }
            }

            // 重新写入 WAV 文件头
            randomAccessFile.seek(0)
            val header = createWavHeader(totalAudioLength, sampleRate, 1, bitDepth)
            randomAccessFile.write(header)

            // 清理资源
            randomAccessFile.close()
            codec.stop()
            codec.release()

            Log.d(TAG, "WAV 音频提取完成，输出文件: ${outputWavFile.absolutePath}")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "提取 WAV 音频时发生错误", e)
            return false
        }
    }

    // 将多声道 PCM 转换为单声道 (固定 16 位)
    private fun convertToMono(pcmData: ByteArray, channelCount: Int): ByteArray {
        if (channelCount == 1) {
            return pcmData // 已经是单声道，直接返回
        }

        val bytesPerSample = 2 // 固定为 16 位，2 字节
        val frameCount = pcmData.size / (channelCount * bytesPerSample)
        val monoData = ByteArray(frameCount * bytesPerSample)

        // 16 位 PCM (有符号)
        for (i in 0 until frameCount) {
            var sum: Int = 0
            for (channel in 0 until channelCount) {
                val offset = i * channelCount * bytesPerSample + channel * bytesPerSample
                val sample = (pcmData[offset + 1].toInt() shl 8) or (pcmData[offset].toInt() and 0xFF)
                sum += sample
            }
            val avgSample = sum / channelCount
            monoData[i * bytesPerSample] = avgSample.toByte()
            monoData[i * bytesPerSample + 1] = (avgSample shr 8).toByte()
        }

        return monoData
    }

    // 创建 WAV 文件头
    private fun createWavHeader(audioDataLength: Long, sampleRate: Int, channels: Int, bitDepth: Int): ByteArray {
        val header = ByteArray(44)
        val totalDataLen = audioDataLength + 36
        val byteRate = sampleRate * channels * bitDepth / 8

        // RIFF 标识符
        header[0] = 'R'.toByte()
        header[1] = 'I'.toByte()
        header[2] = 'F'.toByte()
        header[3] = 'F'.toByte()

        // 文件大小
        header[4] = (totalDataLen and 0xFF).toByte()
        header[5] = ((totalDataLen shr 8) and 0xFF).toByte()
        header[6] = ((totalDataLen shr 16) and 0xFF).toByte()
        header[7] = ((totalDataLen shr 24) and 0xFF).toByte()

        // WAVE 标识符
        header[8] = 'W'.toByte()
        header[9] = 'A'.toByte()
        header[10] = 'V'.toByte()
        header[11] = 'E'.toByte()

        // fmt 子块标识符
        header[12] = 'f'.toByte()
        header[13] = 'm'.toByte()
        header[14] = 't'.toByte()
        header[15] = ' '.toByte()

        // fmt 子块大小
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0

        // 音频格式 (PCM = 1)
        header[20] = 1
        header[21] = 0

        // 声道数
        header[22] = channels.toByte()
        header[23] = 0

        // 采样率
        header[24] = (sampleRate and 0xFF).toByte()
        header[25] = ((sampleRate shr 8) and 0xFF).toByte()
        header[26] = ((sampleRate shr 16) and 0xFF).toByte()
        header[27] = ((sampleRate shr 24) and 0xFF).toByte()

        // 字节率
        header[28] = (byteRate and 0xFF).toByte()
        header[29] = ((byteRate shr 8) and 0xFF).toByte()
        header[30] = ((byteRate shr 16) and 0xFF).toByte()
        header[31] = ((byteRate shr 24) and 0xFF).toByte()

        // 块对齐
        val blockAlign = (channels * bitDepth / 8).toByte()
        header[32] = blockAlign
        header[33] = 0

        // 位深度
        header[34] = bitDepth.toByte()
        header[35] = 0

        // data 子块标识符
        header[36] = 'd'.toByte()
        header[37] = 'a'.toByte()
        header[38] = 't'.toByte()
        header[39] = 'a'.toByte()

        // 数据大小
        header[40] = (audioDataLength and 0xFF).toByte()
        header[41] = ((audioDataLength shr 8) and 0xFF).toByte()
        header[42] = ((audioDataLength shr 16) and 0xFF).toByte()
        header[43] = ((audioDataLength shr 24) and 0xFF).toByte()

        return header
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