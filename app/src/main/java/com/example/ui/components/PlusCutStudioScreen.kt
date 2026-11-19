package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.StudioTab
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StudioBg
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.StudioViewModel
import kotlinx.coroutines.delay

private enum class CompactTray {
  NONE,
  TOOLS,
  INSPECTOR,
}

@Composable
fun PlusCutStudioScreen(viewModel: StudioViewModel) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var compactTray by remember { mutableStateOf(CompactTray.NONE) }

  val mediaPickerLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickVisualMedia(),
      onResult = { uri ->
        if (uri != null) {
          viewModel.importLocalMedia(uri)
        }
      },
    )

  val launchMediaPicker = {
    mediaPickerLauncher.launch(
      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
    )
  }

  LaunchedEffect(uiState.statusMessage) {
    if (uiState.statusMessage != null) {
      delay(2200L)
      viewModel.clearStatusMessage()
    }
  }

  val visibleOverlays =
    remember(uiState.overlayTrack, uiState.playheadMs) {
      uiState.overlayTrack.filter {
        uiState.playheadMs in it.timelineStartMs..it.timelineEndMs
      }
    }

  Scaffold(
    containerColor = StudioBg,
    contentWindowInsets = WindowInsets.safeDrawing,
  ) { innerPadding ->
    BoxWithConstraints(
      modifier =
        Modifier.fillMaxSize()
          .background(StudioBg)
          .padding(innerPadding)
    ) {
      val isWideStudio = maxWidth >= 720.dp

      Column(modifier = Modifier.fillMaxSize()) {
        TopHeaderBar(
          projectTitle = uiState.projectTitle,
          aspectRatio = uiState.aspectRatio,
          isCompact = !isWideStudio,
          onTitleChange = viewModel::updateProjectTitle,
          onAspectRatioSelected = viewModel::setAspectRatio,
          onExportClick = viewModel::openExportModal,
        )

        if (isWideStudio) {
          Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LeftToolsPanel(
              activeTab = uiState.activeTab,
              mediaBin = uiState.mediaBin,
              audioPresets = uiState.audioPresets,
              previewingAudioId = uiState.previewingAudioId,
              activeFilter = uiState.globalFilter,
              showRail = true,
              onSelectTab = viewModel::selectTab,
              onImportMediaClick = launchMediaPicker,
              onAddMediaToTimeline = viewModel::addMediaToTimeline,
              onPreviewAudio = viewModel::togglePreviewAudioPreset,
              onAddAudioToTimeline = viewModel::addAudioToTimeline,
              onAddTextTemplate = viewModel::addTextTemplate,
              onSelectFilter = viewModel::applyFilterPreset,
              onAddSticker = viewModel::addSticker,
              modifier = Modifier.width(304.dp).fillMaxHeight(),
            )

            PreviewCanvasPanel(
              aspectRatio = uiState.aspectRatio,
              activeClip = uiState.activeVideoClip,
              globalFilter = uiState.globalFilter,
              visibleOverlays = visibleOverlays,
              selectedElement = uiState.selectedElement,
              playheadMs = uiState.playheadMs,
              totalDurationMs = uiState.totalDurationMs,
              isPlaying = uiState.isPlaying,
              isFullscreen = uiState.isFullscreen,
              onTogglePlayPause = viewModel::togglePlayPause,
              onSkipBackward = { viewModel.skipBy(-5_000L) },
              onSkipForward = { viewModel.skipBy(5_000L) },
              onToggleFullscreen = viewModel::toggleFullscreen,
              onSelectOverlay = { overlayId ->
                viewModel.selectElement(com.example.model.SelectedElement.Overlay(overlayId))
              },
              onDragOverlay = viewModel::dragOverlayPosition,
              modifier = Modifier.weight(1f).fillMaxHeight(),
            )

            RightInspectorPanel(
              selectedElement = uiState.selectedElement,
              selectedVideo = uiState.selectedVideoClip,
              selectedOverlay = uiState.selectedOverlay,
              selectedAudio = uiState.selectedAudioClip,
              onUpdateAdjustments = viewModel::updateVideoAdjustments,
              onResetAdjustments = viewModel::resetVideoAdjustments,
              onUpdateTextOverlay = viewModel::updateTextOverlay,
              onUpdateStickerScale = viewModel::updateStickerScale,
              onUpdateAudioVolume = viewModel::updateAudioVolume,
              modifier = Modifier.width(248.dp).fillMaxHeight(),
            )
          }

          TimelinePanel(
            videoTrack = uiState.videoTrack,
            overlayTrack = uiState.overlayTrack,
            audioTrack = uiState.audioTrack,
            selectedElement = uiState.selectedElement,
            playheadMs = uiState.playheadMs,
            totalDurationMs = uiState.totalDurationMs,
            timelineZoom = uiState.timelineZoom,
            canUndo = uiState.canUndo,
            canRedo = uiState.canRedo,
            isCompact = false,
            onUndo = viewModel::undo,
            onRedo = viewModel::redo,
            onSplit = viewModel::splitAtPlayhead,
            onDelete = viewModel::deleteSelected,
            onZoomChange = viewModel::setTimelineZoom,
            onSeekTo = viewModel::seekTo,
            onSelectElement = viewModel::selectElement,
            onTrimVideo = viewModel::trimVideoClip,
            onShiftOverlay = viewModel::shiftOverlay,
            onTrimOverlay = viewModel::trimOverlay,
            onShiftAudio = viewModel::shiftAudio,
            onTrimAudio = viewModel::trimAudio,
            modifier = Modifier.height(215.dp).fillMaxWidth(),
          )
        } else {
          // Larger CapCut-Style Portrait Video Preview
          PreviewCanvasPanel(
            aspectRatio = uiState.aspectRatio,
            activeClip = uiState.activeVideoClip,
            globalFilter = uiState.globalFilter,
            visibleOverlays = visibleOverlays,
            selectedElement = uiState.selectedElement,
            playheadMs = uiState.playheadMs,
            totalDurationMs = uiState.totalDurationMs,
            isPlaying = uiState.isPlaying,
            isFullscreen = uiState.isFullscreen,
            onTogglePlayPause = viewModel::togglePlayPause,
            onSkipBackward = { viewModel.skipBy(-5_000L) },
            onSkipForward = { viewModel.skipBy(5_000L) },
            onToggleFullscreen = viewModel::toggleFullscreen,
            onSelectOverlay = { overlayId ->
              viewModel.selectElement(com.example.model.SelectedElement.Overlay(overlayId))
              compactTray = CompactTray.INSPECTOR
            },
            onDragOverlay = viewModel::dragOverlayPosition,
            modifier = Modifier.weight(if (compactTray == CompactTray.NONE) 0.58f else 0.45f).fillMaxWidth(),
          )

          if (compactTray != CompactTray.NONE) {
            Column(
              modifier =
                Modifier.fillMaxWidth()
                  .height(190.dp)
                  .background(StudioPanel)
                  .border(1.dp, StudioBorder)
            ) {
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .height(34.dp)
                    .background(StudioBg)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text =
                    if (compactTray == CompactTray.TOOLS) uiState.activeTab.label
                    else "Edit & Style",
                  style = MaterialTheme.typography.labelMedium,
                  color = NeonCyan,
                )
                IconButton(
                  onClick = { compactTray = CompactTray.NONE },
                  modifier = Modifier.size(32.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close tray",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp),
                  )
                }
              }

              if (compactTray == CompactTray.TOOLS) {
                LeftToolsPanel(
                  activeTab = uiState.activeTab,
                  mediaBin = uiState.mediaBin,
                  audioPresets = uiState.audioPresets,
                  previewingAudioId = uiState.previewingAudioId,
                  activeFilter = uiState.globalFilter,
                  showRail = false,
                  onSelectTab = viewModel::selectTab,
                  onImportMediaClick = launchMediaPicker,
                  onAddMediaToTimeline = viewModel::addMediaToTimeline,
                  onPreviewAudio = viewModel::togglePreviewAudioPreset,
                  onAddAudioToTimeline = viewModel::addAudioToTimeline,
                  onAddTextTemplate = { style ->
                    viewModel.addTextTemplate(style)
                    compactTray = CompactTray.INSPECTOR
                  },
                  onSelectFilter = viewModel::applyFilterPreset,
                  onAddSticker = viewModel::addSticker,
                  modifier = Modifier.fillMaxSize(),
                )
              } else {
                RightInspectorPanel(
                  selectedElement = uiState.selectedElement,
                  selectedVideo = uiState.selectedVideoClip,
                  selectedOverlay = uiState.selectedOverlay,
                  selectedAudio = uiState.selectedAudioClip,
                  onUpdateAdjustments = viewModel::updateVideoAdjustments,
                  onResetAdjustments = viewModel::resetVideoAdjustments,
                  onUpdateTextOverlay = viewModel::updateTextOverlay,
                  onUpdateStickerScale = viewModel::updateStickerScale,
                  onUpdateAudioVolume = viewModel::updateAudioVolume,
                  modifier = Modifier.fillMaxSize(),
                )
              }
            }
          }

          TimelinePanel(
            videoTrack = uiState.videoTrack,
            overlayTrack = uiState.overlayTrack,
            audioTrack = uiState.audioTrack,
            selectedElement = uiState.selectedElement,
            playheadMs = uiState.playheadMs,
            totalDurationMs = uiState.totalDurationMs,
            timelineZoom = uiState.timelineZoom,
            canUndo = uiState.canUndo,
            canRedo = uiState.canRedo,
            isCompact = true,
            onUndo = viewModel::undo,
            onRedo = viewModel::redo,
            onSplit = viewModel::splitAtPlayhead,
            onDelete = viewModel::deleteSelected,
            onZoomChange = viewModel::setTimelineZoom,
            onSeekTo = viewModel::seekTo,
            onSelectElement = viewModel::selectElement,
            onTrimVideo = viewModel::trimVideoClip,
            onShiftOverlay = viewModel::shiftOverlay,
            onTrimOverlay = viewModel::trimOverlay,
            onShiftAudio = viewModel::shiftAudio,
            onTrimAudio = viewModel::trimAudio,
            modifier = Modifier.weight(if (compactTray == CompactTray.NONE) 0.42f else 0.30f).fillMaxWidth(),
          )

          // Compact Bottom Studio Dock
          CompactStudioDock(
            activeTab = uiState.activeTab,
            activeTray = compactTray,
            onSelectToolTab = { tab ->
              if (compactTray == CompactTray.TOOLS && uiState.activeTab == tab) {
                compactTray = CompactTray.NONE
              } else {
                viewModel.selectTab(tab)
                compactTray = CompactTray.TOOLS
              }
            },
            onToggleInspector = {
              compactTray =
                if (compactTray == CompactTray.INSPECTOR) CompactTray.NONE
                else CompactTray.INSPECTOR
            },
          )
        }
      }

      AnimatedVisibility(
        visible = uiState.statusMessage != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.TopCenter).padding(top = 64.dp),
      ) {
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(20.dp))
              .background(StudioSurfaceElevated.copy(alpha = 0.94f))
              .border(1.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
              .padding(horizontal = 14.dp, vertical = 7.dp),
        ) {
          Text(
            text = uiState.statusMessage.orEmpty(),
            style = MaterialTheme.typography.labelMedium,
            color = NeonCyan,
          )
        }
      }

      if (uiState.isFullscreen) {
        BackHandler { viewModel.toggleFullscreen() }
        Box(
          modifier =
            Modifier.fillMaxSize()
              .background(Color.Black)
              .windowInsetsPadding(WindowInsets.safeDrawing)
              .padding(12.dp),
        ) {
          PreviewCanvasPanel(
            aspectRatio = uiState.aspectRatio,
            activeClip = uiState.activeVideoClip,
            globalFilter = uiState.globalFilter,
            visibleOverlays = visibleOverlays,
            selectedElement = uiState.selectedElement,
            playheadMs = uiState.playheadMs,
            totalDurationMs = uiState.totalDurationMs,
            isPlaying = uiState.isPlaying,
            isFullscreen = true,
            onTogglePlayPause = viewModel::togglePlayPause,
            onSkipBackward = { viewModel.skipBy(-5_000L) },
            onSkipForward = { viewModel.skipBy(5_000L) },
            onToggleFullscreen = viewModel::toggleFullscreen,
            onSelectOverlay = { overlayId ->
              viewModel.selectElement(com.example.model.SelectedElement.Overlay(overlayId))
            },
            onDragOverlay = viewModel::dragOverlayPosition,
            modifier = Modifier.fillMaxSize(),
          )
        }
      }

      ExportModalDialog(
        exportState = uiState.exportState,
        projectTitle = uiState.projectTitle,
        aspectRatio = uiState.aspectRatio,
        totalDurationMs = uiState.totalDurationMs,
        onSelectResolution = viewModel::setExportResolution,
        onSelectFps = viewModel::setExportFps,
        onStartExport = viewModel::startExport,
        onDismiss = viewModel::closeExportModal,
      )
    }
  }
}

