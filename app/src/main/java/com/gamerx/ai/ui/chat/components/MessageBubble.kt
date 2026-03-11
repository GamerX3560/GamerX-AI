package com.gamerx.ai.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.gamerx.ai.data.db.entities.Message
import com.gamerx.ai.ui.theme.*
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography

@Composable
fun MessageBubble(
    message: Message,
    isStreaming: Boolean = false,
    onSpeak: ((String) -> Unit)? = null,
    onShare: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (isUser) {
            // User message: subtle dark gray pill, right-aligned
            Box(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp))
                    .background(UserBubbleColor)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
            }
        } else {
            // AI message: rendered as Markdown with MikePenz
            SelectionContainer {
                Markdown(
                    content = message.content,
                    colors = markdownColor(
                        text = TextPrimary,
                        codeText = TextPrimary,
                        codeBackground = DarkSurfaceVariant,
                        linkText = CyanPrimary
                    ),
                    typography = markdownTypography(
                        text = MaterialTheme.typography.bodyLarge,
                        code = MaterialTheme.typography.bodyMedium
                    ),
                    modifier = Modifier.fillMaxWidth().padding(end = 16.dp)
                )
            }

            // Action buttons for AI messages
            if (!isStreaming) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { clipboardManager.setText(AnnotatedString(message.content)) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (onSpeak != null) {
                        IconButton(
                            onClick = { onSpeak(message.content) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = "Speak",
                                tint = TextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (onShare != null) {
                        IconButton(
                            onClick = { onShare(message.content) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Share",
                                tint = TextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
