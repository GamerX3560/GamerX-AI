package com.gamerx.ai.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gamerx.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About", fontWeight = FontWeight.Bold, color = TextPrimary) },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Logo
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(listOf(CyanPrimary, CyanDark))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "GX",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = DarkBackground
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "GamerX AI",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                "Version 1.0.0",
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary
            )

            Spacer(modifier = Modifier.height(32.dp))

            Surface(color = DarkCard, shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    InfoRow("Model", "Qwen 2.5 Coder 32B")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = DarkSurfaceVariant)
                    InfoRow("Developed by", "GamerX")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = DarkSurfaceVariant)
                    InfoRow("Platform", "Android")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = DarkSurfaceVariant)
                    InfoRow("UI Framework", "Jetpack Compose")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = DarkSurfaceVariant)
                    InfoRow("Architecture", "MVVM")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Powered by NVIDIA API\n\nBuilt with ❤️ by GamerX",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}
