package com.panamatrip.app.core.designsystem.labels

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.panamatrip.app.R
import com.panamatrip.app.domain.model.AccessStatus
import com.panamatrip.app.domain.model.Ecosystem
import com.panamatrip.app.domain.model.TerrainStatus
import com.panamatrip.app.domain.model.VehicleType
import java.util.Locale

@Composable
fun VehicleType.displayName(): String = stringResource(
    when (this) {
        VehicleType.SEDAN -> R.string.vehicle_sedan
        VehicleType.SUV -> R.string.vehicle_suv
        VehicleType.FOUR_BY_FOUR -> R.string.vehicle_4x4
    },
)

@Composable
fun Ecosystem.displayName(): String = stringResource(
    when (this) {
        Ecosystem.MOUNTAIN -> R.string.ecosystem_mountain
        Ecosystem.CLOUD_FOREST -> R.string.ecosystem_cloud_forest
        Ecosystem.WATERFALL -> R.string.ecosystem_waterfall
        Ecosystem.LAGOON -> R.string.ecosystem_lagoon
        Ecosystem.PINE_FOREST -> R.string.ecosystem_pine_forest
    },
)

@Composable
fun AccessStatus.displayName(): String = stringResource(
    when (this) {
        AccessStatus.CLEAR -> R.string.access_clear
        AccessStatus.CAUTION -> R.string.access_caution
        AccessStatus.BLOCKED -> R.string.access_blocked
    },
)

@Composable
fun TerrainStatus.displayName(): String = stringResource(
    when (this) {
        TerrainStatus.ROAD_CLEAR -> R.string.terrain_road_clear
        TerrainStatus.HEAVY_MUD -> R.string.terrain_heavy_mud
        TerrainStatus.RIVER_HIGH -> R.string.terrain_river_high
        TerrainStatus.LOW_HANGING_BRANCHES -> R.string.terrain_low_branches
    },
)

/** Sello temporal editorial MM/AAAA. */
fun monthYearLabel(month: Int, year: Int): String =
    String.format(Locale.US, "%02d/%d", month, year)

@Composable
fun distanceLabel(meters: Int): String = if (meters < METERS_PER_KILOMETER) {
    stringResource(R.string.distance_meters, meters)
} else {
    stringResource(R.string.distance_kilometers, meters.toDouble() / METERS_PER_KILOMETER)
}

@Composable
fun walkLabel(meters: Int, minutes: Int): String =
    stringResource(R.string.walk_summary, distanceLabel(meters), minutes)

@Composable
fun driveLabel(minutes: Int): String = stringResource(R.string.drive_minutes, minutes)

@Composable
fun elevationLabel(meters: Int): String = stringResource(R.string.elevation_meters, meters)

@Composable
fun reportsLabel(count: Int): String = if (count == 0) {
    stringResource(R.string.vehicle_reports_empty)
} else {
    stringResource(R.string.vehicle_reports_count, count)
}

@Composable
fun ratingLabel(rating: Double, reviews: Int): String = if (reviews == 0) {
    stringResource(R.string.rating_empty)
} else {
    stringResource(R.string.rating_summary, decimalRating(rating), reviews)
}

/** Valoración compacta sobre la foto de cabecera. */
@Composable
fun ratingOverlayLabel(rating: Double, reviews: Int): String =
    stringResource(R.string.detail_rating_overlay, decimalRating(rating), reviews)

private fun decimalRating(rating: Double): String = String.format(Locale.US, "%.1f", rating)

@Composable
fun likesLabel(count: Int): String = stringResource(R.string.likes_count, count)

@Composable
fun commentsLabel(count: Int): String = stringResource(R.string.comments_count, count)

private const val METERS_PER_KILOMETER = 1000
