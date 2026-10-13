package com.example.gesturecontrol

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.content.ContextCompat

class CallController(private val context: Context) {
    private val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager

    fun answerCall(): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
            Log.d("GestureDebug", "ANSWER_PHONE_CALLS not granted")
            return false
        }
        return try {
            if (Build.VERSION.SDK_INT >= 26) {
                telecomManager.acceptRingingCall()
                true
            } else {
                Log.d("GestureDebug", "acceptRingingCall not supported on API < 26")
                false
            }
        } catch (e: SecurityException) {
            Log.d("GestureDebug", "SecurityException on answerCall: ${e.message}")
            false
        }
    }

    fun endCall(): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
            Log.d("GestureDebug", "ANSWER_PHONE_CALLS not granted")
            return false
        }
        return try {
            if (Build.VERSION.SDK_INT >= 28) {
                telecomManager.endCall()
                true
            } else {
                Log.d("GestureDebug", "endCall not supported on API < 28")
                false
            }
        } catch (e: SecurityException) {
            Log.d("GestureDebug", "SecurityException on endCall: ${e.message}")
            false
        }
    }
}
