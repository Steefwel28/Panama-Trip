package com.panamatrip.app.domain.logistics

/** Momento del día al que corresponde una parada gastronómica. */
enum class FoodStopKind(
    val windowStartMinute: Int,
    val windowEndMinute: Int,
) {
    BREAKFAST(windowStartMinute = 6 * 60, windowEndMinute = 9 * 60 + 30),
    COFFEE(windowStartMinute = 9 * 60, windowEndMinute = 11 * 60),
    LUNCH(windowStartMinute = 11 * 60 + 30, windowEndMinute = 14 * 60 + 30),
    SNACK(windowStartMinute = 15 * 60, windowEndMinute = 17 * 60 + 30),
    DINNER(windowStartMinute = 18 * 60, windowEndMinute = 21 * 60),
}

/** Rango de precio referencial de la parada. */
enum class PriceRange {
    ECONOMIC,
    MODERATE,
}

/** Corredor vial al que sirve una parada. */
enum class FoodCorridor {
    WEST,
    NORTH,
    CITY,
}

enum class RouteLeg {
    OUTBOUND,
    RETURN,
}

/**
 * Parada gastronómica editorial. `progressPercent` indica qué porcentaje del
 * tramo vial se recorre antes de llegar a ella desde la Ciudad de Panamá.
 */
data class FoodStop(
    val id: String,
    val name: String,
    val town: String,
    val corridor: FoodCorridor,
    val kinds: Set<FoodStopKind>,
    val progressPercent: Int,
    val priceRange: PriceRange,
    val note: String,
)

data class FoodStopSuggestion(
    val stop: FoodStop,
    val leg: RouteLeg,
    val clockMinuteOfDay: Int,
    val elapsedMinutes: Int,
)

object FoodStopCatalog {

    val stops: List<FoodStop> = listOf(
        FoodStop(
            id = "fonda-dona-chela",
            name = "Fonda Doña Chela",
            town = "Capira, Panamá Oeste",
            corridor = FoodCorridor.WEST,
            kinds = setOf(FoodStopKind.BREAKFAST, FoodStopKind.COFFEE),
            progressPercent = 15,
            priceRange = PriceRange.ECONOMIC,
            note = "Hojaldres, huevos criollos y café de olla antes de tomar la Interamericana.",
        ),
        FoodStop(
            id = "parrillada-el-puerto",
            name = "Parrillada El Puerto",
            town = "Aguadulce, Coclé",
            corridor = FoodCorridor.WEST,
            kinds = setOf(FoodStopKind.LUNCH),
            progressPercent = 35,
            priceRange = PriceRange.MODERATE,
            note = "Pescado del día y patacones a la orilla de la vía; servicio rápido.",
        ),
        FoodStop(
            id = "fonda-el-higo",
            name = "Fonda El Higo",
            town = "Penonomé, Coclé",
            corridor = FoodCorridor.WEST,
            kinds = setOf(FoodStopKind.LUNCH, FoodStopKind.SNACK),
            progressPercent = 45,
            priceRange = PriceRange.ECONOMIC,
            note = "Comida casera de olla y jugos naturales; cierra temprano los domingos.",
        ),
        FoodStop(
            id = "cafe-de-altura",
            name = "Café de Altura",
            town = "Santiago, Veraguas",
            corridor = FoodCorridor.WEST,
            kinds = setOf(FoodStopKind.COFFEE, FoodStopKind.SNACK),
            progressPercent = 62,
            priceRange = PriceRange.MODERATE,
            note = "Café de finca y repostería para estirar las piernas a mitad de ruta.",
        ),
        FoodStop(
            id = "cocina-la-lena",
            name = "Cocina Típica La Leña",
            town = "David, Chiriquí",
            corridor = FoodCorridor.WEST,
            kinds = setOf(FoodStopKind.LUNCH, FoodStopKind.DINNER),
            progressPercent = 90,
            priceRange = PriceRange.MODERATE,
            note = "Sancocho y carnes a la leña; última parada fuerte antes de la montaña.",
        ),
        FoodStop(
            id = "fonda-portobolena",
            name = "Fonda Portobeleña",
            town = "Sabanitas, Colón",
            corridor = FoodCorridor.NORTH,
            kinds = setOf(FoodStopKind.BREAKFAST, FoodStopKind.LUNCH),
            progressPercent = 45,
            priceRange = PriceRange.ECONOMIC,
            note = "Desayuno costeño y sopa de pescado; punto clásico antes de Portobelo.",
        ),
        FoodStop(
            id = "cafe-rio-lagarto",
            name = "Café Río Lagarto",
            town = "Portobelo, Colón",
            corridor = FoodCorridor.NORTH,
            kinds = setOf(FoodStopKind.COFFEE, FoodStopKind.SNACK),
            progressPercent = 80,
            priceRange = PriceRange.MODERATE,
            note = "Café frío y fruta de temporada junto al sendero del río.",
        ),
        FoodStop(
            id = "restaurante-los-cayos",
            name = "Restaurante Los Cayos",
            town = "Portobelo, Colón",
            corridor = FoodCorridor.NORTH,
            kinds = setOf(FoodStopKind.LUNCH),
            progressPercent = 92,
            priceRange = PriceRange.MODERATE,
            note = "Pescado entero y cocadas frente a la bahía; ideal al volver del sendero.",
        ),
        FoodStop(
            id = "cafe-unido",
            name = "Café Unido",
            town = "Ciudad de Panamá",
            corridor = FoodCorridor.CITY,
            kinds = setOf(FoodStopKind.COFFEE, FoodStopKind.SNACK),
            progressPercent = 5,
            priceRange = PriceRange.MODERATE,
            note = "Café de especialidad y pan fresco para arrancar el día.",
        ),
        FoodStop(
            id = "mercado-de-mariscos",
            name = "Mercado de Mariscos",
            town = "Ciudad de Panamá",
            corridor = FoodCorridor.CITY,
            kinds = setOf(FoodStopKind.LUNCH),
            progressPercent = 8,
            priceRange = PriceRange.MODERATE,
            note = "Ceviche y corvina al mediodía, a pocos minutos del casco urbano.",
        ),
    )

