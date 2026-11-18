package com.example.viewmodel

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.audio.StudioAudioEngine
import com.example.media.MediaExportManager
import com.example.model.AspectRatioOption
import com.example.model.AudioClip
import com.example.model.AudioTrackPreset
import com.example.model.EditorProjectSnapshot
import com.example.model.EditorUiState
import com.example.model.ExportFps
import com.example.model.ExportResolution
import com.example.model.ExportUiState
import com.example.model.FilterPreset
import com.example.model.MediaBinItem
import com.example.model.MediaSource
import com.example.model.OverlayItem
import com.example.model.SelectedElement
import com.example.model.StickerKind
import com.example.model.StudioTab
import com.example.model.TextAlignmentOption
import com.example.model.TextTemplateStyle
import com.example.model.VideoAdjustments
import com.example.model.VideoClip
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class StudioViewModel(application: Application) : AndroidViewModel(application) {
  private val audioEngine = StudioAudioEngine(viewModelScope)
  private val exportManager = MediaExportManager(application.applicationContext, viewModelScope)

  private val undoStack = ArrayDeque<EditorProjectSnapshot>()
  private val redoStack = ArrayDeque<EditorProjectSnapshot>()
  private var playbackJob: Job? = null

  private val _uiState = MutableStateFlow(createInitialState())
  val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

  private fun createInitialState(): EditorUiState {
    val sample1 =
      MediaBinItem(
        id = "sample_neon_city",
        label = "Neon Rain Tokyo",
        isVideo = false,
        defaultDurationMs = 5_000L,
        source =
          MediaSource.BundledSample(
            drawableResId = R.drawable.img_sample_neon_city,
            subtitle = "00:05 • 9:16",
            accentHex = 0xFF00F0FF,
          ),
      )
    val sample2 =
      MediaBinItem(
        id = "sample_golden_coast",
        label = "Golden Coast Aerial",
        isVideo = false,
        defaultDurationMs = 5_000L,
        source =
          MediaSource.BundledSample(
            drawableResId = R.drawable.img_sample_golden_coast,
            subtitle = "00:05 • 16:9",
            accentHex = 0xFFF59E0B,
          ),
      )
    val sample3 =
      MediaBinItem(
        id = "sample_abstract_pulse",
        label = "Obsidian Pulse",
        isVideo = false,
        defaultDurationMs = 4_000L,
        source =
          MediaSource.BundledSample(
            drawableResId = R.drawable.img_sample_abstract_pulse,
            subtitle = "00:04 • 1:1",
            accentHex = 0xFF8B5CF6,
          ),
      )

    val clip1 =
      VideoClip(
        id = newId("clip"),
        label = sample1.label,
        source = sample1.source,
        timelineStartMs = 0L,
        durationMs = 5_000L,
        sourceMaxDurationMs = 15_000L,
      )
    val clip2 =
      VideoClip(
        id = newId("clip"),
        label = sample2.label,
        source = sample2.source,
        timelineStartMs = 5_000L,
        durationMs = 5_000L,
        sourceMaxDurationMs = 15_000L,
      )
    val clip3 =
      VideoClip(
        id = newId("clip"),
        label = sample3.label,
        source = sample3.source,
        timelineStartMs = 10_000L,
        durationMs = 4_000L,
        sourceMaxDurationMs = 15_000L,
      )

    val introText =
      OverlayItem.TextOverlay(
        id = newId("txt"),
        text = "PLUS CUT STUDIO",
        style = TextTemplateStyle.NEON_TITLE,
        fontSizeSp = 28f,
        colorHex = 0xFF00F0FF,
        alignment = TextAlignmentOption.CENTER,
        timelineStartMs = 400L,
        durationMs = 4_200L,
        normalizedX = 0.5f,
        normalizedY = 0.78f,
      )

    val preset0 = audioEngine.builtInPresets.first()
    val introAudio =
      AudioClip(
        id = newId("aud"),
        presetId = preset0.id,
        title = preset0.title,
        bpm = preset0.bpm,
        timelineStartMs = 0L,
        durationMs = 12_000L,
        volume = 0.75f,
        waveform = preset0.waveform,
      )

    return EditorUiState(
      mediaBin = listOf(sample1, sample2, sample3),
      audioPresets = audioEngine.builtInPresets,
      videoTrack = listOf(clip1, clip2, clip3),
      overlayTrack = listOf(introText),
      audioTrack = listOf(introAudio),
      selectedElement = SelectedElement.Video(clip1.id),
    )
  }

  private fun EditorUiState.toSnapshot(): EditorProjectSnapshot =
    EditorProjectSnapshot(
      projectTitle = projectTitle,
      aspectRatio = aspectRatio,
      videoTrack = videoTrack,
      overlayTrack = overlayTrack,
      audioTrack = audioTrack,
      globalFilter = globalFilter,
    )

  private fun commitMutation(transform: (EditorUiState) -> EditorUiState) {
    _uiState.update { current ->
      undoStack.addLast(current.toSnapshot())
      if (undoStack.size > 40) undoStack.removeFirst()
      redoStack.clear()
      val updated = transform(current)
      updated.copy(
        canUndo = undoStack.isNotEmpty(),
        canRedo = false,
      )
    }
  }

  fun undo() {
    val prev = undoStack.removeLastOrNull() ?: return
    _uiState.update { current ->
      redoStack.addLast(current.toSnapshot())
      current.copy(
        projectTitle = prev.projectTitle,
        aspectRatio = prev.aspectRatio,
        videoTrack = prev.videoTrack,
        overlayTrack = prev.overlayTrack,
        audioTrack = prev.audioTrack,
        globalFilter = prev.globalFilter,
        playheadMs = current.playheadMs.coerceAtMost(
          maxOf(
            prev.videoTrack.maxOfOrNull { it.timelineEndMs } ?: 4_000L,
            prev.overlayTrack.maxOfOrNull { it.timelineEndMs } ?: 0L,
            prev.audioTrack.maxOfOrNull { it.timelineEndMs } ?: 0L,
          )
        ),
        canUndo = undoStack.isNotEmpty(),
        canRedo = redoStack.isNotEmpty(),
        statusMessage = "Undid last edit",
      )
    }
  }

  fun redo() {
    val next = redoStack.removeLastOrNull() ?: return
    _uiState.update { current ->
      undoStack.addLast(current.toSnapshot())
      current.copy(
        projectTitle = next.projectTitle,
        aspectRatio = next.aspectRatio,
        videoTrack = next.videoTrack,
        overlayTrack = next.overlayTrack,
        audioTrack = next.audioTrack,
        globalFilter = next.globalFilter,
        canUndo = undoStack.isNotEmpty(),
        canRedo = redoStack.isNotEmpty(),
        statusMessage = "Redid edit",
      )
    }
  }

  fun updateProjectTitle(title: String) {
    _uiState.update { it.copy(projectTitle = title) }
  }

  fun setAspectRatio(ratio: AspectRatioOption) {
    commitMutation { it.copy(aspectRatio = ratio, statusMessage = "Canvas ${ratio.shortTag}") }
  }

  fun selectTab(tab: StudioTab) {
    _uiState.update { it.copy(activeTab = tab) }
  }

  fun selectElement(element: SelectedElement?) {
    _uiState.update { it.copy(selectedElement = element) }
  }

  fun clearStatusMessage() {
    _uiState.update { it.copy(statusMessage = null) }
  }

  fun importLocalMedia(uri: Uri) {
    val ctx = getApplication<Application>()
    val mime = ctx.contentResolver.getType(uri).orEmpty()
    val isVideo = mime.startsWith("video/")
    var detectedDurationMs = 5_000L
    if (isVideo) {
      val retriever = MediaMetadataRetriever()
      try {
        retriever.setDataSource(ctx, uri)
        val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        detectedDurationMs = (durStr?.toLongOrNull() ?: 8_000L).coerceIn(1_000L, 120_000L)
      } catch (_: Throwable) {
        detectedDurationMs = 8_000L
      } finally {
        try {
          retriever.release()
        } catch (_: Throwable) {
        }
      }
    }

    val index = _uiState.value.mediaBin.size + 1
    val label = if (isVideo) "Clip #$index" else "Photo #$index"
    val source =
      if (isVideo) MediaSource.LocalVideo(uri, detectedDurationMs) else MediaSource.LocalImage(uri)
    val binItem =
      MediaBinItem(
        id = newId("media"),
        label = label,
        isVideo = isVideo,
        defaultDurationMs = detectedDurationMs,
        source = source,
      )

    commitMutation { state ->
      val startAt = state.videoTrack.maxOfOrNull { it.timelineEndMs } ?: 0L
      val clip =
        VideoClip(
          id = newId("clip"),
          label = binItem.label,
          source = binItem.source,
          timelineStartMs = startAt,
          durationMs = binItem.defaultDurationMs,
          sourceMaxDurationMs = if (isVideo) detectedDurationMs else 30_000L,
        )
      state.copy(
        mediaBin = listOf(binItem) + state.mediaBin,
        videoTrack = state.videoTrack + clip,
        selectedElement = SelectedElement.Video(clip.id),
        playheadMs = startAt,
        statusMessage = "Added $label to timeline",
      )
    }
  }

  fun addMediaToTimeline(item: MediaBinItem) {
    commitMutation { state ->
      val startAt = state.videoTrack.maxOfOrNull { it.timelineEndMs } ?: 0L
      val newClip =
        VideoClip(
          id = newId("clip"),
          label = item.label,
          source = item.source,
          timelineStartMs = startAt,
          durationMs = item.defaultDurationMs,
          sourceMaxDurationMs = if (item.isVideo) item.defaultDurationMs else 30_000L,
        )
      state.copy(
        videoTrack = state.videoTrack + newClip,
        selectedElement = SelectedElement.Video(newClip.id),
        statusMessage = "Appended ${item.label}",
      )
    }
  }

  fun togglePlayPause() {
    val currentlyPlaying = _uiState.value.isPlaying
    if (currentlyPlaying) {
      pausePlayback()
    } else {
      startPlayback()
    }
  }

  private fun startPlayback() {
    audioEngine.stop()
    _uiState.update { state ->
      val restartFromZero = state.playheadMs >= state.totalDurationMs - 40L
      state.copy(
        isPlaying = true,
        previewingAudioId = null,
        playheadMs = if (restartFromZero) 0L else state.playheadMs,
      )
    }
    playbackJob?.cancel()
    playbackJob =
      viewModelScope.launch {
        var lastTickNs = System.nanoTime()
        while (isActive) {
          delay(33L)
          val nowNs = System.nanoTime()
          val elapsedMs = ((nowNs - lastTickNs) / 1_000_000L).coerceIn(1L, 120L)
          lastTickNs = nowNs

          val st = _uiState.value
          if (!st.isPlaying) break
          val speed = st.activeVideoClip?.adjustments?.speed ?: 1f
          val nextMs = st.playheadMs + (elapsedMs * speed).toLong()

          val activeAudio =
            st.audioTrack.firstOrNull { nextMs in it.timelineStartMs until it.timelineEndMs }
          if (activeAudio != null) {
            audioEngine.startLoop(
              presetId = activeAudio.presetId,
              volume = activeAudio.volume,
              startOffsetMs = nextMs - activeAudio.timelineStartMs,
            )
          } else {
            audioEngine.stop()
          }

          if (nextMs >= st.totalDurationMs) {
            _uiState.update { it.copy(playheadMs = it.totalDurationMs, isPlaying = false) }
            audioEngine.stop()
            break
          } else {
            _uiState.update { it.copy(playheadMs = nextMs) }
          }
        }
      }
  }

  fun pausePlayback() {
    playbackJob?.cancel()
    playbackJob = null
    audioEngine.stop()
    _uiState.update { it.copy(isPlaying = false) }
  }

  fun seekTo(ms: Long) {
    _uiState.update { state ->
      val clamped = ms.coerceIn(0L, state.totalDurationMs)
      state.copy(playheadMs = clamped)
    }
  }

  fun skipBy(deltaMs: Long) {
    seekTo(_uiState.value.playheadMs + deltaMs)
  }

  fun toggleFullscreen() {
    _uiState.update { it.copy(isFullscreen = !it.isFullscreen) }
  }

  fun setTimelineZoom(zoom: Float) {
    _uiState.update { it.copy(timelineZoom = zoom.coerceIn(0.5f, 3.0f)) }
  }

  /**
   * Splits the selected clip (or the clip intersecting playheadMs) into two independent segments.
   */
  fun splitAtPlayhead() {
    val st = _uiState.value
    val playhead = st.playheadMs
    val minPartMs = 250L

    // Try selected element first if under playhead, otherwise any video clip under playhead
    val selectedOverlay =
      (st.selectedElement as? SelectedElement.Overlay)?.let { sel ->
        st.overlayTrack.firstOrNull {
          it.id == sel.overlayId &&
            playhead > it.timelineStartMs + minPartMs &&
            playhead < it.timelineEndMs - minPartMs
        }
      }
    if (selectedOverlay != null) {
      commitMutation { state ->
        val leftDur = playhead - selectedOverlay.timelineStartMs
        val rightDur = selectedOverlay.timelineEndMs - playhead
        val left =
          when (selectedOverlay) {
            is OverlayItem.TextOverlay -> selectedOverlay.copy(durationMs = leftDur)
            is OverlayItem.StickerOverlay -> selectedOverlay.copy(durationMs = leftDur)
          }
        val right =
          when (selectedOverlay) {
            is OverlayItem.TextOverlay ->
              selectedOverlay.copy(
                id = newId("txt"),
                timelineStartMs = playhead,
                durationMs = rightDur,
              )
            is OverlayItem.StickerOverlay ->
              selectedOverlay.copy(
                id = newId("stk"),
                timelineStartMs = playhead,
                durationMs = rightDur,
              )
          }
        val newList =
          state.overlayTrack.flatMap { if (it.id == selectedOverlay.id) listOf(left, right) else listOf(it) }
        state.copy(
          overlayTrack = newList,
          selectedElement = SelectedElement.Overlay(right.id),
          statusMessage = "Split overlay at ${formatTimeMs(playhead)}",
        )
      }
      return
    }

    val selectedAudio =
      (st.selectedElement as? SelectedElement.Audio)?.let { sel ->
        st.audioTrack.firstOrNull {
          it.id == sel.audioId &&
            playhead > it.timelineStartMs + minPartMs &&
            playhead < it.timelineEndMs - minPartMs
        }
      }
    if (selectedAudio != null) {
      commitMutation { state ->
        val leftDur = playhead - selectedAudio.timelineStartMs
        val rightDur = selectedAudio.timelineEndMs - playhead
        val left = selectedAudio.copy(durationMs = leftDur)
        val right =
          selectedAudio.copy(
            id = newId("aud"),
            timelineStartMs = playhead,
            durationMs = rightDur,
          )
        val newList =
          state.audioTrack.flatMap { if (it.id == selectedAudio.id) listOf(left, right) else listOf(it) }
        state.copy(
          audioTrack = newList,
          selectedElement = SelectedElement.Audio(right.id),
          statusMessage = "Split audio at ${formatTimeMs(playhead)}",
        )
      }
      return
    }

    val targetVideo =
      st.selectedVideoClip?.takeIf {
        playhead > it.timelineStartMs + minPartMs && playhead < it.timelineEndMs - minPartMs
      }
        ?: st.videoTrack.firstOrNull {
          playhead > it.timelineStartMs + minPartMs && playhead < it.timelineEndMs - minPartMs
        }

    if (targetVideo == null) {
      _uiState.update {
        it.copy(statusMessage = "Move playhead inside a clip to split")
      }
      return
    }

    commitMutation { state ->
      val leftDuration = playhead - targetVideo.timelineStartMs
      val rightDuration = targetVideo.timelineEndMs - playhead
      val leftClip = targetVideo.copy(durationMs = leftDuration)
      val rightClip =
        targetVideo.copy(
          id = newId("clip"),
          label = "${targetVideo.label} (2)",
          timelineStartMs = playhead,
          durationMs = rightDuration,
          sourceTrimStartMs = targetVideo.sourceTrimStartMs + leftDuration,
        )
      val updatedTrack =
        state.videoTrack.flatMap {
          if (it.id == targetVideo.id) listOf(leftClip, rightClip) else listOf(it)
        }
      state.copy(
        videoTrack = updatedTrack,
        selectedElement = SelectedElement.Video(rightClip.id),
        statusMessage = "Split clip at ${formatTimeMs(playhead)}",
      )
    }
  }

  fun deleteSelected() {
    val sel = _uiState.value.selectedElement ?: return
    commitMutation { state ->
      when (sel) {
        is SelectedElement.Video -> {
          val remaining = state.videoTrack.filterNot { it.id == sel.clipId }
          val compacted = repackVideoTrack(remaining)
          state.copy(
            videoTrack = compacted,
            selectedElement = compacted.firstOrNull()?.let { SelectedElement.Video(it.id) },
            playheadMs = state.playheadMs.coerceAtMost(compacted.maxOfOrNull { it.timelineEndMs } ?: 0L),
            statusMessage = "Deleted video clip",
          )
        }
        is SelectedElement.Overlay -> {
          val remaining = state.overlayTrack.filterNot { it.id == sel.overlayId }
          state.copy(
            overlayTrack = remaining,
            selectedElement = state.videoTrack.firstOrNull()?.let { SelectedElement.Video(it.id) },
            statusMessage = "Deleted overlay layer",
          )
        }
        is SelectedElement.Audio -> {
          val remaining = state.audioTrack.filterNot { it.id == sel.audioId }
          state.copy(
            audioTrack = remaining,
            selectedElement = state.videoTrack.firstOrNull()?.let { SelectedElement.Video(it.id) },
            statusMessage = "Deleted audio track",
          )
        }
      }
    }
  }

  private fun repackVideoTrack(clips: List<VideoClip>): List<VideoClip> {
    var cursor = 0L
    return clips.sortedBy { it.timelineStartMs }.map { clip ->
      val moved = clip.copy(timelineStartMs = cursor)
      cursor = moved.timelineEndMs
      moved
    }
  }

  fun trimVideoClip(clipId: String, deltaStartMs: Long, deltaEndMs: Long) {
    _uiState.update { state ->
      val idx = state.videoTrack.indexOfFirst { it.id == clipId }
      if (idx == -1) return@update state
      val clip = state.videoTrack[idx]
      val minDur = 500L

      val newTrimStart = (clip.sourceTrimStartMs + deltaStartMs).coerceIn(0L, clip.sourceMaxDurationMs - minDur)
      val actualStartDelta = newTrimStart - clip.sourceTrimStartMs
      val newDuration =
        (clip.durationMs - actualStartDelta + deltaEndMs)
          .coerceIn(minDur, clip.sourceMaxDurationMs - newTrimStart)

      val updatedClip =
        clip.copy(
          sourceTrimStartMs = newTrimStart,
          durationMs = newDuration,
        )
      val list = state.videoTrack.toMutableList()
      list[idx] = updatedClip
      state.copy(videoTrack = repackVideoTrack(list))
    }
  }

  fun moveVideoClipOrder(clipId: String, direction: Int) {
    commitMutation { state ->
      val sorted = state.videoTrack.sortedBy { it.timelineStartMs }.toMutableList()
      val idx = sorted.indexOfFirst { it.id == clipId }
      val target = idx + direction
      if (idx == -1 || target !in sorted.indices) return@commitMutation state
      val item = sorted.removeAt(idx)
      sorted.add(target, item)
      state.copy(videoTrack = repackVideoTrack(sorted), statusMessage = "Reordered clip")
    }
  }

  fun shiftOverlay(overlayId: String, deltaMs: Long) {
    _uiState.update { state ->
      state.copy(
        overlayTrack =
          state.overlayTrack.map { item ->
            if (item.id != overlayId) item
            else {
              val newStart = (item.timelineStartMs + deltaMs).coerceAtLeast(0L)
              when (item) {
                is OverlayItem.TextOverlay -> item.copy(timelineStartMs = newStart)
                is OverlayItem.StickerOverlay -> item.copy(timelineStartMs = newStart)
              }
            }
          }
      )
    }
  }

  fun trimOverlay(overlayId: String, deltaStartMs: Long, deltaEndMs: Long) {
    _uiState.update { state ->
      state.copy(
        overlayTrack =
          state.overlayTrack.map { item ->
            if (item.id != overlayId) item
            else {
              val minDur = 500L
              val newStart = (item.timelineStartMs + deltaStartMs).coerceIn(0L, item.timelineEndMs - minDur)
              val startApplied = newStart - item.timelineStartMs
              val newDur = (item.durationMs - startApplied + deltaEndMs).coerceIn(minDur, 60_000L)
              when (item) {
                is OverlayItem.TextOverlay ->
                  item.copy(timelineStartMs = newStart, durationMs = newDur)
                is OverlayItem.StickerOverlay ->
                  item.copy(timelineStartMs = newStart, durationMs = newDur)
              }
            }
          }
      )
    }
  }

  fun shiftAudio(audioId: String, deltaMs: Long) {
    _uiState.update { state ->
      state.copy(
        audioTrack =
          state.audioTrack.map { clip ->
            if (clip.id != audioId) clip
            else clip.copy(timelineStartMs = (clip.timelineStartMs + deltaMs).coerceAtLeast(0L))
          }
      )
    }
  }

  fun trimAudio(audioId: String, deltaStartMs: Long, deltaEndMs: Long) {
    _uiState.update { state ->
      state.copy(
        audioTrack =
          state.audioTrack.map { clip ->
            if (clip.id != audioId) clip
            else {
              val minDur = 500L
              val newStart = (clip.timelineStartMs + deltaStartMs).coerceIn(0L, clip.timelineEndMs - minDur)
              val applied = newStart - clip.timelineStartMs
              val newDur = (clip.durationMs - applied + deltaEndMs).coerceIn(minDur, 60_000L)
              clip.copy(timelineStartMs = newStart, durationMs = newDur)
            }
          }
      )
    }
  }

  fun applyFilterPreset(preset: FilterPreset) {
    commitMutation { state ->
      val targetId = state.selectedVideoClip?.id
      val updatedClips =
        state.videoTrack.map { clip ->
          if (targetId == null || clip.id == targetId) clip.copy(filterPreset = preset) else clip
        }
      state.copy(
        globalFilter = preset,
        videoTrack = updatedClips,
        statusMessage = "Filter: ${preset.label}",
      )
    }
  }

  fun updateVideoAdjustments(update: (VideoAdjustments) -> VideoAdjustments) {
    _uiState.update { state ->
      val targetId = state.selectedVideoClip?.id ?: return@update state
      state.copy(
        videoTrack =
          state.videoTrack.map { clip ->
            if (clip.id == targetId) clip.copy(adjustments = update(clip.adjustments)) else clip
          }
      )
    }
  }

  fun resetVideoAdjustments() {
    commitMutation { state ->
      val targetId = state.selectedVideoClip?.id ?: return@commitMutation state
      state.copy(
        videoTrack =
          state.videoTrack.map { clip ->
            if (clip.id == targetId) clip.copy(adjustments = VideoAdjustments()) else clip
          },
        statusMessage = "Reset clip adjustments",
      )
    }
  }

  fun addTextTemplate(style: TextTemplateStyle) {
    commitMutation { state ->
      val start = state.playheadMs.coerceAtMost((state.totalDurationMs - 1_500L).coerceAtLeast(0L))
      val overlay =
        OverlayItem.TextOverlay(
          id = newId("txt"),
          text = style.defaultText,
          style = style,
          fontSizeSp = style.defaultFontSizeSp,
          colorHex = style.defaultColorHex,
          alignment = TextAlignmentOption.CENTER,
          timelineStartMs = start,
          durationMs = 3_500L,
          normalizedX = 0.5f,
          normalizedY = 0.5f,
        )
      state.copy(
        overlayTrack = state.overlayTrack + overlay,
        selectedElement = SelectedElement.Overlay(overlay.id),
        statusMessage = "Added ${style.label}",
      )
    }
  }

  fun updateTextOverlay(
    overlayId: String,
    transform: (OverlayItem.TextOverlay) -> OverlayItem.TextOverlay,
  ) {
    _uiState.update { state ->
      state.copy(
        overlayTrack =
          state.overlayTrack.map { item ->
            if (item.id == overlayId && item is OverlayItem.TextOverlay) transform(item) else item
          }
      )
    }
  }

  fun addSticker(kind: StickerKind) {
    commitMutation { state ->
      val start = state.playheadMs.coerceAtMost((state.totalDurationMs - 1_500L).coerceAtLeast(0L))
      val sticker =
        OverlayItem.StickerOverlay(
          id = newId("stk"),
          kind = kind,
          scale = 1.0f,
          timelineStartMs = start,
          durationMs = 3_000L,
          normalizedX = 0.5f,
          normalizedY = 0.35f,
        )
      state.copy(
        overlayTrack = state.overlayTrack + sticker,
        selectedElement = SelectedElement.Overlay(sticker.id),
        statusMessage = "Added ${kind.label} sticker",
      )
    }
  }

  fun updateStickerScale(overlayId: String, scale: Float) {
    _uiState.update { state ->
      state.copy(
        overlayTrack =
          state.overlayTrack.map { item ->
            if (item.id == overlayId && item is OverlayItem.StickerOverlay) {
              item.copy(scale = scale.coerceIn(0.5f, 2.5f))
            } else {
              item
            }
          }
      )
    }
  }

  fun dragOverlayPosition(overlayId: String, normalizedX: Float, normalizedY: Float) {
    val x = normalizedX.coerceIn(0.08f, 0.92f)
    val y = normalizedY.coerceIn(0.08f, 0.92f)
    _uiState.update { state ->
      state.copy(
        overlayTrack =
          state.overlayTrack.map { item ->
            if (item.id != overlayId) item
            else {
              when (item) {
                is OverlayItem.TextOverlay -> item.copy(normalizedX = x, normalizedY = y)
                is OverlayItem.StickerOverlay -> item.copy(normalizedX = x, normalizedY = y)
              }
            }
          }
      )
    }
  }

  fun togglePreviewAudioPreset(preset: AudioTrackPreset) {
    val currentlyPreviewing = _uiState.value.previewingAudioId
    if (currentlyPreviewing == preset.id) {
      audioEngine.stop()
      _uiState.update { it.copy(previewingAudioId = null) }
    } else {
      pausePlayback()
      audioEngine.startLoop(preset.id, volume = 0.75f, startOffsetMs = 0L)
      _uiState.update { it.copy(previewingAudioId = preset.id) }
    }
  }

  fun addAudioToTimeline(preset: AudioTrackPreset) {
    audioEngine.stop()
    commitMutation { state ->
      val start = state.playheadMs.coerceAtMost((state.totalDurationMs - 1_500L).coerceAtLeast(0L))
      val clip =
        AudioClip(
          id = newId("aud"),
          presetId = preset.id,
          title = preset.title,
          bpm = preset.bpm,
          timelineStartMs = start,
          durationMs = minOf(preset.durationMs, maxOf(4_000L, state.totalDurationMs - start)),
          volume = 0.8f,
          waveform = preset.waveform,
        )
      state.copy(
        previewingAudioId = null,
        audioTrack = state.audioTrack + clip,
        selectedElement = SelectedElement.Audio(clip.id),
        statusMessage = "Added ${preset.title} to Audio Track",
      )
    }
  }

  fun updateAudioVolume(audioId: String, volume: Float) {
    val vol = volume.coerceIn(0f, 1f)
    audioEngine.updateVolume(vol)
    _uiState.update { state ->
      state.copy(
        audioTrack =
          state.audioTrack.map { if (it.id == audioId) it.copy(volume = vol) else it }
      )
    }
  }

  fun openExportModal() {
    pausePlayback()
    _uiState.update { it.copy(exportState = ExportUiState.Configuring()) }
  }

  fun closeExportModal() {
    exportManager.cancelExport()
    _uiState.update { it.copy(exportState = ExportUiState.Hidden) }
  }

  fun setExportResolution(resolution: ExportResolution) {
    _uiState.update { state ->
      val cfg = state.exportState as? ExportUiState.Configuring ?: return@update state
      state.copy(exportState = cfg.copy(resolution = resolution))
    }
  }

  fun setExportFps(fps: ExportFps) {
    _uiState.update { state ->
      val cfg = state.exportState as? ExportUiState.Configuring ?: return@update state
      state.copy(exportState = cfg.copy(fps = fps))
    }
  }

  fun startExport() {
    val st = _uiState.value
    val cfg = st.exportState as? ExportUiState.Configuring ?: return
    _uiState.update {
      it.copy(
        exportState =
          ExportUiState.Exporting(
            resolution = cfg.resolution,
            fps = cfg.fps,
            progress = 0.02f,
            stageLabel = "Initializing encoder...",
          )
      )
    }
    exportManager.startExport(
      state = st,
      resolution = cfg.resolution,
      fps = cfg.fps,
      onProgress = { progress, stage ->
        _uiState.update { cur ->
          if (cur.exportState is ExportUiState.Exporting) {
            cur.copy(
              exportState =
                ExportUiState.Exporting(
                  resolution = cfg.resolution,
                  fps = cfg.fps,
                  progress = progress,
                  stageLabel = stage,
                )
            )
          } else {
            cur
          }
        }
      },
      onComplete = { fileName, path, summary ->
        _uiState.update { cur ->
          cur.copy(
            exportState =
              ExportUiState.Completed(
                resolution = cfg.resolution,
                fps = cfg.fps,
                outputFileName = fileName,
                outputPath = path,
                summaryText = summary,
              ),
            statusMessage = "Export complete: $fileName",
          )
        }
      },
      onError = { message ->
        _uiState.update { cur ->
          cur.copy(exportState = ExportUiState.Failed(cfg.resolution, cfg.fps, message))
        }
      },
    )
  }

  override fun onCleared() {
    super.onCleared()
    playbackJob?.cancel()
    audioEngine.stop()
    exportManager.cancelExport()
  }

  companion object {
    private fun newId(prefix: String): String = "${prefix}_${UUID.randomUUID().toString().take(8)}"

    fun formatTimeMs(ms: Long): String {
      val totalSec = (ms.coerceAtLeast(0L) / 1000L).toInt()
      val mins = totalSec / 60
      val secs = totalSec % 60
      return String.format("%02d:%02d", mins, secs)
    }
  }
}
