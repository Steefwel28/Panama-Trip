package com.panamatrip.app.domain.ranking

/**
 * Los reportes de acceso caducan exactamente a las 48 horas. La UI recalcula
 * con un ticker, así que un reporte desaparece sin reiniciar la app.
 */
object VehicleReportFreshness {

    const val TTL_MILLIS = 48L * 60L * 60L * 1000L

    fun isFresh(createdAtEpochMillis: Long, nowEpochMillis: Long): Boolean {
        if (createdAtEpochMillis <= 0L) return false
        val age = nowEpochMillis - createdAtEpochMillis
        return age in 0..TTL_MILLIS
    }

    /** Momento exacto en que el reporte deja de mostrarse. */
    fun expiresAt(createdAtEpochMillis: Long): Long = createdAtEpochMillis + TTL_MILLIS
}
