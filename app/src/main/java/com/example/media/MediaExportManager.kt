package com.example.media

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
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
import java.io.FileInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val timestamp = System.currentTimeMillis()
    val finalFileName = "${safeName}_${resolution.label}_${fps.fps}fps_${timestamp}.mp4"
    val tempDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val tempOutputFile = File(tempDir, finalFileName)
    if (tempOutputFile.exists()) tempOutputFile.delete()

    val firstLocalVideo =
      state.videoTrack.firstOrNull { it.source is MediaSource.LocalVideo }

    if (firstLocalVideo != null && firstLocalVideo.source is MediaSource.LocalVideo) {
      startMedia3TransformerExport(
        clip = firstLocalVideo,
        source = firstLocalVideo.source,
        state = state,
        resolution = resolution,
        fps = fps,
        tempOutputFile = tempOutputFile,
        finalFileName = finalFileName,
        onProgress = onProgress,
        onComplete = onComplete,
        onError = onError,
      )
    } else {
      onError("Please add at least one video from your phone gallery before exporting.")
    }
  }

  private fun startMedia3TransformerExport(
    clip: com.example.model.VideoClip,
    source: MediaSource.LocalVideo,
    state: EditorUiState,
    resolution: ExportResolution,
    fps: ExportFps,
    tempOutputFile: File,
    finalFileName: String,
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
              onProgress(0.95f, "Saving video to Phone Gallery...")
              scope.launch(Dispatchers.IO) {
                val galleryPath = saveMp4ToGallery(tempOutputFile, finalFileName)
                val sizeMb =
                  String.format(
                    "%.1f MB",
                    (tempOutputFile.length().coerceAtLeast(1024L)) / (1024f * 1024f),
                  )
                tempOutputFile.delete()
                withContext(Dispatchers.Main) {
                  if (galleryPath != null) {
                    onProgress(1.0f, "Saved to Gallery!")
                    onComplete(
                      finalFileName,
                      galleryPath,
                      "Saved to Gallery (Movies/PlusCut) • ${resolution.label} • ${fps.label} • $sizeMb",
                    )
                  } else {
                    onError("Video rendered but failed to save into Gallery.")
                  }
                }
              }
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
        onProgress(0.05f, "Rendering video...")
        transformer.start(editedMediaItem, tempOutputFile.absolutePath)

        val pollRunnable =
          object : Runnable {
            override fun run() {
              if (finished || activeTransformer == null) return
              val pState = transformer.getProgress(progressHolder)
              if (pState == Transformer.PROGRESS_STATE_AVAILABLE) {
                val pct = (progressHolder.progress / 100f).coerceIn(0.05f, 0.92f)
                onProgress(pct, "Rendering video (${(pct * 100).toInt()}%)...")
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

  private fun saveMp4ToGallery(sourceFile: File, displayName: String): String? {
    if (!sourceFile.exists() || sourceFile.length() == 0L) return null
    return try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values =
          ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(
              MediaStore.Video.Media.RELATIVE_PATH,
              Environment.DIRECTORY_MOVIES + "/PlusCut",
            )
            put(MediaStore.Video.Media.IS_PENDING, 1)
          }
        val resolver = context.contentResolver
        val uri =
          resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        resolver.openOutputStream(uri)?.use { out ->
          FileInputStream(sourceFile).use { input -> input.copyTo(out) }
        }
        values.clear()
        values.put(MediaStore.Video.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        "Movies/PlusCut/$displayName"
      } else {
        @Suppress("DEPRECATION")
        val moviesDir =
          File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "PlusCut",
          ).apply { mkdirs() }
        val destFile = File(moviesDir, displayName)
        sourceFile.copyTo(destFile, overwrite = true)
        MediaScannerConnection.scanFile(
          context,
          arrayOf(destFile.absolutePath),
          arrayOf("video/mp4"),
          null,
        )
        destFile.absolutePath
      }
    } catch (_: Exception) {
      null
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
