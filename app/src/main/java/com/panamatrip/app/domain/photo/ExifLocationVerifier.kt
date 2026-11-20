package com.panamatrip.app.domain.photo

import com.panamatrip.app.domain.geo.GeoCalculator
import com.panamatrip.app.domain.model.GeoPoint

/** Metadatos mínimos extraídos de la foto original. */
data class ExifMetadata(
    val latitude: Double?,
    val longitude: Double?,
    val capturedAtEpochMillis: Long?,
) {
    val point: GeoPoint?
        get() {
            val lat = latitude ?: return null
            val lng = longitude ?: return null
            if (lat !in MIN_LATITUDE..MAX_LATITUDE) return null
            if (lng !in MIN_LONGITUDE..MAX_LONGITUDE) return null
            return GeoPoint(lat, lng)
        }

    private companion object {
        const val MIN_LATITUDE = -90.0
        const val MAX_LATITUDE = 90.0
        const val MIN_LONGITUDE = -180.0
        const val MAX_LONGITUDE = 180.0
    }
}

enum class ExifVerification {
    VERIFIED_ON_SITE,
    OUTSIDE_RADIUS,
    NO_GPS,
}

/** Resultado de comparar la foto contra el atractivo elegido. */
data class ExifCheck(
    val verification: ExifVerification,
    val distanceMeters: Int? = null,
)

/**
 * Doble llave de verificación del SPEC: acepta la foto si sus coordenadas EXIF
 * caen dentro de un radio de 5 km del atractivo. La falta de metadatos nunca
 * bloquea la publicación, solo retira la insignia de verificación.
 */
object ExifLocationVerifier {

    const val VERIFICATION_RADIUS_METERS = 5_000

    fun verify(
        metadata: ExifMetadata?,
        destination: GeoPoint,
        radiusMeters: Int = VERIFICATION_RADIUS_METERS,
    ): ExifCheck {
        require(radiusMeters > 0) { "El radio de verificación debe ser positivo" }
        val photoPoint = metadata?.point
            ?: return ExifCheck(ExifVerification.NO_GPS)
        val distance = GeoCalculator.distanceMeters(photoPoint, destination)
        return ExifCheck(
            verification = if (distance <= radiusMeters) {
                ExifVerification.VERIFIED_ON_SITE
            } else {
                ExifVerification.OUTSIDE_RADIUS
            },
            distanceMeters = distance,
        )
    }
}
