package com.gamerx.ai.data.repository

import android.util.Log
import com.gamerx.ai.data.api.NvidiaService
import com.gamerx.ai.data.db.dao.ConversationDao
import com.gamerx.ai.data.db.dao.MessageDao
import com.gamerx.ai.data.db.entities.Conversation
import com.gamerx.ai.data.db.entities.Message
import com.gamerx.ai.data.preferences.UserPreferences
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.takeWhile
import android.content.Context

@Serializable
data class UserProfile(
    val id: String,
    val context: String? = null
)

class ChatRepository(
    private val nvidiaService: NvidiaService,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val preferences: UserPreferences,
    private val context: Context,
    private val supabase: SupabaseClient
) {
    // In-memory state for true Private/Incognito chats
    private val _privateConversations = MutableStateFlow<List<Conversation>>(emptyList())
    private val _privateMessages = MutableStateFlow<Map<String, List<Message>>>(emptyMap())
    
    // MediaPipe Edge Engine State
    private var llmInference: com.google.mediapipe.tasks.genai.llminference.LlmInference? = null
    private var currentLlmModelId: String? = null
    private val localLlmOutput = MutableSharedFlow<Pair<String, Boolean>>(extraBufferCapacity = 8192)
    
    private var userId: String? = null
    private var userContext: String? = null

    init {
        CoroutineScope(Dispatchers.IO).launch {
            supabase.auth.sessionStatus.collect { status ->
                when (status) {
                    is io.github.jan.supabase.auth.status.SessionStatus.Authenticated -> {
                        userId = status.session.user?.id
                        fetchUserContext()
                        // One-time sync on login/app open
                        syncRemoteConversations()
                    }
                    is io.github.jan.supabase.auth.status.SessionStatus.NotAuthenticated -> {
                        userId = null
                        userContext = null
                        // Optional: Clear local DB on logout to protect privacy.
                        // conversationDao.deleteAllConversations()
                    }
                    else -> {}
                }
            }
        }
    }

    private suspend fun fetchUserContext() {
        if (userId == null) return
        try {
            val profile = supabase.postgrest["users"]
                .select { filter { eq("id", userId!!) } }
                .decodeSingleOrNull<UserProfile>()
            userContext = profile?.context
        } catch (e: Exception) {
            Log.e("Supabase", "Error fetching user context", e)
        }
    }

    private suspend fun syncRemoteConversations() {
        if (userId == null) return
        try {
            val remoteConvs = supabase.postgrest["conversations"]
                .select { order("updated_at", Order.DESCENDING) }
                .decodeList<Conversation>()
                
            remoteConvs.forEach {
                conversationDao.upsertConversation(it)
            }
        } catch (e: Exception) {
            Log.e("Supabase", "Error syncing conversations completely offline bypass", e)
        }
    }

    private suspend fun syncRemoteMessages(conversationId: String) {
        if (userId == null) return
        try {
            val remoteMsgs = supabase.postgrest["messages"]
                .select { filter { eq("conversation_id", conversationId) } }
                .decodeList<Message>()

            remoteMsgs.forEach { messageDao.insertMessage(it) }
        } catch (e: Exception) {
            Log.e("Supabase", "Error syncing messages", e)
        }
    }

    fun getAllConversations(): Flow<List<Conversation>> {
        return combine(
            conversationDao.getAllConversations(),
            _privateConversations
        ) { dbList, privateList ->
            // In Phase 7, we want private conversations to be fully ephemeral and not crowd the persistent sidebar list
            dbList.sortedByDescending { it.updatedAt }
        }
    }

    suspend fun getConversationById(id: String): Conversation? {
        val privateConv = _privateConversations.value.find { it.id == id }
        if (privateConv != null) return privateConv
        return conversationDao.getConversationById(id)
    }

    suspend fun createConversation(isPrivate: Boolean = false): Conversation {
        val conversation = Conversation(
            isPrivate = isPrivate, 
            userId = userId,
            title = "New Chat",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        if (isPrivate) {
            _privateConversations.value = listOf(conversation) + _privateConversations.value
        } else {
            conversationDao.upsertConversation(conversation)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (userId != null) supabase.postgrest["conversations"].insert(conversation)
                } catch(e: Exception) {}
            }
        }
        return conversation
    }

    suspend fun deleteConversation(conversation: Conversation) {
        if (conversation.isPrivate) {
            _privateConversations.value = _privateConversations.value.filter { it.id != conversation.id }
            val map = _privateMessages.value.toMutableMap()
            map.remove(conversation.id)
            _privateMessages.value = map
        } else {
            conversationDao.deleteConversation(conversation)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (userId != null) supabase.postgrest["conversations"].delete { filter { eq("id", conversation.id) } }
                } catch(e: Exception) {}
            }
        }
    }

    suspend fun deleteAllConversations() {
        _privateConversations.value = emptyList()
        _privateMessages.value = emptyMap()
        val all = conversationDao.getAllConversations().first()
        conversationDao.deleteAllConversations()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (userId != null) {
                    all.forEach {
                        supabase.postgrest["conversations"].delete { filter { eq("id", it.id) } }
                    }
                }
            } catch(e: Exception) {}
        }
    }

    fun getMessagesFlow(conversationId: String): Flow<List<Message>> {
        return combine(
            messageDao.getMessagesForConversation(conversationId),
            _privateMessages.map { it[conversationId] }
        ) { dbList, privateList ->
            if (privateList != null) privateList else dbList
        }
    }

    suspend fun loadRemoteMessages(conversationId: String) {
        val conv = getConversationById(conversationId)
        if (conv?.isPrivate == true) return
        CoroutineScope(Dispatchers.IO).launch {
            syncRemoteMessages(conversationId)
        }
    }

    suspend fun addUserMessage(conversationId: String, content: String, imageUri: String? = null): Message {
        val message = Message(
            conversationId = conversationId,
            role = "user",
            content = content,
            imageUri = imageUri,
            userId = userId,
            timestamp = Clock.System.now()
        )
        
        val conv = getConversationById(conversationId)
        
        if (conv?.isPrivate == true) {
            val map = _privateMessages.value.toMutableMap()
            val list = map[conversationId]?.toMutableList() ?: mutableListOf()
            list.add(message)
            map[conversationId] = list
            _privateMessages.value = map
        } else {
            messageDao.insertMessage(message)
            CoroutineScope(Dispatchers.IO).launch {
                try { 
                    if (userId != null) {
                        Log.d("Supabase", "Attempting to push user message to Supabase...")
                        supabase.postgrest["messages"].insert(message) 
                        Log.d("Supabase", "Successfully pushed user message.")
                    }
                } catch(e: Exception) {
                    Log.e("Supabase", "Failed to sync user message $e", e)
                }
            }
            preferences.incrementMessageCount()

            // Update title
            val msgCount = messageDao.getMessageCount(conversationId)
            if (msgCount <= 2) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val title = nvidiaService.generateTitle(content)
                        conversationDao.updateTitle(conversationId, title, Clock.System.now().toEpochMilliseconds())
                        if (userId != null) {
                            supabase.postgrest["conversations"].update({ set("title", title); set("updated_at", Clock.System.now()) }) {
                                filter { eq("id", conversationId) }
                            }
                        }
                    } catch (e: Exception) {}
                }
            } else {
                conversationDao.updateTitle(conversationId, conv?.title ?: "Chat", Clock.System.now().toEpochMilliseconds())
                CoroutineScope(Dispatchers.IO).launch {
                    try { if (userId != null) supabase.postgrest["conversations"].update({ set("updated_at", Clock.System.now()) }) { filter { eq("id", conversationId) } } } catch(e: Exception) {}
                }
            }
        }
        return message
    }

    suspend fun addAiMessage(conversationId: String, content: String): Message {
        val message = Message(
            conversationId = conversationId,
            role = "assistant",
            content = content,
            userId = userId,
            timestamp = Clock.System.now()
        )
        
        val conv = getConversationById(conversationId)
        if (conv?.isPrivate == true) {
            val map = _privateMessages.value.toMutableMap()
            val list = map[conversationId]?.toMutableList() ?: mutableListOf()
            list.add(message)
            map[conversationId] = list
            _privateMessages.value = map
        } else {
            messageDao.insertMessage(message)
            CoroutineScope(Dispatchers.IO).launch {
                try { 
                    if (userId != null) {
                        Log.d("Supabase", "Attempting to push AI message to Supabase...")
                        supabase.postgrest["messages"].insert(message) 
                        Log.d("Supabase", "Successfully pushed AI message.")
                    }
                } catch(e: Exception) {
                    Log.e("Supabase", "Failed to sync AI message $e", e)
                }
            }
        }
        return message
    }

    suspend fun getConversationHistory(conversationId: String): List<NvidiaService.ChatMessage> {
        val conv = getConversationById(conversationId)
        val msgs = if (conv?.isPrivate == true) {
            _privateMessages.value[conversationId] ?: emptyList()
        } else {
            messageDao.getMessagesForConversationSync(conversationId)
        }
        
        val history = msgs.mapNotNull { msg ->
            when (msg.role) {
                "user" -> NvidiaService.ChatMessage("user", msg.content)
                "model", "assistant" -> NvidiaService.ChatMessage("assistant", msg.content)
                else -> null
            }
        }.toMutableList()

        val basePrompt = "You are GamerX AI, a highly intelligent and helpful expert assistant. " +
            "Provide structured, beautiful responses. Break down explanations into clear bullet points. " +
            "Use strategic emojis to make the text engaging. ALWAYS use Markdown. " +
            "If asked about factual info, summarize cleanly. " +
            "ALWAYS output code within terminal-style triple backticks highlighting the correct language."
        
        val systemPrompt = if (!userContext.isNullOrBlank()) {
            "$basePrompt\n\nUser Context: $userContext"
        } else basePrompt
        
        history.add(0, NvidiaService.ChatMessage("system", systemPrompt))
        
        return history
    }

    fun streamResponse(history: List<NvidiaService.ChatMessage>): Flow<String> = flow {
        val isLocal = preferences.isLocalMode.first()
        if (isLocal) {
            val modelId = preferences.localModelId.first() ?: "qwen_0.8b"
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val file = java.io.File(dir, "$modelId.gguf")
            if (!file.exists()) {
                emit("\n*[System Error: Missing Local Model. Please go to Settings > Local LLM and download a model.]*")
                return@flow
            }
            
            // Gracefully catch .gguf format limitation since MediaPipe requires .bin ODML
            if (file.extension == "gguf") {
                emit("\n*[System Error: GamerX AI currently uses Google MediaPipe for local inference, which requires specific .bin Odml models. Running raw .gguf files directly requires a pending Llama.cpp C++ integration update. Please switch to Online Mode or download a compatible model.]*")
                return@flow
            }

            if (llmInference == null || currentLlmModelId != modelId) {
                emit("*[System: Initializing edge inference engine `$modelId` into RAM...]*\n\n")
                try {
                    val options = com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions.builder()
                        .setModelPath(file.absolutePath)
                        .setMaxTokens(1024)
                        .setResultListener { partial, done ->
                            localLlmOutput.tryEmit(Pair(partial ?: "", done))
                        }
                        .build()
                    llmInference = com.google.mediapipe.tasks.genai.llminference.LlmInference.createFromOptions(context, options)
                    currentLlmModelId = modelId
                } catch(e: Exception) {
                    emit("\n*[System Error: Failed to load local engine: ${e.message}]*")
                    return@flow
                }
            }
            
            val formatted = buildString {
                history.forEach {
                    if (it.role == "system") append("<|im_start|>system\n${it.content}<|im_end|>\n")
                    if (it.role == "user") append("<|im_start|>user\n${it.content}<|im_end|>\n")
                    if (it.role == "assistant") append("<|im_start|>assistant\n${it.content}<|im_end|>\n")
                }
                append("<|im_start|>assistant\n")
            }

            try {
                llmInference!!.generateResponseAsync(formatted)
                
                var doneReceived = false
                
                localLlmOutput.takeWhile { !doneReceived }.collect { pair ->
                    val text = pair.first
                    val done = pair.second
                    if (done) doneReceived = true
                    if (text.isNotEmpty()) {
                        val cleaned = text.replace("<|im_end|>", "").replace("<|endoftext|>", "")
                        if (cleaned.isNotEmpty()) emit(cleaned)
                    }
                }
            } catch(e: Exception) {
                emit("\n*[System Error during inference: ${e.message}]*")
            }
        } else {
            nvidiaService.streamChat(history).collect { emit(it) }
        }
    }

    suspend fun getConversationCount(): Int {
        return conversationDao.getConversationCount() + _privateConversations.value.size
    }
}
