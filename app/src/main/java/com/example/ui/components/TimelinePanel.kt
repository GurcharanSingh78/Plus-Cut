package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioClip
import com.example.model.MediaSource
import com.example.model.OverlayItem
import com.example.model.SelectedElement
import com.example.model.VideoClip
import com.example.ui.theme.AudioTeal
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.PlayheadCrimson
import com.example.ui.theme.StudioBg
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHighest
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.StudioViewModel
import kotlin.math.roundToInt

@Composable
fun TimelinePanel(
  videoTrack: List<VideoClip>,
  overlayTrack: List<OverlayItem>,
  audioTrack: List<AudioClip>,
  selectedElement: SelectedElement?,
  playheadMs: Long,
  totalDurationMs: Long,
  timelineZoom: Float,
  canUndo: Boolean,
  canRedo: Boolean,
  isCompact: Boolean,
  onUndo: () -> Unit,
  onRedo: () -> Unit,
  onSplit: () -> Unit,
  onDelete: () -> Unit,
  onZoomChange: (Float) -> Unit,
  onSeekTo: (Long) -> Unit,
  onSelectElement: (SelectedElement) -> Unit,
  onTrimVideo: (String, Long, Long) -> Unit,
  onShiftOverlay: (String, Long) -> Unit,
  onTrimOverlay: (String, Long, Long) -> Unit,
  onShiftAudio: (String, Long) -> Unit,
  onTrimAudio: (String, Long, Long) -> Unit,
  modifier: Modifier = Modifier,
) {
  val density = LocalDensity.current
  val pxPerSecond = 64f * timelineZoom
  val pxPerMs = pxPerSecond / 1000f
  val scrollState = rememberScrollState()
  val timelineSpanMs = maxOf(totalDurationMs + 3_000L, 16_000L)
  val trackAreaWidthDp: Dp = with(density) { (timelineSpanMs * pxPerMs).toDp() }
  val headerWidth = if (isCompact) 48.dp else 86.dp

  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .background(StudioPanel)
        .border(1.dp, StudioBorder)
  ) {
    // Timeline Toolbar
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .height(44.dp)
          .background(StudioBg)
          .border(1.dp, StudioBorder)
          .padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        IconButton(
          onClick = onUndo,
          enabled = canUndo,
          modifier = Modifier.size(38.dp).testTag("undo_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Undo,
            contentDescription = "Undo",
            tint = if (canUndo) TextPrimary else TextMuted,
            modifier = Modifier.size(18.dp),
          )
        }
        IconButton(
          onClick = onRedo,
          enabled = canRedo,
          modifier = Modifier.size(38.dp).testTag("redo_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Redo,
            contentDescription = "Redo",
            tint = if (canRedo) TextPrimary else TextMuted,
            modifier = Modifier.size(18.dp),
          )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Split Button
        Row(
          modifier =
            Modifier.height(32.dp)
              .clip(RoundedCornerShape(7.dp))
              .background(StudioSurfaceElevated)
              .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(7.dp))
              .clickable(onClick = onSplit)
              .padding(horizontal = 10.dp)
              .testTag("split_button"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.ContentCut,
            contentDescription = "Split clip at playhead",
            tint = NeonCyan,
            modifier = Modifier.size(15.dp),
          )
          Text(
            text = "Split",
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
          )
        }

        // Delete Button
        Row(
          modifier =
            Modifier.height(32.dp)
              .clip(RoundedCornerShape(7.dp))
              .background(StudioSurfaceElevated)
              .border(
                1.dp,
                if (selectedElement != null) PlayheadCrimson.copy(alpha = 0.5f) else StudioBorder,
                RoundedCornerShape(7.dp),
              )
              .clickable(enabled = selectedElement != null, onClick = onDelete)
              .padding(horizontal = 9.dp)
              .testTag("delete_button"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.DeleteOutline,
            contentDescription = "Delete selected clip",
            tint = if (selectedElement != null) PlayheadCrimson else TextMuted,
            modifier = Modifier.size(16.dp),
          )
          if (!isCompact) {
            Text(
              text = "Delete",
              style = MaterialTheme.typography.labelMedium,
              color = if (selectedElement != null) TextPrimary else TextMuted,
            )
          }
        }
      }

      // Timeline Zoom Slider
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.width(if (isCompact) 115.dp else 170.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.ZoomIn,
          contentDescription = "Timeline zoom",
          tint = TextSecondary,
          modifier = Modifier.size(16.dp),
        )
        Slider(
          value = timelineZoom,
          onValueChange = onZoomChange,
          valueRange = 0.5f..3.0f,
          colors =
            SliderDefaults.colors(
              thumbColor = NeonCyan,
              activeTrackColor = NeonCyan,
              inactiveTrackColor = StudioSurfaceElevated,
            ),
          modifier = Modifier.weight(1f).height(26.dp).testTag("timeline_zoom_slider"),
        )
      }
    }

    // Track Headers + Scrollable Multi-Track Canvas
    Row(modifier = Modifier.fillMaxSize()) {
      // Fixed Track Headers
      Column(
        modifier =
          Modifier.width(headerWidth)
            .fillMaxHeight()
            .background(StudioBg)
            .border(1.dp, StudioBorder),
      ) {
        Box(
          modifier = Modifier.fillMaxWidth().height(26.dp).border(0.5.dp, StudioBorder),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = StudioViewModel.formatTimeMs(playheadMs),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = NeonCyan,
          )
        }
        TrackLabelHeader(
          icon = Icons.Filled.Movie,
          label = "Video",
          accent = NeonCyan,
          compact = isCompact,
          modifier = Modifier.weight(1.15f),
        )
        TrackLabelHeader(
          icon = Icons.Filled.TextFields,
          label = "Overlay",
          accent = NeonPurple,
          compact = isCompact,
          modifier = Modifier.weight(0.95f),
        )
        TrackLabelHeader(
          icon = Icons.Filled.LibraryMusic,
          label = "Audio",
          accent = AudioTeal,
          compact = isCompact,
          modifier = Modifier.weight(0.95f),
        )
      }

      // Scrollable Ruler + Lanes + Playhead Needle
      Box(
        modifier =
          Modifier.weight(1f)
            .fillMaxHeight()
            .horizontalScroll(scrollState)
      ) {
        Box(modifier = Modifier.width(trackAreaWidthDp).fillMaxHeight()) {
          Column(modifier = Modifier.fillMaxSize()) {
            // Time Ruler (Tap or Drag to Scrub Playhead)
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .height(26.dp)
                  .background(StudioBg)
                  .border(0.5.dp, StudioBorder)
                  .pointerInput(pxPerMs, totalDurationMs) {
                    detectTapGestures { offset ->
                      val targetMs = (offset.x / pxPerMs).toLong().coerceIn(0L, totalDurationMs)
                      onSeekTo(targetMs)
                    }
                  }
                  .pointerInput(pxPerMs, totalDurationMs) {
                    detectDragGestures { change, _ ->
                      change.consume()
                      val targetMs =
                        (change.position.x / pxPerMs).toLong().coerceIn(0L, totalDurationMs)
                      onSeekTo(targetMs)
                    }
                  }
            ) {
              Canvas(modifier = Modifier.fillMaxSize()) {
                val totalSec = (timelineSpanMs / 1000L).toInt()
                for (sec in 0..totalSec) {
                  val x = sec * 1000f * pxPerMs
                  drawLine(
                    color = StudioBorder,
                    start = Offset(x, size.height * 0.45f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.5f,
                  )
                  val halfX = x + 500f * pxPerMs
                  drawLine(
                    color = StudioBorder.copy(alpha = 0.5f),
                    start = Offset(halfX, size.height * 0.72f),
                    end = Offset(halfX, size.height),
                    strokeWidth = 1f,
                  )
                }
              }
              val stepSec = if (timelineZoom < 0.8f) 4 else 2
              for (sec in 0..(timelineSpanMs / 1000L).toInt() step stepSec) {
                val xDp = with(density) { (sec * 1000f * pxPerMs).toDp() }
                Text(
                  text = StudioViewModel.formatTimeMs(sec * 1000L),
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                  color = TextMuted,
                  modifier = Modifier.offset(x = xDp + 3.dp, y = 2.dp),
                )
              }
            }

            // Video Track Lane
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .weight(1.15f)
                  .background(StudioPanel)
                  .border(0.5.dp, StudioBorder)
                  .padding(vertical = 4.dp),
            ) {
              videoTrack.forEach { clip ->
                val isSelected =
                  (selectedElement as? SelectedElement.Video)?.clipId == clip.id
                val startDp = with(density) { (clip.timelineStartMs * pxPerMs).toDp() }
                val widthDp =
                  with(density) { (clip.durationMs * pxPerMs).coerceAtLeast(36f).toDp() }
                VideoTimelineBlock(
                  clip = clip,
                  isSelected = isSelected,
                  pxPerMs = pxPerMs,
                  onSelect = { onSelectElement(SelectedElement.Video(clip.id)) },
                  onTrim = { dStart, dEnd -> onTrimVideo(clip.id, dStart, dEnd) },
                  modifier = Modifier.offset(x = startDp).width(widthDp).fillMaxHeight(),
                )
              }
            }

            // Text / Overlay Track Lane (Purple Blocks)
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .weight(0.95f)
                  .background(StudioBg.copy(alpha = 0.75f))
                  .border(0.5.dp, StudioBorder)
                  .padding(vertical = 4.dp),
            ) {
              overlayTrack.forEach { item ->
                val isSelected =
                  (selectedElement as? SelectedElement.Overlay)?.overlayId == item.id
                val startDp = with(density) { (item.timelineStartMs * pxPerMs).toDp() }
                val widthDp =
                  with(density) { (item.durationMs * pxPerMs).coerceAtLeast(40f).toDp() }
                OverlayTimelineBlock(
                  item = item,
                  isSelected = isSelected,
                  pxPerMs = pxPerMs,
                  onSelect = { onSelectElement(SelectedElement.Overlay(item.id)) },
                  onShift = { deltaMs -> onShiftOverlay(item.id, deltaMs) },
                  onTrim = { dStart, dEnd -> onTrimOverlay(item.id, dStart, dEnd) },
                  modifier = Modifier.offset(x = startDp).width(widthDp).fillMaxHeight(),
                )
              }
            }

            // Audio Track Lane (Teal Waveform Blocks)
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .weight(0.95f)
                  .background(StudioPanel)
                  .border(0.5.dp, StudioBorder)
                  .padding(vertical = 4.dp),
            ) {
              audioTrack.forEach { clip ->
                val isSelected =
                  (selectedElement as? SelectedElement.Audio)?.audioId == clip.id
                val startDp = with(density) { (clip.timelineStartMs * pxPerMs).toDp() }
                val widthDp =
                  with(density) { (clip.durationMs * pxPerMs).coerceAtLeast(44f).toDp() }
                AudioTimelineBlock(
                  clip = clip,
                  isSelected = isSelected,
                  pxPerMs = pxPerMs,
                  onSelect = { onSelectElement(SelectedElement.Audio(clip.id)) },
                  onShift = { deltaMs -> onShiftAudio(clip.id, deltaMs) },
                  onTrim = { dStart, dEnd -> onTrimAudio(clip.id, dStart, dEnd) },
                  modifier = Modifier.offset(x = startDp).width(widthDp).fillMaxHeight(),
                )
              }
            }
          }

          // Playhead Needle spanning Ruler + All Tracks
          val playheadXPx = (playheadMs * pxPerMs).roundToInt()
          Box(
            modifier =
              Modifier.offset { IntOffset(playheadXPx - 10, 0) }
                .width(20.dp)
                .fillMaxHeight()
                .pointerInput(pxPerMs, totalDurationMs) {
                  detectDragGestures { change, dragAmount ->
                    change.consume()
                    val deltaMs = (dragAmount.x / pxPerMs).toLong()
                    onSeekTo((playheadMs + deltaMs).coerceIn(0L, totalDurationMs))
                  }
                }
                .testTag("playhead_needle"),
            contentAlignment = Alignment.TopCenter,
          ) {
            // Playhead Handle Cap
            Box(
              modifier =
                Modifier.size(width = 14.dp, height = 16.dp)
                  .clip(RoundedCornerShape(bottomStart = 5.dp, bottomEnd = 5.dp))
                  .background(Brush.verticalGradient(listOf(PlayheadCrimson, NeonCyan)))
            )
            // Vertical Playhead Line
            Box(
              modifier =
                Modifier.padding(top = 15.dp)
                  .width(2.dp)
                  .fillMaxHeight()
                  .background(Brush.verticalGradient(listOf(PlayheadCrimson, NeonCyan)))
            )
          }
        }
      }
    }
  }
}