    fun corridorFor(province: String): FoodCorridor = when (province) {
        "Colón" -> FoodCorridor.NORTH
        "Panamá" -> FoodCorridor.CITY
        else -> FoodCorridor.WEST
    }
}

/**
 * Selecciona las paradas cuyo horario estimado de paso coincide con el tipo de
 * comida. Se evalúan ida y vuelta, y se conserva la primera coincidencia de
 * cada parada para no repetir sugerencias.
 */
object FoodStopAdvisor {

    private const val MINUTES_PER_DAY = 24 * 60
    private const val MAX_SUGGESTIONS = 3

    fun suggest(
        departureMinuteOfDay: Int,
        driveMinutes: Int,
        walkMinutes: Int,
        corridor: FoodCorridor,
    ): List<FoodStopSuggestion> {
        val rawDeparture = departureMinuteOfDay % MINUTES_PER_DAY
        val departure = (rawDeparture + MINUTES_PER_DAY) % MINUTES_PER_DAY
        val safeDrive = driveMinutes.coerceAtLeast(0)
        val safeWalk = walkMinutes.coerceAtLeast(0)

        return FoodStopCatalog.stops
            .asSequence()
            .filter { it.corridor == corridor }
            .mapNotNull { stop ->
                candidates(stop, departure, safeDrive, safeWalk)
                    .filter { it.matchesWindow() }
                    .minByOrNull { it.elapsedMinutes }
            }
            .sortedBy { it.elapsedMinutes }
            .take(MAX_SUGGESTIONS)
            .toList()
    }

    private fun candidates(
        stop: FoodStop,
        departureMinute: Int,
        driveMinutes: Int,
        walkMinutes: Int,
    ): List<FoodStopSuggestion> {
        val outboundElapsed =
            (driveMinutes.toDouble() * stop.progressPercent / PERCENT_BASE).toInt()
        val returnElapsed = driveMinutes + walkMinutes +
            (driveMinutes.toDouble() * (PERCENT_BASE - stop.progressPercent) / PERCENT_BASE).toInt()

        return listOf(
            FoodStopSuggestion(
                stop = stop,
                leg = RouteLeg.OUTBOUND,
                clockMinuteOfDay = (departureMinute + outboundElapsed) % MINUTES_PER_DAY,
                elapsedMinutes = outboundElapsed,
            ),
            FoodStopSuggestion(
                stop = stop,
                leg = RouteLeg.RETURN,
                clockMinuteOfDay = (departureMinute + returnElapsed) % MINUTES_PER_DAY,
                elapsedMinutes = returnElapsed,
            ),
        )
    }

    private fun FoodStopSuggestion.matchesWindow(): Boolean =
        stop.kinds.any { kind ->
            clockMinuteOfDay in kind.windowStartMinute..kind.windowEndMinute
        }

    private const val PERCENT_BASE = 100.0
}
