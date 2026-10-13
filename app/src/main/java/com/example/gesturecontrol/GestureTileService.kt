package com.example.gesturecontrol

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class GestureTileService : TileService() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    override fun onStartListening() {
        super.onStartListening()
        scope.launch {
            launch {
                GestureBus.running.collect { updateState() }
            }
            launch {
                GestureBus.armed.collect { updateState() }
            }
            launch {
                GestureBus.mediaMode.collect { updateState() }
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        job.cancelChildren()
    }

    private fun updateState() {
        val tile = qsTile ?: return
        val isRunning = GestureBus.running.value
        val isArmed = GestureBus.armed.value
        val isMedia = GestureBus.mediaMode.value

        tile.state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Gesture Control"
        
        if (Build.VERSION.SDK_INT >= 29) {
            tile.subtitle = if (!isRunning) "Off"
                            else if (!isArmed) "Paused"
                            else if (isMedia) "Media mode"
                            else "Armed"
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val isRunning = GestureBus.running.value
        
        if (isRunning) {
            val stopIntent = Intent(this, GestureService::class.java).setAction(GestureService.ACTION_STOP)
            stopService(stopIntent)
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            if (!hasPermission) {
                val intent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                if (Build.VERSION.SDK_INT >= 34) {
                    val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
                    startActivityAndCollapse(pi)
                } else {
                    @Suppress("DEPRECATION")
                    startActivityAndCollapse(intent)
                }
            } else {
                val intent = Intent(this, StartSessionActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or Intent.FLAG_ACTIVITY_NO_HISTORY)
                }
                if (Build.VERSION.SDK_INT >= 34) {
                    val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
                    startActivityAndCollapse(pi)
                } else {
                    @Suppress("DEPRECATION")
                    startActivityAndCollapse(intent)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}

class StartSessionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val serviceIntent = Intent(this, GestureService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        finish()
    }
}
