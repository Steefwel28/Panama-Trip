package com.panamatrip.app.core.designsystem.theme

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class PaletteContrastTest {

    @Test
    fun `light scheme keeps WCAG AA contrast on text pairs`() {
        assertContrast(BrandPalette.White, BrandPalette.Jungle, "onPrimary / primary")
        assertContrast(BrandPalette.JungleDeep, BrandPalette.JungleMist, "onPrimaryContainer / primaryContainer")
        assertContrast(BrandPalette.White, BrandPalette.Stone, "onSecondary / secondary")
        assertContrast(BrandPalette.StoneDeep, BrandPalette.StoneMist, "onSecondaryContainer / secondaryContainer")
        assertContrast(BrandPalette.White, BrandPalette.Terracotta, "onTertiary / tertiary")
        assertContrast(BrandPalette.TerracottaDeep, BrandPalette.TerracottaMist, "onTertiaryContainer / tertiaryContainer")
        assertContrast(BrandPalette.Ink, BrandPalette.Sand, "onSurface / surface")
        assertContrast(BrandPalette.Ink, BrandPalette.SandLow, "onSurface / surfaceContainerLow")
        assertContrast(BrandPalette.InkMuted, BrandPalette.Sand, "onSurfaceVariant / surface")
        assertContrast(BrandPalette.InkMuted, BrandPalette.JungleMist, "onSurfaceVariant / primaryContainer")
        assertContrast(BrandPalette.White, BrandPalette.ErrorLight, "onError / error")
        assertContrast(BrandPalette.OnErrorContainerLight, BrandPalette.ErrorContainerLight, "onErrorContainer / errorContainer")
    }

    @Test
    fun `dark scheme keeps WCAG AA contrast on text pairs`() {
        assertContrast(BrandPalette.JungleDeep, BrandPalette.JungleLight, "onPrimary / primary")
        assertContrast(BrandPalette.JungleMist, BrandPalette.JungleShadow, "onPrimaryContainer / primaryContainer")
        assertContrast(BrandPalette.StoneDeep, BrandPalette.StoneLight, "onSecondary / secondary")
        assertContrast(BrandPalette.StoneMist, BrandPalette.StoneShadow, "onSecondaryContainer / secondaryContainer")
        assertContrast(BrandPalette.TerracottaDeep, BrandPalette.TerracottaLight, "onTertiary / tertiary")
        assertContrast(BrandPalette.TerracottaMist, BrandPalette.TerracottaShadow, "onTertiaryContainer / tertiaryContainer")
        assertContrast(BrandPalette.Paper, BrandPalette.Night, "onSurface / surface")
        assertContrast(BrandPalette.Paper, BrandPalette.NightLow, "onSurface / surfaceContainerLow")
        assertContrast(BrandPalette.PaperMuted, BrandPalette.Night, "onSurfaceVariant / surface")
        assertContrast(BrandPalette.PaperMuted, BrandPalette.JungleShadow, "onSurfaceVariant / primaryContainer")
        assertContrast(BrandPalette.OnErrorDark, BrandPalette.ErrorDark, "onError / error")
        assertContrast(BrandPalette.OnErrorContainerDark, BrandPalette.ErrorContainerDark, "onErrorContainer / errorContainer")
    }

    @Test
    fun `outlines stay visible as non text boundaries`() {
        assertContrast(BrandPalette.OutlineLight, BrandPalette.Sand, "outline / surface claro", minimum = WCAG_AA_GRAPHICS)
        assertContrast(BrandPalette.OutlineDark, BrandPalette.Night, "outline / surface oscuro", minimum = WCAG_AA_GRAPHICS)
    }

    private fun assertContrast(
        foreground: Long,
        background: Long,
        label: String,
        minimum: Double = WCAG_AA_NORMAL_TEXT,
    ) {
        val ratio = contrastRatio(foreground, background)
        assertTrue(
            "$label debe alcanzar $minimum:1 y obtuvo $ratio:1",
            ratio >= minimum,
        )
    }

    private fun contrastRatio(foreground: Long, background: Long): Double {
        val first = relativeLuminance(foreground)
        val second = relativeLuminance(background)
        return (max(first, second) + 0.05) / (min(first, second) + 0.05)
    }

    private fun relativeLuminance(argb: Long): Double {
        fun linearChannel(shift: Int): Double {
            val channel = ((argb shr shift) and CHANNEL_MASK) / CHANNEL_MAX
            return if (channel <= SRGB_LINEAR_THRESHOLD) {
                channel / SRGB_LINEAR_DIVISOR
            } else {
                ((channel + SRGB_OFFSET) / SRGB_SCALE).pow(SRGB_EXPONENT)
            }
        }

        return RED_WEIGHT * linearChannel(16) +
            GREEN_WEIGHT * linearChannel(8) +
            BLUE_WEIGHT * linearChannel(0)
    }

    private companion object {
        const val WCAG_AA_NORMAL_TEXT = 4.5
        const val WCAG_AA_GRAPHICS = 3.0
        const val CHANNEL_MASK = 0xFFL
        const val CHANNEL_MAX = 255.0
        const val SRGB_LINEAR_THRESHOLD = 0.03928
        const val SRGB_LINEAR_DIVISOR = 12.92
        const val SRGB_OFFSET = 0.055
        const val SRGB_SCALE = 1.055
        const val SRGB_EXPONENT = 2.4
        const val RED_WEIGHT = 0.2126
        const val GREEN_WEIGHT = 0.7152
        const val BLUE_WEIGHT = 0.0722
    }
}
