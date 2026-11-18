package com.example.model

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable

enum class AspectRatioOption(
  val label: String,
  val shortTag: String,
  val ratio: Float,
  val widthFactor: Int,
  val heightFactor: Int,
) {
  RATIO_9_16("9:16 TikTok/Reels", "9:16", 9f / 16f, 9, 16),
  RATIO_16_9("16:9 YouTube", "16:9", 16f / 9f, 16, 9),
  RATIO_1_1("1:1 Instagram", "1:1", 1f, 1, 1),
}

enum class StudioTab(val label: String) {
  MEDIA("Media"),
  AUDIO("Audio"),
  TEXT("Text"),
  FILTERS("Filters"),
  STICKERS("Stickers"),
}

enum class FilterPreset(val label: String, val badgeColorHex: Long) {
  NONE("None", 0xFF64748B),
  CYBERPUNK_NEON("Cyberpunk Neon", 0xFF00F0FF),
  VINTAGE_WARM("Vintage Warm", 0xFFF59E0B),
  BW_NOIR("B&W Noir", 0xFFCBD5E1),
  HIGH_CONTRAST("High Contrast", 0xFFEC4899),
  VIVID("Vivid", 0xFF8B5CF6),
}

@Immutable
data class VideoAdjustments(
  val brightness: Float = 0f, // -0.5f .. +0.5f
  val contrast: Float = 1f, // 0.5f .. 2.0f
  val saturation: Float = 1f, // 0.0f .. 2.0f
  val speed: Float = 1f, // 0.5f, 1f, 1.5f, 2f
  val volume: Float = 0.85f, // 0.0f .. 1.0f
)

sealed interface MediaSource {
  data class LocalVideo(val uri: Uri, val durationMs: Long) : MediaSource

  data class LocalImage(val uri: Uri) : MediaSource

  data class BundledSample(
    @DrawableRes val drawableResId: Int,
    val subtitle: String,
    val accentHex: Long,
  ) : MediaSource
}

@Immutable
data class MediaBinItem(
  val id: String,
  val label: String,
  val isVideo: Boolean,
  val defaultDurationMs: Long,
  val source: MediaSource,
)

@Immutable
data class VideoClip(
  val id: String,
  val label: String,
  val source: MediaSource,
  val timelineStartMs: Long,
  val durationMs: Long,
  val sourceTrimStartMs: Long = 0L,
  val sourceMaxDurationMs: Long = 30_000L,
  val adjustments: VideoAdjustments = VideoAdjustments(),
  val filterPreset: FilterPreset = FilterPreset.NONE,
) {
  val timelineEndMs: Long
    get() = timelineStartMs + durationMs
}

enum class TextTemplateStyle(
  val label: String,
  val defaultText: String,
  val defaultColorHex: Long,
  val defaultFontSizeSp: Float,
) {
  NEON_TITLE("Neon Title", "NIGHT CITY", 0xFF00F0FF, 32f),
  CINEMATIC_SUBTITLE("Cinematic Subtitle", "Directed in Plus Cut", 0xFFF4F7FC, 20f),
  BOLD_CAPTION("Bold Caption", "WAIT FOR THE DROP", 0xFFFACC15, 28f),
}

enum class TextAlignmentOption(val label: String) {
  LEFT("Left"),
  CENTER("Center"),
  RIGHT("Right"),
}

enum class StickerKind(val label: String, val tagText: String, val accentHex: Long) {
  REC_BADGE("REC 4K", "● REC", 0xFFFF3B5C),
  NEON_SPARK("Cyber Spark", "✦", 0xFF00F0FF),
  FLAME_HYPE("Trending", "FIRE", 0xFFF59E0B),
  VIBE_WAVE("Audio Pulse", "≈≈≈", 0xFF8B5CF6),
  ARROW_CUE("Focus Arrow", "➜", 0xFF14B8A6),
  STAR_STAMP("Pro Cut", "★ PRO", 0xFFFACC15),
}

sealed interface OverlayItem {
  val id: String
  val label: String
  val timelineStartMs: Long
  val durationMs: Long
  val normalizedX: Float
  val normalizedY: Float

  val timelineEndMs: Long
    get() = timelineStartMs + durationMs

  @Immutable
  data class TextOverlay(
    override val id: String,
    val text: String,
    val style: TextTemplateStyle,
    val fontSizeSp: Float,
    val colorHex: Long,
    val alignment: TextAlignmentOption,
    override val timelineStartMs: Long,
    override val durationMs: Long,
    override val normalizedX: Float = 0.5f,
    override val normalizedY: Float = 0.5f,
  ) : OverlayItem {
    override val label: String
      get() = text.ifBlank { style.label }
  }

