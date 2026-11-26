package com.panamatrip.app.domain.profile

/**
 * Comparador panameño del odómetro. Los rangos son excluyentes y ordenados:
 * cada kilómetro cae en una sola banda, sin solapes ni huecos.
 */
enum class OdometerTier(val minKilometers: Double) {
    PRIMER_PASO(0.0),
    TROCHA_DE_BARRIO(10.0),
    SENDERO_DE_SEMANA(50.0),
    GUIA_DE_TROCHA(150.0),
    VETERANO_DEL_ISTMO(500.0),
    A_PURO_PULMON(1_000.0),
}

object OdometerComparator {

    fun tierFor(kilometers: Double): OdometerTier {
        val safe = kilometers.coerceAtLeast(0.0)
        return OdometerTier.entries.last { safe >= it.minKilometers }
    }

    /** Kilómetros que faltan para la siguiente banda; 0 en la cima. */
    fun metersToNextTier(kilometers: Double): Int {
        val current = tierFor(kilometers)
        val next = OdometerTier.entries.getOrNull(current.ordinal + 1) ?: return 0
        return ((next.minKilometers - kilometers) * 1_000.0).toInt().coerceAtLeast(0)
    }
}
