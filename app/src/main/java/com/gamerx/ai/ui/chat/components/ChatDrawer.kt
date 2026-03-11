package com.gamerx.ai.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gamerx.ai.data.db.entities.Conversation
import com.gamerx.ai.ui.theme.*

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ChatDrawer(
    conversations: List<Conversation>,
    currentConversationId: String?,
    aiName: String,
    userName: String,
    userEmail: String?,
    userProfilePicUrl: String?,
    isPrivateMode: Boolean,
    onNewChat: () -> Unit,
    onSelectConversation: (String) -> Unit,
    onDeleteConversation: (Conversation) -> Unit,
    onShareConversation: (Conversation) -> Unit,
    onDeleteAll: () -> Unit,
    onSettings: () -> Unit,
    onStartPrivateChat: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredConversations = if (searchQuery.isBlank()) conversations
    else conversations.filter { it.title.contains(searchQuery, ignoreCase = true) }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(DarkSurface)
            .statusBarsPadding()
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Search bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(DarkSurfaceVariant)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text("Search chats", color = TextTertiary, style = MaterialTheme.typography.bodyMedium)
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                        cursorBrush = SolidColor(CyanPrimary),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // New Chat row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNewChat() }
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Edit,
                contentDescription = "New Chat",
                tint = TextPrimary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                "New chat",
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }

        // Private Chat row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onStartPrivateChat() }
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Lock,
                contentDescription = "Private Chat",
                tint = TextPrimary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                "Private chat",
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }

        HorizontalDivider(color = DarkSurfaceVariant, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

        // Chats label
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Chats",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
            if (conversations.isNotEmpty()) {
                IconButton(
                    onClick = { showDeleteAllDialog = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = "Delete all",
                        tint = TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Conversation list
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            if (filteredConversations.isEmpty()) {
                item {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No results" else "No chats yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }

            items(filteredConversations, key = { it.id }) { conversation ->
                val isSelected = conversation.id == currentConversationId
                var showMenu by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) DarkSurfaceVariant else DarkSurface)
                            .combinedClickable(
                                onClick = { onSelectConversation(conversation.id) },
                                onLongClick = { showMenu = true }
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = conversation.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (conversation.isPrivate) {
                            Icon(
                                Icons.Outlined.Lock,
                                contentDescription = "Private",
                                tint = TextTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(DarkCard)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Share Transcript", color = TextPrimary) },
                            onClick = { 
                                showMenu = false
                                onShareConversation(conversation)
                            },
                            leadingIcon = { Icon(Icons.Default.Share, null, tint = CyanPrimary) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Chat", color = ErrorRed) },
                            onClick = { 
                                showMenu = false
                                onDeleteConversation(conversation)
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = ErrorRed) }
                        )
                    }
                }
            }
        }

        // Bottom: Settings & Profile
        HorizontalDivider(color = DarkSurfaceVariant, thickness = 0.5.dp)
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSettings() }
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Text("Settings", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (userProfilePicUrl != null) {
                AsyncImage(
                    model = userProfilePicUrl,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.size(36.dp).clip(androidx.compose.foundation.shape.CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier.size(36.dp).clip(androidx.compose.foundation.shape.CircleShape).background(DarkSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(userName.take(1).uppercase(), color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(userName, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (userEmail != null) {
                    Text(userEmail, style = MaterialTheme.typography.bodySmall, color = TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            IconButton(onClick = onLogout, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
        }
    }

    // Delete all dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Delete All Chats", color = TextPrimary) },
            text = { Text("This will permanently delete all conversations.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { onDeleteAll(); showDeleteAllDialog = false }) {
                    Text("Delete All", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
