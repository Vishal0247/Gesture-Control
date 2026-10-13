package com.example.gesturecontrol

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.acos
import kotlin.math.hypot

class GestureClassifier {
    private val rawGestures = ArrayDeque<Gesture>()
    private var lastStableGesture = Gesture.NONE

    companion object {
        const val PINCH_THRESHOLD = 0.05f
        const val FINGER_EXT_RATIO = 1.15f
        const val SWIPE_FINGER_EXT_RATIO = 1.3f
        const val CURLED_FINGER_RATIO = 1.0f
        const val THUMB_UP_Y_MARGIN = 0.05f
        const val UPWARD_ANGLE_THRESHOLD_RAD = 0.785398f // 45 degrees
        const val STABILITY_WINDOW = 5
        const val STABILITY_THRESHOLD = 4
    }

    fun classify(lm: List<NormalizedLandmark>?): Gesture {
        if (lm == null || lm.size < 21) {
            GestureBus.handPresent.value = false
            rawGestures.clear()
            return Gesture.NONE
        }
        GestureBus.handPresent.value = true
        
        val wrist = lm[0]
        GestureBus.wristPos.value = wrist.x() to wrist.y()

        // Distances from wrist to tip / pip
        val fingers = listOf(8 to 6, 12 to 10, 16 to 14, 20 to 18) // index, middle, ring, pinky
        val ratios = mutableListOf<Float>()
        
        var extendedCount = 0
        for ((tip, pip) in fingers) {
            val dTip = hypot(wrist.x() - lm[tip].x(), wrist.y() - lm[tip].y())
            val dPip = hypot(wrist.x() - lm[pip].x(), wrist.y() - lm[pip].y())
            val ratio = if (dPip > 0) dTip / dPip else 0f
            ratios.add(ratio)
            if (ratio > FINGER_EXT_RATIO) {
                extendedCount++
            }
        }
        GestureBus.fingerRatios.value = ratios

        val indexExt = ratios[0]
        val middleExt = ratios[1]
        val ringExt = ratios[2]
        val pinkyExt = ratios[3]

        // Upward angle check: screen-up vector is (0, -1)
        fun isPointingUp(tipIdx: Int, mcpIdx: Int): Boolean {
            val dx = lm[tipIdx].x() - lm[mcpIdx].x()
            val dy = lm[tipIdx].y() - lm[mcpIdx].y()
            val len = hypot(dx, dy)
            if (len == 0f) return false
            // dot product with (0, -1) = -dy
            val dot = -dy
            val cosTheta = dot / len
            return acos(cosTheta) < UPWARD_ANGLE_THRESHOLD_RAD
        }

        val indexUp = isPointingUp(8, 5)
        val middleUp = isPointingUp(12, 9)

        // Strict rules for One/Two Fingers
        val isStrictOneFinger = indexExt > SWIPE_FINGER_EXT_RATIO && 
                                middleExt < CURLED_FINGER_RATIO && 
                                ringExt < CURLED_FINGER_RATIO && 
                                pinkyExt < CURLED_FINGER_RATIO && 
                                indexUp
                                
        val isStrictTwoFingers = indexExt > SWIPE_FINGER_EXT_RATIO && 
                                 middleExt > SWIPE_FINGER_EXT_RATIO && 
                                 ringExt < CURLED_FINGER_RATIO && 
                                 pinkyExt < CURLED_FINGER_RATIO && 
                                 indexUp && middleUp

        val pinch = hypot(lm[4].x() - lm[8].x(), lm[4].y() - lm[8].y()) < PINCH_THRESHOLD

        val thumbTipD = hypot(wrist.x() - lm[4].x(), wrist.y() - lm[4].y())
        val thumbIpD = hypot(wrist.x() - lm[3].x(), wrist.y() - lm[3].y())
        val isThumbExtended = thumbTipD > thumbIpD * FINGER_EXT_RATIO
        val isThumbUp = isThumbExtended && extendedCount == 0 && (lm[4].y() < lm[5].y() - THUMB_UP_Y_MARGIN)
        
        // General relaxed check for three fingers and generic extended counting
        val isThreeFingers = indexExt > FINGER_EXT_RATIO && middleExt > FINGER_EXT_RATIO && ringExt > FINGER_EXT_RATIO && extendedCount == 3

        val rawGesture = when {
            pinch -> Gesture.PINCH
            extendedCount >= 4 -> Gesture.OPEN_PALM
            isThumbUp -> Gesture.THUMBS_UP
            extendedCount == 0 -> Gesture.FIST
            isThreeFingers -> Gesture.THREE_FINGERS
            isStrictTwoFingers -> Gesture.TWO_FINGERS
            isStrictOneFinger -> Gesture.ONE_FINGER
            // Hysteresis fallback
            lastStableGesture == Gesture.TWO_FINGERS && indexExt > FINGER_EXT_RATIO && middleExt > FINGER_EXT_RATIO && extendedCount == 2 -> Gesture.TWO_FINGERS
            lastStableGesture == Gesture.ONE_FINGER && indexExt > FINGER_EXT_RATIO && extendedCount == 1 -> Gesture.ONE_FINGER
            else -> Gesture.NONE
        }

        rawGestures.addLast(rawGesture)
        if (rawGestures.size > STABILITY_WINDOW) rawGestures.removeFirst()

        val counts = rawGestures.groupingBy { it }.eachCount()
        val stable = counts.entries.find { it.value >= STABILITY_THRESHOLD }?.key
        if (stable != null) {
            lastStableGesture = stable
        }

        return lastStableGesture
    }
}