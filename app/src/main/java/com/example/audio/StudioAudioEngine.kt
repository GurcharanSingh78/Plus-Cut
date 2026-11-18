package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.model.AudioTrackPreset
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class StudioAudioEngine(private val scope: CoroutineScope) {
  private var playbackJob: Job? = null
  private var currentTrackId: String? = null
  @Volatile private var targetVolume: Float = 0.8f

  val builtInPresets: List<AudioTrackPreset> =
    listOf(
      AudioTrackPreset(
        id = "track_neon_pulse",
        title = "Cyberpunk Pulse",
        mood = "Synthwave • Energetic",
        bpm = 124,
        durationMs = 15_000L,
        waveform = generateWaveform(48, seed = 11),
      ),
      AudioTrackPreset(
        id = "track_midnight_lofi",
        title = "Midnight Lo-Fi",
        mood = "Chill • Urban",
        bpm = 88,
        durationMs = 18_000L,
        waveform = generateWaveform(48, seed = 23),
      ),
      AudioTrackPreset(
        id = "track_cinema_rise",
        title = "Horizon Rise",
        mood = "Cinematic • Trailer",
        bpm = 110,
        durationMs = 16_000L,
        waveform = generateWaveform(48, seed = 37),
      ),
      AudioTrackPreset(
        id = "track_hyper_cut",
        title = "Hyper Cut Trap",
        mood = "Upbeat • Reels",
        bpm = 136,
        durationMs = 14_000L,
        waveform = generateWaveform(48, seed = 53),
      ),
    )

  fun startLoop(presetId: String, volume: Float, startOffsetMs: Long = 0L) {
    targetVolume = volume.coerceIn(0f, 1f)
    if (currentTrackId == presetId && playbackJob?.isActive == true) {
      return
    }
    stop()
    currentTrackId = presetId
    val preset = builtInPresets.firstOrNull { it.id == presetId } ?: builtInPresets.first()
    playbackJob =
      scope.launch(Dispatchers.Default) {
        val sampleRate = 22050
        val minBuf =
          AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
          )
        if (minBuf <= 0) return@launch
        val track =
          try {
            AudioTrack.Builder()
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
              .setBufferSizeInBytes(maxOf(minBuf, 4096))
              .setTransferMode(AudioTrack.MODE_STREAM)
              .build()
          } catch (_: Throwable) {
            null
          } ?: return@launch

        try {
          track.play()
          val chunkSamples = 1024
          val buffer = ShortArray(chunkSamples)
          var sampleIndex = ((startOffsetMs / 1000.0) * sampleRate).toLong()
          val beatSec = 60.0 / preset.bpm

          while (isActive) {
            val vol = targetVolume
            for (i in 0 until chunkSamples) {
              val t = (sampleIndex + i).toDouble() / sampleRate
              val beatPos = (t % beatSec) / beatSec
              val barStep = ((t / beatSec).toInt()) % 8

              val sample =
                when (preset.id) {
                  "track_neon_pulse" -> {
                    val bassFreq = listOf(55.0, 55.0, 65.41, 73.42, 55.0, 82.41, 73.42, 65.41)[barStep]
                    val arpFreq = bassFreq * 4.0 * if (beatPos < 0.5) 1.0 else 1.5
                    val kick = exp(-beatPos * 14.0) * sin(2 * PI * (110.0 - 65.0 * beatPos) * t)
                    val synth = 0.35 * sin(2 * PI * bassFreq * t) + 0.18 * sin(2 * PI * arpFreq * t)
                    (kick * 0.65 + synth)
                  }
                  "track_midnight_lofi" -> {
                    val chord = listOf(220.0, 261.63, 329.63, 293.66)[barStep % 4]
                    val pad = 0.35 * sin(2 * PI * chord * t) + 0.22 * sin(2 * PI * chord * 1.25 * t)
                    val thump = if (barStep % 2 == 0) exp(-beatPos * 10.0) * sin(2 * PI * 75.0 * t) * 0.4 else 0.0
                    pad + thump
                  }
                  "track_cinema_rise" -> {
                    val root = if (barStep < 4) 110.0 else 130.81
                    val pulse = exp(-beatPos * 6.0) * sin(2 * PI * root * t) * 0.5
                    val shimmer = 0.25 * sin(2 * PI * (root * 3.0) * t)
                    pulse + shimmer
                  }
                  else -> {
                    val freq = listOf(65.41, 65.41, 98.0, 77.78, 65.41, 110.0, 98.0, 87.31)[barStep]
                    val kick = exp(-beatPos * 16.0) * sin(2 * PI * 90.0 * t) * 0.7
                    val hat = if (beatPos > 0.48 && beatPos < 0.55) 0.15 * sin(2 * PI * 1800.0 * t) else 0.0
                    kick + hat + 0.3 * sin(2 * PI * freq * t)
                  }
                }
              val clamped = (sample * vol * 0.45 * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
              buffer[i] = clamped.toShort()
            }
            sampleIndex += chunkSamples
            track.write(buffer, 0, chunkSamples)
          }
        } catch (_: Throwable) {
          // AudioTrack safely released on interruption
        } finally {
          try {
            track.pause()
            track.flush()
            track.release()
          } catch (_: Throwable) {
            // Ignore release errors
          }
        }
      }
  }

  fun updateVolume(volume: Float) {
    targetVolume = volume.coerceIn(0f, 1f)
  }

  fun stop() {
    playbackJob?.cancel()
    playbackJob = null
    currentTrackId = null
  }

  private fun generateWaveform(count: Int, seed: Int): List<Float> {
    return List(count) { idx ->
      val v = kotlin.math.abs(sin((idx + seed) * 0.63)) * 0.65 +
        kotlin.math.abs(sin((idx * 2.1 + seed) * 0.37)) * 0.35
      v.toFloat().coerceIn(0.18f, 1f)
    }
  }
}
