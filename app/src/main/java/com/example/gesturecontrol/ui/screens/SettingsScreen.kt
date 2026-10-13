package com.example.gesturecontrol.ui.screens

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gesturecontrol.GestureTileService
import com.example.gesturecontrol.R
import com.example.gesturecontrol.ui.components.SectionHeader
import com.example.gesturecontrol.ui.theme.*

@Composable
fun SettingsScreen(
    overlayEnabled: Boolean,
    onOverlayToggled: (Boolean) -> Unit
) {
    val context = LocalContext.current
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item { SectionHeader("Behavior") }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ElevatedSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Floating Indicator", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                        Text("Show a small status pill over other apps", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                    Switch(checked = overlayEnabled, onCheckedChange = onOverlayToggled)
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(16.dp)) }
        
        item { SectionHeader("Quick Settings") }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ElevatedSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Control from anywhere", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (Build.VERSION.SDK_INT >= 33) {
                        Button(
                            onClick = {
                                val statusBarManager = context.getSystemService(StatusBarManager::class.java)
                                statusBarManager.requestAddTileService(
                                    ComponentName(context, GestureTileService::class.java),
                                    "Gesture Control",
                                    Icon.createWithResource(context, R.drawable.ic_tile_hand),
                                    context.mainExecutor,
                                    {}
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = Background)
                        ) { Text("Add Quick Settings Tile") }
                    } else {
                        Text(
                            text = "Swipe down the notification shade, tap Edit (pencil icon), and drag the 'Gesture Control' tile into your active tiles.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(16.dp)) }
        
        item { SectionHeader("About") }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ElevatedSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Gesture Control v1.0", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Privacy Statement: Camera frames are processed entirely on-device to detect hand landmarks. No images are ever saved, stored, or sent to the cloud.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}
