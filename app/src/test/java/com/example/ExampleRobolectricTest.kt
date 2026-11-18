package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AspectRatioOption
import com.example.model.ExportFps
import com.example.model.ExportResolution
import com.example.model.ExportUiState
import com.example.model.FilterPreset
import com.example.model.TextTemplateStyle
import com.example.viewmodel.StudioViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Plus Cut", appName)
  }

  @Test
  fun `split clip at playhead and undo restores original count`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = StudioViewModel(app)
    val initialCount = vm.uiState.value.videoTrack.size
    assertTrue(initialCount >= 2)

    vm.seekTo(2_000L)
    vm.splitAtPlayhead()
    assertEquals(initialCount + 1, vm.uiState.value.videoTrack.size)
    assertTrue(vm.uiState.value.canUndo)

    vm.undo()
    assertEquals(initialCount, vm.uiState.value.videoTrack.size)
    assertTrue(vm.uiState.value.canRedo)
  }

  @Test
  fun `filters text templates aspect ratio and export modal update state`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = StudioViewModel(app)

    vm.setAspectRatio(AspectRatioOption.RATIO_16_9)
    assertEquals(AspectRatioOption.RATIO_16_9, vm.uiState.value.aspectRatio)

    vm.applyFilterPreset(FilterPreset.CYBERPUNK_NEON)
    assertEquals(FilterPreset.CYBERPUNK_NEON, vm.uiState.value.globalFilter)

    val overlayCount = vm.uiState.value.overlayTrack.size
    vm.addTextTemplate(TextTemplateStyle.BOLD_CAPTION)
    assertEquals(overlayCount + 1, vm.uiState.value.overlayTrack.size)

    vm.openExportModal()
    vm.setExportResolution(ExportResolution.UHD_4K)
    vm.setExportFps(ExportFps.FPS_60)
    val exp = vm.uiState.value.exportState as ExportUiState.Configuring
    assertEquals(ExportResolution.UHD_4K, exp.resolution)
    assertEquals(ExportFps.FPS_60, exp.fps)
  }
}
