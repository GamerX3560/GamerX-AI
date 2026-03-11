package com.gamerx.ai.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gamerx.ai.data.preferences.UserPreferences
import com.gamerx.ai.data.repository.ChatRepository
import com.gamerx.ai.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataControlScreen(
    preferences: UserPreferences,
    repository: ChatRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val totalMessages by preferences.totalMessagesSent.collectAsState(initial = 0)
    var showClearDialog by remember { mutableStateOf(false) }
    var conversationCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        conversationCount = repository.getConversationCount()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data Control", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Stats
            Text("Usage Stats", style = MaterialTheme.typography.labelLarge,
                color = CyanPrimary, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))

            Surface(color = DarkCard, shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    StatRow("Total Messages Sent", totalMessages.toString())
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = DarkSurfaceVariant)
                    StatRow("Conversations", conversationCount.toString())
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Danger zone
            Text("Danger Zone", style = MaterialTheme.typography.labelLarge,
                color = ErrorRed, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))

            Surface(color = DarkCard, shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsItem(
                        icon = Icons.Default.DeleteForever,
                        title = "Clear All Conversations",
                        subtitle = "Permanently delete all chat history",
                        onClick = { showClearDialog = true }
                    )
                    HorizontalDivider(color = DarkSurfaceVariant)
                    SettingsItem(
                        icon = Icons.Default.RestartAlt,
                        title = "Reset All Settings",
                        subtitle = "Restore defaults for all preferences",
                        onClick = {
                            scope.launch {
                                preferences.clearAll()
                            }
                        }
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Data", color = TextPrimary) },
            text = {
                Text(
                    "This will permanently delete all conversations and cannot be undone.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repository.deleteAllConversations()
                        conversationCount = 0
                        showClearDialog = false
                    }
                }) {
                    Text("Delete Everything", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = CyanPrimary)
                }
            },
            containerColor = DarkCard,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = CyanPrimary, fontWeight = FontWeight.Bold)
    }
}
