package com.example

import android.app.PictureInPictureParams
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.NovaStreamTheme
import com.example.ui.viewmodel.IPTVViewModel
import com.example.data.sync.EpgSyncWorker

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: IPTVViewModel
    private val isInPipModeState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this)[IPTVViewModel::class.java]

        // Schedule periodic background EPG synchronization
        EpgSyncWorker.schedule(applicationContext)

        setContent {
            val currentThemeMode by viewModel.themeMode.collectAsState()
            val currentThemeColor by viewModel.themeColor.collectAsState()
            val isInPipMode by isInPipModeState

            NovaStreamTheme(
                themeMode = currentThemeMode,
                themeColor = currentThemeColor
            ) {
                var currentScreen by rememberSaveable { mutableStateOf("dashboard") }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (!isInPipMode) {
                            NavigationBar(
                                modifier = Modifier.testTag("bottom_nav"),
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp
                            ) {
                                NavigationBarItem(
                                    selected = currentScreen == "dashboard",
                                    onClick = { currentScreen = "dashboard" },
                                    icon = { Icon(imageVector = Icons.Default.Tv, contentDescription = "Streams") },
                                    label = { Text("Live Streams") },
                                    modifier = Modifier.testTag("nav_dashboard"),
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentScreen == "playlists",
                                    onClick = { currentScreen = "playlists" },
                                    icon = { Icon(imageVector = Icons.Default.List, contentDescription = "Playlists") },
                                    label = { Text("Playlists") },
                                    modifier = Modifier.testTag("nav_playlists"),
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentScreen == "settings",
                                    onClick = { currentScreen = "settings" },
                                    icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
                                    label = { Text("Settings") },
                                    modifier = Modifier.testTag("nav_settings"),
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    if (isInPipMode) {
                        DashboardScreen(
                            viewModel = viewModel,
                            isInPipMode = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        when (currentScreen) {
                            "dashboard" -> DashboardScreen(
                                viewModel = viewModel,
                                isInPipMode = false,
                                modifier = Modifier.padding(innerPadding)
                            )
                            "playlists" -> PlaylistsScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                            "settings" -> SettingsScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipModeState.value = isInPictureInPictureMode
    }

    @Deprecated("Deprecated in Java")
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        isInPipModeState.value = isInPictureInPictureMode
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Automatically enter Picture-in-Picture mode if a stream is playing
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && viewModel.currentChannel.value != null) {
            try {
                val params = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    PictureInPictureParams.Builder()
                        .setAspectRatio(Rational(16, 9))
                        .build()
                } else {
                    PictureInPictureParams.Builder().build()
                }
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                // Ignore if PiP fails to initialize
            }
        }
    }
}
