package com.neon.ascent.core.common

import android.content.Context
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

class LevelUpEffectService(private val context: Context) {

    private val soundPool = SoundPool.Builder().setMaxStreams(3).build()
    private var levelUpSoundId: Int? = null

    init {
        // Load cyberpunk-style level-up sound if it exists
        val resId = context.resources.getIdentifier("level_up", "raw", context.packageName)
        if (resId != 0) {
            levelUpSoundId = soundPool.load(context, resId, 1)
        }
    }

    fun triggerLevelUp(delta: Int) {
        // Haptic feedback
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 40, 60, 40, 120),
                        intArrayOf(0, 180, 0, 255, 180),
                        -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(200)
            }
        }

        // Sound
        levelUpSoundId?.let { id ->
            // Amplitude scales with delta (percentile jump)
            val volume = (0.5f + (delta / 20f)).coerceIn(0.5f, 1.0f)
            soundPool.play(id, volume, volume, 1, 0, 1.0f)
        }
    }

    fun release() {
        soundPool.release()
    }
}

@Composable
fun rememberLevelUpService(): LevelUpEffectService {
    val context = LocalContext.current
    val service = remember { LevelUpEffectService(context) }
    DisposableEffect(Unit) {
        onDispose { service.release() }
    }
    return service
}
