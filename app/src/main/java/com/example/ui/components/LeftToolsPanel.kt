package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.media.ColorMatrixUtils
import com.example.model.AudioTrackPreset
import com.example.model.FilterPreset
import com.example.model.MediaBinItem
import com.example.model.MediaSource
import com.example.model.StickerKind
import com.example.model.StudioTab
import com.example.model.TextTemplateStyle
import com.example.model.VideoAdjustments
import com.example.ui.theme.AudioTeal
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StudioBg
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHighest
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LeftToolsPanel(
  activeTab: StudioTab,
  mediaBin: List<MediaBinItem>,
  audioPresets: List<AudioTrackPreset>,
  previewingAudioId: String?,
  activeFilter: FilterPreset,
  showRail: Boolean,
  onSelectTab: (StudioTab) -> Unit,
  onImportMediaClick: () -> Unit,
  onAddMediaToTimeline: (MediaBinItem) -> Unit,
  onPreviewAudio: (AudioTrackPreset) -> Unit,
  onAddAudioToTimeline: (AudioTrackPreset) -> Unit,
  onAddTextTemplate: (TextTemplateStyle) -> Unit,
  onSelectFilter: (FilterPreset) -> Unit,
  onAddSticker: (StickerKind) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier =
      modifier
        .background(StudioPanel)
        .border(1.dp, StudioBorder)
  ) {
    if (showRail) {
      Column(
        modifier =
          Modifier.width(64.dp)
            .fillMaxHeight()
            .background(StudioBg)
            .border(1.dp, StudioBorder)
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        StudioTab.entries.forEach { tab ->
          val selected = tab == activeTab
          Column(
            modifier =
              Modifier.size(width = 54.dp, height = 54.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) StudioSurfaceElevated else Color.Transparent)
                .then(
                  if (selected) Modifier.border(1.dp, NeonCyan.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                  else Modifier
                )
                .clickable { onSelectTab(tab) }
                .testTag("tab_${tab.name.lowercase()}"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            Icon(
              imageVector = tabIcon(tab),
              contentDescription = tab.label,
              tint = if (selected) NeonCyan else TextSecondary,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
              text = tab.label,
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
              color = if (selected) NeonCyan else TextSecondary,
              maxLines = 1,
            )
          }
        }
      }
    }

    // Drawer Content
    Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(12.dp)) {
      when (activeTab) {
        StudioTab.MEDIA ->
          MediaBinContent(
            mediaBin = mediaBin,
            onImportClick = onImportMediaClick,
            onAddMedia = onAddMediaToTimeline,
          )
        StudioTab.AUDIO ->
          AudioLibraryContent(
            presets = audioPresets,
            previewingId = previewingAudioId,
            onTogglePreview = onPreviewAudio,
            onAddAudio = onAddAudioToTimeline,
          )
        StudioTab.TEXT -> TextTemplatesContent(onAddText = onAddTextTemplate)
        StudioTab.FILTERS ->
          FiltersPresetContent(activeFilter = activeFilter, onSelectFilter = onSelectFilter)
        StudioTab.STICKERS -> StickersGridContent(onAddSticker = onAddSticker)
      }
    }
  }
}

private fun tabIcon(tab: StudioTab): ImageVector =
  when (tab) {
    StudioTab.MEDIA -> Icons.Filled.VideoLibrary
    StudioTab.AUDIO -> Icons.Filled.LibraryMusic
    StudioTab.TEXT -> Icons.Filled.TextFields
    StudioTab.FILTERS -> Icons.Filled.AutoFixHigh
    StudioTab.STICKERS -> Icons.Filled.AutoAwesome
  }

