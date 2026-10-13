package com.example.gesturecontrol

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker

class HandTracker(
    context: Context,
    private val onError: (String) -> Unit = {},
    private val onResult: (List<NormalizedLandmark>?) -> Unit
) {
    private val landmarker: HandLandmarker

    private var lastLogTimeMs = 0L
    private var landmarkFrameCount = 0
    private var totalFrameCount = 0

    init {
        try {
            val base = BaseOptions.builder()
                .setModelAssetPath("hand_landmarker.task")
                .build()

            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(base)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumHands(1)
                .setMinHandDetectionConfidence(0.6f)
                .setMinTrackingConfidence(0.5f)
                .setResultListener { result, _ ->
                    try {
                        val landmarks = result.landmarks().firstOrNull()
                        totalFrameCount++
                        if (landmarks != null) {
                            landmarkFrameCount++
                        }
                        val now = SystemClock.uptimeMillis()
                        if (now - lastLogTimeMs >= 1000L) {
                            Log.w(TAG, "Landmarks: $landmarkFrameCount/$totalFrameCount frames with hand detected in last sec")
                            landmarkFrameCount = 0
                            totalFrameCount = 0
                            lastLogTimeMs = now
                        }
                        onResult(landmarks)
                    } catch (e: Exception) {
                        val msg = e.localizedMessage ?: e.message ?: e.javaClass.simpleName
                        Log.w(TAG, "Result listener error: $msg", e)
                        onError(msg)
                    }
                }
                .setErrorListener { error ->
                    val msg = error.localizedMessage ?: error.message ?: error.javaClass.simpleName
                    Log.w(TAG, "MediaPipe error: $msg", error)
                    onError(msg)
                }
                .build()

            landmarker = HandLandmarker.createFromOptions(context, options)
            Log.w(TAG, "MediaPipe HandLandmarker initialized successfully")
        } catch (e: Throwable) {
            val msg = e.localizedMessage ?: e.message ?: e.javaClass.simpleName
            Log.w(TAG, "MediaPipe init exception: $msg", e)
            onError(msg)
            throw e
        }
    }

    fun detect(bitmap: Bitmap, timestampMs: Long) {
        try {
            landmarker.detectAsync(BitmapImageBuilder(bitmap).build(), timestampMs)
        } catch (e: Throwable) {
            val msg = e.localizedMessage ?: e.message ?: e.javaClass.simpleName
            Log.w(TAG, "detectAsync error: $msg", e)
            onError(msg)
        }
    }

    fun close() = landmarker.close()

    companion object {
        private const val TAG = "HandTracker"
    }
}