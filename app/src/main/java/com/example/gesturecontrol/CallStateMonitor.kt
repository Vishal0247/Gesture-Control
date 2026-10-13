package com.example.gesturecontrol

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat

class CallStateMonitor(private val context: Context) {
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private var isRegistered = false

    private val phoneStateListener = object : PhoneStateListener() {
        @Deprecated("Deprecated in Java")
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            handleCallState(state)
        }
    }

    private var telephonyCallback: TelephonyCallback? = null

    init {
        if (Build.VERSION.SDK_INT >= 31) {
            telephonyCallback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    handleCallState(state)
                }
            }
        }
    }

    private fun handleCallState(state: Int) {
        val newState = when (state) {
            TelephonyManager.CALL_STATE_RINGING -> CallState.RINGING
            TelephonyManager.CALL_STATE_OFFHOOK -> CallState.IN_CALL
            else -> CallState.IDLE
        }
        if (GestureBus.callState.value != newState) {
            Log.d("GestureDebug", "Call state changed to $newState")
            GestureBus.callState.value = newState
        }
    }

    fun register() {
        if (isRegistered) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            Log.d("GestureDebug", "CallStateMonitor skip: READ_PHONE_STATE not granted")
            return
        }
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                telephonyCallback?.let {
                    telephonyManager.registerTelephonyCallback(context.mainExecutor, it)
                }
            } else {
                @Suppress("DEPRECATION")
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
            }
            isRegistered = true
        } catch (e: SecurityException) {
            Log.d("GestureDebug", "SecurityException registering CallStateMonitor: ${e.message}")
        }
    }

    fun unregister() {
        if (!isRegistered) return
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                telephonyCallback?.let {
                    telephonyManager.unregisterTelephonyCallback(it)
                }
            } else {
                @Suppress("DEPRECATION")
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
            }
            isRegistered = false
        } catch (e: Exception) {
            Log.d("GestureDebug", "Error unregistering CallStateMonitor: ${e.message}")
        }
    }
}
