package com.panamatrip.app.features.onboarding.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.panamatrip.app.R
import com.panamatrip.app.core.designsystem.haptic.rememberElasticSplashHaptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class PixelShape { Clover, Pill, Scalloped, Squircle }

private enum class PanamaScene { HarpyEagle, AmericasBridge, BaruVolcano, Waterfall }

private data class SplashTile(
    val shape: PixelShape,
    val scene: PanamaScene,
)

private val tiles = listOf(
    SplashTile(PixelShape.Clover, PanamaScene.HarpyEagle),
    SplashTile(PixelShape.Pill, PanamaScene.AmericasBridge),
    SplashTile(PixelShape.Scalloped, PanamaScene.BaruVolcano),
    SplashTile(PixelShape.Squircle, PanamaScene.Waterfall),
)

/**
 * Splash Bento panameño: mosaico 2×2 de siluetas vectoriales de dos tonos,
 * sin texto dentro de los tiles, y wordmark debajo. Vive ~1.35 s y solo se
 * muestra una vez por arranque de Activity (la navegación lo controla).
 */
@Composable
fun PanamaSplash(modifier: Modifier = Modifier) {
    val haptics = rememberElasticSplashHaptics()
    val scope = rememberCoroutineScope()
    val wordmark = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        haptics.play(scope)
        delay(860)
        wordmark.animateTo(1f, animationSpec = tween(durationMillis = 360))
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SplashTileView(tiles[0], index = 0)
                    SplashTileView(tiles[1], index = 1)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SplashTileView(tiles[2], index = 2)
                    SplashTileView(tiles[3], index = 3)
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = stringResource(R.string.home_brand),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = (-1.4).sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.graphicsLayer {
                    alpha = wordmark.value
                    translationY = (1f - wordmark.value) * 18.dp.toPx()
                },
            )
            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.graphicsLayer { alpha = wordmark.value },
            )
        }
    }
}

@Composable
private fun SplashTileView(tile: SplashTile, index: Int) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(index) {
        delay(index * 120L)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        )
    }

    val background = when (index % 4) {
        0 -> MaterialTheme.colorScheme.primaryContainer
        1 -> MaterialTheme.colorScheme.secondaryContainer
        2 -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val ink = when (index % 4) {
        0 -> MaterialTheme.colorScheme.onPrimaryContainer
        1 -> MaterialTheme.colorScheme.onSecondaryContainer
        2 -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val accent = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = Modifier
            .size(112.dp)
            .graphicsLayer {
                val value = progress.value
                alpha = value
                scaleX = 0.82f + 0.18f * value
                scaleY = 0.82f + 0.18f * value
                translationY = (1f - value) * 22.dp.toPx()
            },
    ) {
        val shape = pixelShapePath(tile.shape, size)
        clipPath(shape) {
            drawRect(color = background)
            when (tile.scene) {
                PanamaScene.HarpyEagle -> drawHarpyEagle(ink, accent)
                PanamaScene.AmericasBridge -> drawAmericasBridge(ink, accent)
                PanamaScene.BaruVolcano -> drawBaruVolcano(ink, accent)
                PanamaScene.Waterfall -> drawWaterfall(ink, accent)
            }
        }
    }
}

private fun pixelShapePath(shape: PixelShape, size: Size): Path = Path().apply {
    fillType = PathFillType.NonZero
    when (shape) {
        PixelShape.Clover -> {
            val radius = size.minDimension * 0.29f
            addOval(Rect(center = Offset(size.width * 0.3f, size.height * 0.3f), radius = radius))
            addOval(Rect(center = Offset(size.width * 0.7f, size.height * 0.3f), radius = radius))
            addOval(Rect(center = Offset(size.width * 0.3f, size.height * 0.7f), radius = radius))
            addOval(Rect(center = Offset(size.width * 0.7f, size.height * 0.7f), radius = radius))
        }
        PixelShape.Pill -> addRoundRect(
            RoundRect(
                Rect(
                    Offset(size.width * 0.08f, size.height * 0.2f),
                    Size(size.width * 0.84f, size.height * 0.6f),
                ),
                CornerRadius(size.height * 0.3f, size.height * 0.3f),
            ),
        )
        PixelShape.Scalloped -> {
            val middle = Offset(size.width / 2f, size.height / 2f)
            addOval(Rect(center = middle, radius = size.minDimension * 0.32f))
            repeat(8) { index ->
                val angle = Math.toRadians((index * 45).toDouble())
                val cx = middle.x + (Math.cos(angle) * size.width * 0.26f).toFloat()
                val cy = middle.y + (Math.sin(angle) * size.height * 0.26f).toFloat()
                addOval(Rect(center = Offset(cx, cy), radius = size.minDimension * 0.13f))
            }
        }
        PixelShape.Squircle -> addRoundRect(
            RoundRect(
                Rect(
                    Offset(size.width * 0.1f, size.height * 0.1f),
                    Size(size.width * 0.8f, size.height * 0.8f),
                ),
                CornerRadius(size.width * 0.24f, size.height * 0.24f),
            ),
        )
    }
}