@Composable
private fun CompactStudioDock(
  activeTab: StudioTab,
  activeTray: CompactTray,
  onSelectToolTab: (StudioTab) -> Unit,
  onToggleInspector: () -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .background(StudioPanel)
        .border(1.dp, StudioBorder)
        .navigationBarsPadding()
        .height(58.dp)
        .padding(horizontal = 4.dp),
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    DockButton(
      icon = Icons.Filled.VideoLibrary,
      label = "Media",
      selected = activeTray == CompactTray.TOOLS && activeTab == StudioTab.MEDIA,
      accent = NeonCyan,
      onClick = { onSelectToolTab(StudioTab.MEDIA) },
      testTag = "dock_media",
    )
    DockButton(
      icon = Icons.Filled.LibraryMusic,
      label = "Audio",
      selected = activeTray == CompactTray.TOOLS && activeTab == StudioTab.AUDIO,
      accent = NeonCyan,
      onClick = { onSelectToolTab(StudioTab.AUDIO) },
      testTag = "dock_audio",
    )
    DockButton(
      icon = Icons.Filled.TextFields,
      label = "Text",
      selected = activeTray == CompactTray.TOOLS && activeTab == StudioTab.TEXT,
      accent = NeonPurple,
      onClick = { onSelectToolTab(StudioTab.TEXT) },
      testTag = "dock_text",
    )
    DockButton(
      icon = Icons.Filled.AutoFixHigh,
      label = "Filters",
      selected = activeTray == CompactTray.TOOLS && activeTab == StudioTab.FILTERS,
      accent = NeonCyan,
      onClick = { onSelectToolTab(StudioTab.FILTERS) },
      testTag = "dock_filters",
    )
    DockButton(
      icon = Icons.Filled.AutoAwesome,
      label = "Stickers",
      selected = activeTray == CompactTray.TOOLS && activeTab == StudioTab.STICKERS,
      accent = NeonPurple,
      onClick = { onSelectToolTab(StudioTab.STICKERS) },
      testTag = "dock_stickers",
    )
    DockButton(
      icon = Icons.Filled.Tune,
      label = "Adjust",
      selected = activeTray == CompactTray.INSPECTOR,
      accent = NeonCyan,
      onClick = onToggleInspector,
      testTag = "dock_inspector",
    )
  }
}

@Composable
private fun DockButton(
  icon: ImageVector,
  label: String,
  selected: Boolean,
  accent: Color,
  onClick: () -> Unit,
  testTag: String,
) {
  Column(
    modifier =
      Modifier.height(48.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(if (selected) StudioSurfaceElevated else Color.Transparent)
        .clickable(onClick = onClick)
        .padding(horizontal = 8.dp, vertical = 4.dp)
        .testTag(testTag),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = if (selected) accent else TextSecondary,
      modifier = Modifier.size(19.dp),
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = label,
      fontSize = 10.sp,
      color = if (selected) TextPrimary else TextSecondary,
    )
  }
}
