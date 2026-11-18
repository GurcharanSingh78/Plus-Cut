package com.example.media

import androidx.compose.ui.graphics.ColorMatrix
import androidx.media3.common.Effect
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.RgbMatrix
import com.example.model.FilterPreset
import com.example.model.VideoAdjustments

object ColorMatrixUtils {
  fun buildComposeColorMatrix(
    preset: FilterPreset,
    adjustments: VideoAdjustments,
  ): ColorMatrix {
    val result = ColorMatrix()

    // 1. Saturation
    val satMatrix = ColorMatrix().apply { setToSaturation(adjustments.saturation) }
    result.timesAssign(satMatrix)

    // 2. Contrast & Brightness
    val c = adjustments.contrast
    val b = adjustments.brightness * 255f
    val translate = (1f - c) * 128f + b
    val bcMatrix =
      ColorMatrix(
        floatArrayOf(
          c, 0f, 0f, 0f, translate,
          0f, c, 0f, 0f, translate,
          0f, 0f, c, 0f, translate,
          0f, 0f, 0f, 1f, 0f,
        )
      )
    result.timesAssign(bcMatrix)

    // 3. Filter Preset
    val presetMatrix = presetColorMatrix(preset)
    if (presetMatrix != null) {
      result.timesAssign(presetMatrix)
    }

    return result
  }

  fun presetColorMatrix(preset: FilterPreset): ColorMatrix? =
    when (preset) {
      FilterPreset.NONE -> null
      FilterPreset.CYBERPUNK_NEON ->
        ColorMatrix(
          floatArrayOf(
            0.92f, 0.05f, 0.25f, 0f, -8f,
            0.05f, 1.12f, 0.15f, 0f, 6f,
            0.22f, 0.12f, 1.35f, 0f, 18f,
            0f, 0f, 0f, 1f, 0f,
          )
        )
      FilterPreset.VINTAGE_WARM ->
        ColorMatrix(
          floatArrayOf(
            1.15f, 0.18f, 0.05f, 0f, 12f,
            0.05f, 1.02f, 0.04f, 0f, 4f,
            0.02f, 0.05f, 0.78f, 0f, -10f,
            0f, 0f, 0f, 1f, 0f,
          )
        )
      FilterPreset.BW_NOIR ->
        ColorMatrix().apply {
          setToSaturation(0f)
          val noirContrast =
            ColorMatrix(
              floatArrayOf(
                1.28f, 0f, 0f, 0f, -32f,
                0f, 1.28f, 0f, 0f, -32f,
                0f, 0f, 1.28f, 0f, -32f,
                0f, 0f, 0f, 1f, 0f,
              )
            )
          timesAssign(noirContrast)
        }
      FilterPreset.HIGH_CONTRAST ->
        ColorMatrix(
          floatArrayOf(
            1.42f, 0f, 0f, 0f, -45f,
            0f, 1.42f, 0f, 0f, -45f,
            0f, 0f, 1.42f, 0f, -45f,
            0f, 0f, 0f, 1f, 0f,
          )
        )
      FilterPreset.VIVID ->
        ColorMatrix().apply {
          setToSaturation(1.45f)
          val pop =
            ColorMatrix(
              floatArrayOf(
                1.08f, 0f, 0f, 0f, 4f,
                0f, 1.08f, 0f, 0f, 4f,
                0f, 0f, 1.08f, 0f, 4f,
                0f, 0f, 0f, 1f, 0f,
              )
            )
          timesAssign(pop)
        }
    }

  /**
   * Builds real-time Media3 Effect pipeline for ExoPlayer & Transformer.
   */
  fun buildMedia3Effects(preset: FilterPreset, adjustments: VideoAdjustments): List<Effect> {
    val effects = mutableListOf<Effect>()

    val contrastDelta = (adjustments.contrast - 1f).coerceIn(-0.9f, 0.9f)
    if (kotlin.math.abs(contrastDelta) > 0.01f) {
      effects += Contrast(contrastDelta)
    }

    val satDelta = ((adjustments.saturation - 1f) * 100f).coerceIn(-100f, 100f)
    val lightDelta = (adjustments.brightness * 100f).coerceIn(-50f, 50f)
    if (kotlin.math.abs(satDelta) > 1f || kotlin.math.abs(lightDelta) > 1f) {
      effects += HslAdjustment.Builder().adjustSaturation(satDelta).adjustLightness(lightDelta).build()
    }

    when (preset) {
      FilterPreset.NONE -> Unit
      FilterPreset.BW_NOIR -> {
        effects += RgbFilter.createGrayscaleFilter()
        effects += Contrast(0.28f)
      }
      FilterPreset.HIGH_CONTRAST -> effects += Contrast(0.42f)
      FilterPreset.VIVID ->
        effects += HslAdjustment.Builder().adjustSaturation(45f).build()
      FilterPreset.VINTAGE_WARM ->
        effects +=
          RgbMatrix { _, _ ->
            floatArrayOf(
              1.14f, 0.08f, 0.0f, 0.0f,
              0.04f, 1.02f, 0.0f, 0.0f,
              0.0f, 0.02f, 0.78f, 0.0f,
              0.0f, 0.0f, 0.0f, 1.0f,
            )
          }
      FilterPreset.CYBERPUNK_NEON ->
        effects +=
          RgbMatrix { _, _ ->
            floatArrayOf(
              0.92f, 0.04f, 0.22f, 0.0f,
              0.04f, 1.10f, 0.14f, 0.0f,
              0.20f, 0.10f, 1.32f, 0.0f,
              0.0f, 0.0f, 0.0f, 1.0f,
            )
          }
    }

    return effects
  }
}
