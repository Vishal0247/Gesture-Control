package com.example.gesturecontrol

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {

    private var permissionGranted by mutableStateOf(false)
    private var isAccessibilityEnabled by mutableStateOf(false)
    private var hasCallPermissions by mutableStateOf(false)
    private var overlayEnabled by mutableStateOf(true)
    private var showOnboarding by mutableStateOf(false)

    private val permissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        checkPermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences("gesture_prefs", Context.MODE_PRIVATE)
        showOnboarding = !prefs.getBoolean("onboardingSeen", false)

        setContent {
            MainApp(
                isAccessibilityEnabled = isAccessibilityEnabled,
                hasCameraPermission = permissionGranted,
                hasCallPermission = hasCallPermissions,
                onRequestCamera = { permissions.launch(arrayOf(Manifest.permission.CAMERA)) },
                onRequestCall = { permissions.launch(arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.ANSWER_PHONE_CALLS)) },
                overlayEnabled = overlayEnabled,
                onOverlayToggled = {
                    prefs.edit().putBoolean("overlayEnabled", it).apply()
                    overlayEnabled = it
                },
                showOnboarding = showOnboarding,
                onOnboardingFinished = {
                    prefs.edit().putBoolean("onboardingSeen", true).apply()
                    showOnboarding = false
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
        val prefs = getSharedPreferences("gesture_prefs", Context.MODE_PRIVATE)
        overlayEnabled = prefs.getBoolean("overlayEnabled", true)
    }

    private fun checkPermissions() {
        permissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        hasCallPermissions = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
                             ContextCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        isAccessibilityEnabled = checkAccessibilityService()
    }

    private fun checkAccessibilityService(): Boolean {
        var accessibilityEnabled = 0
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                contentResolver,
                android.provider.Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: Settings.SettingNotFoundException) {
        }
        if (accessibilityEnabled == 1) {
            val settingValue = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (settingValue != null) {
                return settingValue.contains(packageName)
            }
        }
        return false
    }
}