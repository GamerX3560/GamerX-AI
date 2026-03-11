package com.gamerx.ai.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gamerx.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToUserInfo: () -> Unit,
    onNavigateToVoice: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToLocalModels: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // Profile section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkCard,
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(listOf(CyanPrimary, CyanDark))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("GX", fontWeight = FontWeight.Bold, color = DarkBackground,
                            style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "GamerX AI",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "v1.0.0 • Qwen 2.5 Coder 32B",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Settings categories
            Text(
                "General",
                style = MaterialTheme.typography.labelLarge,
                color = CyanPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            SettingsCard {
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = "User Info",
                    subtitle = "Your name & profile",
                    onClick = onNavigateToUserInfo
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Features",
                style = MaterialTheme.typography.labelLarge,
                color = CyanPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            SettingsCard {
                SettingsItem(
                    icon = Icons.Default.RecordVoiceOver,
                    title = "Voice",
                    subtitle = "Text-to-speech settings",
                    onClick = onNavigateToVoice
                )
                HorizontalDivider(color = TextTertiary.copy(alpha = 0.2f))
                SettingsItem(
                    icon = Icons.Default.CloudDownload,
                    title = "Local LLM",
                    subtitle = "On-device AI engine",
                    onClick = onNavigateToLocalModels
                )
                HorizontalDivider(color = TextTertiary.copy(alpha = 0.2f))
                
                var showRootErr by remember { mutableStateOf(false) }
                val coroutineScope = rememberCoroutineScope()
                SettingsItem(
                    icon = Icons.Default.AdminPanelSettings,
                    title = "Root Access",
                    subtitle = "Enable advanced system features",
                    onClick = {
                        try {
                            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo root"))
                            val exitValue = process.waitFor()
                            if (exitValue != 0) showRootErr = true
                        } catch(e: Exception) {
                            showRootErr = true
                        }
                    },
                    trailing = {
                        Switch(
                            checked = false, 
                            onCheckedChange = { 
                                try {
                                    val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo root"))
                                    val exitValue = process.waitFor()
                                    if (exitValue != 0) showRootErr = true
                                } catch(e: Exception) {
                                    showRootErr = true
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyanPrimary,
                                checkedTrackColor = CyanPrimary.copy(alpha = 0.5f)
                            )
                        )
                    }
                )
                if (showRootErr) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showRootErr = false },
                        title = { Text("Root Denied") },
                        text = { Text("Device is not rooted or su permission denied.") },
                        confirmButton = { TextButton(onClick = { showRootErr = false }) { Text("OK") } }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Other",
                style = MaterialTheme.typography.labelLarge,
                color = CyanPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            SettingsCard {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "About",
                    subtitle = "Version, credits & info",
                    onClick = onNavigateToAbout
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DarkCard,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CyanPrimary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
