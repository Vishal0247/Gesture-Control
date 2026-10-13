package com.example.gesturecontrol

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.view.KeyEvent

class MediaController(context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun volumeUp() {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
    }

    fun volumeDown() {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
    }

    fun playPause() {
        sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    }

    fun nextTrack() {
        sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    fun prevTrack() {
        sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    private fun sendMediaKey(keyCode: Int) {
        val eventTime = SystemClock.uptimeMillis()
        val downEvent = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0)
        audioManager.dispatchMediaKeyEvent(downEvent)
        val upEvent = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0)
        audioManager.dispatchMediaKeyEvent(upEvent)
    }
}
