package com.gamerx.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import com.gamerx.ai.navigation.AppNavigation
import com.gamerx.ai.ui.chat.ChatViewModel
import com.gamerx.ai.ui.chat.ChatViewModelFactory
import com.gamerx.ai.ui.theme.DarkBackground
import com.gamerx.ai.ui.theme.GamerXAITheme
import com.gamerx.ai.util.VoiceManager

import androidx.core.view.WindowCompat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks

class MainActivity : ComponentActivity() {

    private lateinit var chatViewModel: ChatViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        val app = application as GamerXAIApplication
        
        var isReady by mutableStateOf(false)
        var startDestination by mutableStateOf<String?>(null)

        lifecycleScope.launch {
            val hasSeen = app.preferences.hasSeenOnboarding.first()

            app.supabase.handleDeeplinks(intent)
            
            // Wait for session to be fully loaded from disk before deciding
            val status = app.supabase.auth.sessionStatus.first { it !is io.github.jan.supabase.auth.status.SessionStatus.Initializing }
            val isAuth = status is io.github.jan.supabase.auth.status.SessionStatus.Authenticated

            startDestination = when {
                !hasSeen -> "onboarding"
                !isAuth -> "login"
                else -> "chat"
            }
            isReady = true
        }

        splashScreen.setKeepOnScreenCondition { !isReady }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        val voiceManager = VoiceManager(this)

        chatViewModel = ViewModelProvider(
            this,
            ChatViewModelFactory(app.repository, app.preferences, voiceManager, app.supabase)
        )[ChatViewModel::class.java]

        setContent {
            if (isReady && startDestination != null) {
                GamerXAITheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = DarkBackground
                    ) {
                        val navController = rememberNavController()
                        AppNavigation(
                            navController = navController,
                            chatViewModel = chatViewModel,
                            preferences = app.preferences,
                            repository = app.repository,
                            startDestination = startDestination!!
                        )
                    }
                }
            }
        }
    }
}
