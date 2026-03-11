package com.gamerx.ai.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.datetime.Instant
import kotlinx.datetime.Clock

@OptIn(ExperimentalSerializationApi::class)
@Entity(tableName = "conversations")
@Serializable
data class Conversation(
    @PrimaryKey
    @EncodeDefault val id: String = java.util.UUID.randomUUID().toString(),
    @SerialName("user_id")
    @EncodeDefault val userId: String? = null,
    @EncodeDefault val title: String = "New Chat",
    @SerialName("created_at")
    @EncodeDefault val createdAt: Instant = Clock.System.now(),
    @SerialName("updated_at")
    @EncodeDefault val updatedAt: Instant = Clock.System.now(),
    @SerialName("is_private")
    @EncodeDefault val isPrivate: Boolean = false,
    @kotlinx.serialization.Transient
    val isPinned: Boolean = false
)
