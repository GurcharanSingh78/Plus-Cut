package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AudioClip
import com.example.model.OverlayItem
import com.example.model.SelectedElement
import com.example.model.TextAlignmentOption
import com.example.model.VideoAdjustments
import com.example.model.VideoClip
import com.example.ui.theme.AudioTeal
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StudioBg
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun RightInspectorPanel(
  selectedElement: SelectedElement?,
  selectedVideo: VideoClip?,
  selectedOverlay: OverlayItem?,
  selectedAudio: AudioClip?,
  onUpdateAdjustments: ((VideoAdjustments) -> VideoAdjustments) -> Unit,
  onResetAdjustments: () -> Unit,
  onUpdateTextOverlay: (String, (OverlayItem.TextOverlay) -> OverlayItem.TextOverlay) -> Unit,
  onUpdateStickerScale: (String, Float) -> Unit,
  onUpdateAudioVolume: (String, Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
      modifier
        .background(StudioPanel)
        .border(1.dp, StudioBorder)
        .padding(12.dp)
        .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.Tune,
          contentDescription = "Inspector",
          tint = NeonCyan,
          modifier = Modifier.size(17.dp),
        )
        Text(
          text =
            when {
              selectedElement is SelectedElement.Overlay && selectedOverlay is OverlayItem.TextOverlay ->
                "Text Properties"
              selectedElement is SelectedElement.Overlay && selectedOverlay is OverlayItem.StickerOverlay ->
                "Sticker Properties"
              selectedElement is SelectedElement.Audio && selectedAudio != null ->
                "Audio Properties"
              else -> "Video Adjustments"
            },
          style = MaterialTheme.typography.titleSmall,
          color = TextPrimary,
        )
      }

      if (selectedElement !is SelectedElement.Overlay && selectedElement !is SelectedElement.Audio) {
        Row(
          modifier =
            Modifier.clip(RoundedCornerShape(6.dp))
              .background(StudioSurfaceElevated)
              .clickable(onClick = onResetAdjustments)
              .padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = "Reset adjustments",
            tint = TextSecondary,
            modifier = Modifier.size(13.dp),
          )
          Text(
            text = "Reset",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
          )
        }
      }
    }

    when {
      selectedElement is SelectedElement.Overlay && selectedOverlay is OverlayItem.TextOverlay -> {
        TextOverlayInspector(
          overlay = selectedOverlay,
          onUpdate = { transform -> onUpdateTextOverlay(selectedOverlay.id, transform) },
        )
      }
      selectedElement is SelectedElement.Overlay && selectedOverlay is OverlayItem.StickerOverlay -> {
        StickerInspector(
          overlay = selectedOverlay,
          onScaleChange = { onUpdateStickerScale(selectedOverlay.id, it) },
        )
      }
      selectedElement is SelectedElement.Audio && selectedAudio != null -> {
        AudioClipInspector(
          clip = selectedAudio,
          onVolumeChange = { onUpdateAudioVolume(selectedAudio.id, it) },
        )
      }
      selectedVideo != null -> {
        VideoAdjustmentsInspector(
          clip = selectedVideo,
          onUpdateAdjustments = onUpdateAdjustments,
        )
      }
      else -> {
        Text(
          text = "Select a clip, text layer, or audio block on the timeline to inspect its properties.",
          style = MaterialTheme.typography.bodySmall,
          color = TextMuted,
        )
      }
    }
  }
}

@Composable
private fun VideoAdjustmentsInspector(
  clip: VideoClip,
  onUpdateAdjustments: ((VideoAdjustments) -> VideoAdjustments) -> Unit,
) {
  val adj = clip.adjustments
  Text(
    text = clip.label,
    style = MaterialTheme.typography.labelMedium,
    color = NeonCyan,
  )

  InspectorSlider(
    label = "Brightness",
    valueLabel = "${if (adj.brightness >= 0f) "+" else ""}${(adj.brightness * 100).toInt()}%",
    value = adj.brightness,
    valueRange = -0.45f..0.45f,
    accentColor = NeonCyan,
    onValueChange = { v -> onUpdateAdjustments { it.copy(brightness = v) } },
    testTag = "slider_brightness",
  )

  InspectorSlider(
    label = "Contrast",
    valueLabel = "${(adj.contrast * 100).toInt()}%",
    value = adj.contrast,
    valueRange = 0.5f..1.8f,
    accentColor = NeonCyan,
    onValueChange = { v -> onUpdateAdjustments { it.copy(contrast = v) } },
    testTag = "slider_contrast",
  )

  InspectorSlider(
    label = "Saturation",
    valueLabel = "${(adj.saturation * 100).toInt()}%",
    value = adj.saturation,
    valueRange = 0.0f..2.0f,
    accentColor = NeonPurple,
    onValueChange = { v -> onUpdateAdjustments { it.copy(saturation = v) } },
    testTag = "slider_saturation",
  )

  // Speed Chips
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(
      text = "Playback Speed",
      style = MaterialTheme.typography.bodySmall,
      color = TextSecondary,
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { spd ->
        val selected = kotlin.math.abs(adj.speed - spd) < 0.05f
        Box(
          modifier =
            Modifier.weight(1f)
              .height(34.dp)
              .clip(RoundedCornerShape(7.dp))
              .background(if (selected) NeonCyan.copy(alpha = 0.18f) else StudioSurfaceElevated)
              .border(
                1.dp,
                if (selected) NeonCyan else StudioBorder,
                RoundedCornerShape(7.dp),
              )
              .clickable { onUpdateAdjustments { it.copy(speed = spd) } },
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "${if (spd == 1.0f || spd == 2.0f) spd.toInt().toString() else spd.toString()}x",
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) NeonCyan else TextPrimary,
          )
        }
      }
    }
  }

  InspectorSlider(
    label = "Clip Volume",
    valueLabel = "${(adj.volume * 100).toInt()}%",
    value = adj.volume,
    valueRange = 0f..1f,
    accentColor = AudioTeal,
    onValueChange = { v -> onUpdateAdjustments { it.copy(volume = v) } },
    testTag = "slider_volume",
  )
}

