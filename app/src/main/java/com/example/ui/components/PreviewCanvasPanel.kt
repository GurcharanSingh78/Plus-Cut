package com.example.ui.components

import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.os.Build
import android.view.TextureView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.example.media.ColorMatrixUtils
import com.example.model.AspectRatioOption
import com.example.model.FilterPreset
import com.example.model.MediaSource
import com.example.model.OverlayItem
import com.example.model.SelectedElement
import com.example.model.TextAlignmentOption
import com.example.model.TextTemplateStyle
import com.example.model.VideoAdjustments
import com.example.model.VideoClip
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StudioBg
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.StudioViewModel
import kotlin.math.roundToInt

@Composable
fun PreviewCanvasPanel(
  aspectRatio: AspectRatioOption,
  activeClip: VideoClip?,
  globalFilter: FilterPreset,
  visibleOverlays: List<OverlayItem>,
  selectedElement: SelectedElement?,
  playheadMs: Long,
  totalDurationMs: Long,
  isPlaying: Boolean,
  isFullscreen: Boolean,
  onTogglePlayPause: () -> Unit,
  onSkipBackward: () -> Unit,
  onSkipForward: () -> Unit,
  onToggleFullscreen: () -> Unit,
  onSelectOverlay: (String) -> Unit,
  onDragOverlay: (String, Float, Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.background(StudioBg).padding(horizontal = 10.dp, vertical = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    // Responsive Aspect-Locked Preview Frame
    Box(
      modifier = Modifier.weight(1f).fillMaxWidth(),
      contentAlignment = Alignment.Center,
    ) {
      BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
      ) {
        val ratio = aspectRatio.ratio
        val maxW = maxWidth
        val maxH = maxHeight
        val boxModifier =
          if (maxW / maxH > ratio) {
            Modifier.fillMaxHeight().aspectRatio(ratio)
          } else {
            Modifier.fillMaxWidth().aspectRatio(ratio)
          }

        BoxWithConstraints(
          modifier =
            boxModifier
              .clip(RoundedCornerShape(12.dp))
              .background(Color.Black)
              .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
              .testTag("preview_canvas"),
        ) {
          val canvasWidthPx = with(LocalDensity.current) { maxWidth.toPx() }.coerceAtLeast(1f)
          val canvasHeightPx = with(LocalDensity.current) { maxHeight.toPx() }.coerceAtLeast(1f)

          val effectiveFilter = activeClip?.filterPreset ?: globalFilter
          val adjustments = activeClip?.adjustments ?: VideoAdjustments()
          val colorMatrix =
            remember(effectiveFilter, adjustments) {
              ColorMatrixUtils.buildComposeColorMatrix(effectiveFilter, adjustments)
            }

          if (activeClip == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Text(
                text = "Add a clip from Media to preview",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
              )
            }
          } else {
            val clipElapsed = (playheadMs - activeClip.timelineStartMs).coerceAtLeast(0L)
            val clipFraction =
              if (activeClip.durationMs > 0L) (clipElapsed.toFloat() / activeClip.durationMs).coerceIn(0f, 1f)
              else 0f

            when (val source = activeClip.source) {
              is MediaSource.LocalVideo -> {
                LocalVideoSurface(
                  uri = source.uri,
                  targetPositionMs = activeClip.sourceTrimStartMs + clipElapsed,
                  isPlaying = isPlaying,
                  speed = adjustments.speed,
                  volume = adjustments.volume,
                  colorMatrix = colorMatrix,
                )
              }
              is MediaSource.LocalImage -> {
                val kenBurnsScale = 1f + clipFraction * 0.08f
                AsyncImage(
                  model = source.uri,
                  contentDescription = activeClip.label,
                  contentScale = ContentScale.Crop,
                  colorFilter = ColorFilter.colorMatrix(colorMatrix),
                  modifier =
                    Modifier.fillMaxSize().graphicsLayer {
                      scaleX = kenBurnsScale
                      scaleY = kenBurnsScale
                    },
                )
              }
              is MediaSource.BundledSample -> {
                val kenBurnsScale = 1.02f + clipFraction * 0.10f
                Image(
                  painter = painterResource(id = source.drawableResId),
                  contentDescription = activeClip.label,
                  contentScale = ContentScale.Crop,
                  colorFilter = ColorFilter.colorMatrix(colorMatrix),
                  modifier =
                    Modifier.fillMaxSize().graphicsLayer {
                      scaleX = kenBurnsScale
                      scaleY = kenBurnsScale
                    },
                )
              }
            }
          }

          // Active Overlays (Text & Stickers)
          visibleOverlays.forEach { overlay ->
            val isSelected =
              (selectedElement as? SelectedElement.Overlay)?.overlayId == overlay.id
            DraggableOverlayItem(
              overlay = overlay,
              isSelected = isSelected,
              canvasWidthPx = canvasWidthPx,
              canvasHeightPx = canvasHeightPx,
              onSelect = { onSelectOverlay(overlay.id) },
              onDragNorm = { nx, ny -> onDragOverlay(overlay.id, nx, ny) },
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Playback Transport Bar
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .height(48.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(StudioPanel)
          .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
          .padding(horizontal = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      // Timecode
      Text(
        text =
          "${StudioViewModel.formatTimeMs(playheadMs)} / ${StudioViewModel.formatTimeMs(totalDurationMs)}",
        style = MaterialTheme.typography.labelSmall,
        color = TextPrimary,
        modifier = Modifier.testTag("timecode_display"),
      )

      // Center Controls
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        IconButton(onClick = onSkipBackward, modifier = Modifier.size(40.dp)) {
          Icon(
            imageVector = Icons.Filled.Replay5,
            contentDescription = "Skip back 5 seconds",
            tint = TextSecondary,
          )
        }
        Box(
          modifier =
            Modifier.size(38.dp)
              .clip(CircleShape)
              .background(Brush.linearGradient(listOf(NeonCyan, NeonPurple)))
              .clickable(onClick = onTogglePlayPause)
              .testTag("play_pause_button"),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = StudioBg,
            modifier = Modifier.size(22.dp),
          )
        }
        IconButton(onClick = onSkipForward, modifier = Modifier.size(40.dp)) {
          Icon(
            imageVector = Icons.Filled.Forward5,
            contentDescription = "Skip forward 5 seconds",
            tint = TextSecondary,
          )
        }
      }

      // Fullscreen toggle
      IconButton(
        onClick = onToggleFullscreen,
        modifier = Modifier.size(40.dp).testTag("fullscreen_button"),
      ) {
        Icon(
          imageVector =
            if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
          contentDescription = "Toggle fullscreen preview",
          tint = NeonCyan,
        )
      }
    }
  }
}

@Composable
private fun LocalVideoSurface(
  uri: android.net.Uri,
  targetPositionMs: Long,
  isPlaying: Boolean,
  speed: Float,
  volume: Float,
  colorMatrix: androidx.compose.ui.graphics.ColorMatrix,
) {
  val context = LocalContext.current
  val exoPlayer = remember(context) {
    ExoPlayer.Builder(context).build().apply {
      repeatMode = Player.REPEAT_MODE_OFF
    }
  }
  var textureViewRef by remember { mutableStateOf<TextureView?>(null) }

  DisposableEffect(exoPlayer) {
    onDispose { exoPlayer.release() }
  }

  LaunchedEffect(uri) {
    exoPlayer.setMediaItem(MediaItem.fromUri(uri))
    exoPlayer.prepare()
  }

  LaunchedEffect(isPlaying, speed, volume) {
    exoPlayer.volume = volume
    exoPlayer.setPlaybackSpeed(speed)
    exoPlayer.playWhenReady = isPlaying
  }

  LaunchedEffect(targetPositionMs, isPlaying) {
    val diff = kotlin.math.abs(exoPlayer.currentPosition - targetPositionMs)
    if (!isPlaying || diff > 350L) {
      exoPlayer.seekTo(targetPositionMs)
    }
  }

  LaunchedEffect(colorMatrix, textureViewRef) {
    val tv = textureViewRef ?: return@LaunchedEffect
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val androidMatrix = android.graphics.ColorMatrix(colorMatrix.values)
      tv.setRenderEffect(
        RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(androidMatrix))
      )
    }
  }

  AndroidView(
    factory = { ctx ->
      TextureView(ctx).also { tv ->
        textureViewRef = tv
        exoPlayer.setVideoTextureView(tv)
      }
    },
    modifier = Modifier.fillMaxSize(),
  )
}

@Composable
private fun DraggableOverlayItem(
  overlay: OverlayItem,
  isSelected: Boolean,
  canvasWidthPx: Float,
  canvasHeightPx: Float,
  onSelect: () -> Unit,
  onDragNorm: (Float, Float) -> Unit,
) {
  val xPx = (overlay.normalizedX * canvasWidthPx).roundToInt()
  val yPx = (overlay.normalizedY * canvasHeightPx).roundToInt()

  Box(
    modifier =
      Modifier.offset { IntOffset(xPx - 90, yPx - 30) }
        .clip(RoundedCornerShape(8.dp))
        .then(
          if (isSelected) Modifier.border(1.5.dp, NeonCyan, RoundedCornerShape(8.dp))
          else Modifier
        )
        .pointerInput(overlay.id, canvasWidthPx, canvasHeightPx) {
          detectDragGestures(
            onDragStart = { onSelect() },
            onDrag = { change, dragAmount ->
              change.consume()
              val nextX = overlay.normalizedX + (dragAmount.x / canvasWidthPx)
              val nextY = overlay.normalizedY + (dragAmount.y / canvasHeightPx)
              onDragNorm(nextX, nextY)
            },
          )
        }
        .clickable { onSelect() }
        .padding(horizontal = 10.dp, vertical = 6.dp),
  ) {
    when (overlay) {
      is OverlayItem.TextOverlay -> {
        val textAlign =
          when (overlay.alignment) {
            TextAlignmentOption.LEFT -> TextAlign.Start
            TextAlignmentOption.CENTER -> TextAlign.Center
            TextAlignmentOption.RIGHT -> TextAlign.End
          }
        val shadow =
          when (overlay.style) {
            TextTemplateStyle.NEON_TITLE ->
              Shadow(color = Color(overlay.colorHex), blurRadius = 16f)
            else -> Shadow(color = Color.Black, blurRadius = 8f)
          }
        val bgModifier =
          if (overlay.style == TextTemplateStyle.BOLD_CAPTION) {
            Modifier.background(Color(0xB30B0D12), RoundedCornerShape(6.dp))
              .padding(horizontal = 8.dp, vertical = 3.dp)
          } else {
            Modifier
          }

        Text(
          text = overlay.text.ifBlank { "Text" },
          style =
            MaterialTheme.typography.titleMedium.copy(
              fontSize = overlay.fontSizeSp.sp,
              fontWeight =
                if (overlay.style == TextTemplateStyle.BOLD_CAPTION) FontWeight.ExtraBold
                else FontWeight.Bold,
              shadow = shadow,
              letterSpacing =
                if (overlay.style == TextTemplateStyle.CINEMATIC_SUBTITLE) 2.sp else 0.sp,
            ),
          color = Color(overlay.colorHex),
          textAlign = textAlign,
          modifier = bgModifier,
        )
      }
      is OverlayItem.StickerOverlay -> {
        Box(
          modifier =
            Modifier.graphicsLayer {
              scaleX = overlay.scale
              scaleY = overlay.scale
            }
              .clip(RoundedCornerShape(10.dp))
              .background(StudioSurfaceElevated.copy(alpha = 0.85f))
              .border(1.5.dp, Color(overlay.kind.accentHex), RoundedCornerShape(10.dp))
              .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
          Text(
            text = overlay.kind.tagText,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = Color(overlay.kind.accentHex),
          )
        }
      }
    }
  }
}
