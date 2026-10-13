package com.example.gesturecontrol

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

enum class Gesture { 
    NONE, OPEN_PALM, FIST, PINCH, 
    THUMBS_UP, ONE_FINGER, TWO_FINGERS, THREE_FINGERS 
}

enum class CallState {
    IDLE, RINGING, IN_CALL
}

data class HoldProgress(val label: String, val fraction: Float)

object GestureBus {
    val state = MutableStateFlow(Gesture.NONE)
    val actionEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val armed = MutableStateFlow(false)
    val mediaMode = MutableStateFlow(false)
    val callState = MutableStateFlow(CallState.IDLE)
    val wristPos = MutableStateFlow(0f to 0f)
    val fingerRatios = MutableStateFlow(listOf<Float>())
    
    val running = MutableStateFlow(false)
    val handPresent = MutableStateFlow(false)
    val holdProgress = MutableStateFlow<HoldProgress?>(null)
    val trackerError = MutableStateFlow<String?>(null)
}