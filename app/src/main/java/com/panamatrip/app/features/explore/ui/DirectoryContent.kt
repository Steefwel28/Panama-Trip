package com.panamatrip.app.features.explore.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.panamatrip.app.R
import com.panamatrip.app.core.designsystem.component.EmptyState
import com.panamatrip.app.core.designsystem.component.RatingRow
import com.panamatrip.app.core.designsystem.component.TonalImage
import com.panamatrip.app.core.designsystem.component.TonalTag
import com.panamatrip.app.core.designsystem.component.Tone
import com.panamatrip.app.core.designsystem.component.VehicleAccessDots
import com.panamatrip.app.core.designsystem.icons.PanamaIcons
import com.panamatrip.app.core.designsystem.labels.displayName
import com.panamatrip.app.core.designsystem.labels.distanceLabel
import com.panamatrip.app.core.designsystem.labels.driveLabel
import com.panamatrip.app.core.designsystem.theme.Dimens
import com.panamatrip.app.domain.geo.DistanceBand
import com.panamatrip.app.domain.model.Ecosystem
import com.panamatrip.app.domain.model.VehicleType
import com.panamatrip.app.domain.ranking.DirectoryFilters

@Composable
fun DirectoryContent(
    entries: List<DirectoryEntryUi>,
    filters: DirectoryFilters,
    onToggleVehicle: (VehicleType) -> Unit,
    onToggleEcosystem: (Ecosystem) -> Unit,
    onToggleDistance: (Int) -> Unit,
    onClearFilters: () -> Unit,
    onOpenDestination: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.cardPadding,
            end = Dimens.cardPadding,
            bottom = Dimens.sectionGap,
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.itemGap),
    ) {
        item(key = "filters") {
            DirectoryFiltersBlock(
                filters = filters,
                onToggleVehicle = onToggleVehicle,
                onToggleEcosystem = onToggleEcosystem,
                onToggleDistance = onToggleDistance,
                onClearFilters = onClearFilters,
            )
        }

        if (entries.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    title = stringResource(R.string.directory_empty_title),
                    body = stringResource(R.string.directory_empty_body),
                    actionLabel = stringResource(R.string.directory_clear_filters),
                    onAction = onClearFilters,
                )
            }
        } else {
            item(key = "count") {
                Text(
                    text = stringResource(R.string.directory_result_count, entries.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Dimens.tightGap),
                )
            }
            items(items = entries, key = { it.destination.id }) { entry ->
                DestinationRow(
                    entry = entry,
                    onClick = { onOpenDestination(entry.destination.id) },
                )
            }
        }
    }
}

@Composable
private fun DirectoryFiltersBlock(
    filters: DirectoryFilters,
    onToggleVehicle: (VehicleType) -> Unit,
    onToggleEcosystem: (Ecosystem) -> Unit,
    onToggleDistance: (Int) -> Unit,
    onClearFilters: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.tightGap)) {
        Text(
            text = stringResource(R.string.directory_vehicle_question),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.tightGap)) {
            VehicleType.entries.forEach { vehicle ->
                FilterChip(
                    selected = filters.vehicle == vehicle,
                    onClick = { onToggleVehicle(vehicle) },
                    label = { Text(vehicle.displayName()) },
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.tightGap)) {
            Ecosystem.entries.forEach { ecosystem ->
                FilterChip(
                    selected = filters.ecosystem == ecosystem,
                    onClick = { onToggleEcosystem(ecosystem) },
                    label = { Text(ecosystem.displayName()) },
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.tightGap)) {
            FilterChip(
                selected = filters.maxDistanceMeters == DistanceBand.NEAR_LIMIT_METERS,
                onClick = { onToggleDistance(DistanceBand.NEAR_LIMIT_METERS) },
                label = { Text(stringResource(R.string.directory_distance_near)) },
            )
            FilterChip(
                selected = filters.maxDistanceMeters == DistanceBand.MID_LIMIT_METERS,
                onClick = { onToggleDistance(DistanceBand.MID_LIMIT_METERS) },
                label = { Text(stringResource(R.string.directory_distance_mid)) },
            )
            if (filters.isActive) {
                FilterChip(
                    selected = true,
                    onClick = onClearFilters,
                    label = { Text(stringResource(R.string.directory_clear_filters)) },
                )
            }
        }
        Text(
            text = stringResource(R.string.directory_distance_origin),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DestinationRow(
    entry: DirectoryEntryUi,
    onClick: () -> Unit,
) {
    val destination = entry.destination
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Dimens.cardCorner),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = Dimens.tonalElevation,
    ) {
        Row(
            modifier = Modifier.padding(Dimens.itemGap),
            horizontalArrangement = Arrangement.spacedBy(Dimens.itemGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TonalImage(
                imageUrl = destination.heroImageUrl,
                contentDescription = stringResource(R.string.photo_of, destination.name),
                seed = destination.id,
                corner = Dimens.innerCorner,
                modifier = Modifier.size(88.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = destination.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        R.string.destination_meta,
                        destination.province,
                        destination.ecosystem.displayName(),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = distanceLabel(entry.distanceMeters),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.separator_dot),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = driveLabel(entry.driveMinutes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                RatingRow(rating = destination.averageRating, reviews = destination.reviewCount)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.tightGap),
                ) {
                    TonalTag(
                        text = destination.recommendedVehicle.displayName(),
                        tone = Tone.Secondary,
                    )
                    VehicleAccessDots(summaries = destination.vehicleAccess)
                }
            }
            Icon(
                imageVector = PanamaIcons.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