@Composable
private fun TrackLabelHeader(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  accent: Color,
  compact: Boolean,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth().border(0.5.dp, StudioBorder).padding(horizontal = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = if (compact) Arrangement.Center else Arrangement.Start,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = "$label track",
      tint = accent,
      modifier = Modifier.size(15.dp),
    )
    if (!compact) {
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
        color = TextSecondary,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun VideoTimelineBlock(
  clip: VideoClip,
  isSelected: Boolean,
  pxPerMs: Float,
  onSelect: () -> Unit,
  onTrim: (Long, Long) -> Unit,
  modifier: Modifier = Modifier,
) {
  val borderColor = if (isSelected) NeonCyan else StudioBorder
  Box(
    modifier =
      modifier
        .clip(RoundedCornerShape(7.dp))
        .background(StudioSurfaceHighest)
        .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(7.dp))
        .clickable(onClick = onSelect)
        .testTag("video_block_${clip.id}"),
  ) {
    // Background sample thumbnail tint if bundled
    if (clip.source is MediaSource.BundledSample) {
      Image(
        painter = painterResource(id = clip.source.drawableResId),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alpha = 0.35f,
        modifier = Modifier.fillMaxSize(),
      )
    }

    Row(
      modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = clip.label,
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
        color = TextPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      Text(
        text = "${String.format("%.1f", clip.durationMs / 1000f)}s",
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
        color = NeonCyan,
      )
    }

    // Left Trim Handle
    TrimHandle(
      accent = if (isSelected) NeonCyan else TextSecondary,
      pxPerMs = pxPerMs,
      onDragMs = { delta -> onTrim(delta, 0L) },
      modifier = Modifier.align(Alignment.CenterStart),
    )

    // Right Trim Handle
    TrimHandle(
      accent = if (isSelected) NeonCyan else TextSecondary,
      pxPerMs = pxPerMs,
      onDragMs = { delta -> onTrim(0L, delta) },
      modifier = Modifier.align(Alignment.CenterEnd),
    )
  }
}

@Composable
private fun OverlayTimelineBlock(
  item: OverlayItem,
  isSelected: Boolean,
  pxPerMs: Float,
  onSelect: () -> Unit,
  onShift: (Long) -> Unit,
  onTrim: (Long, Long) -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .clip(RoundedCornerShape(7.dp))
        .background(NeonPurple.copy(alpha = 0.32f))
        .border(
          if (isSelected) 1.5.dp else 1.dp,
          if (isSelected) NeonCyan else NeonPurple,
          RoundedCornerShape(7.dp),
        )
        .pointerInput(item.id, pxPerMs) {
          detectDragGestures(
            onDragStart = { onSelect() },
            onDrag = { change, dragAmount ->
              change.consume()
              onShift((dragAmount.x / pxPerMs).toLong())
            },
          )
        }
        .clickable(onClick = onSelect)
        .testTag("overlay_block_${item.id}"),
  ) {
    Text(
      text = item.label,
      style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
      color = TextPrimary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = 12.dp),
    )

    TrimHandle(
      accent = NeonPurple,
      pxPerMs = pxPerMs,
      onDragMs = { delta -> onTrim(delta, 0L) },
      modifier = Modifier.align(Alignment.CenterStart),
    )
    TrimHandle(
      accent = NeonPurple,
      pxPerMs = pxPerMs,
      onDragMs = { delta -> onTrim(0L, delta) },
      modifier = Modifier.align(Alignment.CenterEnd),
    )
  }
}

