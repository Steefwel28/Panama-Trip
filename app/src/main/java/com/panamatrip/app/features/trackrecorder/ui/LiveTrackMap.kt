package com.panamatrip.app.features.trackrecorder.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.panamatrip.app.R
import com.panamatrip.app.core.designsystem.icons.PanamaIcons
import com.panamatrip.app.core.designsystem.theme.Dimens
import com.panamatrip.app.domain.model.GeoPoint
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

/**
 * Mapa vivo del grabador. Dibuja la polilínea GPS sobre una retícula sólida
 * que funciona sin red ni teselas externas: si el estilo remoto falla, el GPS
 * y el cronómetro siguen intactos. Arrastrar desplaza; el botón recentra.
 */
@Composable
fun LiveTrackMap(
    path: List<GeoPoint>,
    modifier: Modifier = Modifier,
) {
    var pan by remember { mutableStateOf(Offset.Zero) }
    val outline = MaterialTheme.colorScheme.outlineVariant
    val grid = MaterialTheme.colorScheme.surfaceContainerHighest
    val route = MaterialTheme.colorScheme.primary
    val head = MaterialTheme.colorScheme.tertiary

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(Dimens.cardCorner),
        tonalElevation = Dimens.tonalElevation,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(MAP_HEIGHT)
                .clip(RoundedCornerShape(Dimens.cardCorner)),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MAP_HEIGHT)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            pan += dragAmount
                        }
                    },
            ) {
                // Retícula offline: legible sin conexión y sin teselas.
                val step = size.minDimension / GRID_DIVISIONS
                var x = 0f
                while (x <= size.width) {
                    drawLine(grid, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                    x += step
                }
                var y = 0f
                while (y <= size.height) {
                    drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += step
                }

                if (path.size < 2) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(color = outline, radius = 10f, center = center)
                    drawCircle(color = route, radius = 4f, center = center)
                    return@Canvas
                }

                val projected = project(path, size.width, size.height)
                val shifted = projected.map { it + pan }
                val polyline = Path().apply {
                    moveTo(shifted.first().x, shifted.first().y)
                    shifted.drop(1).forEach { point -> lineTo(point.x, point.y) }
                }
                drawPath(
                    path = polyline,
                    color = route,
                    style = Stroke(
                        width = 6f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
                drawCircle(color = head, radius = 9f, center = shifted.last())
                drawCircle(color = Color.White, radius = 4f, center = shifted.last())
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(Dimens.tightGap),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.recorder_map_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.recorder_map_offline),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            FilledIconButton(
                onClick = { pan = Offset.Zero },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Dimens.tightGap)
                    .size(Dimens.minimumTouchTarget),
            ) {
                Icon(
                    imageVector = PanamaIcons.Compass,
                    contentDescription = stringResource(R.string.recorder_map_recenter),
                )
            }
        }
    }
}

/** Proyección equirectangular suficiente para un recorrido de senderismo. */
private fun project(
    path: List<GeoPoint>,
    width: Float,
    height: Float,
): List<Offset> {
    val minLat = path.minOf { it.latitude }
    val maxLat = path.maxOf { it.latitude }
    val minLng = path.minOf { it.longitude }
    val maxLng = path.maxOf { it.longitude }
    val meanLat = (minLat + maxLat) / 2.0
    val lngScale = cos(Math.toRadians(meanLat)).coerceAtLeast(0.2)
    val spanX = ((maxLng - minLng) * lngScale).coerceAtLeast(MIN_SPAN)
    val spanY = (maxLat - minLat).coerceAtLeast(MIN_SPAN)
    val padding = min(width, height) * MAP_PADDING_FRACTION
    val usableWidth = (width - padding * 2f).coerceAtLeast(1f)
    val usableHeight = (height - padding * 2f).coerceAtLeast(1f)
    val scale = min(usableWidth / spanX.toFloat(), usableHeight / spanY.toFloat())
    val centerX = width / 2f
    val centerY = height / 2f
    val midLng = (minLng + maxLng) / 2.0
    val midLat = (minLat + maxLat) / 2.0
    return path.map { point ->
        Offset(
            x = centerX + ((point.longitude - midLng) * lngScale * scale).toFloat(),
            y = centerY - ((point.latitude - midLat) * scale).toFloat(),
        )
    }
}

private val MAP_HEIGHT = 240.dp
private const val GRID_DIVISIONS = 8f
private const val MAP_PADDING_FRACTION = 0.12f
private const val MIN_SPAN = 0.0004
