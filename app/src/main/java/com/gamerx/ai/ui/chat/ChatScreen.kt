package com.gamerx.ai.ui.chat

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamerx.ai.MainActivity
import com.gamerx.ai.ui.chat.components.*
import com.gamerx.ai.ui.theme.*
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val isListening by viewModel.voiceManager.isListening.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Do nothing on grant except update state. 
        // User must tap the mic again to actually start recording.
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Auto-scroll
    LaunchedEffect(uiState.messages.size, uiState.streamingText) {
        if (uiState.messages.isNotEmpty() || uiState.streamingText.isNotEmpty()) {
            listState.animateScrollToItem(
                (uiState.messages.size + if (uiState.isStreaming) 1 else 0).coerceAtLeast(0)
            )
        }
    }

    // Sync drawer
    LaunchedEffect(uiState.isDrawerOpen) {
        if (uiState.isDrawerOpen) drawerState.open() else drawerState.close()
    }
    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.currentValue == DrawerValue.Closed && uiState.isDrawerOpen) {
            viewModel.closeDrawer()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ChatDrawer(
                conversations = uiState.conversations,
                currentConversationId = uiState.currentConversationId,
                aiName = uiState.aiName,
                userName = uiState.userName,
                userEmail = uiState.userEmail,
                userProfilePicUrl = uiState.userProfilePicUrl,
                isPrivateMode = uiState.isPrivateMode,
                onNewChat = { viewModel.createNewChat() },
                onSelectConversation = { viewModel.loadConversation(it) },
                onDeleteConversation = { viewModel.deleteConversation(it) },
                onShareConversation = { conversation ->
                    scope.launch {
                        val text = viewModel.getConversationShareText(conversation.id)
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, text)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Transcript"))
                    }
                },
                onDeleteAll = { viewModel.deleteAllConversations() },
                onSettings = {
                    scope.launch { drawerState.close() }
                    onNavigateToSettings()
                },
                onStartPrivateChat = { viewModel.startPrivateChat() },
                onLogout = {
                    viewModel.logout {
                        // Restart activity or clear stack to simulate logout
                        val intent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        context.startActivity(intent)
                    }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = uiState.aiName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            // Show lock icon if current conversation is private
                            if (uiState.currentConversationIsPrivate) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Private",
                                    tint = WarningAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                            viewModel.toggleDrawer()
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = TextPrimary)
                        }
                    },
                    actions = {
                        val isLocal = uiState.isLocalMode
                        IconButton(onClick = { viewModel.toggleLocalMode() }) {
                            Icon(
                                if (isLocal) Icons.Default.CloudOff else Icons.Default.Cloud, 
                                contentDescription = "Toggle Local Search", 
                                tint = if (isLocal) CyanPrimary else TextTertiary
                            )
                        }
                        IconButton(onClick = { viewModel.createNewChat() }) {
                            Icon(Icons.Default.Add, contentDescription = "New Chat", tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = DarkBackground,
                        scrolledContainerColor = DarkBackground
                    )
                )
            },
            containerColor = DarkBackground,
            bottomBar = {
                ChatInput(
                    onSendMessage = { 
                        if (uiState.currentConversationId == null) {
                            viewModel.createNewChatWithMessage(it)
                        } else {
                            viewModel.sendMessage(it) 
                        }
                    },
                    onVoiceInput = {
                        if (isListening) {
                            viewModel.voiceManager.stopListening()
                        } else {
                            val permissionCheckResult = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                            if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                                viewModel.voiceManager.startListening { text ->
                                    if (text.isNotBlank()) {
                                        if (uiState.currentConversationId == null) {
                                            viewModel.createNewChatWithMessage(text)
                                        } else {
                                            viewModel.sendMessage(text)
                                        }
                                    }
                                }
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    isLoading = uiState.isLoading || uiState.isStreaming,
                    isListening = isListening,
                    modifier = Modifier.imePadding()
                )
            }
        ) { innerPadding ->
            if (uiState.currentConversationId == null) {
                WelcomeScreen(
                    userName = uiState.userName,
                    onSuggestionClick = { suggestion ->
                        viewModel.createNewChatWithMessage(suggestion)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            onSpeak = { viewModel.voiceManager.speak(it) },
                            onShare = { text ->
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, text)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share"))
                            }
                        )
                    }

                    // Streaming response
                    if (uiState.isStreaming && uiState.streamingText.isNotEmpty()) {
                        item {
                            MessageBubble(
                                message = com.gamerx.ai.data.db.entities.Message(
                                    conversationId = uiState.currentConversationId ?: "",
                                    role = "assistant",
                                    content = uiState.streamingText + "▌"
                                ),
                                isStreaming = true
                            )
                        }
                    }

                    // Typing indicator
                    if (uiState.isLoading && uiState.streamingText.isEmpty()) {
                        item { TypingIndicator() }
                    }
                }
            }

            // Error snackbar
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss", color = TextSecondary)
                        }
                    },
                    containerColor = DarkCard,
                    contentColor = ErrorRed
                ) { Text(error) }
            }
        }
    }
}

@Composable
private fun WelcomeScreen(
    userName: String,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Hi $userName",
            style = MaterialTheme.typography.headlineSmall,
            color = TextSecondary,
            fontWeight = FontWeight.Normal
        )
        Text(
            text = "Where should we start?",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Suggestion chips
        val suggestions = listOf(
            "✨ Help me write an email",
            "💻 Explain a coding concept",
            "📝 Draft a quick summary",
            "💡 Brainstorm new ideas"
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            suggestions.forEach { suggestion ->
                val cleanText = suggestion.substringAfter(" ")
                Surface(
                    onClick = { onSuggestionClick(cleanText) },
                    shape = RoundedCornerShape(24.dp),
                    color = DarkSurfaceVariant,
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}
