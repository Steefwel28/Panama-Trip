package com.panamatrip.app.domain.affinity

import com.panamatrip.app.domain.model.Destination
import com.panamatrip.app.domain.model.Ecosystem

/**
 * Ponderaciones del motor silencioso de afinidad. Modo invitado: todo se
 * guarda localmente en DataStore y nunca requiere cuenta.
 */
object AffinityWeights {
    const val VIEW = 1.0
    const val LIKE = 2.0
    const val SAVE = 3.5
    const val ECOSYSTEM_FACTOR = 0.75
    const val RECENCY_FACTOR = 0.4
    const val MAX_SCORE = 40.0
}

/** Instantánea de intereses acumulados por destino y ecosistema. */
data class AffinitySnapshot(
    val destinationScores: Map<String, Double> = emptyMap(),
    val ecosystemScores: Map<String, Double> = emptyMap(),
    val totalInteractions: Int = 0,
) {
    val isColdStart: Boolean get() = totalInteractions == 0

    fun scoreFor(destination: Destination): Double {
        val direct = destinationScores[destination.id] ?: 0.0
        val ecosystem = (ecosystemScores[destination.ecosystem.name] ?: 0.0) *
            AffinityWeights.ECOSYSTEM_FACTOR
        return (direct + ecosystem).coerceAtMost(AffinityWeights.MAX_SCORE)
    }

    fun normalizedFor(destination: Destination): Float {
        val raw = scoreFor(destination)
        return (raw / AffinityWeights.MAX_SCORE).toFloat().coerceIn(0f, 1f)
    }

    fun withInteraction(
        destinationId: String,
        ecosystem: Ecosystem,
        weight: Double,
    ): AffinitySnapshot {
        val nextDestination = destinationScores + (
            destinationId to ((destinationScores[destinationId] ?: 0.0) + weight)
                .coerceAtMost(AffinityWeights.MAX_SCORE)
            )
        val nextEcosystem = ecosystemScores + (
            ecosystem.name to ((ecosystemScores[ecosystem.name] ?: 0.0) + weight)
                .coerceAtMost(AffinityWeights.MAX_SCORE)
            )
        return copy(
            destinationScores = nextDestination,
            ecosystemScores = nextEcosystem,
            totalInteractions = totalInteractions + 1,
        )
    }
}

/**
 * Códec estable para DataStore: evita serializers externos y se puede probar
 * en JVM puro. Formato `clave:valor|clave:valor`.
 */
object AffinityCodec {

    private const val ENTRY_SEPARATOR = "|"
    private const val KEY_SEPARATOR = ":"

    fun encode(scores: Map<String, Double>): String = scores.entries
        .filter { it.value > 0.0 }
        .sortedBy { it.key }
        .joinToString(ENTRY_SEPARATOR) { (key, value) -> "$key$KEY_SEPARATOR${format(value)}" }

    fun decode(encoded: String?): Map<String, Double> {
        if (encoded.isNullOrBlank()) return emptyMap()
        return encoded.split(ENTRY_SEPARATOR)
            .mapNotNull { entry ->
                val separator = entry.lastIndexOf(KEY_SEPARATOR)
                if (separator <= 0) return@mapNotNull null
                val key = entry.substring(0, separator)
                val value = entry.substring(separator + 1).toDoubleOrNull()
                if (key.isBlank() || value == null || value <= 0.0) null else key to value
            }
            .toMap()
    }

    fun decodeSnapshot(destinationScores: String?, ecosystemScores: String?, interactions: Int): AffinitySnapshot =
        AffinitySnapshot(
            destinationScores = decode(destinationScores),
            ecosystemScores = decode(ecosystemScores),
            totalInteractions = interactions.coerceAtLeast(0),
        )

    private fun format(value: Double): String = "%.2f".format(java.util.Locale.US, value)
}
