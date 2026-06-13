package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.repository.VidoraRepository

enum class MainScreen {
    Onboarding, Home, MediaDetails, Downloads, Library, Settings
}

@Composable
fun MainContainer(
    repository: VidoraRepository,
    activeTheme: String,
    onThemeChanged: (String) -> Unit
) {
    var isOnboardingCompleted by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf(MainScreen.Home) }
    
    // Custom robust state navigation parameters
    var detailsUrl by remember { mutableStateOf("") }
    var detailsProviderId by remember { mutableStateOf("") }

    // Screen Backstack Navigation Engine (Robust Fallback)
    val backStack = remember { mutableStateListOf(MainScreen.Home) }

    fun navigateTo(screen: MainScreen) {
        if (backStack.lastOrNull() != screen) {
            backStack.add(screen)
        }
        currentScreen = screen
    }

    fun navigateBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
            currentScreen = backStack.last()
        }
    }

    // Intercept hardware system back actions cleanly
    BackHandler(enabled = backStack.size > 1) {
        navigateBack()
    }

    if (!isOnboardingCompleted) {
        OnboardingScreen(onFinished = { isOnboardingCompleted = true })
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                // Render Bottom Bar if we are on global tabs (Home, Downloads, Library, Settings)
                if (currentScreen != MainScreen.Onboarding && currentScreen != MainScreen.MediaDetails) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = currentScreen == MainScreen.Home,
                            onClick = { navigateTo(MainScreen.Home) },
                            icon = { Icon(Icons.Default.Home, "Home Dashboard tab") },
                            label = { Text("Home") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        )
                        NavigationBarItem(
                            selected = currentScreen == MainScreen.Downloads,
                            onClick = { navigateTo(MainScreen.Downloads) },
                            icon = { Icon(Icons.Default.CloudDownload, "Downloads list tab") },
                            label = { Text("Downloads") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        )
                        NavigationBarItem(
                            selected = currentScreen == MainScreen.Library,
                            onClick = { navigateTo(MainScreen.Library) },
                            icon = { Icon(Icons.Default.FolderCopy, "Local Cached library tab") },
                            label = { Text("Library") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        )
                        NavigationBarItem(
                            selected = currentScreen == MainScreen.Settings,
                            onClick = { navigateTo(MainScreen.Settings) },
                            icon = { Icon(Icons.Default.Settings, "Preferences setting tab") },
                            label = { Text("Settings") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "CoreNavigationAnimation"
                ) { screen ->
                    when (screen) {
                        MainScreen.Home -> HomeScreen(
                            repository = repository,
                            onActiveBannerClicked = { navigateTo(MainScreen.Downloads) },
                            onNavigateToDetails = { url, providerId ->
                                detailsUrl = url
                                detailsProviderId = providerId
                                navigateTo(MainScreen.MediaDetails)
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                        MainScreen.MediaDetails -> MediaDetailsScreen(
                            url = detailsUrl,
                            providerId = detailsProviderId,
                            repository = repository,
                            onBack = { navigateBack() },
                            onDownloadEnqueued = {
                                // Once download is initialized, clear the details format backstack 
                                // and navigate directly to the downloads overview!
                                backStack.removeAt(backStack.lastIndex) // clear details screen
                                navigateTo(MainScreen.Downloads)
                            }
                        )
                        MainScreen.Downloads -> DownloadsScreen(
                            repository = repository,
                            modifier = Modifier.padding(innerPadding)
                        )
                        MainScreen.Library -> LibraryScreen(
                            repository = repository,
                            modifier = Modifier.padding(innerPadding)
                        )
                        MainScreen.Settings -> SettingsScreen(
                            repository = repository,
                            activeTheme = activeTheme,
                            onThemeChanged = onThemeChanged,
                            modifier = Modifier.padding(innerPadding)
                        )
                        else -> {
                            HomeScreen(
                                repository = repository,
                                onActiveBannerClicked = { navigateTo(MainScreen.Downloads) },
                                onNavigateToDetails = { _, _ -> },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}
