package com.panamatrip.app.domain.model

/** Idioma declarado por un guía local, mapeado a etiquetas de la UI. */
enum class GuideLanguage {
    SPANISH,
    ENGLISH,
    FRENCH,
    GERMAN,
}

/**
 * Guía certificado asociado a un destino. El teléfono se normaliza a dígitos
 * internacionales para abrir WhatsApp con un único Intent.
 */
data class LocalGuide(
    val id: String,
    val destinationId: String,
    val destinationName: String,
    val province: String,
    val name: String,
    val headline: String,
    val languages: List<GuideLanguage>,
    val rateUsdCents: Int,
    val verified: Boolean,
    val whatsappNumber: String,
)
