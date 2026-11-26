package com.panamatrip.app.features.routeplan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.panamatrip.app.R
import com.panamatrip.app.core.designsystem.component.EmptyState
import com.panamatrip.app.core.designsystem.component.ErrorState
import com.panamatrip.app.core.designsystem.component.InfoNotice
import com.panamatrip.app.core.designsystem.component.ScreenLoading
import com.panamatrip.app.core.designsystem.component.SecondaryActionButton
import com.panamatrip.app.core.designsystem.component.SectionHeader
import com.panamatrip.app.core.designsystem.component.TonalTag
import com.panamatrip.app.core.designsystem.component.Tone
import com.panamatrip.app.core.designsystem.icons.PanamaIcons
import com.panamatrip.app.core.designsystem.theme.Dimens
import com.panamatrip.app.core.intents.MapIntents
import com.panamatrip.app.core.intents.launchFirstAvailable
import com.panamatrip.app.core.ui.LocalSnackbarHostState
import com.panamatrip.app.core.ui.noticeTextRes
import com.panamatrip.app.di.AppContainer
import com.panamatrip.app.domain.logistics.FuelType
import com.panamatrip.app.domain.model.Destination

@Composable
fun RoutePlanRoute(
    container: AppContainer,
    onExplore: () -> Unit,
    onOpenRecorder: (String?) -> Unit,
) {
    val viewModel: RoutePlanViewModel = viewModel(factory = RoutePlanViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbarHostState = LocalSnackbarHostState.current

    LaunchedEffect(viewModel) {
        viewModel.notices.collect { notice ->
            snackbarHostState.showSnackbar(resources.getString(notice.noticeTextRes()))
        }
    }

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
        onSelectFuel = viewModel::selectFuel,
        onSetSurcharge = viewModel::setSurcharge,
        onSelectDeparture = viewModel::selectDeparture,
        onSelectOrigin = viewModel::selectOrigin,
        onSavePlan = viewModel::saveCurrentPlan,
        onOpenRecorder = onOpenRecorder,
    )
}

@Composable
fun RoutePlanScreen(
    state: RoutePlanUiState,
    onRetry: () -> Unit,
    onExplore: () -> Unit,
    onWaze: (Destination) -> Unit,
    onGoogleMaps: (Destination) -> Unit,
    onSelectFuel: (FuelType) -> Unit,
    onSetSurcharge: (Boolean) -> Unit,
    onSelectDeparture: (DepartureWindow) -> Unit,
    onSelectOrigin: (OriginMode) -> Unit,
    onSavePlan: () -> Unit,
    onOpenRecorder: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    onSelectFuel = onSelectFuel,
                    onSetSurcharge = onSetSurcharge,
                    onSelectDeparture = onSelectDeparture,
                    onSelectOrigin = onSelectOrigin,
                    onSavePlan = onSavePlan,
                    onOpenRecorder = { onOpenRecorder(state.destination.id) },
                )
            }
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
    onSelectFuel: (FuelType) -> Unit,
    onSetSurcharge: (Boolean) -> Unit,
    onSelectDeparture: (DepartureWindow) -> Unit,
    onSelectOrigin: (OriginMode) -> Unit,
    onSavePlan: () -> Unit,
    onOpenRecorder: () -> Unit,
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

        item(key = "origin") {
            OriginSelector(
                content = content,
                onSelectOrigin = onSelectOrigin,
            )
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

        item(key = "recorder") {
            RecorderEntryCard(onOpenRecorder = onOpenRecorder)
        }

        item(key = "details_header") {
            SectionHeader(
                title = stringResource(R.string.route_logistics_title),
                eyebrow = stringResource(R.string.route_logistics_eyebrow),
            )
        }

        item(key = "details") {
            RouteDetailsCard(content = content)
        }

        item(key = "engine_header") {
            SectionHeader(
                title = stringResource(R.string.route_engine_title),
                eyebrow = stringResource(R.string.route_engine_eyebrow),
                subtitle = stringResource(R.string.route_engine_subtitle),
            )
        }

        item(key = "engine") {
            LogisticsControls(
                content = content,
                onSelectFuel = onSelectFuel,
                onSetSurcharge = onSetSurcharge,
                onSelectDeparture = onSelectDeparture,
            )
        }

        item(key = "fuel") {
            FuelCard(plan = content.plan)
        }

        item(key = "tolls_header") {
            SectionHeader(
                title = stringResource(R.string.route_tolls_title),
                eyebrow = stringResource(R.string.route_tolls_eyebrow),
            )
        }

        item(key = "tolls") {
            TollSection(
                plan = content.plan,
                showCashAlert = content.cashAlertsEnabled,
            )
        }

        item(key = "weather_header") {
            SectionHeader(
                title = stringResource(R.string.route_weather_title),
                eyebrow = stringResource(R.string.route_weather_eyebrow),
            )
        }

        item(key = "weather") {
            WeatherCard(weather = content.plan.weather)
        }

        item(key = "food_header") {
            SectionHeader(
                title = stringResource(R.string.route_food_title),
                eyebrow = stringResource(R.string.route_food_eyebrow),
            )
        }

        item(key = "food") {
            FoodStopsSection(suggestions = content.plan.foodStops)
        }

        item(key = "saved_header") {
            SectionHeader(
                title = stringResource(R.string.route_saved_title),
                eyebrow = stringResource(R.string.route_saved_eyebrow),
            )
        }

        item(key = "saved") {
            SavedPlansSection(
                savedPlans = content.savedPlans,
                onSavePlan = onSavePlan,
            )
        }

        item(key = "change") {
            Spacer(modifier = Modifier.height(Dimens.tightGap))
            TextButton(onClick = onExplore) {
                Text(
                    text = stringResource(R.string.route_change_destination),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
