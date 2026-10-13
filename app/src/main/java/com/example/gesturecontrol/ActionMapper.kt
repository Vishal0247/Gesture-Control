package com.example.gesturecontrol

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.hypot

class ActionMapper(private val service: AccessibilityService, private val scope: CoroutineScope) {
    private var lastActionTime = 0L
    private var lastSwipeTime = 0L
    
    private var currentGesture = Gesture.NONE
    private var pendingGesture = Gesture.NONE
    private var holdJob: Job? = null
    private var flickerJob: Job? = null
    
    private var actionConsumed = false
    private var highestTierReached = 0
    private var wristStartPos = 0f to 0f
    private var holdStartTime = 0L

    private val mediaController = MediaController(service)
    private val callController = CallController(service)

    companion object {
        const val WRIST_MOVE_THRESHOLD = 0.05f

        const val SWIPE_DWELL_MS = 600L
        const val SWIPE_COOLDOWN_MS = 1000L
        const val VOLUME_REPEAT_MS = 400L
        
        const val THUMBS_UP_DWELL_MS = 400L
        const val HOME_HOLD_MS = 1000L
        
        // FIST Tiers
        const val FIST_TIER1_MS = 1000L // BACK / PREV TRACK
        const val FIST_TIER2_MS = 3000L // LOCK SCREEN
        
        // THREE_FINGERS Tiers
        const val THREE_FINGERS_TIER1_MS = 1000L // WhatsApp / NEXT TRACK
        const val THREE_FINGERS_TIER2_MS = 2500L // Toggle ARMED
        
        const val PINCH_MEDIA_TOGGLE_MS = 1500L
        
        // Call Mode
        const val CALL_ANSWER_MS = 700L
        const val CALL_REJECT_MS = 1000L
        const val CALL_END_MS = 2000L
    }
    
    init {
        scope.launch {
            GestureBus.armed.collect { isArmed ->
                if (!isArmed) {
                    Log.d("GestureDebug", "Armed became false, cancelling active holds")
                    flickerJob?.cancel()
                    holdJob?.cancel()
                    currentGesture = Gesture.NONE
                    actionConsumed = false
                    flickerJob = null
                    holdJob = null
                    GestureBus.mediaMode.value = false
                    GestureBus.holdProgress.value = null
                }
            }
        }
        
        scope.launch {
            GestureBus.callState.collect { state ->
                Log.d("GestureDebug", "Call state updated to $state")
                flickerJob?.cancel()
                holdJob?.cancel()
                currentGesture = Gesture.NONE
                actionConsumed = false
                flickerJob = null
                holdJob = null
                GestureBus.holdProgress.value = null
            }
        }
    }

    fun processState(newGesture: Gesture) {
        val now = SystemClock.uptimeMillis()
        Log.d("GestureDebug", "Received state: $newGesture")

        if (newGesture == currentGesture) {
            if (flickerJob != null) {
                Log.d("GestureDebug", "Flicker job cancelled (returned to $currentGesture)")
                flickerJob?.cancel()
                flickerJob = null
            }
            pendingGesture = Gesture.NONE
            return
        }

        pendingGesture = newGesture

        if (flickerJob != null) {
            return
        }

        val flickerDelay = if (currentGesture == Gesture.ONE_FINGER || currentGesture == Gesture.TWO_FINGERS) 150L else 300L
        flickerJob = scope.launch {
            delay(flickerDelay)
            val target = pendingGesture
            Log.d("GestureDebug", "Flicker timeout reached. Transitioning $currentGesture -> $target")
            if (currentGesture != Gesture.NONE) {
                cancelHold("State changed to $target")
            }
            currentGesture = target
            if (target != Gesture.NONE) {
                startNewHold(target)
            }
            flickerJob = null
        }
    }

    private fun startNewHold(newGesture: Gesture) {
        actionConsumed = false
        highestTierReached = 0
        wristStartPos = GestureBus.wristPos.value
        holdStartTime = SystemClock.uptimeMillis()
        Log.d("GestureDebug", "Job started for: $newGesture")
        
        holdJob = scope.launch {
            try {
                handleHold(newGesture)
            } finally {
                GestureBus.holdProgress.value = null
            }
        }
    }

