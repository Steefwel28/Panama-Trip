package com.panamatrip.app.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.panamatrip.app.R

private val GoogleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val TitleGoogleFont = GoogleFont("Plus Jakarta Sans")
private val MetricGoogleFont = GoogleFont("JetBrains Mono")

/** Titulares con carácter outdoor: geométrica, redonda y de peso alto. */
private val TitleFamily = FontFamily(
    Font(
        googleFont = TitleGoogleFont,
        fontProvider = GoogleFontProvider,
        weight = FontWeight.SemiBold,
    ),
    Font(
        googleFont = TitleGoogleFont,
        fontProvider = GoogleFontProvider,
        weight = FontWeight.Bold,
    ),
)

/**
 * Cifras tabulares para cronómetros, distancias, desniveles, coordenadas y
 * precios: cada dígito ocupa el mismo ancho y el valor no baila al cambiar.
 */
private val MetricFamily = FontFamily(
    Font(
        googleFont = MetricGoogleFont,
        fontProvider = GoogleFontProvider,
        weight = FontWeight.Medium,
    ),
    Font(
        googleFont = MetricGoogleFont,
        fontProvider = GoogleFontProvider,
        weight = FontWeight.SemiBold,
    ),
)

private val InterfaceFamily = FontFamily.SansSerif

/** Estilos numéricos compartidos por las métricas de toda la app. */
object MetricType {
    val Large = TextStyle(
        fontFamily = MetricFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = "tnum",
    )
    val Medium = TextStyle(
        fontFamily = MetricFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp,
        fontFeatureSettings = "tnum",
    )
    val Small = TextStyle(
        fontFamily = MetricFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum",
    )
}

/** Tipografía editorial: titulares con personalidad y lectura sans neutra. */
val PanamaTripTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = TitleFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.8).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = TitleFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = TitleFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = TitleFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = InterfaceFamily,
        fontSize = 17.sp,
        lineHeight = 26.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = InterfaceFamily,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = TitleFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.2.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = InterfaceFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
    ),
)
