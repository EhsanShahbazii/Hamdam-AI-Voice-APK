package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayer
import com.example.audio.AudioRecorder
import com.example.data.database.AppDatabase
import com.example.data.database.VoiceMessage
import com.example.data.repository.SettingsRepository
import com.example.data.repository.VoiceChatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class VoiceChatViewModel(
    application: Application,
    private val repository: VoiceChatRepository,
    private val settingsRepository: SettingsRepository,
    val audioPlayer: AudioPlayer
) : AndroidViewModel(application) {

    val messages: StateFlow<List<VoiceMessage>> = repository.allMessages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI Status states
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Settings flows
    val motherName = settingsRepository.motherName
    val apiKey = settingsRepository.apiKey
    val baseUrl = settingsRepository.baseUrl
    val ttsVoice = settingsRepository.ttsVoice
    val whisperModel = settingsRepository.whisperModel
    val chatModel = settingsRepository.chatModel
    val ttsModel = settingsRepository.ttsModel
    val darkMode = settingsRepository.darkMode
    val autoSilenceStop = settingsRepository.autoSilenceStop
    val silenceDelaySeconds = settingsRepository.silenceDelaySeconds

    // Playback state exposed directly from the shared player
    val playingPath: StateFlow<String?> = audioPlayer.playingPath
    val isPlaying: StateFlow<Boolean> = audioPlayer.isPlaying
    val playbackProgress: StateFlow<Float> = audioPlayer.playbackProgress
    val currentPositionMs: StateFlow<Int> = audioPlayer.currentPositionMs
    val durationMs: StateFlow<Int> = audioPlayer.durationMs

    private var silenceJob: kotlinx.coroutines.Job? = null

    fun startRecording() {
        if (_isRecording.value || _isProcessing.value) return
        audioPlayer.stop() // Stop any ongoing playback
        _errorMessage.value = null
        _isRecording.value = true
        repository.startRecording()

        if (settingsRepository.getAutoSilenceStop()) {
            startSilenceDetection()
        }
    }

    private fun startSilenceDetection() {
        silenceJob?.cancel()
        silenceJob = viewModelScope.launch {
            val delayMs = 150L
            val limitMs = settingsRepository.getSilenceDelaySeconds() * 1000L
            var silentTimeMs = 0L
            kotlinx.coroutines.delay(800) // Initial delay to warm up recording
            while (_isRecording.value) {
                kotlinx.coroutines.delay(delayMs)
                val amp = repository.getMaxAmplitude()
                // A typical silence threshold is around 1500 max amplitude
                if (amp < 1500) {
                    silentTimeMs += delayMs
                } else {
                    silentTimeMs = 0L
                }

                if (silentTimeMs >= limitMs) {
                    android.util.Log.d("VoiceChatViewModel", "Silence detected for $silentTimeMs ms. Stopping recording automatically.")
                    stopRecording()
                    break
                }
            }
        }
    }

    fun stopRecording() {
        if (!_isRecording.value) return
        silenceJob?.cancel()
        silenceJob = null
        _isRecording.value = false
        _isProcessing.value = true
        
        viewModelScope.launch {
            val userMsg = repository.stopRecordingAndSave()
            if (userMsg != null) {
                repository.processUserMessage(userMsg) { error ->
                    _errorMessage.value = error
                }
            } else {
                _errorMessage.value = "صدایی ضبط نشد. لطفاً دکمه را نگه‌دارید و صحبت کنید."
            }
            _isProcessing.value = false
        }
    }

    fun playVoice(path: String) {
        audioPlayer.play(path)
    }

    fun stopVoice() {
        audioPlayer.stop()
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearArchive()
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // Settings actions
    fun updateMotherName(name: String) {
        settingsRepository.setMotherName(name)
    }

    fun updateApiKey(key: String) {
        settingsRepository.setApiKey(key)
    }

    fun updateBaseUrl(url: String) {
        settingsRepository.setBaseUrl(url)
    }

    fun updateTtsVoice(voice: String) {
        settingsRepository.setTtsVoice(voice)
    }

    fun updateWhisperModel(model: String) {
        settingsRepository.setWhisperModel(model)
    }

    fun updateChatModel(model: String) {
        settingsRepository.setChatModel(model)
    }

    fun updateTtsModel(model: String) {
        settingsRepository.setTtsModel(model)
    }

    fun updateDarkMode(enabled: Boolean) {
        settingsRepository.setDarkMode(enabled)
    }

    fun updateAutoSilenceStop(enabled: Boolean) {
        settingsRepository.setAutoSilenceStop(enabled)
    }

    fun updateSilenceDelaySeconds(seconds: Int) {
        settingsRepository.setSilenceDelaySeconds(seconds)
    }

    fun playSampleVoice(voice: String, text: String, onFinished: (String?) -> Unit) {
        audioPlayer.stop()
        viewModelScope.launch {
            repository.generateAndPlaySample(voice, text, onFinished)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val database = AppDatabase.getDatabase(application)
            val settingsRepo = SettingsRepository(application)
            val audioPlayer = AudioPlayer(application)
            val audioRecorder = AudioRecorder(application)
            val voiceChatRepo = VoiceChatRepository(
                application,
                database.voiceMessageDao(),
                settingsRepo,
                audioPlayer,
                audioRecorder
            )
            return VoiceChatViewModel(application, voiceChatRepo, settingsRepo, audioPlayer) as T
        }
    }
}
