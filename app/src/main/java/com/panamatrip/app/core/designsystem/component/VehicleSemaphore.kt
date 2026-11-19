package com.panamatrip.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.panamatrip.app.core.designsystem.icons.PanamaIcons
import com.panamatrip.app.core.designsystem.labels.displayName
import com.panamatrip.app.core.designsystem.labels.ratingLabel
import com.panamatrip.app.core.designsystem.labels.reportsLabel
import com.panamatrip.app.core.designsystem.theme.Dimens
import com.panamatrip.app.domain.model.AccessStatus
import com.panamatrip.app.domain.model.VehicleAccessSummary
import com.panamatrip.app.domain.model.VehicleType

@Composable
fun accessStatusColor(status: AccessStatus): Color = when (status) {
    AccessStatus.CLEAR -> MaterialTheme.colorScheme.primary
    AccessStatus.CAUTION -> MaterialTheme.colorScheme.tertiary
    AccessStatus.BLOCKED -> MaterialTheme.colorScheme.error
}

/** Semáforo vehicular comunitario: un renglón por tipo de vehículo. */
@Composable
fun VehicleTrafficLightCard(
    summaries: Map<VehicleType, VehicleAccessSummary>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(Dimens.innerCorner),
        tonalElevation = Dimens.tonalElevation,
    ) {
        Column(
            modifier = Modifier.padding(Dimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.itemGap),
        ) {
            VehicleType.entries.forEach { vehicle ->
                val summary = summaries[vehicle]
                val status = summary?.status ?: AccessStatus.CAUTION
                val vehicleLabel = vehicle.displayName()
                val statusText = status.displayName()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.tightGap),
                ) {
                    Text(
                        text = vehicleLabel,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(
                        modifier = Modifier
                            .semantics {
                                contentDescription = "$vehicleLabel: $statusText"
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(accessStatusColor(status), CircleShape),
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = reportsLabel(summary?.reports ?: 0),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun RatingRow(
    rating: Double,
    reviews: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = PanamaIcons.Star,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.tertiary,
        )
        Text(
            text = ratingLabel(rating, reviews),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Estrellas de una reseña (1 a 5). */
@Composable
fun StarRating(
    rating: Int,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(MAX_RATING) { index ->
            Icon(
                imageVector = PanamaIcons.Star,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (index < rating) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
            )
        }
    }
}

private const val MAX_RATING = 5

@Composable
fun VehicleAccessDots(
    summaries: Map<VehicleType, VehicleAccessSummary>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VehicleType.entries.forEach { vehicle ->
            val status = summaries[vehicle]?.status ?: AccessStatus.CAUTION
            val label = "${vehicle.displayName()}: ${status.displayName()}"
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(accessStatusColor(status), CircleShape)
                    .semantics { contentDescription = label },
            )
        }
    }
}