/** Águila Harpía: cresta, pico ganchudo y alas desplegadas. */
private fun DrawScope.drawHarpyEagle(ink: Color, accent: Color) {
    val w = size.width
    val h = size.height

    // Alas desplegadas.
    val leftWing = Path().apply {
        moveTo(w * 0.46f, h * 0.42f)
        cubicTo(w * 0.28f, h * 0.36f, w * 0.12f, h * 0.46f, w * 0.08f, h * 0.64f)
        cubicTo(w * 0.24f, h * 0.58f, w * 0.34f, h * 0.62f, w * 0.48f, h * 0.56f)
        close()
    }
    val rightWing = Path().apply {
        moveTo(w * 0.54f, h * 0.42f)
        cubicTo(w * 0.72f, h * 0.36f, w * 0.88f, h * 0.46f, w * 0.92f, h * 0.64f)
        cubicTo(w * 0.76f, h * 0.58f, w * 0.66f, h * 0.62f, w * 0.52f, h * 0.56f)
        close()
    }
    drawPath(leftWing, ink)
    drawPath(rightWing, ink)

    // Cola.
    val tail = Path().apply {
        moveTo(w * 0.44f, h * 0.68f)
        lineTo(w * 0.56f, h * 0.68f)
        lineTo(w * 0.62f, h * 0.9f)
        lineTo(w * 0.5f, h * 0.84f)
        lineTo(w * 0.38f, h * 0.9f)
        close()
    }
    drawPath(tail, ink)

    // Cuerpo y cabeza.
    drawOval(
        color = ink,
        topLeft = Offset(w * 0.38f, h * 0.36f),
        size = Size(w * 0.24f, h * 0.34f),
    )
    drawCircle(color = ink, radius = w * 0.12f, center = Offset(w * 0.5f, h * 0.3f))

    // Cresta de tres plumas.
    repeat(3) { index ->
        val crest = Path().apply {
            val baseX = w * (0.42f + index * 0.08f)
            moveTo(baseX, h * 0.23f)
            lineTo(baseX + w * 0.035f, h * 0.08f)
            lineTo(baseX + w * 0.07f, h * 0.23f)
            close()
        }
        drawPath(crest, ink)
    }

    // Pico.
    val beak = Path().apply {
        moveTo(w * 0.6f, h * 0.28f)
        lineTo(w * 0.72f, h * 0.32f)
        lineTo(w * 0.6f, h * 0.36f)
        close()
    }
    drawPath(beak, accent)
    drawCircle(color = accent, radius = w * 0.018f, center = Offset(w * 0.53f, h * 0.28f))
}

/** Puente de las Américas sobre las líneas del Canal. */
private fun DrawScope.drawAmericasBridge(ink: Color, accent: Color) {
    val w = size.width
    val h = size.height

    drawCircle(color = accent, radius = w * 0.09f, center = Offset(w * 0.8f, h * 0.2f))

    // Agua del canal.
    repeat(3) { index ->
        val y = h * (0.74f + index * 0.07f)
        drawLine(
            color = ink.copy(alpha = 0.55f - index * 0.12f),
            start = Offset(w * 0.06f, y),
            end = Offset(w * 0.94f, y),
            strokeWidth = h * 0.018f,
            cap = StrokeCap.Round,
        )
    }

    // Tablero.
    drawRoundRect(
        color = ink,
        topLeft = Offset(w * 0.06f, h * 0.56f),
        size = Size(w * 0.88f, h * 0.055f),
        cornerRadius = CornerRadius(h * 0.02f, h * 0.02f),
    )

    // Torres.
    listOf(0.3f, 0.7f).forEach { x ->
        drawRoundRect(
            color = ink,
            topLeft = Offset(w * (x - 0.035f), h * 0.22f),
            size = Size(w * 0.07f, h * 0.36f),
            cornerRadius = CornerRadius(w * 0.012f, w * 0.012f),
        )
        val top = Path().apply {
            moveTo(w * (x - 0.055f), h * 0.22f)
            lineTo(w * x, h * 0.1f)
            lineTo(w * (x + 0.055f), h * 0.22f)
            close()
        }
        drawPath(top, ink)
    }

    // Tirantes.
    listOf(0.3f, 0.7f).forEach { x ->
        drawLine(
            color = ink.copy(alpha = 0.8f),
            start = Offset(w * x, h * 0.13f),
            end = Offset(w * (x - 0.2f), h * 0.56f),
            strokeWidth = h * 0.012f,
        )
        drawLine(
            color = ink.copy(alpha = 0.8f),
            start = Offset(w * x, h * 0.13f),
            end = Offset(w * (x + 0.2f), h * 0.56f),
            strokeWidth = h * 0.012f,
        )
    }
}

