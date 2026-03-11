package com.gamerx.ai.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.datetime.Instant
import kotlinx.datetime.Clock

@OptIn(ExperimentalSerializationApi::class)
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = Conversation::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["conversationId"])]
)
@Serializable
data class Message(
    @PrimaryKey
    @EncodeDefault val id: String = java.util.UUID.randomUUID().toString(),
    @SerialName("conversation_id")
    val conversationId: String,
    @SerialName("user_id")
    @EncodeDefault val userId: String? = null,
    val role: String, // "user" or "model"
    val content: String,
    @EncodeDefault val timestamp: Instant = Clock.System.now(),
    @SerialName("image_uri")
    @EncodeDefault val imageUri: String? = null,
    @SerialName("is_error")
    @EncodeDefault val isError: Boolean = false
)