@Composable
private fun AudioTimelineBlock(
  clip: AudioClip,
  isSelected: Boolean,
  pxPerMs: Float,
  onSelect: () -> Unit,
  onShift: (Long) -> Unit,
  onTrim: (Long, Long) -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .clip(RoundedCornerShape(7.dp))
        .background(AudioTeal.copy(alpha = 0.22f))
        .border(
          if (isSelected) 1.5.dp else 1.dp,
          if (isSelected) NeonCyan else AudioTeal,
          RoundedCornerShape(7.dp),
        )
        .pointerInput(clip.id, pxPerMs) {
          detectDragGestures(
            onDragStart = { onSelect() },
            onDrag = { change, dragAmount ->
              change.consume()
              onShift((dragAmount.x / pxPerMs).toLong())
            },
          )
        }
        .clickable(onClick = onSelect)
        .testTag("audio_block_${clip.id}"),
  ) {
    // Waveform Canvas
    Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp)) {
      val bars = clip.waveform
      if (bars.isNotEmpty()) {
        val barStep = size.width / bars.size
        bars.forEachIndexed { idx, amp ->
          val barH = size.height * amp
          val x = idx * barStep + barStep * 0.5f
          drawLine(
            color = AudioTeal.copy(alpha = 0.65f),
            start = Offset(x, (size.height - barH) / 2f),
            end = Offset(x, (size.height + barH) / 2f),
            strokeWidth = (barStep * 0.45f).coerceIn(2f, 5f),
          )
        }
      }
    }

    Text(
      text = "${clip.title} • ${clip.bpm} BPM",
      style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
      color = TextPrimary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.align(Alignment.TopStart).padding(horizontal = 12.dp, vertical = 2.dp),
    )

    TrimHandle(
      accent = AudioTeal,
      pxPerMs = pxPerMs,
      onDragMs = { delta -> onTrim(delta, 0L) },
      modifier = Modifier.align(Alignment.CenterStart),
    )
    TrimHandle(
      accent = AudioTeal,
      pxPerMs = pxPerMs,
      onDragMs = { delta -> onTrim(0L, delta) },
      modifier = Modifier.align(Alignment.CenterEnd),
    )
  }
}

@Composable
private fun TrimHandle(
  accent: Color,
  pxPerMs: Float,
  onDragMs: (Long) -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .width(10.dp)
        .fillMaxHeight()
        .background(accent.copy(alpha = 0.85f))
        .pointerInput(pxPerMs) {
          detectDragGestures { change, dragAmount ->
            change.consume()
            onDragMs((dragAmount.x / pxPerMs).toLong())
          }
        },
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier =
        Modifier.width(2.dp)
          .height(14.dp)
          .clip(RoundedCornerShape(1.dp))
          .background(StudioBg)
    )
  }
}
