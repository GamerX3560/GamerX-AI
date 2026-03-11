package com.gamerx.ai.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gamerx.ai.data.preferences.UserPreferences
import com.gamerx.ai.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsScreen(
    preferences: UserPreferences,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val ttsEnabled by preferences.ttsEnabled.collectAsState(initial = false)
    val ttsSpeed by preferences.ttsSpeed.collectAsState(initial = 1.0f)
    val autoRead by preferences.autoReadResponses.collectAsState(initial = false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Settings", fontWeight = FontWeight.Bold, color = TextPrimary) },
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
            Text("Text-to-Speech", style = MaterialTheme.typography.labelLarge,
                color = CyanPrimary, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))

            Surface(color = DarkCard, shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Enable TTS", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            Text("Read AI responses aloud", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                        }
                        Switch(
                            checked = ttsEnabled,
                            onCheckedChange = { scope.launch { preferences.setTtsEnabled(it) } },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyanPrimary,
                                checkedTrackColor = CyanPrimary.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextTertiary,
                                uncheckedTrackColor = DarkSurfaceVariant
                            )
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DarkSurfaceVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Auto-Read Responses", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            Text("Automatically read new responses", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                        }
                        Switch(
                            checked = autoRead,
                            onCheckedChange = { scope.launch { preferences.setAutoReadResponses(it) } },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyanPrimary,
                                checkedTrackColor = CyanPrimary.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextTertiary,
                                uncheckedTrackColor = DarkSurfaceVariant
                            )
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DarkSurfaceVariant)

                    Text("Speech Speed", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                    Text("${String.format("%.1f", ttsSpeed)}x", style = MaterialTheme.typography.bodySmall, color = CyanPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = ttsSpeed,
                        onValueChange = { scope.launch { preferences.setTtsSpeed(it) } },
                        valueRange = 0.5f..2.0f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanPrimary,
                            activeTrackColor = CyanPrimary,
                            inactiveTrackColor = DarkSurfaceVariant
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0.5x", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                        Text("2.0x", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                    }
                }
            }
        }
    }
}
