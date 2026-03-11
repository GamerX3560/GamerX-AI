package com.gamerx.ai.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gamerx.ai.data.preferences.UserPreferences
import com.gamerx.ai.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import android.app.DownloadManager
import android.net.Uri
import android.content.Context
import android.database.Cursor
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalModelScreen(
    preferences: UserPreferences,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val isLocalMode by preferences.isLocalMode.collectAsState(initial = false)
    val localModelId by preferences.localModelId.collectAsState(initial = "qwen_0.8b")
    val downloadedModels by preferences.downloadedModels.collectAsState(initial = emptySet())

    var downloadProgresses by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var downloadTexts by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val context = LocalContext.current
    val downloadManager = remember { context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager }

    val models = listOf(
        Pair("qwen_0.8b", "Qwen 2.5 0.5B (Fast, Low RAM)"),
        Pair("qwen_2b", "Qwen 2.5 1.5B (Balanced)"),
        Pair("qwen_4b", "Qwen 2.5 3B (High Quality, 4GB+ RAM)")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Local On-Device AI", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
    
        // DownloadManager Polling Loop
        LaunchedEffect(Unit) {
            while (true) {
                val query = DownloadManager.Query().setFilterByStatus(
                    DownloadManager.STATUS_RUNNING or DownloadManager.STATUS_PENDING or DownloadManager.STATUS_SUCCESSFUL
                )
                val cursor = downloadManager.query(query)
                val newProgresses = mutableMapOf<String, Float>()
                val newTexts = mutableMapOf<String, String>()
                var missingDownloads = setOf("qwen_0.8b", "qwen_2b", "qwen_4b")
                
                if (cursor != null) {
                    val titleIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
                    val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val bytesIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val totalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    
                    while (cursor.moveToNext()) {
                        if (titleIdx < 0) continue
                        val title = cursor.getString(titleIdx) ?: continue
                        
                        val matchedId = models.find { title.contains(it.first) }?.first
                        if (matchedId != null) {
                            missingDownloads = missingDownloads - matchedId
                            val status = cursor.getInt(statusIdx)
                            val bytes = cursor.getLong(bytesIdx)
                            val total = cursor.getLong(totalIdx)
                            
                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                preferences.addDownloadedModel(matchedId)
                            } else if (status == DownloadManager.STATUS_RUNNING || status == DownloadManager.STATUS_PENDING) {
                                if (total > 0) {
                                    newProgresses[matchedId] = bytes.toFloat() / total.toFloat()
                                    newTexts[matchedId] = "${bytes / (1024 * 1024)}MB / ${total / (1024 * 1024)}MB"
                                } else {
                                    newProgresses[matchedId] = 0f
                                    newTexts[matchedId] = "Pending..."
                                }
                            }
                        }
                    }
                    cursor.close()
                }
                
                // Double check if file exists for safety on missing statuses
                missingDownloads.forEach { id ->
                    val dir = context.getExternalFilesDir(null) ?: context.filesDir
                    val file = java.io.File(dir, "$id.gguf")
                    if (file.exists() && file.length() > 100 * 1024 * 1024) { // Basic sanity check >100MB
                        preferences.addDownloadedModel(id)
                    }
                }
                
                downloadProgresses = newProgresses
                downloadTexts = newTexts
                delay(1000)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkCard,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Local Mode", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("Run AI 100% offline on your device", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                    }
                    Switch(
                        checked = isLocalMode,
                        onCheckedChange = { 
                            coroutineScope.launch { preferences.setLocalMode(it) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyanPrimary,
                            checkedTrackColor = CyanPrimary.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Model Selection",
                style = MaterialTheme.typography.labelLarge,
                color = CyanPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            models.forEach { (id, name) ->
                val isSelected = localModelId == id && downloadedModels.contains(id)
                val isDownloaded = downloadedModels.contains(id)
                val isDownloading = downloadProgresses.containsKey(id)

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(enabled = isDownloaded && !isDownloading) {
                            coroutineScope.launch { preferences.setLocalModelId(id) }
                        },
                    color = if (isSelected) CyanPrimary.copy(alpha = 0.15f) else DarkCard,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { coroutineScope.launch { preferences.setLocalModelId(id) } },
                                colors = RadioButtonDefaults.colors(selectedColor = CyanPrimary),
                                enabled = isDownloaded
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = name, 
                                style = MaterialTheme.typography.bodyLarge, 
                                color = if (isDownloaded) TextPrimary else TextSecondary, 
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            // Download / Delete actions
                            if (isDownloaded) {
                                IconButton(onClick = { 
                                    coroutineScope.launch { 
                                        val dir = context.getExternalFilesDir(null) ?: context.filesDir
                                        val destFile = java.io.File(dir, "$id.gguf")
                                        if (destFile.exists()) destFile.delete()
                                        preferences.removeDownloadedModel(id) 
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Model", tint = ErrorRed)
                                }
                            } else if (isDownloading) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${((downloadProgresses[id] ?: 0f) * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = CyanPrimary)
                                    Text(downloadTexts[id] ?: "Starting...", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                }
                            } else {
                                IconButton(onClick = {
                                    val url = when(id) {
                                        "qwen_0.8b" -> "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf"
                                        "qwen_2b" -> "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf"
                                        "qwen_4b" -> "https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/main/qwen2.5-3b-instruct-q4_k_m.gguf"
                                        else -> ""
                                    }
                                    if (url.isNotEmpty()) {
                                        val request = DownloadManager.Request(Uri.parse(url))
                                            .setTitle("GamerX AI Model ($id)")
                                            .setDescription("Downloading Background Model File")
                                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                            .setAllowedOverMetered(true)
                                            .setDestinationInExternalFilesDir(context, null, "$id.gguf")
                                        downloadManager.enqueue(request)
                                    }
                                }) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = "Download Model", tint = CyanPrimary)
                                }
                            }
                        }
                        if (isDownloading) {
                            LinearProgressIndicator(
                                progress = { downloadProgresses[id] ?: 0f },
                                modifier = Modifier.fillMaxWidth().height(2.dp),
                                color = CyanPrimary,
                                trackColor = DarkSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Note: Models will be downloaded directly from HuggingFace to your app's internal secure storage (.gguf format).",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}
