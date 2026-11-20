package com.panamatrip.app.domain.logistics

import com.panamatrip.app.domain.model.Ecosystem

/** Condición predominante esperada durante la expedición. */
enum class WeatherCondition {
    CLEAR,
    PARTLY_CLOUDY,
    CLOUDY,
    MISTY,
    RAINY,
}

/** Recomendaciones accionables derivadas del clima estimado. */
enum class WeatherAdvice {
    SUNSCREEN,
    RAIN_GEAR,
    LAYERS,
    EARLY_START,
    INSECT_REPELLENT,
    WATER,
}

/**
 * Clima de expedición estimado por ecosistema y temporada. No es un pronóstico
 * en vivo: es una referencia offline que permite empacar antes de salir y que
 * funciona incluso cuando la app se consulta sin señal.
 */
data class WeatherOutlook(
    val condition: WeatherCondition,
    val minTemperatureC: Int,
    val maxTemperatureC: Int,
    val rainChancePercent: Int,
    val advice: List<WeatherAdvice>,
)

object WeatherAdvisor {

    private data class ClimateProfile(
        val dryMinC: Int,
        val dryMaxC: Int,
        val rainyMinC: Int,
        val rainyMaxC: Int,
        val dryRainChance: Int,
        val rainyRainChance: Int,
    )

    private val profiles = mapOf(
        Ecosystem.MOUNTAIN to ClimateProfile(17, 25, 16, 24, 20, 55),
        Ecosystem.CLOUD_FOREST to ClimateProfile(15, 22, 14, 21, 35, 75),
        Ecosystem.WATERFALL to ClimateProfile(21, 29, 20, 28, 25, 65),
        Ecosystem.LAGOON to ClimateProfile(24, 32, 23, 31, 15, 50),
        Ecosystem.PINE_FOREST to ClimateProfile(16, 25, 15, 24, 20, 55),
    )

    fun outlookFor(ecosystem: Ecosystem, month: Int): WeatherOutlook {
        val safeMonth = month.coerceIn(JANUARY, DECEMBER)
        val profile = profiles.getValue(ecosystem)
        val drySeason = safeMonth in DRY_SEASON_MONTHS

        val minTemp = if (drySeason) profile.dryMinC else profile.rainyMinC
        val maxTemp = if (drySeason) profile.dryMaxC else profile.rainyMaxC
        val rainChance = if (drySeason) profile.dryRainChance else profile.rainyRainChance

        return WeatherOutlook(
            condition = conditionFor(ecosystem, rainChance),
            minTemperatureC = minTemp,
            maxTemperatureC = maxTemp,
            rainChancePercent = rainChance,
            advice = adviceFor(ecosystem, rainChance, maxTemp),
        )
    }

    private fun conditionFor(ecosystem: Ecosystem, rainChance: Int): WeatherCondition = when {
        ecosystem == Ecosystem.CLOUD_FOREST && rainChance >= MIST_THRESHOLD -> WeatherCondition.MISTY
        rainChance >= RAIN_THRESHOLD -> WeatherCondition.RAINY
        rainChance >= CLOUD_THRESHOLD -> WeatherCondition.CLOUDY
        rainChance >= PARTIAL_THRESHOLD -> WeatherCondition.PARTLY_CLOUDY
        else -> WeatherCondition.CLEAR
    }

    private fun adviceFor(
        ecosystem: Ecosystem,
        rainChance: Int,
        maxTemp: Int,
    ): List<WeatherAdvice> = buildList {
        if (rainChance >= RAIN_GEAR_THRESHOLD) add(WeatherAdvice.RAIN_GEAR)
        if (maxTemp >= SUN_THRESHOLD) add(WeatherAdvice.SUNSCREEN)
        if (rainChance >= EARLY_START_THRESHOLD) add(WeatherAdvice.EARLY_START)
        if (ecosystem in LAYER_ECOSYSTEMS) add(WeatherAdvice.LAYERS)
        if (ecosystem == Ecosystem.LAGOON) add(WeatherAdvice.INSECT_REPELLENT)
        add(WeatherAdvice.WATER)
    }.distinct().take(MAX_ADVICE)

    private val DRY_SEASON_MONTHS = 1..4
    private const val JANUARY = 1
    private const val DECEMBER = 12
    private const val PARTIAL_THRESHOLD = 25
    private const val CLOUD_THRESHOLD = 40
    private const val RAIN_THRESHOLD = 60
    private const val MIST_THRESHOLD = 55
    private const val RAIN_GEAR_THRESHOLD = 45
    private const val EARLY_START_THRESHOLD = 55
    private const val SUN_THRESHOLD = 30
    private const val MAX_ADVICE = 3
    private val LAYER_ECOSYSTEMS = setOf(
        Ecosystem.MOUNTAIN,
        Ecosystem.CLOUD_FOREST,
        Ecosystem.PINE_FOREST,
    )
}
