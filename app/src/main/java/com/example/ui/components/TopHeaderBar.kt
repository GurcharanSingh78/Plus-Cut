package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AspectRatioOption
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.SpaceGroteskFamily
import com.example.ui.theme.StudioBg
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TopHeaderBar(
  projectTitle: String,
  aspectRatio: AspectRatioOption,
  isCompact: Boolean,
  onTitleChange: (String) -> Unit,
  onAspectRatioSelected: (AspectRatioOption) -> Unit,
  onExportClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var ratioMenuExpanded by remember { mutableStateOf(false) }

  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .height(56.dp)
        .background(StudioPanel)
        .border(width = 1.dp, color = StudioBorder)
        .padding(horizontal = if (isCompact) 10.dp else 16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    // Brand Logo + Title
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.weight(1f, fill = false),
    ) {
      Box(
        modifier =
          Modifier.size(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(Brush.linearGradient(listOf(NeonCyan, NeonPurple))),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Filled.ContentCut,
          contentDescription = "Plus Cut Logo",
          tint = StudioBg,
          modifier = Modifier.size(19.dp),
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      if (!isCompact) {
        Text(
          text = stringResource(R.string.app_name),
          style =
            TextStyle(
              fontFamily = SpaceGroteskFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 17.sp,
              brush = Brush.linearGradient(listOf(NeonCyan, NeonPurple)),
            ),
          maxLines = 1,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(modifier = Modifier.width(1.dp).height(20.dp).background(StudioBorder))
        Spacer(modifier = Modifier.width(12.dp))
      }

      // Project Title Input
      Box(
        modifier =
          Modifier.widthIn(min = 90.dp, max = if (isCompact) 140.dp else 210.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(StudioSurfaceElevated)
            .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.CenterStart,
      ) {
        BasicTextField(
          value = projectTitle,
          onValueChange = onTitleChange,
          singleLine = true,
          textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
          cursorBrush = SolidColor(NeonCyan),
          modifier = Modifier.fillMaxWidth().testTag("project_title_input"),
        )
      }
    }

    Spacer(modifier = Modifier.width(8.dp))

    // Aspect Ratio Switcher + Export CTA
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Box {
        Row(
          modifier =
            Modifier.height(38.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(StudioSurfaceElevated)
              .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
              .clickable { ratioMenuExpanded = true }
              .padding(horizontal = if (isCompact) 8.dp else 12.dp)
              .testTag("aspect_ratio_button"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.AspectRatio,
            contentDescription = "Aspect ratio",
            tint = NeonCyan,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = if (isCompact) aspectRatio.shortTag else aspectRatio.label,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = "Open aspect ratio menu",
            tint = TextSecondary,
            modifier = Modifier.size(16.dp),
          )
        }

        DropdownMenu(
          expanded = ratioMenuExpanded,
          onDismissRequest = { ratioMenuExpanded = false },
          modifier = Modifier.background(StudioPanel).border(1.dp, StudioBorder),
        ) {
          AspectRatioOption.entries.forEach { option ->
            DropdownMenuItem(
              text = {
                Text(
                  text = option.label,
                  style = MaterialTheme.typography.bodyMedium,
                  color = if (option == aspectRatio) NeonCyan else TextPrimary,
                )
              },
              onClick = {
                onAspectRatioSelected(option)
                ratioMenuExpanded = false
              },
            )
          }
        }
      }

      // Export Button
      Row(
        modifier =
          Modifier.height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.horizontalGradient(listOf(NeonCyan, NeonPurple)))
            .clickable(onClick = onExportClick)
            .padding(horizontal = if (isCompact) 12.dp else 16.dp)
            .testTag("export_button"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.FileUpload,
          contentDescription = stringResource(R.string.action_export),
          tint = Color(0xFF07090D),
          modifier = Modifier.size(17.dp),
        )
        Text(
          text = stringResource(R.string.action_export),
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = Color(0xFF07090D),
        )
      }
    }
  }
}
