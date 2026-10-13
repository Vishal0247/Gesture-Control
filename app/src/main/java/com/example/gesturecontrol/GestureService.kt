package com.example.gesturecontrol

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.os.SystemClock
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class GestureService : LifecycleService() {

    private var tracker: HandTracker? = null
    private var provider: ProcessCameraProvider? = null
    private val classifier = GestureClassifier()
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var callMonitor: CallStateMonitor? = null

    override fun onCreate() {
        super.onCreate()
        callMonitor = CallStateMonitor(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startInForeground()
        GestureBus.running.value = true
        android.service.quicksettings.TileService.requestListeningState(this, android.content.ComponentName(this, GestureTileService::class.java))
        if (tracker == null) startCamera()
        return START_NOT_STICKY
    }

    private fun startInForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Gesture control", NotificationManager.IMPORTANCE_LOW)
        )
        val stop = PendingIntent.getService(
            this, 0,
            Intent(this, GestureService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("Gesture control chal raha hai")
            .setContentText("Camera on hai. Band karne ke liye Stop dabao.")
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stop)
            .build()

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun startCamera() {
        callMonitor?.register()
        GestureBus.trackerError.value = null
        try {
            tracker = HandTracker(
                context = this,
                onError = { errorMsg ->
                    android.util.Log.w("GestureService", "HandTracker reported error: $errorMsg")
                    GestureBus.trackerError.value = errorMsg
                },
                onResult = { landmarks ->
                    GestureBus.state.value = classifier.classify(landmarks)
                }
            )
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: e.message ?: e.javaClass.simpleName
            android.util.Log.w("GestureService", "MediaPipe init failed: $msg", e)
            GestureBus.trackerError.value = msg
            stopSelf()
            return
        }

        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val cameraProvider = future.get().also { provider = it }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            analysis.setAnalyzer(executor) { proxy ->
                try {
                    val raw = Bitmap.createBitmap(proxy.width, proxy.height, Bitmap.Config.ARGB_8888)
                    proxy.use { raw.copyPixelsFromBuffer(it.planes[0].buffer) }

                    val m = Matrix().apply {
                        postRotate(proxy.imageInfo.rotationDegrees.toFloat())
                        postScale(-1f, 1f) // front camera mirror
                    }
                    val frame = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true)
                    tracker?.detect(frame, SystemClock.uptimeMillis())
                } catch (e: Exception) {
                    val msg = e.localizedMessage ?: e.message ?: e.javaClass.simpleName
                    android.util.Log.w("GestureService", "Analyzer frame error: $msg", e)
                }
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, analysis)
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: e.message ?: e.javaClass.simpleName
                android.util.Log.w("GestureService", "Camera binding failed: $msg", e)
                GestureBus.trackerError.value = "Camera bind failed: $msg"
                stopSelf()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        callMonitor?.unregister()
        provider?.unbindAll()
        tracker?.close()
        tracker = null
        executor.shutdown()
        GestureBus.state.value = Gesture.NONE
        GestureBus.armed.value = false
        GestureBus.running.value = false
        android.service.quicksettings.TileService.requestListeningState(this, android.content.ComponentName(this, GestureTileService::class.java))
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.example.gesturecontrol.STOP"
        private const val CHANNEL_ID = "gesture_channel"
        private const val NOTIF_ID = 1
    }
}