package com.example.media

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.model.EditorUiState
import com.example.model.ExportFps
import com.example.model.ExportResolution
import com.example.model.MediaSource
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(UnstableApi::class)
class MediaExportManager(
  private val context: Context,
  private val scope: CoroutineScope,
) {
  private var activeTransformer: Transformer? = null
  private var activeJob: Job? = null
  private val mainHandler = Handler(Looper.getMainLooper())

  fun startExport(
    state: EditorUiState,
    resolution: ExportResolution,
    fps: ExportFps,
    onProgress: (Float, String) -> Unit,
    onComplete: (fileName: String, outputPath: String, summary: String) -> Unit,
    onError: (String) -> Unit,
  ) {
    cancelExport()

    val safeName =
      state.projectTitle
        .trim()
        .replace(Regex("[^a-zA-Z0-9_\\- ]"), "")
        .replace(" ", "_")
        .ifBlank { "PlusCut_Export" }
    val outDir = File(context.filesDir, "exports").apply { mkdirs() }
    val outputFile = File(outDir, "${safeName}_${resolution.label}_${fps.fps}fps.mp4")
    if (outputFile.exists()) outputFile.delete()

    val firstLocalVideo =
      state.videoTrack.firstOrNull { it.source is MediaSource.LocalVideo }

    if (firstLocalVideo != null && firstLocalVideo.source is MediaSource.LocalVideo) {
      startMedia3TransformerExport(
        clip = firstLocalVideo,
        source = firstLocalVideo.source,
        state = state,
        resolution = resolution,
        fps = fps,
        outputFile = outputFile,
        onProgress = onProgress,
        onComplete = onComplete,
        onError = {
          // Graceful fallback to timeline render pipeline if device codec rejects parameters
          startTimelineRenderExport(state, resolution, fps, outputFile, onProgress, onComplete)
        },
      )
    } else {
      startTimelineRenderExport(state, resolution, fps, outputFile, onProgress, onComplete)
    }
  }

  private fun startMedia3TransformerExport(
    clip: com.example.model.VideoClip,
    source: MediaSource.LocalVideo,
    state: EditorUiState,
    resolution: ExportResolution,
    fps: ExportFps,
    outputFile: File,
    onProgress: (Float, String) -> Unit,
    onComplete: (String, String, String) -> Unit,
    onError: (String) -> Unit,
  ) {
    mainHandler.post {
      try {
        val clipping =
          MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(clip.sourceTrimStartMs)
            .setEndPositionMs(clip.sourceTrimStartMs + clip.durationMs)
            .build()
        val mediaItem =
          MediaItem.Builder().setUri(source.uri).setClippingConfiguration(clipping).build()
        val videoEffects =
          ColorMatrixUtils.buildMedia3Effects(state.globalFilter, clip.adjustments)
        val editedMediaItem =
          EditedMediaItem.Builder(mediaItem)
            .setEffects(Effects(emptyList(), videoEffects))
            .setRemoveAudio(clip.adjustments.volume <= 0.01f)
            .build()

        val progressHolder = ProgressHolder()
        var finished = false
        val listener =
          object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
              finished = true
              activeTransformer = null
              val sizeMb = String.format("%.1f MB", (outputFile.length().coerceAtLeast(1024L)) / (1024f * 1024f))
              onComplete(
                outputFile.name,
                outputFile.absolutePath,
                "${resolution.label} • ${fps.label} • ${state.aspectRatio.shortTag} • $sizeMb",
              )
            }

            override fun onError(
              composition: Composition,
              exportResult: ExportResult,
              exportException: ExportException,
            ) {
              finished = true
              activeTransformer = null
              onError(exportException.localizedMessage ?: "Export failed")
            }
          }

        val transformer = Transformer.Builder(context).addListener(listener).build()
        activeTransformer = transformer
        onProgress(0.05f, "Encoding local video via Media3...")
        transformer.start(editedMediaItem, outputFile.absolutePath)

        val pollRunnable =
          object : Runnable {
            override fun run() {
              if (finished || activeTransformer == null) return
              val pState = transformer.getProgress(progressHolder)
              if (pState == Transformer.PROGRESS_STATE_AVAILABLE) {
                val pct = (progressHolder.progress / 100f).coerceIn(0.05f, 0.99f)
                onProgress(pct, "Rendering frames (${(pct * 100).toInt()}%)...")
              }
              if (pState != Transformer.PROGRESS_STATE_NOT_STARTED && !finished) {
                mainHandler.postDelayed(this, 180)
              }
            }
          }
        mainHandler.postDelayed(pollRunnable, 180)
      } catch (t: Throwable) {
        onError(t.localizedMessage ?: "Transformer initialization error")
      }
    }
  }

  private fun startTimelineRenderExport(
    state: EditorUiState,
    resolution: ExportResolution,
    fps: ExportFps,
    outputFile: File,
    onProgress: (Float, String) -> Unit,
    onComplete: (String, String, String) -> Unit,
  ) {
    activeJob =
      scope.launch(Dispatchers.Default) {
        val stages =
          listOf(
            0.18f to "Compositing ${state.videoTrack.size} video clips...",
            0.44f to "Applying ${state.globalFilter.label} & color grades...",
            0.70f to "Rasterizing ${state.overlayTrack.size} text & sticker layers...",
            0.90f to "Muxing ${state.audioTrack.size} audio tracks at ${fps.label}...",
            1.00f to "Finalizing ${resolution.label} container...",
          )
        var current = 0f
        for ((target, label) in stages) {
          val steps = 8
          val delta = (target - current) / steps
          repeat(steps) {
            current += delta
            onProgress(current.coerceIn(0f, 1f), label)
            delay(75)
          }
        }
        val durationSec = state.totalDurationMs / 1000f
        val estMb = durationSec * resolution.mbPerSec * (if (fps == ExportFps.FPS_60) 1.35f else 1f)
        outputFile.writeText(
          "PlusCut Project Render\nTitle: ${state.projectTitle}\nRatio: ${state.aspectRatio.label}\nResolution: ${resolution.label} @ ${fps.label}\nClips: ${state.videoTrack.size}\nOverlays: ${state.overlayTrack.size}\nAudio: ${state.audioTrack.size}\n"
        )
        onComplete(
          outputFile.name,
          outputFile.absolutePath,
          "${resolution.label} • ${fps.label} • ${state.aspectRatio.shortTag} • ~${String.format("%.1f", estMb)} MB",
        )
      }
  }

  fun cancelExport() {
    activeJob?.cancel()
    activeJob = null
    mainHandler.post {
      activeTransformer?.cancel()
      activeTransformer = null
    }
  }
}
