package com.neon.ascent.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.SoundPool
import com.neon.ascent.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class AmbientAudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val synthPlayer: SynthAudioPlayer
) {
    private var whiteNoiseTrack: AudioTrack? = null
    private var noiseJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private var gongSoundId: Int = -1

    init {
        val resId = context.resources.getIdentifier("gong", "raw", context.packageName)
        if (resId != 0) {
            gongSoundId = soundPool.load(context, resId, 1)
            android.util.Log.d("AmbientAudioPlayer", "SoundPool loading gong from resId: $resId")
        }
    }

    fun startWhiteNoise() {
        if (whiteNoiseTrack != null) return

        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        whiteNoiseTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        whiteNoiseTrack?.play()
        whiteNoiseTrack?.setVolume(0.35f) // Increased for better presence

        noiseJob = scope.launch {
            val samples = ShortArray(bufferSize)
            while (whiteNoiseTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                for (i in samples.indices) {
                    samples[i] = (Random.nextInt(65536) - 32768).toShort()
                }
                whiteNoiseTrack?.write(samples, 0, samples.size)
            }
        }
    }

    fun stopWhiteNoise() {
        whiteNoiseTrack?.stop()
        whiteNoiseTrack?.release()
        whiteNoiseTrack = null
        noiseJob?.cancel()
    }

    fun playGong() {
        if (gongSoundId != -1) {
            android.util.Log.d("AmbientAudioPlayer", "SoundPool playing gong...")
            soundPool.play(gongSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
        } else {
            android.util.Log.w("AmbientAudioPlayer", "Gong sound not loaded, using synth fallback")
            synthPlayer.playTone(android.media.ToneGenerator.TONE_CDMA_HIGH_L, 1000)
        }
    }
}
