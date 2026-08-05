package com.neon.ascent.core.common

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HapticService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    /** Subtle double-tap pulse */
    @SuppressLint("MissingPermission")
    fun heartbeat() {
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 50, 100, 50), -1)
        vibrator.vibrate(effect)
    }

    /** Rhythmic sync pulse */
    @SuppressLint("MissingPermission")
    fun syncSuccess() {
        val effect = VibrationEffect.createWaveform(
            longArrayOf(0, 100, 50, 100, 50, 200),
            intArrayOf(0, 150, 0, 200, 0, 255),
            -1
        )
        vibrator.vibrate(effect)
    }

    /** Intense alert pulse for rest timer completion */
    @SuppressLint("MissingPermission")
    fun alertRestOver() {
        val effect = VibrationEffect.createWaveform(
            longArrayOf(0, 300, 100, 300, 100, 300),
            intArrayOf(0, 255, 0, 255, 0, 255),
            -1
        )
        vibrator.vibrate(effect)
    }

    /** Heavy, glitchy burst for Ascension */
    @SuppressLint("MissingPermission")
    fun ascensionBurst() {
        val effect = VibrationEffect.createWaveform(
            longArrayOf(0, 30, 30, 30, 30, 30, 200, 50, 50, 50, 400),
            intArrayOf(0, 255, 0, 255, 0, 255, 255, 0, 100, 0, 255),
            -1
        )
        vibrator.vibrate(effect)
    }

    /** Gentle pulse for breathing guidance */
    @SuppressLint("MissingPermission")
    fun breathingPulse() {
        // Soft swell (ramp up)
        val effect = VibrationEffect.createWaveform(
            longArrayOf(0, 100, 200, 300),
            intArrayOf(0, 50, 100, 150),
            -1
        )
        vibrator.vibrate(effect)
    }
}
