package com.panamatrip.app.domain.logistics

import com.panamatrip.app.domain.model.GeoPoint

/**
 * Corredores viales reales que usa el planificador para calibrar tiempos.
 * La velocidad media ya incluye casetas, tráfico urbano y tramos de montaña.
 */
enum class PanamanianCorridor(val averageSpeedKmh: Double) {
    METRO(42.0),
    INTERAMERICANA(72.0),
    COLON_HIGHWAY(88.0),
    EAST_HILLS(52.0),
}

/** Selecciona el corredor a partir de la provincia y la coordenada de acceso. */
object CorridorCatalog {

    /**
     * Cerro Azul y los accesos al este de la capital quedan al este de este
     * meridiano: no cruzan casetas y bajan más lento por la carretera de cerro.
     */
    const val EAST_LONGITUDE_LIMIT = -79.5

    fun of(province: String, parking: GeoPoint): PanamanianCorridor = when {
        province == "Colón" -> PanamanianCorridor.COLON_HIGHWAY
        province == "Panamá" && parking.longitude > EAST_LONGITUDE_LIMIT ->
            PanamanianCorridor.EAST_HILLS
        province == "Panamá" || province == "Panamá Oeste" -> PanamanianCorridor.METRO
        else -> PanamanianCorridor.INTERAMERICANA
    }
}
