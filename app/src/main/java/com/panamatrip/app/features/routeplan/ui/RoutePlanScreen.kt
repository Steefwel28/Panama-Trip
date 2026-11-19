package com.panamatrip.app.features.routeplan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.panamatrip.app.R
import com.panamatrip.app.core.designsystem.component.EmptyState
import com.panamatrip.app.core.designsystem.component.ErrorState
import com.panamatrip.app.core.designsystem.component.InfoNotice
import com.panamatrip.app.core.designsystem.component.OverlayTag
import com.panamatrip.app.core.designsystem.component.ScreenLoading
import com.panamatrip.app.core.designsystem.component.SectionHeader
import com.panamatrip.app.core.designsystem.component.SecondaryActionButton
import com.panamatrip.app.core.designsystem.component.TonalTag
import com.panamatrip.app.core.designsystem.component.Tone
import com.panamatrip.app.core.designsystem.component.tonalGradient
import com.panamatrip.app.core.designsystem.icons.PanamaIcons
import com.panamatrip.app.core.designsystem.labels.distanceLabel
import com.panamatrip.app.core.designsystem.labels.driveLabel
import com.panamatrip.app.core.designsystem.labels.elevationLabel
import com.panamatrip.app.core.designsystem.labels.walkLabel
import com.panamatrip.app.core.designsystem.theme.Dimens
import com.panamatrip.app.core.intents.MapIntents
import com.panamatrip.app.core.intents.launchFirstAvailable
import com.panamatrip.app.di.AppContainer
import com.panamatrip.app.domain.model.Destination

@Composable
fun RoutePlanRoute(
    container: AppContainer,
    onExplore: () -> Unit,
) {
    val viewModel: RoutePlanViewModel = viewModel(factory = RoutePlanViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    RoutePlanScreen(
        state = state,
        onRetry = viewModel::retry,
        onExplore = {
            viewModel.clearRoute()
            onExplore()
        },
        onWaze = { destination ->
            context.launchFirstAvailable(MapIntents.waze(destination.parking))
        },
        onGoogleMaps = { destination ->
            context.launchFirstAvailable(MapIntents.googleMaps(destination.parking, destination.name))
        },
    )
}

@Composable
fun RoutePlanScreen(
    state: RoutePlanUiState,
    onRetry: () -> Unit,
    onExplore: () -> Unit,
    onWaze: (Destination) -> Unit,
    onGoogleMaps: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        RouteHeader(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))

        when (state) {
            RoutePlanUiState.Loading -> ScreenLoading()
            is RoutePlanUiState.Error -> ErrorState(
                title = stringResource(R.string.error_title),
                body = stringResource(R.string.error_body),
                retryLabel = stringResource(R.string.error_retry),
                onRetry = onRetry,
                modifier = Modifier.padding(top = Dimens.sectionGap),
            )
            RoutePlanUiState.Empty -> EmptyState(
                title = stringResource(R.string.route_empty_title),
                body = stringResource(R.string.route_empty_body),
                actionLabel = stringResource(R.string.route_empty_action),
                onAction = onExplore,
                modifier = Modifier.padding(top = Dimens.sectionGap),
                icon = PanamaIcons.Route,
            )
            is RoutePlanUiState.Content -> RoutePlanContent(
                content = state,
                onWaze = onWaze,
                onGoogleMaps = onGoogleMaps,
                onExplore = onExplore,
            )
        }
    }
}

@Composable
private fun RouteHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.cardPadding, vertical = Dimens.itemGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.route_eyebrow),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                text = stringResource(R.string.route_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        TonalTag(text = stringResource(R.string.route_version_tag), tone = Tone.Neutral)
    }
}

@Composable
private fun RoutePlanContent(
    content: RoutePlanUiState.Content,
    onWaze: (Destination) -> Unit,
    onGoogleMaps: (Destination) -> Unit,
    onExplore: () -> Unit,
) {
    val destination = content.destination
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.cardPadding,
            end = Dimens.cardPadding,
            top = Dimens.tightGap,
            bottom = Dimens.sectionGap,
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.itemGap),
    ) {
        item(key = "destination") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = destination.name,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(
                        R.string.route_header_summary,
                        destination.province,
                        destination.summary,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item(key = "map") {
            ParkingMapPanel(destination = destination)
        }

        item(key = "actions") {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.tightGap)) {
                SecondaryActionButton(
                    text = stringResource(R.string.action_waze),
                    onClick = { onWaze(destination) },
                    modifier = Modifier.weight(1f),
                )
                SecondaryActionButton(
                    text = stringResource(R.string.action_google_maps),
                    onClick = { onGoogleMaps(destination) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item(key = "notice") {
            InfoNotice(
                text = stringResource(R.string.route_notice_v02),
                icon = PanamaIcons.Route,
            )
        }

        item(key = "details") {
            SectionHeader(
                title = stringResource(R.string.route_logistics_title),
                eyebrow = stringResource(R.string.route_logistics_eyebrow),
            )
        }

        item(key = "origin") {
            InfoRow(
                label = stringResource(R.string.route_origin_label),
                value = stringResource(
                    R.string.route_origin_value,
                    distanceLabel(content.originDistanceMeters),
                    driveLabel(content.originDriveMinutes),
                ),
            )
        }

        item(key = "walk") {
            InfoRow(
                label = stringResource(R.string.route_walk_label),
                value = walkLabel(destination.walkDistanceMeters, destination.walkMinutes),
            )
        }

        item(key = "elevation") {
            InfoRow(
                label = stringResource(R.string.route_elevation_label),
                value = elevationLabel(destination.elevationGainMeters),
            )
        }

        item(key = "attraction") {
            InfoRow(
                label = stringResource(R.string.route_attraction_coords_label),
                value = destination.attraction.formatted,
            )
        }

        item(key = "change") {
            TextButton(onClick = onExplore) {
                Text(
                    text = stringResource(R.string.route_change_destination),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun ParkingMapPanel(destination: Destination) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(MAP_PANEL_HEIGHT)
            .clip(RoundedCornerShape(Dimens.cardCorner))
            .background(tonalGradient("parking-${destination.id}")),
    ) {
        OverlayTag(
            text = stringResource(R.string.route_parking_label),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(Dimens.itemGap),
        )
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.tightGap),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.92f),
                shape = RoundedCornerShape(Dimens.innerCorner),
                tonalElevation = Dimens.raisedElevation,
            ) {
                Icon(
                    imageVector = PanamaIcons.Pin,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(Dimens.itemGap)
                        .size(28.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = destination.parking.formatted,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(Dimens.innerCorner),
        tonalElevation = Dimens.tonalElevation,
    ) {
        Row(
            modifier = Modifier.padding(Dimens.itemGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private val MAP_PANEL_HEIGHT = 220.dp
