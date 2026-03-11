package com.gamerx.ai

import android.app.Application
import com.gamerx.ai.data.api.NvidiaService
import com.gamerx.ai.data.db.AppDatabase
import com.gamerx.ai.data.preferences.UserPreferences
import com.gamerx.ai.data.repository.ChatRepository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.appleNativeLogin
import io.github.jan.supabase.compose.auth.googleNativeLogin
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

class GamerXAIApplication : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var repository: ChatRepository
        private set
    lateinit var preferences: UserPreferences
        private set
    lateinit var supabase: SupabaseClient
        private set

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getInstance(this)
        preferences = UserPreferences(this)

        supabase = createSupabaseClient(
            supabaseUrl = "https://zemeprnemejxnrlqjbwd.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InplbWVwcm5lbWVqeG5ybHFqYndkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzI3NjIwMDgsImV4cCI6MjA4ODMzODAwOH0.DFs5_3KuOCH-9eZfnRPydvlMOldqsAn1GtTcnwAgRck"
        ) {
            install(Postgrest)
            install(Auth) {
                scheme = "gamerx"
                host = "login-callback"
            }
            install(ComposeAuth) {
                appleNativeLogin()
                googleNativeLogin(serverClientId = "428146682044-6pburs08cgdbndl8a90ok5b6etf174ms.apps.googleusercontent.com")
            }
        }

        val nvidiaService = NvidiaService()
        repository = ChatRepository(
            nvidiaService = nvidiaService,
            conversationDao = database.conversationDao(),
            messageDao = database.messageDao(),
            preferences = preferences,
            context = this,
            supabase = supabase
        )
    }
}
