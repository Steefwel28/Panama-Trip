package com.panamatrip.app.core.designsystem.labels

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.panamatrip.app.R
import com.panamatrip.app.core.format.formatElapsedDuration
import com.panamatrip.app.domain.logistics.FoodStopKind
import com.panamatrip.app.domain.logistics.FuelType
import com.panamatrip.app.domain.logistics.PriceRange
import com.panamatrip.app.domain.logistics.RouteLeg
import com.panamatrip.app.domain.logistics.TollPayment
import com.panamatrip.app.domain.logistics.WeatherAdvice
import com.panamatrip.app.domain.logistics.WeatherCondition
import com.panamatrip.app.domain.model.AccessStatus
import com.panamatrip.app.domain.model.Ecosystem
import com.panamatrip.app.domain.model.GuideLanguage
import com.panamatrip.app.domain.model.TerrainStatus
import com.panamatrip.app.domain.model.VehicleType
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
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
fun distanceFromYouLabel(meters: Int): String = if (meters < METERS_PER_KILOMETER) {
    stringResource(R.string.distance_from_you_meters, meters)
} else {
    stringResource(
        R.string.distance_from_you_kilometers,
        (meters + METERS_PER_KILOMETER / 2) / METERS_PER_KILOMETER,
    )
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

@Composable
fun FuelType.shortLabel(): String = stringResource(
    when (this) {
        FuelType.GASOLINE_91 -> R.string.fuel_91_short
        FuelType.GASOLINE_95 -> R.string.fuel_95_short
        FuelType.DIESEL -> R.string.fuel_diesel_short
    },
)

@Composable
fun FuelType.displayName(): String = stringResource(
    when (this) {
        FuelType.GASOLINE_91 -> R.string.fuel_91
        FuelType.GASOLINE_95 -> R.string.fuel_95
        FuelType.DIESEL -> R.string.fuel_diesel
    },
)

@Composable
fun TollPayment.displayName(): String = stringResource(
    when (this) {
        TollPayment.PANAPASS -> R.string.toll_panapass
        TollPayment.CASH_ONLY -> R.string.toll_cash_only
    },
)

@Composable
fun WeatherCondition.displayName(): String = stringResource(
    when (this) {
        WeatherCondition.CLEAR -> R.string.weather_clear
        WeatherCondition.PARTLY_CLOUDY -> R.string.weather_partly_cloudy
        WeatherCondition.CLOUDY -> R.string.weather_cloudy
        WeatherCondition.MISTY -> R.string.weather_misty
        WeatherCondition.RAINY -> R.string.weather_rainy
    },
)

@Composable
fun WeatherAdvice.displayName(): String = stringResource(
    when (this) {
        WeatherAdvice.SUNSCREEN -> R.string.advice_sunscreen
        WeatherAdvice.RAIN_GEAR -> R.string.advice_rain_gear
        WeatherAdvice.LAYERS -> R.string.advice_layers
        WeatherAdvice.EARLY_START -> R.string.advice_early_start
        WeatherAdvice.INSECT_REPELLENT -> R.string.advice_insect_repellent
        WeatherAdvice.WATER -> R.string.advice_water
    },
)

@Composable
fun FoodStopKind.displayName(): String = stringResource(
    when (this) {
        FoodStopKind.BREAKFAST -> R.string.food_breakfast
        FoodStopKind.COFFEE -> R.string.food_coffee
        FoodStopKind.LUNCH -> R.string.food_lunch
        FoodStopKind.SNACK -> R.string.food_snack
        FoodStopKind.DINNER -> R.string.food_dinner
    },
)

@Composable
fun PriceRange.displayName(): String = stringResource(
    when (this) {
        PriceRange.ECONOMIC -> R.string.price_economic
        PriceRange.MODERATE -> R.string.price_moderate
    },
)

@Composable
fun RouteLeg.displayName(): String = stringResource(
    when (this) {
        RouteLeg.OUTBOUND -> R.string.leg_outbound
        RouteLeg.RETURN -> R.string.leg_return
    },
)

@Composable
fun GuideLanguage.displayName(): String = stringResource(
    when (this) {
        GuideLanguage.SPANISH -> R.string.language_spanish
        GuideLanguage.ENGLISH -> R.string.language_english
        GuideLanguage.FRENCH -> R.string.language_french
        GuideLanguage.GERMAN -> R.string.language_german
    },
)

fun usdLabel(cents: Int): String = String.format(Locale.US, "\$%.2f", cents / CENTS_PER_DOLLAR)

@Composable
fun gallonsPriceLabel(cents: Int): String =
    stringResource(R.string.price_per_gallon, cents / CENTS_PER_DOLLAR.toFloat())

@Composable
fun litersLabel(liters: Double): String =
    stringResource(R.string.liters_value, liters.toFloat())

@Composable
fun kilometersLabel(kilometers: Double): String =
    stringResource(R.string.kilometers_value, kilometers.toFloat())

/** Cronómetro activo del grabador, sin pausas. */
fun durationLabel(millis: Long): String = formatElapsedDuration(millis)

@Composable
fun speedLabel(kilometersPerHour: Double): String =
    stringResource(R.string.speed_kmh, kilometersPerHour)

fun trackTimestampLabel(epochMillis: Long): String =
    TimestampFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

fun dateLabel(epochMillis: Long): String =
    DateFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

private object TimestampFormatter {
    private val formatter = DateTimeFormatter.ofPattern("dd MMM · HH:mm", Locale.forLanguageTag("es-PA"))
    fun format(value: ZonedDateTime): String = formatter.format(value)
}

private object DateFormatter {
    private val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("es-PA"))
    fun format(value: ZonedDateTime): String = formatter.format(value)
}

fun clockLabel(minuteOfDay: Int): String {
    val normalized = ((minuteOfDay % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY
    return String.format(
        Locale.US,
        "%02d:%02d",
        normalized / MINUTES_PER_HOUR,
        normalized % MINUTES_PER_HOUR,
    )
}

fun planTimestampLabel(epochMillis: Long): String =
    TimestampFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

private const val METERS_PER_KILOMETER = 1000
private const val CENTS_PER_DOLLAR = 100.0
private const val MINUTES_PER_DAY = 24 * 60
private const val MINUTES_PER_HOUR = 60
