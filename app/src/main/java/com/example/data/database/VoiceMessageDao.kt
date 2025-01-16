package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceMessageDao {
    @Query("SELECT * FROM voice_messages ORDER BY timestamp DESC")
    fun getAllMessagesFlow(): Flow<List<VoiceMessage>>

    @Query("SELECT * FROM voice_messages ORDER BY timestamp ASC")
    suspend fun getAllMessagesList(): List<VoiceMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: VoiceMessage)

    @Query("DELETE FROM voice_messages")
    suspend fun clearAllMessages()
}
