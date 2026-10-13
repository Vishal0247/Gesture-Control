package com.example.gesturecontrol

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class GestureAccessibilityService : AccessibilityService() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private lateinit var mapper: ActionMapper
    private var overlay: OverlayIndicator? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        mapper = ActionMapper(this, scope)
        overlay = OverlayIndicator(this)
        
        scope.launch {
            GestureBus.state.collect { gesture ->
                mapper.processState(gesture)
            }
        }
        
        scope.launch {
            kotlinx.coroutines.flow.combine(GestureBus.running, GestureBus.trackerError) { running, error ->
                running || error != null
            }.collect { shouldShow ->
                if (shouldShow) overlay?.attach()
                else overlay?.detach()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        job.cancel()
        mapper.reset()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        overlay?.detach()
        overlay = null
        job.cancel()
        mapper.reset()
        super.onDestroy()
    }
}
