package com.panamatrip.app.features.routeplan.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.panamatrip.app.R
import com.panamatrip.app.core.designsystem.component.InfoNotice
import com.panamatrip.app.core.designsystem.component.OverlayTag
import com.panamatrip.app.core.designsystem.component.PrimaryActionButton
import com.panamatrip.app.core.designsystem.component.TonalTag
import com.panamatrip.app.core.designsystem.component.Tone
import com.panamatrip.app.core.designsystem.component.tonalGradient
import com.panamatrip.app.core.designsystem.haptic.rememberPanamaHaptics
import com.panamatrip.app.core.designsystem.icons.PanamaIcons
import com.panamatrip.app.core.designsystem.labels.clockLabel
import com.panamatrip.app.core.designsystem.labels.distanceLabel
import com.panamatrip.app.core.designsystem.labels.displayName
import com.panamatrip.app.core.designsystem.labels.driveLabel
import com.panamatrip.app.core.designsystem.labels.elevationLabel
import com.panamatrip.app.core.designsystem.labels.gallonsPriceLabel
import com.panamatrip.app.core.designsystem.labels.kilometersLabel
import com.panamatrip.app.core.designsystem.labels.litersLabel
import com.panamatrip.app.core.designsystem.labels.planTimestampLabel
import com.panamatrip.app.core.designsystem.labels.shortLabel
import com.panamatrip.app.core.designsystem.labels.usdLabel
import com.panamatrip.app.core.designsystem.labels.walkLabel
import com.panamatrip.app.core.designsystem.theme.Dimens
import com.panamatrip.app.core.designsystem.theme.MetricType
import com.panamatrip.app.domain.logistics.FoodStopSuggestion
import com.panamatrip.app.domain.logistics.FuelCalculator
import com.panamatrip.app.domain.logistics.FuelType
import com.panamatrip.app.domain.logistics.LogisticsPlan
import com.panamatrip.app.domain.logistics.WeatherOutlook
import com.panamatrip.app.domain.model.Destination
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
internal fun ParkingMapPanel(destination: Destination) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(MAP_PANEL_HEIGHT)
            .clip(RoundedCornerShape(Dimens.cardCorner))
            .background(tonalGradient("parking-${destination.id}")),
    ) {
        TopographicContours(
            color = MaterialTheme.colorScheme.primary.copy(alpha = CONTOUR_ALPHA),
            modifier = Modifier.matchParentSize(),
        )
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
                style = MetricType.Small,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
internal fun OriginSelector(
    content: RoutePlanUiState.Content,
    onSelectOrigin: (OriginMode) -> Unit,
) {
    val haptics = rememberPanamaHaptics()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(Dimens.cardCorner),
        tonalElevation = Dimens.tonalElevation,
    ) {
        Column(
            modifier = Modifier.padding(Dimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.itemGap),
        ) {
            Text(
                text = stringResource(R.string.route_origin_selector_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
            val options = OriginMode.entries
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = content.originMode == mode,
                        onClick = {
                            haptics.segment()
                            onSelectOrigin(mode)
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        label = {
                            Text(
                                text = stringResource(
                                    when (mode) {
                                        OriginMode.CURRENT_LOCATION ->
                                            R.string.route_origin_current
                                        OriginMode.CUSTOM -> R.string.route_origin_custom
                                    },
                                ),
                            )
                        },
                    )
                }
            }
            Text(
                text = if (content.isLocatingOrigin) {
                    stringResource(R.string.route_origin_locating)
                } else {
                    stringResource(
                        if (content.originMode == OriginMode.CURRENT_LOCATION) {
                            R.string.route_origin_current_value
                        } else {
                            R.string.route_origin_custom_value
                        },
                        content.origin.formatted,
                    )
                },
                style = MetricType.Small,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun RouteDetailsCard(content: RoutePlanUiState.Content) {
    val destination = content.destination
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(Dimens.cardCorner),
        tonalElevation = Dimens.tonalElevation,
    ) {
        Column(
            modifier = Modifier.padding(Dimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.itemGap),
        ) {
            InfoRow(
                label = stringResource(R.string.route_origin_label),
                value = stringResource(
                    R.string.route_origin_value,
                    distanceLabel(content.plan.oneWayDistanceMeters),
                    driveLabel(content.plan.driveMinutes),
                ),
            )
            InfoRow(
                label = stringResource(R.string.route_walk_label),
                value = walkLabel(destination.walkDistanceMeters, destination.walkMinutes),
            )
            InfoRow(
                label = stringResource(R.string.route_elevation_label),
                value = elevationLabel(destination.elevationGainMeters),
            )
            InfoRow(
                label = stringResource(R.string.route_attraction_coords_label),
                value = destination.attraction.formatted,
            )
        }
    }
}

@Composable
internal fun LogisticsControls(
    content: RoutePlanUiState.Content,
    onSelectFuel: (FuelType) -> Unit,
    onSetSurcharge: (Boolean) -> Unit,
    onSelectDeparture: (DepartureWindow) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(Dimens.cardCorner),
        tonalElevation = Dimens.tonalElevation,
    ) {
        val haptics = rememberPanamaHaptics()
        Column(
            modifier = Modifier.padding(Dimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.cardPadding),
        ) {
            VehicleBlock(content = content)
            FuelBlock(
                fuelType = content.fuelType,
                onSelectFuel = { fuel ->
                    haptics.segment()
                    onSelectFuel(fuel)
                },
            )
            SurchargeBlock(
                enabled = content.surchargeEnabled,
                onSetSurcharge = { enabled ->
                    haptics.toggle(enabled)
                    onSetSurcharge(enabled)
                },
            )
            DepartureBlock(
                selected = content.departureWindow,
                onSelectDeparture = { window ->
                    haptics.segment()
                    onSelectDeparture(window)
                },
            )
        }
    }
}

@Composable
private fun VehicleBlock(content: RoutePlanUiState.Content) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.route_vehicle_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = content.vehicle.displayName(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        TonalTag(
            text = stringResource(
                R.string.route_vehicle_value,
                FuelCalculator.consumptionLitersPer100Km(content.vehicle).toFloat(),
            ),
            tone = Tone.Neutral,
            style = MetricType.Small,
        )
    }
}

@Composable
private fun FuelBlock(
    fuelType: FuelType,
    onSelectFuel: (FuelType) -> Unit,
) {
    val options = FuelType.entries
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.tightGap)) {
        Text(
            text = stringResource(R.string.route_fuel_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, fuel ->
                SegmentedButton(
                    selected = fuelType == fuel,
                    onClick = { onSelectFuel(fuel) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    label = {
                        Text(
                            text = fuel.shortLabel(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun SurchargeBlock(
    enabled: Boolean,
    onSetSurcharge: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(Dimens.innerCorner),
    ) {
        Row(
            modifier = Modifier.padding(Dimens.itemGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.route_surcharge_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.route_surcharge_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(Dimens.tightGap))
            Switch(checked = enabled, onCheckedChange = onSetSurcharge)
        }
    }
}

@Composable
private fun DepartureBlock(
    selected: DepartureWindow,
    onSelectDeparture: (DepartureWindow) -> Unit,
) {
    val options = DepartureWindow.entries
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.tightGap)) {
        Text(
            text = stringResource(R.string.route_departure_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, window ->
                SegmentedButton(
                    selected = selected == window,
                    onClick = { onSelectDeparture(window) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    label = {
                        Text(text = window.label(), maxLines = 1)
                    },
                )
            }
        }
    }
}

@Composable
private fun DepartureWindow.label(): String = stringResource(
    when (this) {
        DepartureWindow.DAWN -> R.string.route_departure_dawn
        DepartureWindow.MORNING -> R.string.route_departure_morning
        DepartureWindow.NOON -> R.string.route_departure_noon
    },
)

@Composable
internal fun FuelCard(plan: LogisticsPlan) {
    val fuel = plan.fuel
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(Dimens.cardCorner),
        tonalElevation = Dimens.tonalElevation,
    ) {
        Column(
            modifier = Modifier.padding(Dimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.itemGap),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = PanamaIcons.Fuel,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(Dimens.tightGap))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.route_fuel_card_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = fuel.fuelType.displayName(),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (fuel.surchargeApplied) {
                    TonalTag(
                        text = stringResource(R.string.route_surcharge_tag),
                        tone = Tone.Tertiary,
                    )
                }
            }
            Text(
                text = usdLabel(fuel.costCents),
                style = MetricType.Large,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.itemGap)) {
                FuelStat(
                    label = stringResource(R.string.route_fuel_round_trip),
                    value = kilometersLabel(fuel.roundTripKilometers),
                    numeric = true,
                    modifier = Modifier.weight(1f),
                )
                FuelStat(
                    label = stringResource(R.string.route_fuel_liters),
                    value = litersLabel(fuel.effectiveLiters),
                    numeric = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.itemGap)) {
                FuelStat(
                    label = stringResource(R.string.route_fuel_price),
                    value = gallonsPriceLabel(fuel.pricePerGallonCents),
                    numeric = true,
                    modifier = Modifier.weight(1f),
                )
                FuelStat(
                    label = stringResource(R.string.route_fuel_vehicle),
                    value = fuel.vehicle.displayName(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun FuelStat(
    label: String,
    value: String,
    numeric: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = LABEL_ALPHA),
        )
        Text(
            text = value,
            style = if (numeric) MetricType.Medium else MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
internal fun TollSection(plan: LogisticsPlan, showCashAlert: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.itemGap)) {
        if (plan.tollSegments.isEmpty()) {
            InfoNotice(
                text = stringResource(R.string.route_tolls_none),
                icon = PanamaIcons.Pin,
            )
            return@Column
        }

        if (showCashAlert && plan.cashOnlyRoundTripCents > 0) {
            CashOnlyAlert(plan = plan)
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(Dimens.cardCorner),
            tonalElevation = Dimens.tonalElevation,
        ) {
            Column(
                modifier = Modifier.padding(Dimens.cardPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.itemGap),
            ) {
                plan.tollSegments.forEach { segment ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = segment.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(
                                    R.string.route_tolls_each_way_value,
                                    stringResource(R.string.route_tolls_each_way),
                                    usdLabel(segment.oneWayCents),
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TonalTag(
                            text = segment.payment.displayName(),
                            tone = if (segment.isCashOnly) Tone.Tertiary else Tone.Secondary,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.route_tolls_total),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = usdLabel(plan.tollRoundTripCents),
                        style = MetricType.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun CashOnlyAlert(plan: LogisticsPlan) {
    val separator = stringResource(R.string.route_cash_alert_names_separator)
    val names = plan.cashOnlyTollSegments.joinToString(separator = separator) { it.name }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(Dimens.cardCorner),
        tonalElevation = Dimens.tonalElevation,
    ) {
        Column(
            modifier = Modifier.padding(Dimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.tightGap),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = PanamaIcons.Pin,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(Dimens.tightGap))
                Text(
                    text = stringResource(R.string.route_cash_alert_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                )
                AlertTag(text = stringResource(R.string.route_cash_alert_tag))
            }
            Text(
                text = stringResource(
                    R.string.route_cash_alert_body,
                    names,
                    usdLabel(plan.cashOnlyRoundTripCents),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun AlertTag(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = ALERT_TAG_ALPHA),
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(percent = 50),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

@Composable
internal fun WeatherCard(weather: WeatherOutlook) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(Dimens.cardCorner),
        tonalElevation = Dimens.tonalElevation,
    ) {
        Column(
            modifier = Modifier.padding(Dimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.itemGap),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(Dimens.innerCorner),
                ) {
                    Icon(
                        imageVector = PanamaIcons.Sun,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(Dimens.itemGap)
                            .size(26.dp),
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                }
                Spacer(modifier = Modifier.width(Dimens.itemGap))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = weather.condition.displayName(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(
                            R.string.route_weather_range,
                            weather.minTemperatureC,
                            weather.maxTemperatureC,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(
                            R.string.route_weather_rain,
                            weather.rainChancePercent,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.tightGap)) {
                weather.advice.forEach { advice ->
                    TonalTag(text = advice.displayName(), tone = Tone.Neutral)
                }
            }
            Text(
                text = stringResource(R.string.route_weather_note),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun FoodStopsSection(suggestions: List<FoodStopSuggestion>) {
    if (suggestions.isEmpty()) {
        InfoNotice(
            text = stringResource(R.string.route_food_empty),
            icon = PanamaIcons.Fork,
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.itemGap)) {
        suggestions.forEach { suggestion ->
            FoodStopCard(suggestion = suggestion)
        }
    }
}

@Composable
private fun FoodStopCard(suggestion: FoodStopSuggestion) {
    val stop = suggestion.stop
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(Dimens.cardCorner),
        tonalElevation = Dimens.tonalElevation,
    ) {
        Row(
            modifier = Modifier.padding(Dimens.itemGap),
            horizontalArrangement = Arrangement.spacedBy(Dimens.itemGap),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(Dimens.innerCorner),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = Dimens.itemGap, vertical = Dimens.tightGap),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = clockLabel(suggestion.clockMinuteOfDay),
                        style = MetricType.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = suggestion.leg.displayName(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stop.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stop.town,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        R.string.route_food_km,
                        suggestion.absoluteKilometer.roundToInt(),
                    ),
                    style = MetricType.Small,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stop.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.tightGap)) {
                    stop.kinds.forEach { kind ->
                        TonalTag(text = kind.displayName(), tone = Tone.Primary)
                    }
                    TonalTag(
                        text = stringResource(
                            R.string.route_food_price,
                            stop.priceRange.displayName(),
                        ),
                        tone = Tone.Neutral,
                    )
                }
            }
        }
    }
}

@Composable
internal fun SavedPlansSection(
    savedPlans: List<SavedPlanUi>,
    onSavePlan: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.itemGap)) {
        PrimaryActionButton(
            text = stringResource(R.string.route_saved_save),
            onClick = onSavePlan,
            icon = PanamaIcons.Bookmark,
        )
        if (savedPlans.isEmpty()) {
            InfoNotice(
                text = stringResource(R.string.route_saved_empty),
                icon = PanamaIcons.BookmarkOutline,
            )
            return@Column
        }
        savedPlans.forEach { plan ->
            SavedPlanRow(plan = plan)
        }
    }
}

@Composable
private fun SavedPlanRow(plan: SavedPlanUi) {
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
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = planTimestampLabel(plan.createdAtEpochMillis),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        R.string.route_saved_meta,
                        plan.fuelType.displayName(),
                        clockLabel(plan.departureMinuteOfDay),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (plan.mountainSurcharge) {
                    Text(
                        text = stringResource(R.string.route_surcharge_tag),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.route_saved_total, usdLabel(plan.totalCents)),
                    style = MetricType.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (plan.cashOnlyCents > 0) {
                    Text(
                        text = stringResource(
                            R.string.route_saved_cash,
                            usdLabel(plan.cashOnlyCents),
                        ),
                        style = MetricType.Small,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }
    }
}

@Composable
internal fun RecorderEntryCard(onOpenRecorder: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(Dimens.cardCorner),
        tonalElevation = Dimens.raisedElevation,
    ) {
        Column(
            modifier = Modifier.padding(Dimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.tightGap),
        ) {
            Text(
                text = stringResource(R.string.route_recorder_eyebrow),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                text = stringResource(R.string.route_recorder_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.route_recorder_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            PrimaryActionButton(
                text = stringResource(R.string.route_recorder_action),
                onClick = onOpenRecorder,
                icon = PanamaIcons.Play,
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MetricType.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Curvas de nivel dibujadas con Canvas: dan contexto de mapa topográfico
 * mientras el Bloque 4 integra el mapa real, sin añadir dependencias ni assets.
 */
@Composable
private fun TopographicContours(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width * CONTOUR_CENTER_X, size.height * CONTOUR_CENTER_Y)
        val shortestSide = minOf(size.width, size.height)
        val stroke = Stroke(width = CONTOUR_STROKE.toPx(), cap = StrokeCap.Round)
        repeat(CONTOUR_COUNT) { index ->
            val radius = shortestSide * (CONTOUR_BASE_RADIUS + index * CONTOUR_STEP)
            if (radius <= 0f) return@repeat
            val path = Path()
            for (step in 0..CONTOUR_SEGMENTS) {
                val angle = step.toFloat() / CONTOUR_SEGMENTS * TWO_PI
                val wobble = 1f +
                    CONTOUR_WOBBLE * sin(angle * 3f + index * 0.8f) +
                    CONTOUR_WOBBLE_DETAIL * cos(angle * 5f - index * 0.6f)
                val x = center.x + cos(angle) * radius * CONTOUR_ASPECT * wobble
                val y = center.y + sin(angle) * radius * wobble
                if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path = path, color = color, style = stroke)
        }
    }
}

private val MAP_PANEL_HEIGHT = 220.dp

private const val LABEL_ALPHA = 0.72f
private const val ALERT_TAG_ALPHA = 0.14f

private const val CONTOUR_ALPHA = 0.16f
private const val CONTOUR_CENTER_X = 0.5f
private const val CONTOUR_CENTER_Y = 0.54f
private const val CONTOUR_BASE_RADIUS = 0.12f
private const val CONTOUR_STEP = 0.09f
private const val CONTOUR_ASPECT = 1.5f
private const val CONTOUR_WOBBLE = 0.12f
private const val CONTOUR_WOBBLE_DETAIL = 0.05f
private const val CONTOUR_SEGMENTS = 96
private const val CONTOUR_COUNT = 7
private val CONTOUR_STROKE = 1.dp
private val TWO_PI = (2 * PI).toFloat()
