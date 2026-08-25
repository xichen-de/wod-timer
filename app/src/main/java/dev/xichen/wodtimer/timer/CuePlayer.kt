package dev.xichen.wodtimer.timer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin

enum class Cue { COUNTDOWN, WORK, REST, FINISH }

/**
 * Central phase-sound policy. Modes expose timing state; they never choose their own sounds.
 * Every new active-work period uses [Cue.WORK], and every recovery period uses [Cue.REST].
 */
internal fun transitionCue(previous: TimerSnapshot?, current: TimerSnapshot): Cue? {
    if (previous == null || current.status == TimerStatus.PAUSED) return null
    if (current.status == TimerStatus.FINISHED && previous.status != TimerStatus.FINISHED) {
        return Cue.FINISH
    }

    val workoutJustStarted = previous.status == TimerStatus.IDLE &&
        current.status == TimerStatus.RUNNING
    val phaseChanged = current.phase != previous.phase
    if (workoutJustStarted || phaseChanged) {
        return when (current.phase) {
            TimerPhase.WORK, TimerPhase.RUNNING -> Cue.WORK
            TimerPhase.REST -> Cue.REST
            else -> null
        }
    }

    val nextWorkInterval = current.currentRound != null &&
        current.currentRound != previous.currentRound
    return if (nextWorkInterval) Cue.WORK else null
}

/** Plays distinctive workout cues synthesized as PCM, independent of notification sounds. */
class CuePlayer(context: Context) {
    private val appContext = context.applicationContext
    private val audioExecutor = Executors.newCachedThreadPool()

    fun play(cue: Cue, config: TimerConfig) {
        if (config.soundEnabled) audioExecutor.execute { playPattern(cue.pattern()) }
        if (config.vibrationEnabled && cue != Cue.COUNTDOWN) vibrate(cue.vibrationPattern())
    }

    private fun playPattern(pattern: List<Note>) {
        val samples = pattern.flatMap { note -> synthesize(note).asIterable() }.toShortArray()
        if (samples.isEmpty()) return
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(samples.size * Short.SIZE_BYTES)
            .build()
        try {
            track.write(samples, 0, samples.size)
            track.setVolume(1f)
            track.play()
            Thread.sleep(pattern.sumOf { it.durationMillis + it.gapMillis }.toLong() + 80L)
        } finally {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
            track.release()
        }
    }

    private fun synthesize(note: Note): ShortArray {
        val toneSamples = SAMPLE_RATE * note.durationMillis / 1_000
        val gapSamples = SAMPLE_RATE * note.gapMillis / 1_000
        return ShortArray(toneSamples + gapSamples) { index ->
            if (index >= toneSamples) return@ShortArray 0
            val attack = (index / (SAMPLE_RATE * .008)).coerceIn(0.0, 1.0)
            val releaseStart = toneSamples - SAMPLE_RATE * .025
            val release = if (index < releaseStart) 1.0 else
                ((toneSamples - index) / (SAMPLE_RATE * .025)).coerceIn(0.0, 1.0)
            val fundamental = sin(2.0 * PI * note.frequencyHz * index / SAMPLE_RATE)
            val harmonic = sin(2.0 * PI * note.frequencyHz * 2.0 * index / SAMPLE_RATE) * .22
            ((fundamental + harmonic) * attack * release * Short.MAX_VALUE * .72)
                .toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    private fun vibrate(pattern: LongArray) {
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            appContext.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION") appContext.getSystemService(Vibrator::class.java)
        }
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    private fun Cue.pattern(): List<Note> = when (this) {
        Cue.COUNTDOWN -> listOf(Note(1_050, 150))
        Cue.WORK -> listOf(Note(650, 100, 35), Note(880, 100, 35), Note(1_220, 300))
        Cue.REST -> listOf(Note(1_050, 150, 45), Note(620, 300))
        Cue.FINISH -> listOf(Note(660, 150, 45), Note(880, 150, 45), Note(1_100, 180, 70), Note(1_320, 520))
    }

    private fun Cue.vibrationPattern(): LongArray = when (this) {
        Cue.COUNTDOWN -> longArrayOf(0, 35)
        Cue.WORK -> longArrayOf(0, 80, 45, 180)
        Cue.REST -> longArrayOf(0, 220)
        Cue.FINISH -> longArrayOf(0, 100, 60, 100, 60, 350)
    }

    private data class Note(val frequencyHz: Int, val durationMillis: Int, val gapMillis: Int = 0)

    private companion object { const val SAMPLE_RATE = 44_100 }
}
