package com.gamerx.ai.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gamerx_ai_prefs")

class UserPreferences(private val context: Context) {

    companion object {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

        // Personalization
        val AI_NAME = stringPreferencesKey("ai_name")
        val GREETING_MESSAGE = stringPreferencesKey("greeting_message")

        // User Info
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_AVATAR_URI = stringPreferencesKey("user_avatar_uri")

        // Voice
        val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        val TTS_SPEED = floatPreferencesKey("tts_speed")
        val AUTO_READ_RESPONSES = booleanPreferencesKey("auto_read_responses")

        // Local LLM
        val IS_LOCAL_MODE = booleanPreferencesKey("is_local_mode")
        val LOCAL_MODEL_ID = stringPreferencesKey("local_model_id")
        val DOWNLOADED_MODELS = stringSetPreferencesKey("downloaded_models")

        // Privacy
        val PRIVATE_MODE = booleanPreferencesKey("private_mode")

        // Root Access
        val USE_ROOT = booleanPreferencesKey("use_root")

        // Data
        val TOTAL_MESSAGES_SENT = intPreferencesKey("total_messages_sent")
    }

    // Onboarding
    val hasSeenOnboarding: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETED] = completed
        }
    }

    // AI Name
    val aiName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[AI_NAME] ?: "GamerX AI"
    }

    suspend fun setAiName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[AI_NAME] = name
        }
    }

    // Greeting
    val greetingMessage: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[GREETING_MESSAGE] ?: "Hey! I'm GamerX AI 🚀\nHow can I help you today?"
    }

    suspend fun setGreetingMessage(message: String) {
        context.dataStore.edit { prefs ->
            prefs[GREETING_MESSAGE] = message
        }
    }

    // User Name
    val userName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[USER_NAME] ?: "User"
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_NAME] = name
        }
    }

    // User Avatar
    val userAvatarUri: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[USER_AVATAR_URI]
    }

    suspend fun setUserAvatarUri(uri: String?) {
        context.dataStore.edit { prefs ->
            if (uri != null) prefs[USER_AVATAR_URI] = uri
            else prefs.remove(USER_AVATAR_URI)
        }
    }

    // TTS Enabled
    val ttsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[TTS_ENABLED] ?: false
    }

    suspend fun setTtsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[TTS_ENABLED] = enabled
        }
    }

    // TTS Speed
    val ttsSpeed: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[TTS_SPEED] ?: 1.0f
    }

    suspend fun setTtsSpeed(speed: Float) {
        context.dataStore.edit { prefs ->
            prefs[TTS_SPEED] = speed
        }
    }

    // Auto Read
    val autoReadResponses: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[AUTO_READ_RESPONSES] ?: false
    }

    suspend fun setAutoReadResponses(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[AUTO_READ_RESPONSES] = enabled
        }
    }

    // Local LLM Mode
    val isLocalMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_LOCAL_MODE] ?: false
    }

    suspend fun setLocalMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_LOCAL_MODE] = enabled
        }
    }

    // Local LLM Model Selection
    val localModelId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[LOCAL_MODEL_ID] // e.g., "qwen2.5-0.5b", "qwen2.5-1.5b" null means default
    }

    suspend fun setLocalModelId(modelId: String?) {
        context.dataStore.edit { prefs ->
            if (modelId != null) prefs[LOCAL_MODEL_ID] = modelId
            else prefs.remove(LOCAL_MODEL_ID)
        }
    }

    // Downloaded Models Track
    val downloadedModels: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[DOWNLOADED_MODELS] ?: emptySet()
    }

    suspend fun addDownloadedModel(modelId: String) {
        context.dataStore.edit { prefs ->
            val set = prefs[DOWNLOADED_MODELS]?.toMutableSet() ?: mutableSetOf()
            set.add(modelId)
            prefs[DOWNLOADED_MODELS] = set
        }
    }

    suspend fun removeDownloadedModel(modelId: String) {
        context.dataStore.edit { prefs ->
            val set = prefs[DOWNLOADED_MODELS]?.toMutableSet() ?: mutableSetOf()
            set.remove(modelId)
            prefs[DOWNLOADED_MODELS] = set
            if (prefs[LOCAL_MODEL_ID] == modelId) {
                prefs.remove(LOCAL_MODEL_ID)
            }
        }
    }

    // Private Mode
    val privateMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PRIVATE_MODE] ?: false
    }

    suspend fun setPrivateMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PRIVATE_MODE] = enabled
        }
    }

    // Root Access
    val useRoot: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[USE_ROOT] ?: false
    }

    suspend fun setUseRoot(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[USE_ROOT] = enabled
        }
    }

    // Stats
    val totalMessagesSent: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[TOTAL_MESSAGES_SENT] ?: 0
    }

    suspend fun incrementMessageCount() {
        context.dataStore.edit { prefs ->
            prefs[TOTAL_MESSAGES_SENT] = (prefs[TOTAL_MESSAGES_SENT] ?: 0) + 1
        }
    }

    // Clear all preferences
    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
