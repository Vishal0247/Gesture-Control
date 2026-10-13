package com.example.gesturecontrol

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.gesturecontrol.ui.screens.*
import com.example.gesturecontrol.ui.theme.GestureControlTheme
import com.example.gesturecontrol.ui.theme.PrimaryTeal
import com.example.gesturecontrol.ui.theme.TextSecondary
import com.example.gesturecontrol.ui.theme.Background

@Composable
fun MainApp(
    isAccessibilityEnabled: Boolean,
    hasCameraPermission: Boolean,
    hasCallPermission: Boolean,
    onRequestCamera: () -> Unit,
    onRequestCall: () -> Unit,
    overlayEnabled: Boolean,
    onOverlayToggled: (Boolean) -> Unit,
    showOnboarding: Boolean,
    onOnboardingFinished: () -> Unit
) {
    GestureControlTheme {
        if (showOnboarding) {
            OnboardingScreen(onFinish = onOnboardingFinished)
            return@GestureControlTheme
        }

        val navController = rememberNavController()
        Scaffold(
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                NavigationBar(
                    containerColor = Background,
                    contentColor = PrimaryTeal
                ) {
                    NavigationBarItem(
                        selected = currentRoute == "home",
                        onClick = { navController.navigate("home") { launchSingleTop = true; popUpTo("home") } },
                        icon = { Text("Home", color = if (currentRoute == "home") PrimaryTeal else TextSecondary) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "gestures",
                        onClick = { navController.navigate("gestures") { launchSingleTop = true; popUpTo("home") } },
                        icon = { Text("Gestures", color = if (currentRoute == "gestures") PrimaryTeal else TextSecondary) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "settings",
                        onClick = { navController.navigate("settings") { launchSingleTop = true; popUpTo("home") } },
                        icon = { Text("Settings", color = if (currentRoute == "settings") PrimaryTeal else TextSecondary) }
                    )
                }
            },
            containerColor = Background
        ) { innerPadding ->
            NavHost(navController = navController, startDestination = "home", modifier = Modifier.padding(innerPadding)) {
                composable("home") {
                    HomeScreen(isAccessibilityEnabled, hasCameraPermission, hasCallPermission, onRequestCamera, onRequestCall)
                }
                composable("gestures") {
                    GesturesScreen()
                }
                composable("settings") {
                    SettingsScreen(overlayEnabled, onOverlayToggled)
                }
            }
        }
    }
}
