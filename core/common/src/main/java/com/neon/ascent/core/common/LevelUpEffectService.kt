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
import com.neon.ascent.core.domain.effects.LevelUpEffectService
import com.neon.ascent.core.domain.model.SpecialType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidLevelUpEffectService @Inject constructor(
    @ApplicationContext private val context: Context
) : LevelUpEffectService {

    private val soundPool = SoundPool.Builder().setMaxStreams(3).build()
    private var levelUpSoundId: Int? = null

    init {
        val resId = context.resources.getIdentifier("level_up", "raw", context.packageName)
        if (resId != 0) {
            levelUpSoundId = soundPool.load(context, resId, 1)
        }
    }

    override fun triggerLevelUp(type: SpecialType, xpGained: Int) {
        triggerLevelUp(xpGained / 2) // Approximate delta for sound/haptic
    }

    override fun triggerLevelUp(delta: Int) {
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
            val volume = (0.5f + (delta / 50f)).coerceIn(0.5f, 1.0f)
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
    val service = remember { AndroidLevelUpEffectService(context) }
    DisposableEffect(Unit) {
        onDispose { service.release() }
    }
    return service
}
