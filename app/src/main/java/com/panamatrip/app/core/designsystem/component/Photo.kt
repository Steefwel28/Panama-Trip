package com.panamatrip.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.panamatrip.app.core.designsystem.icons.PanamaIcons
import kotlin.math.abs

/**
 * Degradado tonal determinista: es el respaldo visual cuando la foto aún no
 * está en caché o el dispositivo está sin señal.
 */
@Composable
fun tonalGradient(seed: String): Brush {
    val colors = MaterialTheme.colorScheme
    val pair = remember(seed, colors.primaryContainer, colors.tertiaryContainer, colors.secondaryContainer) {
        val options = listOf(
            colors.primaryContainer to colors.tertiaryContainer,
            colors.tertiaryContainer to colors.secondaryContainer,
            colors.secondaryContainer to colors.primaryContainer,
            colors.surfaceContainerHigh to colors.primaryContainer,
        )
        options[abs(seed.hashCode()) % options.size]
    }
    return remember(pair) { Brush.linearGradient(listOf(pair.first, pair.second)) }
}

/** Imagen con caché persistente de Coil y respaldo tonal offline. */
@Composable
fun TonalImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    corner: Dp = 0.dp,
    seed: String = imageUrl.orEmpty(),
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(tonalGradient(seed)),
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(44.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = PanamaIcons.Camera,
                        contentDescription = contentDescription,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/** Cabecera fotográfica edge-to-edge con degradado inferior legible. */
@Composable
fun PhotoHero(
    imageUrl: String?,
    contentDescription: String?,
    height: Dp,
    modifier: Modifier = Modifier,
    seed: String = imageUrl.orEmpty(),
    scrimColor: Color = MaterialTheme.colorScheme.scrim,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(0.dp)),
    ) {
        TonalImage(
            imageUrl = imageUrl,
            contentDescription = contentDescription,
            seed = seed,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to scrimColor.copy(alpha = 0.55f),
                            0.45f to Color.Transparent,
                            1f to scrimColor.copy(alpha = 0.88f),
                        ),
                    ),
                ),
        )
        content()
    }
}
