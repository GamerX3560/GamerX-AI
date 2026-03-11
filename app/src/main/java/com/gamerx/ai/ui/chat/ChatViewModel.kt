package com.gamerx.ai.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gamerx.ai.data.db.entities.Conversation
import com.gamerx.ai.data.db.entities.Message
import com.gamerx.ai.data.preferences.UserPreferences
import com.gamerx.ai.data.repository.ChatRepository
import com.gamerx.ai.util.VoiceManager
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val conversations: List<Conversation> = emptyList(),
    val currentConversationId: String? = null,
    val currentConversationIsPrivate: Boolean = false,
    val isLoading: Boolean = false,
    val streamingText: String = "",
    val isStreaming: Boolean = false,
    val error: String? = null,
    val isLocalMode: Boolean = false,
    val isPrivateMode: Boolean = false,
    val aiName: String = "GamerX AI",
    val greetingMessage: String = "Where should we start?",
    val userName: String = "User",
    val userEmail: String? = null,
    val userProfilePicUrl: String? = null,
    val isDrawerOpen: Boolean = false
)

class ChatViewModel(
    private val repository: ChatRepository,
    private val preferences: UserPreferences,
    val voiceManager: VoiceManager,
    private val supabase: io.github.jan.supabase.SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var messageCollectionJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            repository.getAllConversations().collect { conversations ->
                _uiState.update { it.copy(conversations = conversations) }
            }
        }
        viewModelScope.launch {
            preferences.privateMode.collect { isPrivate ->
                _uiState.update { it.copy(isPrivateMode = isPrivate) }
            }
        }
        viewModelScope.launch {
            preferences.isLocalMode.collect { isLocal ->
                _uiState.update { it.copy(isLocalMode = isLocal) }
            }
        }
        viewModelScope.launch {
            preferences.aiName.collect { name ->
                _uiState.update { it.copy(aiName = name) }
            }
        }
        viewModelScope.launch {
            preferences.greetingMessage.collect { greeting ->
                _uiState.update { it.copy(greetingMessage = greeting) }
            }
        }
        viewModelScope.launch {
            preferences.userName.collect { name ->
                _uiState.update { it.copy(userName = name) }
            }
        }
        viewModelScope.launch {
            supabase.auth.sessionStatus.collect { status ->
                if (status is io.github.jan.supabase.auth.status.SessionStatus.Authenticated) {
                    val user = status.session.user
                    if (user != null) {
                        val metaName = user.userMetadata?.get("full_name")?.toString()?.removeSurrounding("\"")
                            ?: user.userMetadata?.get("name")?.toString()?.removeSurrounding("\"")
                            ?: user.email?.substringBefore("@") ?: "User"
                            
                        val metaAvatar = user.userMetadata?.get("avatar_url")?.toString()?.removeSurrounding("\"")
                            ?: user.userMetadata?.get("picture")?.toString()?.removeSurrounding("\"")
                        
                        _uiState.update { it.copy(userName = metaName) }
                        launch { preferences.setUserName(metaName) }
                        _uiState.update { it.copy(userEmail = user.email, userProfilePicUrl = metaAvatar) }
                    }
                } else {
                    _uiState.update { it.copy(userName = "User", userEmail = null, userProfilePicUrl = null) }
                }
            }
        }
        voiceManager.initTts()
    }

    fun createNewChat() {
        viewModelScope.launch {
            try {
                val conversation = repository.createConversation(isPrivate = false)
                loadConversation(conversation.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Database Error: ${e.message}") }
            }
        }
    }

    fun startPrivateChat() {
        viewModelScope.launch {
            val conversation = repository.createConversation(isPrivate = true)
            loadConversation(conversation.id)
        }
    }

    fun createNewChatWithMessage(text: String) {
        viewModelScope.launch {
            try {
                val conversation = repository.createConversation()
                loadConversation(conversation.id)
                // Small delay to let the conversation load, then send
                kotlinx.coroutines.delay(100)
                sendMessage(text)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to start chat: ${e.message}") }
            }
        }
    }

    fun loadConversation(conversationId: String) {
        messageCollectionJob?.cancel()

        viewModelScope.launch {
            val conversation = repository.getConversationById(conversationId)
            _uiState.update {
                it.copy(
                    currentConversationId = conversationId,
                    currentConversationIsPrivate = conversation?.isPrivate == true,
                    isDrawerOpen = false,
                    error = null,
                    streamingText = "",
                    isStreaming = false
                )
            }

            // Immediately fetch remote messages
            repository.loadRemoteMessages(conversationId)

            messageCollectionJob = launch {
                repository.getMessagesFlow(conversationId).collect { messages ->
                    _uiState.update { it.copy(messages = messages) }
                }
            }
        }
    }

    fun sendMessage(text: String) {
        val conversationId = _uiState.value.currentConversationId ?: return
        if (text.isBlank()) return

        viewModelScope.launch {
            try {
                repository.addUserMessage(conversationId, text, null)

                _uiState.update {
                    it.copy(isLoading = true, isStreaming = true, streamingText = "", error = null)
                }

                val history = repository.getConversationHistory(conversationId)
                val responseFlow = repository.streamResponse(history)

                val fullResponse = StringBuilder()

                responseFlow.collect { chunk ->
                    fullResponse.append(chunk)
                    _uiState.update {
                        it.copy(streamingText = fullResponse.toString(), isLoading = false)
                    }
                }

                repository.addAiMessage(conversationId, fullResponse.toString())

                _uiState.update { it.copy(isStreaming = false, streamingText = "") }

                // Auto-read
                viewModelScope.launch {
                    if (preferences.autoReadResponses.first()) {
                        voiceManager.speak(fullResponse.toString())
                    }
                }

            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("network", true) == true -> "Network error. Check connection."
                    e.message?.contains("402", true) == true -> "API credits exhausted."
                    else -> "Error: ${e.message ?: "Something went wrong"}"
                }

                repository.addAiMessage(conversationId, "⚠️ $errorMessage")

                _uiState.update {
                    it.copy(isLoading = false, isStreaming = false, streamingText = "", error = errorMessage)
                }
            }
        }
    }

    fun deleteConversation(conversation: Conversation) {
        viewModelScope.launch {
            repository.deleteConversation(conversation)
            if (_uiState.value.currentConversationId == conversation.id) {
                _uiState.update { it.copy(currentConversationId = null, messages = emptyList()) }
            }
        }
    }

    fun deleteAllConversations() {
        viewModelScope.launch {
            repository.deleteAllConversations()
            _uiState.update { it.copy(currentConversationId = null, messages = emptyList()) }
        }
    }

    fun toggleDrawer() {
        _uiState.update { it.copy(isDrawerOpen = !it.isDrawerOpen) }
    }

    fun closeDrawer() {
        _uiState.update { it.copy(isDrawerOpen = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun togglePrivateMode() {
        viewModelScope.launch {
            val current = preferences.privateMode.first()
            preferences.setPrivateMode(!current)
        }
    }
    
    fun toggleLocalMode() {
        viewModelScope.launch {
            val current = preferences.isLocalMode.first()
            preferences.setLocalMode(!current)
        }
    }
    
    suspend fun getConversationShareText(conversationId: String): String {
        val messages = repository.getMessagesFlow(conversationId).first()
        val title = repository.getConversationById(conversationId)?.title ?: "Chat"
        
        return buildString {
            appendLine("Transcript: $title")
            appendLine("-------------------")
            messages.forEach {
                val roleName = if (it.role == "user") _uiState.value.userName else _uiState.value.aiName
                appendLine("$roleName: ${it.content.replace(Regex("▌$"), "")}")
                appendLine()
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                supabase.auth.signOut()
            } catch(e: Exception) {}
            onComplete()
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}

class ChatViewModelFactory(
    private val repository: ChatRepository,
    private val preferences: UserPreferences,
    private val voiceManager: VoiceManager,
    private val supabase: io.github.jan.supabase.SupabaseClient
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ChatViewModel(repository, preferences, voiceManager, supabase) as T
    }
}