    private suspend fun CoroutineScope.holdWithProgress(durationMs: Long, label: String, initialProgressMs: Long = 0) {
        val totalMs = durationMs + initialProgressMs
        val start = SystemClock.uptimeMillis() - initialProgressMs
        while (true) {
            val elapsed = SystemClock.uptimeMillis() - start
            if (elapsed >= totalMs) {
                GestureBus.holdProgress.value = HoldProgress(label, 1f)
                return
            }
            GestureBus.holdProgress.value = HoldProgress(label, elapsed.toFloat() / totalMs)
            delay(50)
            ensureActive()
        }
    }

    private suspend fun CoroutineScope.handleHold(gesture: Gesture) {
        val callState = GestureBus.callState.value
        val isRinging = callState == CallState.RINGING
        val isInCall = callState == CallState.IN_CALL
        val isMedia = GestureBus.mediaMode.value && GestureBus.armed.value && !isInCall
        val isArmed = GestureBus.armed.value

        try {
            when (gesture) {
                Gesture.PINCH -> {
                    if (isRinging) return
                    holdWithProgress(PINCH_MEDIA_TOGGLE_MS, "MEDIA MODE")
                    if (isArmed && !isInCall && !actionConsumed) {
                        val newMode = !GestureBus.mediaMode.value
                        GestureBus.mediaMode.value = newMode
                        emitAction(if (newMode) "MEDIA MODE ON" else "MEDIA MODE OFF")
                        actionConsumed = true
                    } else Log.d("GestureDebug", "Job completed: PINCH action skipped")
                }
                Gesture.ONE_FINGER -> {
                    if (isRinging) return
                    holdWithProgress(SWIPE_DWELL_MS, if (isMedia) "VOLUME UP" else "SWIPE UP")
                    val now = SystemClock.uptimeMillis()
                    if (isMedia) {
                        emitAction("VOLUME UP")
                        mediaController.volumeUp()
                        actionConsumed = true
                        while (true) {
                            delay(VOLUME_REPEAT_MS)
                            ensureActive()
                            emitAction("VOLUME UP")
                            mediaController.volumeUp()
                        }
                    } else {
                        if (now - lastSwipeTime < SWIPE_COOLDOWN_MS) {
                            Log.d("GestureDebug", "Job completed: Swipe skipped (cooldown active)")
                        } else if (isArmed) {
                            emitAction("SWIPE UP")
                            performSwipe(0.5f, 0.5f, 0.8f, 0.2f)
                            lastSwipeTime = now
                            val ratios = GestureBus.fingerRatios.value.joinToString(", ") { "%.2f".format(it) }
                            Log.d("GestureDebug", "Job completed: Swipe UP fired. Ratios: $ratios")
                        } else Log.d("GestureDebug", "Job completed: Action skipped (paused): $gesture")
                        actionConsumed = true
                    }
                }
                Gesture.TWO_FINGERS -> {
                    if (isRinging) return
                    holdWithProgress(SWIPE_DWELL_MS, if (isMedia) "VOLUME DOWN" else "SWIPE DOWN")
                    val now = SystemClock.uptimeMillis()
                    if (isMedia) {
                        emitAction("VOLUME DOWN")
                        mediaController.volumeDown()
                        actionConsumed = true
                        while (true) {
                            delay(VOLUME_REPEAT_MS)
                            ensureActive()
                            emitAction("VOLUME DOWN")
                            mediaController.volumeDown()
                        }
                    } else {
                        if (now - lastSwipeTime < SWIPE_COOLDOWN_MS) {
                            Log.d("GestureDebug", "Job completed: Swipe skipped (cooldown active)")
                        } else if (isArmed) {
                            emitAction("SWIPE DOWN")
                            performSwipe(0.5f, 0.5f, 0.2f, 0.8f)
                            lastSwipeTime = now
                            val ratios = GestureBus.fingerRatios.value.joinToString(", ") { "%.2f".format(it) }
                            Log.d("GestureDebug", "Job completed: Swipe DOWN fired. Ratios: $ratios")
                        } else Log.d("GestureDebug", "Job completed: Action skipped (paused): $gesture")
                        actionConsumed = true
                    }
                }
                Gesture.THUMBS_UP -> {
                    if (isRinging) {
                        holdWithProgress(CALL_ANSWER_MS, "ANSWER")
                        if (!actionConsumed) {
                            if (callController.answerCall()) emitAction("CALL ANSWERED")
                            else emitAction("NOT SUPPORTED")
                            actionConsumed = true
                        }
                    } else {
                        holdWithProgress(THUMBS_UP_DWELL_MS, if (isMedia) "PLAY/PAUSE" else "TAP")
                        if (!actionConsumed) {
                            if (isArmed) {
                                if (isMedia) {
                                    emitAction("PLAY/PAUSE")
                                    mediaController.playPause()
                                } else {
                                    emitAction("TAP")
                                    performTap()
                                }
                            } else Log.d("GestureDebug", "Job completed: Action skipped (paused): $gesture")
                            actionConsumed = true
                        }
                    }
                }
                Gesture.OPEN_PALM -> {
                    if (isRinging) {
                        holdWithProgress(CALL_REJECT_MS, "REJECT")
                        if (!actionConsumed) {
                            if (callController.endCall()) emitAction("CALL REJECTED")
                            else emitAction("NOT SUPPORTED")
                            actionConsumed = true
                        }
                    } else {
                        holdWithProgress(HOME_HOLD_MS, "HOME")
                        if (!actionConsumed) {
                            if (isArmed) {
                                val currentWrist = GestureBus.wristPos.value
                                val dist = hypot(currentWrist.first - wristStartPos.first, currentWrist.second - wristStartPos.second)
                                if (dist < WRIST_MOVE_THRESHOLD) {
                                    emitAction("HOME")
                                    service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                                    Log.d("GestureDebug", "Job completed: HOME fired")
                                } else Log.d("GestureDebug", "Job completed: Action skipped (wrist moved $dist)")
                            } else Log.d("GestureDebug", "Job completed: Action skipped (paused): $gesture")
                            actionConsumed = true
                        }
                    }
                }
                Gesture.FIST -> {
                    if (isRinging) return
                    if (isInCall) {
                        holdWithProgress(CALL_END_MS, "END CALL")
                        if (!actionConsumed) {
                            if (callController.endCall()) emitAction("CALL ENDED")
                            else emitAction("NOT SUPPORTED")
                            actionConsumed = true
                        }
                    } else {
                        holdWithProgress(FIST_TIER1_MS, if (isMedia) "PREV TRACK" else "BACK")
                        highestTierReached = 1
                        Log.d("GestureDebug", "Tier 1 reached for $gesture")
                        
                        holdWithProgress(FIST_TIER2_MS - FIST_TIER1_MS, "LOCK", FIST_TIER1_MS)
                        highestTierReached = 2
                        Log.d("GestureDebug", "Tier 2 reached for $gesture")
                        
                        if (!actionConsumed) {
                            if (isArmed && Build.VERSION.SDK_INT >= 28) {
                                emitAction("LOCK")
                                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
                                GestureBus.armed.value = false
                                Log.d("GestureDebug", "Job completed: LOCK fired")
                            } else Log.d("GestureDebug", "Job completed: Action skipped (paused or low API)")
                            actionConsumed = true
                        }
                    }
                }
                Gesture.THREE_FINGERS -> {
                    if (isRinging) return
                    holdWithProgress(THREE_FINGERS_TIER1_MS, if (isMedia) "NEXT TRACK" else "WHATSAPP")
                    highestTierReached = 1
                    Log.d("GestureDebug", "Tier 1 reached for $gesture")
                    
                    holdWithProgress(THREE_FINGERS_TIER2_MS - THREE_FINGERS_TIER1_MS, "ARMED TOGGLE", THREE_FINGERS_TIER1_MS)
                    highestTierReached = 2
                    Log.d("GestureDebug", "Tier 2 reached for $gesture")
                    
                    if (!actionConsumed) {
                        val newArmed = !GestureBus.armed.value
                        GestureBus.armed.value = newArmed
                        emitAction(if (newArmed) "ARMED ON" else "ARMED OFF")
                        Log.d("GestureDebug", "Job completed: ARMED toggled to $newArmed")
                        actionConsumed = true
                    }
                }
                else -> {}
            }
        } catch (e: CancellationException) {
            Log.d("GestureDebug", "Job cancelled ($gesture): ${e.message}")
            throw e
        }
    }

