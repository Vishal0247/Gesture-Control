package com.example.gesturecontrol.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gesturecontrol.CallState
import com.example.gesturecontrol.GestureBus
import com.example.gesturecontrol.GestureService
import com.example.gesturecontrol.ui.components.*
import com.example.gesturecontrol.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    isAccessibilityEnabled: Boolean,
    hasCameraPermission: Boolean,
    hasCallPermission: Boolean,
    onRequestCamera: () -> Unit,
    onRequestCall: () -> Unit
) {
    val context = LocalContext.current
    val running by GestureBus.running.collectAsStateWithLifecycle()
    val armed by GestureBus.armed.collectAsStateWithLifecycle()
    val mediaMode by GestureBus.mediaMode.collectAsStateWithLifecycle()
    val callState by GestureBus.callState.collectAsStateWithLifecycle()
    val gesture by GestureBus.state.collectAsStateWithLifecycle()
    val holdProgress by GestureBus.holdProgress.collectAsStateWithLifecycle()
    val trackerError by GestureBus.trackerError.collectAsStateWithLifecycle()
    
    var lastAction by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        GestureBus.actionEvents.collect {
            lastAction = it
            delay(2000)
            if (lastAction == it) lastAction = null
        }
    }

    val statusText = when {
        !running -> "OFF"
        callState == CallState.RINGING -> "RINGING"
        callState == CallState.IN_CALL -> "IN CALL"
        mediaMode && armed -> "MEDIA MODE"
        !armed -> "PAUSED"
        else -> "ARMED"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        PowerButton(
            isRunning = running,
            isArmed = armed,
            isMedia = mediaMode,
            onClick = {
                if (running) {
                    context.stopService(Intent(context, GestureService::class.java).setAction(GestureService.ACTION_STOP))
                } else {
                    if (hasCameraPermission) {
                        androidx.core.content.ContextCompat.startForegroundService(context, Intent(context, GestureService::class.java))
                    } else {
                        onRequestCamera()
                    }
                }
            }
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = statusText,
            style = MaterialTheme.typography.displayLarge,
            color = TextPrimary
        )
        Text(
            text = if (running) "Hold palm to pause" else "Tap to start camera",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        if (trackerError != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Tracker error: $trackerError",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
        
        GestureCard(
            gestureName = gesture.name.replace("_", " "),
            holdLabel = holdProgress?.label,
            holdFraction = holdProgress?.fraction ?: 0f,
            lastAction = lastAction
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        SectionHeader("System Status", Modifier.fillMaxWidth())
        
        StatusChip(
            label = "Accessibility Service",
            state = isAccessibilityEnabled,
            isError = !isAccessibilityEnabled,
            onClick = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )
        
        StatusChip(
            label = "Camera Permission",
            state = hasCameraPermission,
            isError = !hasCameraPermission,
            onClick = { if (!hasCameraPermission) onRequestCamera() else {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                context.startActivity(intent)
            } },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )
        
        StatusChip(
            label = "Call Control",
            state = hasCallPermission,
            isActive = hasCallPermission,
            onClick = { if (!hasCallPermission) onRequestCall() },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
