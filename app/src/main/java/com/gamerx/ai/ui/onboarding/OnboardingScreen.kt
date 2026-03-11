package com.gamerx.ai.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamerx.ai.data.preferences.UserPreferences
import com.gamerx.ai.ui.theme.*
import androidx.compose.foundation.ExperimentalFoundationApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    preferences: UserPreferences,
    onComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 5 }) // Increased from 3
    
    val pages = listOf(
        OnboardingPageData(
            title = "Welcome to\nGamerX AI",
            description = "Your intelligent companion for coding, writing, and brainstorming. Powered by state-of-the-art AI.",
            iconText = "GX"
        ),
        OnboardingPageData(
            title = "Voice Enabled",
            description = "Speak directly to GamerX AI and get spoken responses back. Hands-free AI assistance.",
            iconText = "🎤"
        ),
        OnboardingPageData(
            title = "Code & Terminals",
            description = "Natively formats code blocks with syntax highlighting like a real IDE. Copy snippets in one tap.",
            iconText = "💻"
        ),
        OnboardingPageData(
            title = "Offline LLM Mode",
            description = "Download Gemma 2B to your device to use the AI completely offline. No internet required.",
            iconText = "📶"
        ),
        OnboardingPageData(
            title = "Total Privacy",
            description = "Your data stays yours. Use Private Chat mode to have unsaved, incognito conversations.",
            iconText = "🔒"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(top = 24.dp, bottom = 24.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            OnboardingPage(pageData = pages[page])
        }

        // Indicators & Buttons
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page Indicators
            Row(
                Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pagerState.pageCount) { iteration ->
                    val isSelected = pagerState.currentPage == iteration
                    val color = if (isSelected) CyanPrimary else DarkSurfaceVariant
                    val width = if (isSelected) 24.dp else 8.dp
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(color)
                            .height(8.dp)
                            .width(width)
                    )
                }
            }

            // Next / Get Started Button
            Button(
                onClick = {
                    if (pagerState.currentPage < 4) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        scope.launch {
                            preferences.setOnboardingCompleted(true)
                            onComplete()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimary,
                    contentColor = DarkBackground
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    if (pagerState.currentPage == 4) "Get Started" else "Next",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (pagerState.currentPage == 4) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

data class OnboardingPageData(
    val title: String,
    val description: String,
    val iconText: String
)

@Composable
fun OnboardingPage(pageData: OnboardingPageData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Large Icon Box
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(CyanPrimary, CyanDark)
                    ),
                    shape = RoundedCornerShape(32.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = pageData.iconText,
                fontSize = if (pageData.iconText == "GX") 54.sp else 48.sp,
                fontWeight = FontWeight.Bold,
                color = DarkBackground
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = pageData.title,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 44.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = pageData.description,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}