    private fun cancelHold(reason: String) {
        val holdDuration = SystemClock.uptimeMillis() - holdStartTime
        Log.d("GestureDebug", "Job interrupted: $currentGesture (Reason: $reason, Held: ${holdDuration}ms)")
        
        val callState = GestureBus.callState.value
        val isRinging = callState == CallState.RINGING
        val isInCall = callState == CallState.IN_CALL
        val isMedia = GestureBus.mediaMode.value && GestureBus.armed.value && !isInCall
        val isArmed = GestureBus.armed.value

        if (!actionConsumed && !isRinging) {
            when (currentGesture) {
                Gesture.FIST -> {
                    if (!isInCall && highestTierReached == 1) {
                        if (isArmed) {
                            if (isMedia) {
                                emitAction("PREV TRACK")
                                mediaController.prevTrack()
                            } else {
                                emitAction("BACK")
                                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                            }
                            Log.d("GestureDebug", "Action fired on release: FIST Tier 1")
                        } else Log.d("GestureDebug", "Action skipped on release (paused)")
                    } else Log.d("GestureDebug", "Action skipped on release (too early or in call)")
                }
                Gesture.THREE_FINGERS -> {
                    if (highestTierReached == 1) {
                        if (isArmed) {
                            if (isMedia) {
                                emitAction("NEXT TRACK")
                                mediaController.nextTrack()
                            } else {
                                emitAction("WHATSAPP")
                                launchWhatsApp()
                            }
                            Log.d("GestureDebug", "Action fired on release: 3-Fingers Tier 1")
                        } else Log.d("GestureDebug", "Action skipped on release (paused)")
                    } else Log.d("GestureDebug", "Action skipped on release (too early)")
                }
                else -> {}
            }
        }
        
        holdJob?.cancel()
        holdJob = null
        currentGesture = Gesture.NONE
        actionConsumed = false
        highestTierReached = 0
        GestureBus.holdProgress.value = null
    }

