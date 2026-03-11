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
import com.gamerx.ai.util.ShellExecutor

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
                        syncRemoteConversations()
                    }
                    is io.github.jan.supabase.auth.status.SessionStatus.NotAuthenticated -> {
                        userId = null
                        userContext = null
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

        val useRoot = preferences.useRoot.first()

        val shellBlock = buildString {
            appendLine()
            appendLine("## SYSTEM SHELL ACCESS")
            appendLine("You have DIRECT access to an Android shell on the user's device.")
            appendLine("When the user's request requires ANY system-level interaction (listing files, opening apps, checking battery, managing processes, modifying system settings, sending texts, installing/removing apps, scheduling tasks, spoofing values, changing CPU governors, rebooting, or ANY other shell operation), you MUST execute it yourself.")
            appendLine()
            appendLine("To execute a command, output EXACTLY this fenced block:")
            appendLine("```SHELL_EXEC")
            appendLine("command_here")
            appendLine("```")
            appendLine()
            appendLine("Rules:")
            if (useRoot) {
                appendLine("- You have ROOT (su) access. For root-only operations, prefix: su -c \"command\"")
            } else {
                appendLine("- Root access is NOT enabled. Only use standard shell commands. If root is needed, tell the user to enable Root Access in Settings.")
            }
            appendLine("- You may emit MULTIPLE SHELL_EXEC blocks in one response for multi-step tasks")
            appendLine("- After each block is executed, you receive the stdout/stderr as a [SHELL OUTPUT] system message")
            appendLine("- Then provide a clean human-readable summary of the results")
            appendLine("- NEVER tell the user to run commands manually. YOU execute them directly")
            appendLine("- For dangerous operations (delete, format, reboot), warn the user but still execute if they explicitly asked")
            appendLine("- If a command fails, analyze the error and try an alternative")
            appendLine("- Always prefer concise, efficient commands")
        }

        val basePrompt = "You are GamerX AI, a highly intelligent and helpful expert assistant. " +
            "Provide structured, beautiful responses using Markdown with bullet points and strategic emojis. " +
            "ALWAYS output code in triple-backtick fenced blocks with correct language tags." +
            shellBlock
        
        val systemPrompt = if (!userContext.isNullOrBlank()) {
            "$basePrompt\n\nUser Context: $userContext"
        } else basePrompt
        
        history.add(0, NvidiaService.ChatMessage("system", systemPrompt))
        
        return history
    }

    private val shellBlockPattern = Regex("```SHELL_EXEC\\s*\\n([\\s\\S]*?)\\n\\s*```")
    private val MAX_AGENT_ROUNDS = 5

    fun streamResponse(history: List<NvidiaService.ChatMessage>): Flow<String> = flow {
        val isLocal = preferences.isLocalMode.first()
        if (isLocal) {
            // Local LLM path (no shell execution in local mode)
            val modelId = preferences.localModelId.first() ?: "qwen_0.8b"
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val file = java.io.File(dir, "$modelId.gguf")
            if (!file.exists()) {
                emit("\n*[System Error: Missing Local Model. Please go to Settings > Local LLM and download a model.]*")
                return@flow
            }
            if (file.extension == "gguf") {
                emit("\n*[System Error: .gguf files require Llama.cpp integration. Please switch to Online Mode.]*")
                return@flow
            }
            if (llmInference == null || currentLlmModelId != modelId) {
                emit("*[System: Initializing edge inference engine...]*\n\n")
                try {
                    val options = com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions.builder()
                        .setModelPath(file.absolutePath)
                        .setMaxTokens(1024)
                        .setResultListener { partial, done -> localLlmOutput.tryEmit(Pair(partial ?: "", done)) }
                        .build()
                    llmInference = com.google.mediapipe.tasks.genai.llminference.LlmInference.createFromOptions(context, options)
                    currentLlmModelId = modelId
                } catch(e: Exception) {
                    emit("\n*[System Error: Failed to load local engine: ${e.message}]*")
                    return@flow
                }
            }
            val imEnd = "<" + "|im_end|" + ">"
            val eot = "<" + "|endoftext|" + ">"
            val formatted = buildString {
                history.forEach {
                    val imStart = "<" + "|im_start|" + ">"
                    when (it.role) {
                        "system" -> append("${imStart}system\n${it.content}${imEnd}\n")
                        "user" -> append("${imStart}user\n${it.content}${imEnd}\n")
                        "assistant" -> append("${imStart}assistant\n${it.content}${imEnd}\n")
                    }
                }
                val imStart = "<" + "|im_start|" + ">"
                append("${imStart}assistant\n")
            }
            try {
                llmInference!!.generateResponseAsync(formatted)
                var doneReceived = false
                localLlmOutput.takeWhile { !doneReceived }.collect { pair ->
                    if (pair.second) doneReceived = true
                    if (pair.first.isNotEmpty()) {
                        val cleaned = pair.first.replace(imEnd, "").replace(eot, "")
                        if (cleaned.isNotEmpty()) emit(cleaned)
                    }
                }
            } catch(e: Exception) {
                emit("\n*[System Error during inference: ${e.message}]*")
            }
        } else {
            // ===== ONLINE MODE WITH AGENTIC SHELL EXECUTION =====
            val useRoot = preferences.useRoot.first()
            val agentHistory = history.toMutableList()
            var round = 0

            while (round < MAX_AGENT_ROUNDS) {
                round++
                val fullResponse = StringBuilder()

                // Stream the AI response
                nvidiaService.streamChat(agentHistory).collect { chunk ->
                    fullResponse.append(chunk)
                    emit(chunk)
                }

                val responseText = fullResponse.toString()

                // Check if the AI emitted any SHELL_EXEC blocks
                val matches = shellBlockPattern.findAll(responseText).toList()
                if (matches.isEmpty()) {
                    // No commands to execute, we're done
                    break
                }

                // Execute each command block
                val shellOutputs = StringBuilder()
                for (match in matches) {
                    val command = match.groupValues[1].trim()
                    if (command.isEmpty()) continue

                    emit("\n\n> \u26a1 *Executing: `$command`*\n\n")

                    val result = ShellExecutor.execute(command, useRoot)

                    val outputBlock = buildString {
                        appendLine("```")
                        if (result.stdout.isNotBlank()) appendLine(result.stdout)
                        if (result.stderr.isNotBlank()) appendLine("[stderr] ${result.stderr}")
                        if (result.timedOut) appendLine("[TIMED OUT]")
                        appendLine("Exit code: ${result.exitCode} | Duration: ${result.durationMs}ms")
                        appendLine("```")
                    }

                    emit(outputBlock)
                    shellOutputs.appendLine("[SHELL OUTPUT for command: $command]")
                    if (result.stdout.isNotBlank()) shellOutputs.appendLine(result.stdout)
                    if (result.stderr.isNotBlank()) shellOutputs.appendLine("[stderr] ${result.stderr}")
                    shellOutputs.appendLine("[Exit code: ${result.exitCode}]")
                }

                // Feed the AI's response + shell outputs back into the conversation
                agentHistory.add(NvidiaService.ChatMessage("assistant", responseText))
                agentHistory.add(NvidiaService.ChatMessage("system", 
                    "[SHELL OUTPUT]\n${shellOutputs}\n\nNow interpret and summarize the results for the user in a clean, readable format."))

                emit("\n\n")
            }

            if (round >= MAX_AGENT_ROUNDS) {
                emit("\n*[System: Maximum execution rounds reached. Stopping.]*")
            }
        }
    }

    suspend fun getConversationCount(): Int {
        return conversationDao.getConversationCount() + _privateConversations.value.size
    }
}
