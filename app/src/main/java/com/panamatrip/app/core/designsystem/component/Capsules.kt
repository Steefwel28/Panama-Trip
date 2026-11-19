package com.panamatrip.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.panamatrip.app.core.designsystem.theme.Dimens

data class MetricItem(
    val label: String,
    val value: String,
)

/**
 * Cápsula translúcida estilo Strava: se apoya sobre la foto y separa los
 * datos verificados de la publicación.
 */
@Composable
fun MetricCapsule(
    items: List<MetricItem>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.18f),
        shape = RoundedCornerShape(Dimens.innerCorner),
        contentColor = Color.White,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                MetricCell(item)
                if (index != items.lastIndex) {
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCell(item: MetricItem) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.value,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            maxLines = 1,
        )
    }
}

/** Etiqueta compacta con tono tonal de marca (sin bordes). */
@Composable
fun TonalTag(
    text: String,
    modifier: Modifier = Modifier,
    tone: Tone = Tone.Primary,
) {
    Surface(
        modifier = modifier,
        color = tone.containerColor(),
        contentColor = tone.contentColor(),
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

/** Etiqueta translúcida para usar sobre fotografía. */
@Composable
fun OverlayTag(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.42f),
        contentColor = Color.White,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

enum class Tone {
    Primary,
    Secondary,
    Tertiary,
    Neutral,
}

@Composable
fun Tone.containerColor(): Color = when (this) {
    Tone.Primary -> MaterialTheme.colorScheme.primaryContainer
    Tone.Secondary -> MaterialTheme.colorScheme.secondaryContainer
    Tone.Tertiary -> MaterialTheme.colorScheme.tertiaryContainer
    Tone.Neutral -> MaterialTheme.colorScheme.surfaceContainerHigh
}

@Composable
fun Tone.contentColor(): Color = when (this) {
    Tone.Primary -> MaterialTheme.colorScheme.onPrimaryContainer
    Tone.Secondary -> MaterialTheme.colorScheme.onSecondaryContainer
    Tone.Tertiary -> MaterialTheme.colorScheme.onTertiaryContainer
    Tone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
}