    fun reset() {
        Log.d("GestureDebug", "Mapper reset called, cancelling everything")
        GestureBus.armed.value = false
        GestureBus.mediaMode.value = false
        GestureBus.holdProgress.value = null
        flickerJob?.cancel()
        holdJob?.cancel()
        flickerJob = null
        holdJob = null
        currentGesture = Gesture.NONE
        actionConsumed = false
    }
    
    private fun emitAction(actionName: String) {
        Log.d("GestureDebug", "Action fired: $actionName")
        GestureBus.actionEvents.tryEmit(actionName)
        lastActionTime = SystemClock.uptimeMillis()
    }

    private fun launchWhatsApp() {
        val pm = service.packageManager
        var intent = pm.getLaunchIntentForPackage("com.whatsapp")
        if (intent == null) {
            intent = pm.getLaunchIntentForPackage("com.whatsapp.w4b")
        }
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                service.startActivity(intent)
            } catch (e: Exception) {
                Log.d("GestureDebug", "Failed to launch WhatsApp", e)
            }
        } else {
            Log.d("GestureDebug", "WhatsApp not found")
        }
    }

    private fun performTap() {
        val displayMetrics = service.resources.displayMetrics
        val x = displayMetrics.widthPixels / 2f
        val y = displayMetrics.heightPixels / 2f
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 100)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        service.dispatchGesture(gesture, null, null)
    }

    private fun performSwipe(startXFraction: Float, endXFraction: Float, startYFraction: Float, endYFraction: Float) {
        val displayMetrics = service.resources.displayMetrics
        val width = displayMetrics.widthPixels.toFloat()
        val height = displayMetrics.heightPixels.toFloat()
        val path = Path().apply {
            moveTo(width * startXFraction, height * startYFraction)
            lineTo(width * endXFraction, height * endYFraction)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 250)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        service.dispatchGesture(gesture, null, null)
    }
}
