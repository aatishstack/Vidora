package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.data.database.VidoraDatabase
import com.example.data.repository.VidoraRepository
import com.example.download.DownloadEngine
import com.example.ui.screens.MainContainer
import com.example.ui.theme.VidoraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Core Dependency Orchestration
        val database = VidoraDatabase.getDatabase(applicationContext)
        val downloadDao = database.downloadDao()
        val downloadEngine = DownloadEngine(applicationContext, downloadDao)
        val repository = VidoraRepository(downloadDao, downloadEngine)

        setContent {
            var activeThemeId by remember { mutableStateOf("midnight") }

            VidoraTheme(themeId = activeThemeId) {
                MainContainer(
                    repository = repository,
                    activeTheme = activeThemeId,
                    onThemeChanged = { activeThemeId = it }
                )
            }
        }
    }
}
