package com.panamatrip.app.core.intents

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.panamatrip.app.domain.model.GeoPoint

/**
 * Construye intents nativos apuntando siempre al estacionamiento: es el punto
 * al que el usuario puede llegar en carro.
 */
object MapIntents {

    fun waze(point: GeoPoint): List<Intent> = listOf(
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://waze.com/ul?ll=${point.latitude},${point.longitude}&navigate=yes&zoom=17"),
        ),
        googleMapsWebFallback(point),
    )

    fun googleMaps(point: GeoPoint, label: String): List<Intent> = listOf(
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse(
                "geo:${point.latitude},${point.longitude}" +
                    "?q=${point.latitude},${point.longitude}(${Uri.encode(label)})",
            ),
        ),
        googleMapsWebFallback(point),
    )

    private fun googleMapsWebFallback(point: GeoPoint): Intent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://www.google.com/maps/search/?api=1&query=${point.latitude},${point.longitude}"),
    )
}

/** Abre el primer intent disponible y cae al navegador si no hay app instalada. */
fun Context.launchFirstAvailable(intents: List<Intent>) {
    intents.forEach { intent ->
        try {
            startActivity(intent)
            return
        } catch (notInstalled: ActivityNotFoundException) {
            // Se intenta el siguiente candidato (fallback web).
        } catch (blocked: SecurityException) {
            // Política del dispositivo: se intenta el siguiente candidato.
        }
    }
}
