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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MovieCreation
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.AspectRatioOption
import com.example.model.ExportFps
import com.example.model.ExportResolution
import com.example.model.ExportUiState
import com.example.ui.theme.AudioTeal
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.PlayheadCrimson
import com.example.ui.theme.StudioBg
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ExportModalDialog(
  exportState: ExportUiState,
  projectTitle: String,
  aspectRatio: AspectRatioOption,
  totalDurationMs: Long,
  onSelectResolution: (ExportResolution) -> Unit,
  onSelectFps: (ExportFps) -> Unit,
  onStartExport: () -> Unit,
  onDismiss: () -> Unit,
) {
  if (exportState is ExportUiState.Hidden) return

  Dialog(onDismissRequest = onDismiss) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(StudioPanel)
          .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
          .padding(18.dp)
          .testTag("export_modal"),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(
            modifier =
              Modifier.size(34.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Brush.linearGradient(listOf(NeonCyan, NeonPurple))),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Filled.MovieCreation,
              contentDescription = null,
              tint = StudioBg,
              modifier = Modifier.size(19.dp),
            )
          }
          Column {
            Text(
              text = "Export Video",
              style = MaterialTheme.typography.titleMedium,
              color = TextPrimary,
            )
            Text(
              text = "$projectTitle • ${aspectRatio.shortTag}",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
            )
          }
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
          Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Close export modal",
            tint = TextSecondary,
          )
        }
      }

      when (exportState) {
        is ExportUiState.Configuring -> {
          // Resolution Selector
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "Resolution",
              style = MaterialTheme.typography.labelMedium,
              color = TextSecondary,
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              ExportResolution.entries.forEach { res ->
                val selected = res == exportState.resolution
                Box(
                  modifier =
                    Modifier.weight(1f)
                      .height(42.dp)
                      .clip(RoundedCornerShape(9.dp))
                      .background(
                        if (selected) NeonCyan.copy(alpha = 0.16f) else StudioSurfaceElevated
                      )
                      .border(
                        1.dp,
                        if (selected) NeonCyan else StudioBorder,
                        RoundedCornerShape(9.dp),
                      )
                      .clickable { onSelectResolution(res) }
                      .testTag("export_res_${res.label}"),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = res.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) NeonCyan else TextPrimary,
                  )
                }
              }
            }
          }

          // FPS Selector
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "Frame Rate",
              style = MaterialTheme.typography.labelMedium,
              color = TextSecondary,
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              ExportFps.entries.forEach { fps ->
                val selected = fps == exportState.fps
                Box(
                  modifier =
                    Modifier.weight(1f)
                      .height(42.dp)
                      .clip(RoundedCornerShape(9.dp))
                      .background(
                        if (selected) NeonPurple.copy(alpha = 0.20f) else StudioSurfaceElevated
                      )
                      .border(
                        1.dp,
                        if (selected) NeonPurple else StudioBorder,
                        RoundedCornerShape(9.dp),
                      )
                      .clickable { onSelectFps(fps) }
                      .testTag("export_fps_${fps.label}"),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = fps.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) NeonPurple else TextPrimary,
                  )
                }
              }
            }
          }

          // Summary Estimate
          val estSizeMb =
            (totalDurationMs / 1000f) *
              exportState.resolution.mbPerSec *
              (if (exportState.fps == ExportFps.FPS_60) 1.35f else 1f)
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(StudioBg)
                .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "Estimated Size",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
            )
            Text(
              text = "~${String.format("%.1f", estSizeMb)} MB (MP4 / H.264)",
              style = MaterialTheme.typography.labelSmall,
              color = NeonCyan,
            )
          }

          // Start Export Button
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.horizontalGradient(listOf(NeonCyan, NeonPurple)))
                .clickable(onClick = onStartExport)
                .testTag("start_export_button"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "Start Export (${exportState.resolution.label} • ${exportState.fps.label})",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = Color(0xFF07090D),
            )
          }
        }

        is ExportUiState.Exporting -> {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = exportState.stageLabel,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
              )
              Text(
                text = "${(exportState.progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = NeonCyan,
              )
            }

            // Animated Gradient Progress Bar
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .height(12.dp)
                  .clip(CircleShape)
                  .background(StudioBg)
                  .border(1.dp, StudioBorder, CircleShape)
            ) {
              Box(
                modifier =
                  Modifier.fillMaxWidth(exportState.progress.coerceIn(0.03f, 1f))
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(NeonCyan, NeonPurple)))
              )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .height(44.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(StudioSurfaceElevated)
                  .border(1.dp, PlayheadCrimson.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                  .clickable(onClick = onDismiss),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "Cancel Export",
                style = MaterialTheme.typography.labelMedium,
                color = PlayheadCrimson,
              )
            }
          }
        }

        is ExportUiState.Completed -> {
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.CheckCircle,
              contentDescription = "Export completed",
              tint = AudioTeal,
              modifier = Modifier.size(44.dp),
            )
            Text(
              text = "Export Ready",
              style = MaterialTheme.typography.titleMedium,
              color = TextPrimary,
            )
            Text(
              text = exportState.outputFileName,
              style = MaterialTheme.typography.labelSmall,
              color = NeonCyan,
            )
            Text(
              text = exportState.summaryText,
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .height(46.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(Brush.horizontalGradient(listOf(NeonCyan, NeonPurple)))
                  .clickable(onClick = onDismiss),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "Back to Studio",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF07090D),
              )
            }
          }
        }

        is ExportUiState.Failed -> {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = exportState.message,
              style = MaterialTheme.typography.bodyMedium,
              color = PlayheadCrimson,
            )
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .height(44.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(StudioSurfaceElevated)
                  .clickable(onClick = onDismiss),
              contentAlignment = Alignment.Center,
            ) {
              Text("Close", color = TextPrimary, style = MaterialTheme.typography.labelMedium)
            }
          }
        }

        ExportUiState.Hidden -> Unit
      }
    }
  }
}
