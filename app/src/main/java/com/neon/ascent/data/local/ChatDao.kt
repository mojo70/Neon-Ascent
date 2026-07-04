package com.neon.ascent.data.local

import androidx.room.*
import com.neon.ascent.model.ChatMessage
import com.neon.ascent.model.ChatSession
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions ORDER BY lastTimestamp DESC")
    fun getChatSessions(): Flow<List<ChatSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatSession(session: ChatSession)

    @Update
    suspend fun updateChatSession(session: ChatSession)

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE contactName = :contactName ORDER BY timestamp ASC")
    fun getMessagesForContact(contactName: String): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_sessions WHERE contactName = :contactName ORDER BY lastTimestamp DESC")
    fun getSessionsForContact(contactName: String): Flow<List<ChatSession>>

    @Query("SELECT * FROM chat_sessions WHERE contactName = :contactName ORDER BY lastTimestamp DESC LIMIT 1")
    suspend fun getLatestSessionForContact(contactName: String): ChatSession?

    @Query("SELECT * FROM chat_sessions WHERE sessionId = :sessionId")
    suspend fun getSessionById(sessionId: String): ChatSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("UPDATE chat_sessions SET isUnread = 0 WHERE sessionId = :sessionId")
    suspend fun markAsRead(sessionId: String)

    @Query("UPDATE chat_sessions SET isUnread = 0 WHERE contactName = :contactName")
    suspend fun markAllAsReadForContact(contactName: String)
}
