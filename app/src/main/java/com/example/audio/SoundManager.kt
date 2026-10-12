package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Procedural low-latency sound generator for Stack Ball.
 * Generates crisp, arcade-quality sound effects dynamically without needing external asset files.
 */
class SoundManager(private val isSoundEnabled: () -> Boolean) {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val sampleRate = 22050

    // Concurrency guard to prevent AudioTrack native resource exhaustion
    private val activeSfxCount = AtomicInteger(0)
    private val maxConcurrentSfx = 4

    // Background Music (BGM) state
    @Volatile
    private var bgmTrack: AudioTrack? = null
    @Volatile
    private var isBgmPlaying: Boolean = false
    @Volatile
    private var isBgmPaused: Boolean = false
    private val bgmVolume: Float = 0.25f // Sound sound set to 25% volume as requested

    // Pre-generated audio sample buffers for lowest possible latency
    private val bounceSamples: ByteArray by lazy { generateBounceWave() }
    private val gameOverSamples: ByteArray by lazy { generateGameOverWave() }
    private val winSamples: ByteArray by lazy { generateWinFanfareWave() }
    private val clickSamples: ByteArray by lazy { generateClickWave() }
    @Volatile
    private var cachedBgmSamples: ByteArray? = null

    init {
        // Pre-generate BGM in background immediately so UI thread NEVER freezes when Play is pressed
        scope.launch(Dispatchers.Default) {
            cachedBgmSamples = generateBgmLoopWave()
        }
    }

    private fun generateBounceWave(): ByteArray {
        val durationMs = 70
        val numSamples = (sampleRate * durationMs) / 1000
        val buffer = ByteArray(numSamples * 2)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            // Frequency drop: 520Hz down to 240Hz
            val freq = 520.0 - 280.0 * progress
            val amplitude = (1.0 - progress) * 0.75
            val sample = (sin(2.0 * PI * freq * t) * amplitude * Short.MAX_VALUE).toInt().toShort()
            buffer[2 * i] = (sample.toInt() and 0xFF).toByte()
            buffer[2 * i + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }
        return buffer
    }

    private fun generateClickWave(): ByteArray {
        val durationMs = 35
        val numSamples = (sampleRate * durationMs) / 1000
        val buffer = ByteArray(numSamples * 2)
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val freq = 800.0
            val amplitude = (1.0 - progress) * 0.4
            val sample = (sin(2.0 * PI * freq * (i.toDouble() / sampleRate)) * amplitude * Short.MAX_VALUE).toInt().toShort()
            buffer[2 * i] = (sample.toInt() and 0xFF).toByte()
            buffer[2 * i + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }
        return buffer
    }

