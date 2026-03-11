package com.gamerx.ai.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamerx.ai.R
import com.gamerx.ai.ui.theme.*
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.compose.auth.composable.NativeSignInResult
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithGoogle
import io.github.jan.supabase.compose.auth.composeAuth
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    supabase: io.github.jan.supabase.SupabaseClient,
    onLoginSuccess: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val action = supabase.composeAuth.rememberSignInWithGoogle(
        onResult = { result ->
            when (result) {
                is NativeSignInResult.Success -> {
                    isLoading = false
                    onLoginSuccess()
                }
                is NativeSignInResult.NetworkError -> {
                    isLoading = false
                    errorMessage = "Network Error: Please check your connection."
                }
                is NativeSignInResult.Error -> {
                    isLoading = false
                    errorMessage = "Login Error: ${result.message}"
                }
                is NativeSignInResult.ClosedByUser -> {
                    isLoading = false
                    errorMessage = "Login cancelled."
                }
            }
        },
        fallback = {
            scope.launch {
                try {
                    supabase.auth.signInWith(io.github.jan.supabase.auth.providers.Google)
                } catch (e: Exception) {
                    isLoading = false
                    errorMessage = "Web Login Error: ${e.message}"
                }
            }
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "GamerX AI",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Sign in to synchronize your chats across devices using Supabase.",
                fontSize = 16.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))

            if (isLoading) {
                CircularProgressIndicator(color = CyanPrimary)
            } else {
                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        action.startFlow()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Continue with Google", color = TextPrimary, fontSize = 16.sp)
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage!!,
                    color = androidx.compose.ui.graphics.Color.Red,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
