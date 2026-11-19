package com.panamatrip.app.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Color(BrandPalette.Jungle),
    onPrimary = Color(BrandPalette.White),
    primaryContainer = Color(BrandPalette.JungleMist),
    onPrimaryContainer = Color(BrandPalette.JungleDeep),
    inversePrimary = Color(BrandPalette.JungleLight),
    secondary = Color(BrandPalette.Stone),
    onSecondary = Color(BrandPalette.White),
    secondaryContainer = Color(BrandPalette.StoneMist),
    onSecondaryContainer = Color(BrandPalette.StoneDeep),
    tertiary = Color(BrandPalette.Terracotta),
    onTertiary = Color(BrandPalette.White),
    tertiaryContainer = Color(BrandPalette.TerracottaMist),
    onTertiaryContainer = Color(BrandPalette.TerracottaDeep),
    background = Color(BrandPalette.Sand),
    onBackground = Color(BrandPalette.Ink),
    surface = Color(BrandPalette.Sand),
    onSurface = Color(BrandPalette.Ink),
    surfaceVariant = Color(BrandPalette.SandVariant),
    onSurfaceVariant = Color(BrandPalette.InkMuted),
    surfaceTint = Color(BrandPalette.Jungle),
    inverseSurface = Color(BrandPalette.Ink),
    inverseOnSurface = Color(BrandPalette.Sand),
    error = Color(BrandPalette.ErrorLight),
    onError = Color(BrandPalette.White),
    errorContainer = Color(BrandPalette.ErrorContainerLight),
    onErrorContainer = Color(BrandPalette.OnErrorContainerLight),
    outline = Color(BrandPalette.OutlineLight),
    outlineVariant = Color(BrandPalette.OutlineVariantLight),
    scrim = Color(BrandPalette.Black),
    surfaceContainerLowest = Color(BrandPalette.White),
    surfaceContainerLow = Color(BrandPalette.SandLow),
    surfaceContainer = Color(BrandPalette.SandMid),
    surfaceContainerHigh = Color(BrandPalette.SandHigh),
    surfaceContainerHighest = Color(BrandPalette.SandHighest),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(BrandPalette.JungleLight),
    onPrimary = Color(BrandPalette.JungleDeep),
    primaryContainer = Color(BrandPalette.JungleShadow),
    onPrimaryContainer = Color(BrandPalette.JungleMist),
    inversePrimary = Color(BrandPalette.Jungle),
    secondary = Color(BrandPalette.StoneLight),
    onSecondary = Color(BrandPalette.StoneDeep),
    secondaryContainer = Color(BrandPalette.StoneShadow),
    onSecondaryContainer = Color(BrandPalette.StoneMist),
    tertiary = Color(BrandPalette.TerracottaLight),
    onTertiary = Color(BrandPalette.TerracottaDeep),
    tertiaryContainer = Color(BrandPalette.TerracottaShadow),
    onTertiaryContainer = Color(BrandPalette.TerracottaMist),
    background = Color(BrandPalette.Night),
    onBackground = Color(BrandPalette.Paper),
    surface = Color(BrandPalette.Night),
    onSurface = Color(BrandPalette.Paper),
    surfaceVariant = Color(BrandPalette.NightVariant),
    onSurfaceVariant = Color(BrandPalette.PaperMuted),
    surfaceTint = Color(BrandPalette.JungleLight),
    inverseSurface = Color(BrandPalette.Paper),
    inverseOnSurface = Color(BrandPalette.Ink),
    error = Color(BrandPalette.ErrorDark),
    onError = Color(BrandPalette.OnErrorDark),
    errorContainer = Color(BrandPalette.ErrorContainerDark),
    onErrorContainer = Color(BrandPalette.OnErrorContainerDark),
    outline = Color(BrandPalette.OutlineDark),
    outlineVariant = Color(BrandPalette.NightVariant),
    scrim = Color(BrandPalette.Black),
    surfaceContainerLowest = Color(BrandPalette.SurfaceLowestDark),
    surfaceContainerLow = Color(BrandPalette.NightLow),
    surfaceContainer = Color(BrandPalette.NightMid),
    surfaceContainerHigh = Color(BrandPalette.NightHigh),
    surfaceContainerHighest = Color(BrandPalette.NightHighest),
)

@Composable
fun PanamaTripTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PanamaTripTypography,
        content = content,
    )
}
