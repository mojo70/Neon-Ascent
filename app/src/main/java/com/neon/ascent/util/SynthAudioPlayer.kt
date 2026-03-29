package com.neon.ascent.util

import android.media.AudioManager
import android.media.ToneGenerator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SynthAudioPlayer @Inject constructor() {
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)

    fun playTone(toneType: Int, duration: Int = 100) {
        toneGenerator.startTone(toneType, duration)
    }

    fun playBreachStart() {
        playTone(ToneGenerator.TONE_CDMA_PIP, 200)
    }

    fun playPhaseSuccess() {
        playTone(ToneGenerator.TONE_PROP_ACK, 150)
    }

    fun playPhaseFail() {
        playTone(ToneGenerator.TONE_PROP_NACK, 300)
    }

    fun playKeyClick() {
        playTone(ToneGenerator.TONE_PROP_BEEP2, 50)
    }

    fun playSuccess() {
        playTone(ToneGenerator.TONE_CDMA_HIGH_L, 500)
    }

    fun playGlitch() {
        playTone(ToneGenerator.TONE_SUP_ERROR, 100)
    }
}
