package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class AudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _playingPath = MutableStateFlow<String?>(null)
    val playingPath: StateFlow<String?> = _playingPath.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f) // 0.0 to 1.0
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0)
    val currentPositionMs: StateFlow<Int> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0)
    val durationMs: StateFlow<Int> = _durationMs.asStateFlow()

    fun play(path: String) {
        // If already playing this path, toggle pause
        if (_playingPath.value == path) {
            if (_isPlaying.value) {
                pause()
            } else {
                resume()
            }
            return
        }

        // Stop any previous playback
        stopPlaybackSilently()

        val file = File(path)
        if (!file.exists()) {
            Log.e("AudioPlayer", "Audio file does not exist: $path")
            return
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                setOnCompletionListener {
                    stopPlaybackSilently()
                }
                start()
            }
            _playingPath.value = path
            _isPlaying.value = true
            _durationMs.value = mediaPlayer?.duration ?: 0
            startProgressUpdateLoop()
            Log.d("AudioPlayer", "Started playing: $path")
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error preparing MediaPlayer for: $path", e)
            stopPlaybackSilently()
        }
    }

    fun pause() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    _isPlaying.value = false
                    progressJob?.cancel()
                }
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error pausing MediaPlayer", e)
        }
    }

    private fun resume() {
        try {
            mediaPlayer?.let {
                it.start()
                _isPlaying.value = true
                startProgressUpdateLoop()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error resuming MediaPlayer", e)
        }
    }

    fun stop() {
        stopPlaybackSilently()
    }

    private fun stopPlaybackSilently() {
        progressJob?.cancel()
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error releasing MediaPlayer", e)
        } finally {
            mediaPlayer = null
            _isPlaying.value = false
            _playingPath.value = null
            _playbackProgress.value = 0f
            _currentPositionMs.value = 0
            _durationMs.value = 0
        }
    }

    private fun startProgressUpdateLoop() {
        progressJob?.cancel()
        progressJob = coroutineScope.launch {
            while (isActive && _isPlaying.value) {
                val player = mediaPlayer
                if (player != null && player.isPlaying) {
                    val current = player.currentPosition
                    val total = player.duration
                    if (total > 0) {
                        _currentPositionMs.value = current
                        _playbackProgress.value = current.toFloat() / total.toFloat()
                    }
                }
                delay(50) // Update 20 times a second for super smooth UI seeking/sliders
            }
        }
    }

    fun release() {
        stopPlaybackSilently()
        coroutineScope.cancel()
    }
}