  @Immutable
  data class StickerOverlay(
    override val id: String,
    val kind: StickerKind,
    val scale: Float = 1f,
    override val timelineStartMs: Long,
    override val durationMs: Long,
    override val normalizedX: Float = 0.5f,
    override val normalizedY: Float = 0.35f,
  ) : OverlayItem {
    override val label: String
      get() = kind.label
  }
}

@Immutable
data class AudioTrackPreset(
  val id: String,
  val title: String,
  val mood: String,
  val bpm: Int,
  val durationMs: Long,
  val waveform: List<Float>,
)

@Immutable
data class AudioClip(
  val id: String,
  val presetId: String,
  val title: String,
  val bpm: Int,
  val timelineStartMs: Long,
  val durationMs: Long,
  val volume: Float = 0.8f,
  val waveform: List<Float>,
) {
  val timelineEndMs: Long
    get() = timelineStartMs + durationMs
}

sealed interface SelectedElement {
  data class Video(val clipId: String) : SelectedElement

  data class Overlay(val overlayId: String) : SelectedElement

  data class Audio(val audioId: String) : SelectedElement
}

enum class ExportResolution(val label: String, val widthPx: Int, val heightPx: Int, val mbPerSec: Float) {
  P720("720p", 720, 1280, 1.4f),
  P1080("1080p", 1080, 1920, 2.8f),
  UHD_4K("4K", 2160, 3840, 7.2f),
}

enum class ExportFps(val label: String, val fps: Int) {
  FPS_30("30fps", 30),
  FPS_60("60fps", 60),
}

sealed interface ExportUiState {
  data object Hidden : ExportUiState

  data class Configuring(
    val resolution: ExportResolution = ExportResolution.P1080,
    val fps: ExportFps = ExportFps.FPS_30,
  ) : ExportUiState

  data class Exporting(
    val resolution: ExportResolution,
    val fps: ExportFps,
    val progress: Float,
    val stageLabel: String,
  ) : ExportUiState

  data class Completed(
    val resolution: ExportResolution,
    val fps: ExportFps,
    val outputFileName: String,
    val outputPath: String,
    val summaryText: String,
  ) : ExportUiState

  data class Failed(val resolution: ExportResolution, val fps: ExportFps, val message: String) :
    ExportUiState
}

@Immutable
data class EditorProjectSnapshot(
  val projectTitle: String,
  val aspectRatio: AspectRatioOption,
  val videoTrack: List<VideoClip>,
  val overlayTrack: List<OverlayItem>,
  val audioTrack: List<AudioClip>,
  val globalFilter: FilterPreset,
)

@Immutable
data class EditorUiState(
  val projectTitle: String = "Untitled Project",
  val aspectRatio: AspectRatioOption = AspectRatioOption.RATIO_9_16,
  val activeTab: StudioTab = StudioTab.MEDIA,
  val mediaBin: List<MediaBinItem> = emptyList(),
  val audioPresets: List<AudioTrackPreset> = emptyList(),
  val previewingAudioId: String? = null,
  val videoTrack: List<VideoClip> = emptyList(),
  val overlayTrack: List<OverlayItem> = emptyList(),
  val audioTrack: List<AudioClip> = emptyList(),
  val globalFilter: FilterPreset = FilterPreset.NONE,
  val selectedElement: SelectedElement? = null,
  val playheadMs: Long = 0L,
  val isPlaying: Boolean = false,
  val isFullscreen: Boolean = false,
  val timelineZoom: Float = 1.0f,
  val canUndo: Boolean = false,
  val canRedo: Boolean = false,
  val exportState: ExportUiState = ExportUiState.Hidden,
  val statusMessage: String? = null,
) {
  val totalDurationMs: Long
    get() {
      val videoEnd = videoTrack.maxOfOrNull { it.timelineEndMs } ?: 0L
      val overlayEnd = overlayTrack.maxOfOrNull { it.timelineEndMs } ?: 0L
      val audioEnd = audioTrack.maxOfOrNull { it.timelineEndMs } ?: 0L
      return maxOf(videoEnd, overlayEnd, audioEnd, 4_000L)
    }

  val activeVideoClip: VideoClip?
    get() =
      videoTrack.firstOrNull { playheadMs in it.timelineStartMs until it.timelineEndMs }
        ?: videoTrack.lastOrNull { playheadMs >= it.timelineEndMs }
        ?: videoTrack.firstOrNull()

  val selectedVideoClip: VideoClip?
    get() =
      (selectedElement as? SelectedElement.Video)?.let { sel ->
        videoTrack.firstOrNull { it.id == sel.clipId }
      } ?: activeVideoClip

  val selectedOverlay: OverlayItem?
    get() =
      (selectedElement as? SelectedElement.Overlay)?.let { sel ->
        overlayTrack.firstOrNull { it.id == sel.overlayId }
      }

  val selectedAudioClip: AudioClip?
    get() =
      (selectedElement as? SelectedElement.Audio)?.let { sel ->
        audioTrack.firstOrNull { it.id == sel.audioId }
      }
}
