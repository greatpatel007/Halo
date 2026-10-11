package com.halo.core.designsystem.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTimestamp
import android.media.AudioTrack
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Semaphore
import kotlin.math.cos
import kotlin.math.sin

/**
 * Plays Halo's synthesised UI sounds with low latency: one stereo float [AudioTrack] in low-latency mode, fed by a
 * small mixer thread in 4 ms blocks, so overlapping sounds mix instead of cutting each other off. The track pauses
 * after a moment of silence and the thread sleeps, so it costs nothing at rest.
 *
 * Etiquette, as on iOS: UI sounds follow the ringer (silent and vibrate mean no sound), the system "touch sounds"
 * setting and the in-app switch, and play as sonification (system volume).
 */
internal class SoundEngine(context: Context) {

    @Volatile var enabled: Boolean = true

    /**
     * Measured time from writing a sample to it leaving the speaker (ms), from the track's own timestamps; −1 until
     * measured. Used to land a haptic and its sound together.
     */
    @Volatile var outputLatencyMs: Float = -1f
        private set

    private val app = context.applicationContext
    private val audio = app.getSystemService(AudioManager::class.java)
    private val buffers = ConcurrentHashMap<HaloSoundCue, FloatArray>()
    private val incoming = ConcurrentLinkedQueue<MixVoice>()
    private val wake = Semaphore(0)
    @Volatile private var allowedUntil = 0L
    @Volatile private var allowed = true
    private val thread = Thread(::mixLoop, "HaloSound").apply { isDaemon = true; start() }

    /** Renders [cues] ahead of time (off the main thread), so the first play has no synthesis cost. */
    fun prepare(cues: Collection<HaloSoundCue>) {
        Thread({ cues.forEach { buffer(it) } }, "HaloSoundPrepare").apply { isDaemon = true; start() }
    }

    /** Plays [cue], panned by [pan] (−1 left … +1 right). Returns false when etiquette keeps it silent. */
    fun play(cue: HaloSoundCue, pan: Float = 0f): Boolean = play(Voice(buffer(cue), pan.coerceIn(-1f, 1f)))

    /** Plays a live voice (e.g. the Tide's water). Returns false when etiquette keeps it silent. */
    fun play(voice: MixVoice): Boolean {
        if (!enabled || !systemAllows()) return false
        incoming += voice
        wake.release()
        return true
    }

    private fun buffer(cue: HaloSoundCue): FloatArray = buffers.getOrPut(cue) { SoundSynth.render(cue) }

    /** Ringer mode and the system touch-sounds setting, re-read at most once a second. */
    private fun systemAllows(): Boolean {
        val now = SystemClock.uptimeMillis()
        if (now > allowedUntil) {
            val ringer = audio?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
            val touchSounds = Settings.System.getInt(app.contentResolver, Settings.System.SOUND_EFFECTS_ENABLED, 1) != 0
            allowed = ringer == AudioManager.RINGER_MODE_NORMAL && touchSounds
            allowedUntil = now + ETIQUETTE_TTL_MS
        }
        return allowed
    }

    /** A pre-rendered sound, equal-power panned. */
    private class Voice(val pcm: FloatArray, pan: Float) : MixVoice {
        private var at = 0
        private val left: Float = cos((pan + 1f) * QUARTER_PI)
        private val right: Float = sin((pan + 1f) * QUARTER_PI)

        override fun mix(block: FloatArray, frames: Int): Boolean {
            val n = minOf(frames, pcm.size - at)
            for (f in 0 until n) {
                val s = pcm[at + f]
                block[f * 2] += s * left
                block[f * 2 + 1] += s * right
            }
            at += n
            return at < pcm.size
        }
    }

    private fun mixLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(SoundSynth.SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build(),
            )
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        val block = FloatArray(BLOCK_FRAMES * 2)
        val stamp = AudioTimestamp()
        var written = 0L
        var blocksSinceStamp = 0
        val active = ArrayList<MixVoice>()
        var silentBlocks = 0
        var playing = false
        while (true) {
            if (active.isEmpty() && incoming.isEmpty()) {
                if (playing && silentBlocks > IDLE_BLOCKS) {
                    track.pause()
                    track.flush()
                    playing = false
                }
                if (!playing) {
                    wake.acquireUninterruptibly()
                    wake.drainPermits()
                }
            }
            while (true) active += incoming.poll() ?: break
            if (!playing && active.isNotEmpty()) {
                track.play()
                // After a pause the track restarts its frame count.
                written = 0L
                playing = true
            }
            block.fill(0f)
            val it = active.iterator()
            while (it.hasNext()) {
                if (!it.next().mix(block, BLOCK_FRAMES)) it.remove()
            }
            silentBlocks = if (active.isEmpty()) silentBlocks + 1 else 0
            if (playing) {
                // Soft limit so several overlapping sounds never clip.
                for (i in block.indices) {
                    val x = block[i]
                    block[i] = if (x > 0.8f) 0.8f + (x - 0.8f) / (1f + (x - 0.8f) * 5f) else if (x < -0.8f) -0.8f + (x + 0.8f) / (1f - (x + 0.8f) * 5f) else x
                }
                track.write(block, 0, block.size, AudioTrack.WRITE_BLOCKING)
                written += BLOCK_FRAMES
                // Latency = frames queued ahead of the one being presented now, plus time since that timestamp.
                if (++blocksSinceStamp >= STAMP_EVERY_BLOCKS && track.getTimestamp(stamp)) {
                    blocksSinceStamp = 0
                    val queuedMs = (written - stamp.framePosition) * 1000f / SoundSynth.SAMPLE_RATE
                    val sinceMs = (System.nanoTime() - stamp.nanoTime) / 1_000_000f
                    val latency = (queuedMs - sinceMs).coerceIn(0f, MAX_SANE_LATENCY_MS)
                    outputLatencyMs = if (outputLatencyMs < 0f) latency else outputLatencyMs * 0.8f + latency * 0.2f
                }
            }
        }
    }

    private companion object {
        /** 4 ms at 48 kHz: small blocks keep latency low. */
        const val BLOCK_FRAMES = 192
        /** ~300 ms of silence before the track pauses. */
        const val IDLE_BLOCKS = 75
        const val ETIQUETTE_TTL_MS = 1_000L
        const val QUARTER_PI = 0.7853982f
        const val STAMP_EVERY_BLOCKS = 25
        const val MAX_SANE_LATENCY_MS = 300f
    }
}
