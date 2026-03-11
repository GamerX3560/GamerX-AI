package com.gamerx.ai.data.db.dao

import androidx.room.*
import com.gamerx.ai.data.db.entities.Conversation
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE isPrivate = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllConversations(): Flow<List<Conversation>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversationById(id: String): Conversation?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertConversation(conversation: Conversation): Long

    @Update
    suspend fun updateConversation(conversation: Conversation)

    @Transaction
    suspend fun upsertConversation(conversation: Conversation) {
        val id = insertConversation(conversation)
        if (id == -1L) {
            updateConversation(conversation)
        }
    }



    @Delete
    suspend fun deleteConversation(conversation: Conversation)

    @Query("DELETE FROM conversations WHERE isPrivate = 0")
    suspend fun deleteAllConversations()

    @Query("DELETE FROM conversations WHERE isPrivate = 1")
    suspend fun deletePrivateConversations()

    @Query("UPDATE conversations SET title = :title, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTitle(id: String, title: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM conversations WHERE isPrivate = 0")
    suspend fun getConversationCount(): Int

    @Query("SELECT * FROM conversations WHERE title LIKE '%' || :query || '%' AND isPrivate = 0 ORDER BY updatedAt DESC")
    fun searchConversations(query: String): Flow<List<Conversation>>
}
