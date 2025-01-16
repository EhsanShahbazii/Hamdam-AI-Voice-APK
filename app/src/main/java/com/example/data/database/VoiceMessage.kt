package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_messages")
data class VoiceMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val role: String, // "user" or "assistant"
    val audioPath: String, // Local absolute path to the audio file
    val durationMs: Long, // Duration of audio in milliseconds
    val timestamp: Long = System.currentTimeMillis(),
    val transcript: String = "" // Optional transcribed text
)
