package com.gamerx.ai.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.gamerx.ai.data.preferences.UserPreferences
import com.gamerx.ai.data.repository.ChatRepository
import com.gamerx.ai.ui.chat.ChatScreen
import com.gamerx.ai.ui.chat.ChatViewModel
import com.gamerx.ai.ui.settings.*
import com.gamerx.ai.ui.auth.LoginScreen
import androidx.compose.ui.platform.LocalContext
import com.gamerx.ai.GamerXAIApplication

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Chat : Screen("chat")
    data object Settings : Screen("settings")
    data object UserInfo : Screen("settings/user_info")
    data object VoiceSettings : Screen("settings/voice")
    data object LocalModels : Screen("settings/local_models")
    data object About : Screen("settings/about")
    data object Onboarding : Screen("onboarding")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    chatViewModel: ChatViewModel,
    preferences: UserPreferences,
    repository: ChatRepository,
    startDestination: String = Screen.Chat.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            com.gamerx.ai.ui.onboarding.OnboardingScreen(
                preferences = preferences,
                onComplete = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Login.route) {
            val app = LocalContext.current.applicationContext as GamerXAIApplication
            LoginScreen(
                supabase = app.supabase,
                onLoginSuccess = {
                    navController.navigate(Screen.Chat.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Chat.route) {
            ChatScreen(
                viewModel = chatViewModel,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToUserInfo = {
                    navController.navigate(Screen.UserInfo.route)
                },
                onNavigateToVoice = {
                    navController.navigate(Screen.VoiceSettings.route)
                },
                onNavigateToAbout = {
                    navController.navigate(Screen.About.route)
                },
                onNavigateToLocalModels = {
                    navController.navigate(Screen.LocalModels.route)
                }
            )
        }

        composable(Screen.VoiceSettings.route) {
            VoiceSettingsScreen(
                preferences = preferences,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.UserInfo.route) {
            UserInfoScreen(
                preferences = preferences,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.LocalModels.route) {
            LocalModelScreen(
                preferences = preferences,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
