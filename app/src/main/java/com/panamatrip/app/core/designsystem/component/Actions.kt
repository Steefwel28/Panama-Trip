package com.panamatrip.app.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.panamatrip.app.core.designsystem.theme.Dimens
import com.panamatrip.app.core.designsystem.theme.PanamaMotion

/** Acción principal de pantalla: una sola por vista. */
@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.buttonHeight),
        enabled = enabled,
        shape = RoundedCornerShape(Dimens.innerCorner),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.tightGap),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(Dimens.iconSize))
            }
            Text(text = text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Acción secundaria, elevación tonal sin bordes. */
@Composable
fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.buttonHeight),
        enabled = enabled,
        shape = RoundedCornerShape(Dimens.innerCorner),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.tightGap),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(Dimens.iconSize))
            }
            Text(text = text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Acción social compacta (corazón, comentario, guardado). */
@Composable
fun SocialAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    selected: Boolean = false,
    selectedTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.tertiary,
) {
    val tint by animateColorAsState(
        targetValue = if (selected) selectedTint else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = PanamaMotion.spatial(),
        label = "socialActionTint",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.9f,
        animationSpec = PanamaMotion.bouncy(),
        label = "socialActionScale",
    )
    Row(
        modifier = modifier.defaultMinSize(minHeight = Dimens.minimumTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                tint = tint,
            )
        }
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