@Composable
private fun TextOverlayInspector(
  overlay: OverlayItem.TextOverlay,
  onUpdate: ((OverlayItem.TextOverlay) -> OverlayItem.TextOverlay) -> Unit,
) {
  OutlinedTextField(
    value = overlay.text,
    onValueChange = { newText -> onUpdate { it.copy(text = newText) } },
    label = { Text("Text Content", style = MaterialTheme.typography.bodySmall) },
    singleLine = false,
    maxLines = 3,
    colors =
      OutlinedTextFieldDefaults.colors(
        focusedBorderColor = NeonPurple,
        unfocusedBorderColor = StudioBorder,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedContainerColor = StudioSurfaceElevated,
        unfocusedContainerColor = StudioSurfaceElevated,
      ),
    modifier = Modifier.fillMaxWidth().testTag("text_overlay_input"),
  )

  InspectorSlider(
    label = "Font Size",
    valueLabel = "${overlay.fontSizeSp.toInt()}sp",
    value = overlay.fontSizeSp,
    valueRange = 14f..52f,
    accentColor = NeonPurple,
    onValueChange = { v -> onUpdate { it.copy(fontSizeSp = v) } },
    testTag = "slider_text_size",
  )

  // Color Picker Swatches
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(
      text = "Color",
      style = MaterialTheme.typography.bodySmall,
      color = TextSecondary,
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      val swatches =
        listOf(
          0xFFF4F7FC,
          0xFF00F0FF,
          0xFF8B5CF6,
          0xFFFF3B5C,
          0xFFFACC15,
          0xFF14B8A6,
          0xFFF59E0B,
        )
      swatches.forEach { hex ->
        val selected = overlay.colorHex == hex
        Box(
          modifier =
            Modifier.size(26.dp)
              .clip(CircleShape)
              .background(Color(hex))
              .border(
                width = if (selected) 2.5.dp else 1.dp,
                color = if (selected) Color.White else StudioBorder,
                shape = CircleShape,
              )
              .clickable { onUpdate { it.copy(colorHex = hex) } },
        )
      }
    }
  }

  // Alignment Buttons
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(
      text = "Alignment",
      style = MaterialTheme.typography.bodySmall,
      color = TextSecondary,
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      TextAlignmentOption.entries.forEach { opt ->
        val selected = overlay.alignment == opt
        Box(
          modifier =
            Modifier.weight(1f)
              .height(38.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(if (selected) NeonPurple.copy(alpha = 0.25f) else StudioSurfaceElevated)
              .border(
                1.dp,
                if (selected) NeonPurple else StudioBorder,
                RoundedCornerShape(8.dp),
              )
              .clickable { onUpdate { it.copy(alignment = opt) } },
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector =
              when (opt) {
                TextAlignmentOption.LEFT -> Icons.AutoMirrored.Filled.FormatAlignLeft
                TextAlignmentOption.CENTER -> Icons.Filled.FormatAlignCenter
                TextAlignmentOption.RIGHT -> Icons.AutoMirrored.Filled.FormatAlignRight
              },
            contentDescription = opt.label,
            tint = if (selected) NeonPurple else TextSecondary,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun StickerInspector(
  overlay: OverlayItem.StickerOverlay,
  onScaleChange: (Float) -> Unit,
) {
  Text(
    text = overlay.kind.label,
    style = MaterialTheme.typography.labelMedium,
    color = Color(overlay.kind.accentHex),
  )
  InspectorSlider(
    label = "Sticker Scale",
    valueLabel = "${String.format("%.1f", overlay.scale)}x",
    value = overlay.scale,
    valueRange = 0.6f..2.2f,
    accentColor = Color(overlay.kind.accentHex),
    onValueChange = onScaleChange,
    testTag = "slider_sticker_scale",
  )
}

@Composable
private fun AudioClipInspector(
  clip: AudioClip,
  onVolumeChange: (Float) -> Unit,
) {
  Text(
    text = clip.title,
    style = MaterialTheme.typography.labelMedium,
    color = AudioTeal,
  )
  Text(
    text = "${clip.bpm} BPM • Synthesized Studio Loop",
    style = MaterialTheme.typography.bodySmall,
    color = TextSecondary,
  )
  InspectorSlider(
    label = "Track Volume",
    valueLabel = "${(clip.volume * 100).toInt()}%",
    value = clip.volume,
    valueRange = 0f..1f,
    accentColor = AudioTeal,
    onValueChange = onVolumeChange,
    testTag = "slider_audio_volume",
  )
}

@Composable
private fun InspectorSlider(
  label: String,
  valueLabel: String,
  value: Float,
  valueRange: ClosedFloatingPointRange<Float>,
  accentColor: Color,
  onValueChange: (Float) -> Unit,
  testTag: String,
) {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
      Text(text = valueLabel, style = MaterialTheme.typography.labelSmall, color = accentColor)
    }
    Slider(
      value = value,
      onValueChange = onValueChange,
      valueRange = valueRange,
      colors =
        SliderDefaults.colors(
          thumbColor = accentColor,
          activeTrackColor = accentColor,
          inactiveTrackColor = StudioBg,
        ),
      modifier = Modifier.fillMaxWidth().height(28.dp).testTag(testTag),
    )
  }
}