/** Volcán Barú con sol y fumarola. */
private fun DrawScope.drawBaruVolcano(ink: Color, accent: Color) {
    val w = size.width
    val h = size.height

    drawCircle(color = accent, radius = w * 0.11f, center = Offset(w * 0.74f, h * 0.24f))

    val mountain = Path().apply {
        moveTo(w * 0.06f, h * 0.84f)
        lineTo(w * 0.34f, h * 0.42f)
        lineTo(w * 0.44f, h * 0.3f)
        lineTo(w * 0.5f, h * 0.38f)
        lineTo(w * 0.56f, h * 0.3f)
        lineTo(w * 0.66f, h * 0.42f)
        lineTo(w * 0.94f, h * 0.84f)
        close()
    }
    drawPath(mountain, ink)

    // Nieve / cráter en el pico.
    val crater = Path().apply {
        moveTo(w * 0.44f, h * 0.3f)
        lineTo(w * 0.5f, h * 0.38f)
        lineTo(w * 0.56f, h * 0.3f)
        lineTo(w * 0.52f, h * 0.34f)
        lineTo(w * 0.48f, h * 0.34f)
        close()
    }
    drawPath(crater, accent)

    // Fumarola.
    drawCircle(color = ink.copy(alpha = 0.35f), radius = w * 0.035f, center = Offset(w * 0.5f, h * 0.22f))
    drawCircle(color = ink.copy(alpha = 0.22f), radius = w * 0.05f, center = Offset(w * 0.46f, h * 0.15f))
    drawCircle(color = ink.copy(alpha = 0.16f), radius = w * 0.06f, center = Offset(w * 0.53f, h * 0.09f))

    // Base de vegetación.
    drawLine(
        color = ink,
        start = Offset(w * 0.04f, h * 0.86f),
        end = Offset(w * 0.96f, h * 0.86f),
        strokeWidth = h * 0.03f,
        cap = StrokeCap.Round,
    )
}

/** Cascada con heliconias y ondas. */
private fun DrawScope.drawWaterfall(ink: Color, accent: Color) {
    val w = size.width
    val h = size.height

    // Acantilado.
    val cliff = Path().apply {
        moveTo(w * 0.08f, h * 0.12f)
        lineTo(w * 0.46f, h * 0.18f)
        lineTo(w * 0.42f, h * 0.7f)
        lineTo(w * 0.08f, h * 0.78f)
        close()
    }
    drawPath(cliff, ink)

    // Caída de agua.
    val water = Path().apply {
        moveTo(w * 0.46f, h * 0.2f)
        lineTo(w * 0.62f, h * 0.22f)
        lineTo(w * 0.66f, h * 0.68f)
        cubicTo(w * 0.6f, h * 0.74f, w * 0.52f, h * 0.72f, w * 0.44f, h * 0.68f)
        close()
    }
    drawPath(water, accent.copy(alpha = 0.85f))
    repeat(3) { index ->
        drawLine(
            color = Color.White.copy(alpha = 0.6f - index * 0.12f),
            start = Offset(w * (0.49f + index * 0.04f), h * 0.26f),
            end = Offset(w * (0.5f + index * 0.05f), h * 0.62f),
            strokeWidth = h * 0.012f,
            cap = StrokeCap.Round,
        )
    }

    // Pozo y ondas.
    drawOval(
        color = accent.copy(alpha = 0.55f),
        topLeft = Offset(w * 0.36f, h * 0.68f),
        size = Size(w * 0.48f, h * 0.16f),
    )
    repeat(2) { index ->
        drawArc(
            color = ink.copy(alpha = 0.5f),
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(w * (0.28f + index * 0.08f), h * (0.76f + index * 0.04f)),
            size = Size(w * (0.5f + index * 0.14f), h * (0.12f + index * 0.03f)),
            style = Stroke(width = h * 0.014f, cap = StrokeCap.Round),
        )
    }

    // Heliconias.
    listOf(0.16f to 0.62f, 0.24f to 0.58f).forEach { (x, y) ->
        val flower = Path().apply {
            moveTo(w * x, h * y)
            cubicTo(w * (x + 0.08f), h * (y - 0.12f), w * (x + 0.14f), h * (y - 0.04f), w * x, h * (y + 0.06f))
            close()
        }
        drawPath(flower, accent)
        drawLine(
            color = ink,
            start = Offset(w * x, h * (y + 0.06f)),
            end = Offset(w * x, h * (y + 0.18f)),
            strokeWidth = h * 0.012f,
            cap = StrokeCap.Round,
        )
    }
}
