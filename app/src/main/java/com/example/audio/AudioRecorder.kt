package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

class AudioRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var startTime: Long = 0L
    private var isRecording = false

    fun start(outputFile: File) {
        if (isRecording) return
        
        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(64000)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            startTime = System.currentTimeMillis()
            isRecording = true
            Log.d("AudioRecorder", "Recording started successfully. Output: ${outputFile.absolutePath}")
        } catch (e: IOException) {
            Log.e("AudioRecorder", "Failed to prepare MediaRecorder", e)
        } catch (e: IllegalStateException) {
            Log.e("AudioRecorder", "Failed to start MediaRecorder", e)
        }
    }

    fun stop(): Long {
        if (!isRecording) return 0L
        
        var duration = 0L
        try {
            mediaRecorder?.let {
                it.stop()
                it.release()
            }
            duration = System.currentTimeMillis() - startTime
            Log.d("AudioRecorder", "Recording stopped. Duration: ${duration}ms")
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error stopping MediaRecorder", e)
        } finally {
            mediaRecorder = null
            isRecording = false
        }
        return duration
    }

    fun isRecording() = isRecording

    fun getMaxAmplitude(): Int {
        if (!isRecording) return 0
        return try {
            mediaRecorder?.maxAmplitude ?: 0
        } catch (e: Exception) {
            0
        }
    }
}