@Composable
private fun MediaBinContent(
  mediaBin: List<MediaBinItem>,
  onImportClick: () -> Unit,
  onAddMedia: (MediaBinItem) -> Unit,
) {
  Column(modifier = Modifier.fillMaxSize()) {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .height(44.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.22f), NeonPurple.copy(alpha = 0.22f))))
          .border(1.dp, NeonCyan.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
          .clickable(onClick = onImportClick)
          .testTag("import_media_button"),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Filled.AddPhotoAlternate,
        contentDescription = "Import local video or photo",
        tint = NeonCyan,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "Import Video / Photo",
        style = MaterialTheme.typography.labelMedium,
        color = TextPrimary,
      )
    }

    Spacer(modifier = Modifier.height(10.dp))
    Text(
      text = "MEDIA BIN (${mediaBin.size})",
      style = MaterialTheme.typography.labelSmall,
      color = TextMuted,
    )
    Spacer(modifier = Modifier.height(8.dp))

    LazyVerticalGrid(
      columns = GridCells.Adaptive(minSize = 104.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxSize(),
    ) {
      items(mediaBin, key = { it.id }) { item ->
        Box(
          modifier =
            Modifier.height(88.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(StudioSurfaceElevated)
              .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
              .clickable { onAddMedia(item) }
              .testTag("media_item_${item.id}")
        ) {
          when (val src = item.source) {
            is MediaSource.BundledSample ->
              Image(
                painter = painterResource(id = src.drawableResId),
                contentDescription = item.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
              )
            is MediaSource.LocalImage ->
              AsyncImage(
                model = src.uri,
                contentDescription = item.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
              )
            is MediaSource.LocalVideo ->
              Box(
                modifier =
                  Modifier.fillMaxSize()
                    .background(Brush.linearGradient(listOf(StudioSurfaceHighest, StudioBg))),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Filled.Movie,
                  contentDescription = item.label,
                  tint = NeonCyan,
                  modifier = Modifier.size(28.dp),
                )
              }
          }

          // Bottom gradient overlay
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                  Brush.verticalGradient(listOf(Color.Transparent, Color(0xE60B0D12)))
                )
                .padding(horizontal = 6.dp, vertical = 4.dp)
          ) {
            Text(
              text = item.label,
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
              color = TextPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          // Quick Add Badge
          Box(
            modifier =
              Modifier.align(Alignment.TopEnd)
                .padding(5.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(NeonCyan),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Filled.Add,
              contentDescription = "Add ${item.label} to timeline",
              tint = StudioBg,
              modifier = Modifier.size(14.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AudioLibraryContent(
  presets: List<AudioTrackPreset>,
  previewingId: String?,
  onTogglePreview: (AudioTrackPreset) -> Unit,
  onAddAudio: (AudioTrackPreset) -> Unit,
) {
  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(bottom = 12.dp),
  ) {
    items(presets, key = { it.id }) { preset ->
      val isPreviewing = preset.id == previewingId
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(StudioSurfaceElevated)
            .border(
              1.dp,
              if (isPreviewing) AudioTeal else StudioBorder,
              RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          onClick = { onTogglePreview(preset) },
          modifier =
            Modifier.size(40.dp)
              .clip(CircleShape)
              .background(if (isPreviewing) AudioTeal else StudioSurfaceHighest),
        ) {
          Icon(
            imageVector = if (isPreviewing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPreviewing) "Pause preview" else "Play preview",
            tint = if (isPreviewing) StudioBg else AudioTeal,
            modifier = Modifier.size(20.dp),
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = preset.title,
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "${preset.mood} • ${preset.bpm} BPM",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(7.dp))
              .background(AudioTeal.copy(alpha = 0.18f))
              .border(1.dp, AudioTeal.copy(alpha = 0.6f), RoundedCornerShape(7.dp))
              .clickable { onAddAudio(preset) }
              .padding(horizontal = 10.dp, vertical = 7.dp)
              .testTag("add_audio_${preset.id}"),
        ) {
          Text(
            text = "+ Add",
            style = MaterialTheme.typography.labelMedium,
            color = AudioTeal,
          )
        }
      }
    }
  }
}

@Composable
private fun TextTemplatesContent(onAddText: (TextTemplateStyle) -> Unit) {
  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(10.dp),
    contentPadding = PaddingValues(bottom = 12.dp),
  ) {
    items(TextTemplateStyle.entries) { style ->
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(StudioSurfaceElevated)
            .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
            .clickable { onAddText(style) }
            .padding(12.dp)
            .testTag("text_template_${style.name.lowercase()}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = style.defaultText,
            style =
              MaterialTheme.typography.titleMedium.copy(
                fontWeight =
                  if (style == TextTemplateStyle.BOLD_CAPTION) FontWeight.ExtraBold
                  else FontWeight.SemiBold,
                letterSpacing = if (style == TextTemplateStyle.CINEMATIC_SUBTITLE) 2.sp else 0.sp,
              ),
            color = Color(style.defaultColorHex),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = style.label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
          )
        }
        Icon(
          imageVector = Icons.Filled.Add,
          contentDescription = "Add ${style.label}",
          tint = NeonPurple,
          modifier = Modifier.size(20.dp),
        )
      }
    }
  }
}

@Composable
private fun FiltersPresetContent(
  activeFilter: FilterPreset,
  onSelectFilter: (FilterPreset) -> Unit,
) {
  LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 96.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    items(FilterPreset.entries) { preset ->
      val selected = preset == activeFilter
      val matrix = ColorMatrixUtils.buildComposeColorMatrix(preset, VideoAdjustments())
      Column(
        modifier =
          Modifier.clip(RoundedCornerShape(10.dp))
            .background(StudioSurfaceElevated)
            .border(
              1.5.dp,
              if (selected) NeonCyan else StudioBorder,
              RoundedCornerShape(10.dp),
            )
            .clickable { onSelectFilter(preset) }
            .padding(6.dp)
            .testTag("filter_${preset.name.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_sample_neon_city),
          contentDescription = preset.label,
          contentScale = ContentScale.Crop,
          colorFilter = ColorFilter.colorMatrix(matrix),
          modifier =
            Modifier.fillMaxWidth()
              .height(56.dp)
              .clip(RoundedCornerShape(6.dp)),
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
          text = preset.label,
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
          color = if (selected) NeonCyan else TextPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun StickersGridContent(onAddSticker: (StickerKind) -> Unit) {
  LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 92.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    items(StickerKind.entries) { kind ->
      Column(
        modifier =
          Modifier.height(78.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(StudioSurfaceElevated)
            .border(1.dp, Color(kind.accentHex).copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .clickable { onAddSticker(kind) }
            .padding(8.dp)
            .testTag("sticker_${kind.name.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Text(
          text = kind.tagText,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = Color(kind.accentHex),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = kind.label,
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
          color = TextSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}
