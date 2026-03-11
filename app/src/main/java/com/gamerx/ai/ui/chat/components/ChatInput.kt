package com.gamerx.ai.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.gamerx.ai.ui.theme.*

@Composable
fun ChatInput(
    onSendMessage: (String) -> Unit,
    onVoiceInput: () -> Unit,
    isLoading: Boolean,
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkBackground)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(color = DarkSurfaceVariant, thickness = 0.5.dp)

        // Text field row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Input field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp, max = 150.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(DarkSurfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (textFieldValue.text.isEmpty()) {
                    Text(
                        text = "Ask GamerX AI",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextTertiary
                    )
                }
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                    cursorBrush = SolidColor(CyanPrimary),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
            }

            // Send / Mic button
            if (textFieldValue.text.isNotBlank()) {
                IconButton(
                    onClick = {
                        onSendMessage(textFieldValue.text)
                        textFieldValue = TextFieldValue("")
                        keyboardController?.hide()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CyanPrimary),
                    enabled = !isLoading
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = DarkBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onVoiceInput,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isListening) ErrorRed else DarkSurfaceVariant),
                    enabled = !isLoading
                ) {
                    Icon(
                        if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Voice",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
