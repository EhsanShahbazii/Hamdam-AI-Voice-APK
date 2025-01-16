package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mother_voice_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_API_KEY = "api_key"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_WHISPER_MODEL = "whisper_model"
        private const val KEY_CHAT_MODEL = "chat_model"
        private const val KEY_TTS_MODEL = "tts_model"
        private const val KEY_TTS_VOICE = "tts_voice"
        private const val KEY_MOTHER_NAME = "mother_name"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_AUTO_SILENCE_STOP = "auto_silence_stop"
        private const val KEY_SILENCE_DELAY_SECONDS = "silence_delay_seconds"

        // Default credentials from the user's prompt
        private const val DEFAULT_API_KEY = ""
        private const val DEFAULT_BASE_URL = "https://api.gapgpt.app/v1"
        private const val DEFAULT_WHISPER_MODEL = "gapgpt/whisper-1"
        private const val DEFAULT_CHAT_MODEL = "gpt-4o-mini"
        private const val DEFAULT_TTS_MODEL = "tts-1"
        private const val DEFAULT_TTS_VOICE = "nova"
        private const val DEFAULT_MOTHER_NAME = "مادر"
        private const val DEFAULT_DARK_MODE = false
        private const val DEFAULT_AUTO_SILENCE_STOP = true
        private const val DEFAULT_SILENCE_DELAY_SECONDS = 3
    }

    private val _apiKey = MutableStateFlow(getApiKey())
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _baseUrl = MutableStateFlow(getBaseUrl())
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

    private val _whisperModel = MutableStateFlow(getWhisperModel())
    val whisperModel: StateFlow<String> = _whisperModel.asStateFlow()

    private val _chatModel = MutableStateFlow(getChatModel())
    val chatModel: StateFlow<String> = _chatModel.asStateFlow()

    private val _ttsModel = MutableStateFlow(getTtsModel())
    val ttsModel: StateFlow<String> = _ttsModel.asStateFlow()

    private val _ttsVoice = MutableStateFlow(getTtsVoice())
    val ttsVoice: StateFlow<String> = _ttsVoice.asStateFlow()

    private val _motherName = MutableStateFlow(getMotherName())
    val motherName: StateFlow<String> = _motherName.asStateFlow()

    private val _darkMode = MutableStateFlow(getDarkMode())
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    private val _autoSilenceStop = MutableStateFlow(getAutoSilenceStop())
    val autoSilenceStop: StateFlow<Boolean> = _autoSilenceStop.asStateFlow()

    private val _silenceDelaySeconds = MutableStateFlow(getSilenceDelaySeconds())
    val silenceDelaySeconds: StateFlow<Int> = _silenceDelaySeconds.asStateFlow()

    fun getApiKey(): String {
        val current = prefs.getString(KEY_API_KEY, DEFAULT_API_KEY) ?: DEFAULT_API_KEY
        if (current == "sk-S79crtynmJHKl4BdnxPIBaRGRfDoLi4oFhdRZ0j8y5kGlRo9" || current == "sk-4prDPVOymnlo3xG05GRDZzakjxxvUdiWuxzhuWAoSn2LcVsg") {
            return ""
        }
        return current
    }
    fun setApiKey(value: String) {
        prefs.edit().putString(KEY_API_KEY, value).apply()
        _apiKey.value = value
    }

    fun getBaseUrl(): String = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    fun setBaseUrl(value: String) {
        prefs.edit().putString(KEY_BASE_URL, value).apply()
        _baseUrl.value = value
    }

    fun getWhisperModel(): String = prefs.getString(KEY_WHISPER_MODEL, DEFAULT_WHISPER_MODEL) ?: DEFAULT_WHISPER_MODEL
    fun setWhisperModel(value: String) {
        prefs.edit().putString(KEY_WHISPER_MODEL, value).apply()
        _whisperModel.value = value
    }

    fun getChatModel(): String = prefs.getString(KEY_CHAT_MODEL, DEFAULT_CHAT_MODEL) ?: DEFAULT_CHAT_MODEL
    fun setChatModel(value: String) {
        prefs.edit().putString(KEY_CHAT_MODEL, value).apply()
        _chatModel.value = value
    }

    fun getTtsModel(): String = prefs.getString(KEY_TTS_MODEL, DEFAULT_TTS_MODEL) ?: DEFAULT_TTS_MODEL
    fun setTtsModel(value: String) {
        prefs.edit().putString(KEY_TTS_MODEL, value).apply()
        _ttsModel.value = value
    }

    fun getTtsVoice(): String = prefs.getString(KEY_TTS_VOICE, DEFAULT_TTS_VOICE) ?: DEFAULT_TTS_VOICE
    fun setTtsVoice(value: String) {
        prefs.edit().putString(KEY_TTS_VOICE, value).apply()
        _ttsVoice.value = value
    }

    fun getMotherName(): String = prefs.getString(KEY_MOTHER_NAME, DEFAULT_MOTHER_NAME) ?: DEFAULT_MOTHER_NAME
    fun setMotherName(value: String) {
        prefs.edit().putString(KEY_MOTHER_NAME, value).apply()
        _motherName.value = value
    }

    fun getDarkMode(): Boolean = prefs.getBoolean(KEY_DARK_MODE, DEFAULT_DARK_MODE)
    fun setDarkMode(value: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()
        _darkMode.value = value
    }

    fun getAutoSilenceStop(): Boolean = prefs.getBoolean(KEY_AUTO_SILENCE_STOP, DEFAULT_AUTO_SILENCE_STOP)
    fun setAutoSilenceStop(value: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SILENCE_STOP, value).apply()
        _autoSilenceStop.value = value
    }

    fun getSilenceDelaySeconds(): Int = prefs.getInt(KEY_SILENCE_DELAY_SECONDS, DEFAULT_SILENCE_DELAY_SECONDS)
    fun setSilenceDelaySeconds(value: Int) {
        prefs.edit().putInt(KEY_SILENCE_DELAY_SECONDS, value).apply()
        _silenceDelaySeconds.value = value
    }

    fun getSystemPrompt(): String {
        val name = getMotherName()
        val addressName = if (name == "مادر" || name.isEmpty()) "مامان عزیز" else "مامان $name"
        val greetingName = if (name == "مادر" || name.isEmpty()) "مادر عزیز" else "«$name» خانم"
        return "تو یک دستیار صوتی بسیار مهربان، دلسوز و صمیمی هستی. مخاطب تو مادر من است. تو باید همیشه او را با نام «$addressName» یا «$greetingName» با احترام و محبت زیاد خطاب کنی و با او بسیار گرم، صمیمی، دلسوزانه و با محبت صحبت کنی. فقط و فقط به زبان فارسی پاسخ بده. پاسخ‌هایت را بسیار کوتاه، ساده، واضح و شنیدنی نگه دار تا خسته نشود (حداکثر دو یا سه جمله کوتاه). هرگز از واژگان سخت یا انگلیسی استفاده نکن."
    }
}
