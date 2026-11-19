package com.panamatrip.app.domain.model

/** Coordenada geográfica inmutable, independiente de cualquier SDK de mapas. */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
) {
    val formatted: String
        get() = "%.5f, %.5f".format(LocaleHolder.LOCALE, latitude, longitude)

    private object LocaleHolder {
        val LOCALE: java.util.Locale = java.util.Locale.US
    }
}