    private fun generateSmashWave(pitchMultiplier: Float): ByteArray {
        val durationMs = 90
        val numSamples = (sampleRate * durationMs) / 1000
        val buffer = ByteArray(numSamples * 2)
        val rnd = Random(42)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val decay = (1.0 - progress) * (1.0 - progress)
            // Crunchy impact: low punch + high noise
            val toneFreq = 180.0 * pitchMultiplier
            val tone = sin(2.0 * PI * toneFreq * t) * 0.6
            val noise = (rnd.nextDouble() * 2.0 - 1.0) * 0.4
            val amplitude = decay * 0.85
            val sample = ((tone + noise) * amplitude * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            buffer[2 * i] = (sample.toInt() and 0xFF).toByte()
            buffer[2 * i + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }
        return buffer
    }

    private fun generateFireSmashWave(): ByteArray {
        val durationMs = 130
        val numSamples = (sampleRate * durationMs) / 1000
        val buffer = ByteArray(numSamples * 2)
        val rnd = Random(123)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val decay = (1.0 - progress)
            // Heavy blazing fireball whoosh & explosion
            val subBass = sin(2.0 * PI * (90.0 + 80.0 * (1.0 - progress)) * t) * 0.5
            val hiss = (rnd.nextDouble() * 2.0 - 1.0) * 0.5
            val amplitude = decay * 0.95
            val sample = ((subBass + hiss) * amplitude * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            buffer[2 * i] = (sample.toInt() and 0xFF).toByte()
            buffer[2 * i + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }
        return buffer
    }

    private fun generateGameOverWave(): ByteArray {
        val durationMs = 280
        val numSamples = (sampleRate * durationMs) / 1000
        val buffer = ByteArray(numSamples * 2)
        val rnd = Random(999)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val decay = (1.0 - progress)
            // Harsh shattering clash and pitch dive
            val freq = 400.0 * (1.0 - progress * 0.7)
            val tone = sin(2.0 * PI * freq * t) * 0.4
            val noise = (rnd.nextDouble() * 2.0 - 1.0) * 0.6
            val amplitude = decay * 0.9
            val sample = ((tone + noise) * amplitude * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            buffer[2 * i] = (sample.toInt() and 0xFF).toByte()
            buffer[2 * i + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }
        return buffer
    }

    private fun generateWinFanfareWave(): ByteArray {
        val durationMs = 450
        val numSamples = (sampleRate * durationMs) / 1000
        val buffer = ByteArray(numSamples * 2)
        // 4 notes: C5 (523), E5 (659), G5 (784), C6 (1046)
        val noteDur = numSamples / 4
        for (i in 0 until numSamples) {
            val noteIndex = (i / noteDur).coerceIn(0, 3)
            val freq = when (noteIndex) {
                0 -> 523.25
                1 -> 659.25
                2 -> 783.99
                else -> 1046.50
            }
            val noteProgress = (i % noteDur).toDouble() / noteDur
            val decay = 1.0 - noteProgress * 0.5
            val t = i.toDouble() / sampleRate
            val tone = sin(2.0 * PI * freq * t) * 0.7
            val sample = (tone * decay * Short.MAX_VALUE).toInt().toShort()
            buffer[2 * i] = (sample.toInt() and 0xFF).toByte()
            buffer[2 * i + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }
        return buffer
    }

    private fun playPcm(pcm: ByteArray) {
        if (!isSoundEnabled()) return
        if (activeSfxCount.get() >= maxConcurrentSfx) return
        scope.launch(Dispatchers.Default) {
            activeSfxCount.incrementAndGet()
            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(pcm.size)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(pcm, 0, pcm.size)
                track.play()
                val durationMs = (pcm.size.toDouble() / (sampleRate * 2) * 1000).toLong() + 20
                delay(durationMs)
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {}
            } catch (_: Exception) {
            } finally {
                activeSfxCount.decrementAndGet()
            }
        }
    }

    private val unlockSamples: ByteArray by lazy { generateUnlockWave() }
    private val gameStartSamples: ByteArray by lazy { generateGameStartWave() }

    private fun generateUnlockWave(): ByteArray {
        val durationMs = 180
        val numSamples = (sampleRate * durationMs) / 1000
        val buffer = ByteArray(numSamples * 2)
        val half = numSamples / 2
        for (i in 0 until numSamples) {
            val freq = if (i < half) 880.0 else 1318.5
            val progress = (i % half).toDouble() / half
            val decay = 1.0 - progress * 0.7
            val sample = (sin(2.0 * PI * freq * (i.toDouble() / sampleRate)) * decay * 0.75 * Short.MAX_VALUE).toInt().toShort()
            buffer[2 * i] = (sample.toInt() and 0xFF).toByte()
            buffer[2 * i + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }
        return buffer
    }

    private fun generateGameStartWave(): ByteArray {
        val durationMs = 200
        val numSamples = (sampleRate * durationMs) / 1000
        val buffer = ByteArray(numSamples * 2)
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val freq = 300.0 + 500.0 * progress
            val decay = sin(progress * PI)
            val sample = (sin(2.0 * PI * freq * (i.toDouble() / sampleRate)) * decay * 0.75 * Short.MAX_VALUE).toInt().toShort()
            buffer[2 * i] = (sample.toInt() and 0xFF).toByte()
            buffer[2 * i + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }
        return buffer
    }

    /**
     * Procedural upbeat arcade EDM background music loop (128 BPM, 4-bar phrase in Am - F - C - G).
     * Synthesized with punchy kick drum, snare, hi-hats, bouncy bassline, and catchy arpeggiated lead.
     * Perfectly loops indefinitely with zero seams.
     */
    private fun generateBgmLoopWave(): ByteArray {
        val bpm = 128.0
        val beatDuration = 60.0 / bpm // ~0.46875s
        val totalBeats = 16 // 4 bars * 4 beats = ~7.5 seconds loop
        val loopDurationSec = totalBeats * beatDuration
        val numSamples = (sampleRate * loopDurationSec).toInt()
        val buffer = ByteArray(numSamples * 2)

        // Melody note frequencies for each 8th note in 4 bars (32 half-beat notes)
        val melodyNotes = doubleArrayOf(
            // Bar 1: Am (A, C, E, G)
            659.25, 783.99, 880.00, 1046.50, 880.00, 783.99, 659.25, 587.33,
            // Bar 2: F (F, A, C, E)
            523.25, 698.46, 880.00, 1046.50, 880.00, 698.46, 659.25, 523.25,
            // Bar 3: C (C, E, G, B)
            659.25, 783.99, 1046.50, 1318.51, 1046.50, 783.99, 659.25, 587.33,
            // Bar 4: G (G, B, D, F)
            587.33, 783.99, 987.77, 1174.66, 987.77, 783.99, 659.25, 587.33
        )

        // Root bass frequencies for the 4 bars
        val bassRoots = doubleArrayOf(55.0, 43.65, 65.41, 48.99)

        val rnd = Random(777)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val currentBeat = t / beatDuration
            val currentBar = ((currentBeat / 4.0).toInt()) % 4
            val beatFrac = currentBeat - currentBeat.toInt()

            // 1. Bassline (16th note plucks, 4 plucks per beat)
            val sub16th = (beatFrac * 4.0)
            val sub16thFrac = sub16th - sub16th.toInt()
            val bassEnv = (1.0 - sub16thFrac) * (1.0 - sub16thFrac)
            val rootFreq = bassRoots[currentBar]
            val bassTone = (sin(2.0 * PI * rootFreq * t) * 0.75 + sin(4.0 * PI * rootFreq * t) * 0.25) * bassEnv

            // 2. Drums: Punchy Kick on every beat
            val kickT = beatFrac * beatDuration
            val kickEnv = (1.0 - (kickT / 0.18).coerceIn(0.0, 1.0))
            val kickFreq = 48.0 + 95.0 * kickEnv * kickEnv
            val kick = if (kickT < 0.18) sin(2.0 * PI * kickFreq * kickT) * kickEnv else 0.0

            // 3. Drums: Crisp Snare on beats 2 and 4 (beat index 1 and 3)
            val isSnareBeat = (currentBeat.toInt() % 2 == 1)
            val snareT = beatFrac * beatDuration
            val snareEnv = if (isSnareBeat && snareT < 0.16) {
                (1.0 - snareT / 0.16) * (1.0 - snareT / 0.16)
            } else 0.0
            val snare = (rnd.nextDouble() * 2.0 - 1.0) * snareEnv * 0.85

            // 4. Drums: Hi-hat on every 8th note
            val sub8th = (beatFrac * 2.0)
            val sub8thFrac = sub8th - sub8th.toInt()
            val hatT = sub8thFrac * (beatDuration / 2.0)
            val hatEnv = if (hatT < 0.04) (1.0 - hatT / 0.04) else 0.0
            val hihat = (rnd.nextDouble() * 2.0 - 1.0) * hatEnv * 0.35

            // 5. Synth Lead Arpeggio (8th notes, 2 notes per beat)
            val melodyIdx = ((currentBeat * 2.0).toInt()).coerceIn(0, melodyNotes.size - 1)
            val noteFreq = melodyNotes[melodyIdx]
            val leadEnv = (1.0 - sub8thFrac * 0.85).coerceIn(0.0, 1.0)
            val lead = (sin(2.0 * PI * noteFreq * t) * 0.65 +
                sin(4.0 * PI * noteFreq * t) * 0.25 +
                sin(6.0 * PI * noteFreq * t) * 0.10) * leadEnv

            // 6. Master Mix (Balanced for crystal clarity, set to 25% master volume)
            val mix = (bassTone * 0.28 + kick * 0.38 + snare * 0.22 + hihat * 0.14 + lead * 0.32)
            // User explicit requirement: "song sound 25%"
            val finalAmplitude = (mix * 0.25 * Short.MAX_VALUE)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()

            buffer[2 * i] = (finalAmplitude.toInt() and 0xFF).toByte()
            buffer[2 * i + 1] = ((finalAmplitude.toInt() shr 8) and 0xFF).toByte()
        }
        return buffer
    }

    fun startMusic() {
        if (!isSoundEnabled()) return
        scope.launch(Dispatchers.Default) {
            try {
                synchronized(this@SoundManager) {
                    if (bgmTrack == null) {
                        val pcm = cachedBgmSamples ?: generateBgmLoopWave().also { cachedBgmSamples = it }
                        val numFrames = pcm.size / 2
                        val track = AudioTrack.Builder()
                            .setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_GAME)
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
                            .setBufferSizeInBytes(pcm.size)
                            .setTransferMode(AudioTrack.MODE_STATIC)
                            .build()

                        track.write(pcm, 0, pcm.size)
                        track.setLoopPoints(0, numFrames, -1)
                        track.setVolume(bgmVolume)
                        bgmTrack = track
                    }
                    bgmTrack?.let { track ->
                        track.setVolume(bgmVolume)
                        track.play()
                        isBgmPlaying = true
                        isBgmPaused = false
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun pauseMusic() {
        scope.launch(Dispatchers.Default) {
            try {
                synchronized(this@SoundManager) {
                    if (isBgmPlaying && bgmTrack != null) {
                        bgmTrack?.pause()
                        isBgmPlaying = false
                        isBgmPaused = true
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun resumeMusic() {
        if (!isSoundEnabled()) return
        if (isBgmPaused) {
            startMusic()
        }
    }

    fun stopMusic() {
        scope.launch(Dispatchers.Default) {
            try {
                synchronized(this@SoundManager) {
                    bgmTrack?.let { track ->
                        track.pause()
                        track.reloadStaticData()
                    }
                    isBgmPlaying = false
                    isBgmPaused = false
                }
            } catch (_: Exception) {}
        }
    }

    fun release() {
        scope.launch(Dispatchers.Default) {
            try {
                synchronized(this@SoundManager) {
                    bgmTrack?.let { track ->
                        track.stop()
                        track.release()
                    }
                    bgmTrack = null
                    isBgmPlaying = false
                    isBgmPaused = false
                }
            } catch (_: Exception) {}
        }
    }

    fun playBounce() {
        playPcm(bounceSamples)
    }

    fun playSmash(combo: Int) {
        val pitch = (1.0f + (combo.coerceAtMost(15) * 0.05f))
        val pcm = generateSmashWave(pitch)
        playPcm(pcm)
    }

    fun playFireSmash() {
        val pcm = generateFireSmashWave()
        playPcm(pcm)
    }

    fun playGameOver() {
        playPcm(gameOverSamples)
    }

    fun playWin() {
        playPcm(winSamples)
    }

    fun playClick() {
        playClickInternal()
    }

    private fun playClickInternal() {
        playPcm(clickSamples)
    }

    fun playUnlock() {
        playPcm(unlockSamples)
    }

    fun playGameStart() {
        playPcm(gameStartSamples)
    }
}

