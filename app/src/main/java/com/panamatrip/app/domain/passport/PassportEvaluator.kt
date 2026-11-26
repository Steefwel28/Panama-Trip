package com.panamatrip.app.domain.passport

import java.time.Instant
import java.time.ZoneId

/** División política usada por el pasaporte: 10 provincias + 6 comarcas. */
data class PassportRegion(
    val code: String,
    val name: String,
    val isComarca: Boolean,
)

object PassportCatalog {
    val provinces = listOf(
        PassportRegion("PA-BOC", "Bocas del Toro", false),
        PassportRegion("PA-CHI", "Chiriquí", false),
        PassportRegion("PA-COC", "Coclé", false),
        PassportRegion("PA-COL", "Colón", false),
        PassportRegion("PA-DAR", "Darién", false),
        PassportRegion("PA-HER", "Herrera", false),
        PassportRegion("PA-LOS", "Los Santos", false),
        PassportRegion("PA-PAN", "Panamá", false),
        PassportRegion("PA-POE", "Panamá Oeste", false),
        PassportRegion("PA-VER", "Veraguas", false),
    )

    val comarcas = listOf(
        PassportRegion("PA-EMB", "Emberá-Wounaan", true),
        PassportRegion("PA-GUN", "Guna Yala", true),
        PassportRegion("PA-NGB", "Ngäbe-Buglé", true),
        PassportRegion("PA-NAS", "Naso Tjërdi", true),
        PassportRegion("PA-MAD", "Madugandí", true),
        PassportRegion("PA-WAR", "Wargandí", true),
    )

    val all: List<PassportRegion> = provinces + comarcas
}

/** Insignias del pasaporte. El evaluador es puro e idempotente. */
enum class PassportBadge {
    FIRST_STEP,
    A_PURO_PULMON,
    CAZADOR_DE_AMANECERES,
    DOS_OCEANOS,
    A_PRUEBA_DE_BAJAREQUE,
}

/** Evidencia mínima de una caminata completada. */
data class HikeEvidence(
    val province: String?,
    val startedAtEpochMillis: Long,
    val elevationGainMeters: Int,
)

data class PassportProgress(
    val unlockedRegionCodes: Set<String>,
    val badges: Set<PassportBadge>,
) {
    val unlockedRegions: Int get() = unlockedRegionCodes.size
    val totalRegions: Int get() = PassportCatalog.all.size
}

object PassportEvaluator {

    /** Provincias costeras del Caribe y del Pacífico para «Dos Océanos». */
    private val caribbean = setOf("Bocas del Toro", "Colón", "Guna Yala", "Ngäbe-Buglé")
    private val pacific = setOf(
        "Chiriquí",
        "Coclé",
        "Herrera",
        "Los Santos",
        "Panamá",
        "Panamá Oeste",
        "Veraguas",
        "Darién",
    )

    fun evaluate(
        hikes: List<HikeEvidence>,
        odometerMeters: Int,
        zoneId: ZoneId = ZoneId.of("America/Panama"),
    ): PassportProgress {
        val normalized = hikes.mapNotNull { hike ->
            val province = hike.province?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            province to hike
        }
        val unlocked = normalized
            .map { (province, _) -> province }
            .mapNotNull { province -> PassportCatalog.all.firstOrNull { it.name == province }?.code }
            .toSet()

        val badges = buildSet {
            if (hikes.isNotEmpty()) add(PassportBadge.FIRST_STEP)
            if (odometerMeters >= A_PURO_PULMON_METERS) add(PassportBadge.A_PURO_PULMON)
            if (hikes.any { hike -> isDawn(hike.startedAtEpochMillis, zoneId) }) {
                add(PassportBadge.CAZADOR_DE_AMANECERES)
            }
            val provinces = normalized.map { it.first }.toSet()
            if (provinces.any { it in caribbean } && provinces.any { it in pacific }) {
                add(PassportBadge.DOS_OCEANOS)
            }
            if (hikes.any { hike -> isRainySeason(hike.startedAtEpochMillis, zoneId) }) {
                add(PassportBadge.A_PRUEBA_DE_BAJAREQUE)
            }
        }
        return PassportProgress(unlockedRegionCodes = unlocked, badges = badges)
    }

    private fun isDawn(epochMillis: Long, zoneId: ZoneId): Boolean {
        val hour = Instant.ofEpochMilli(epochMillis).atZone(zoneId).hour
        return hour in DAWN_START_HOUR until DAWN_END_HOUR
    }

    /** Temporada lluviosa panameña: mayo a noviembre. */
    private fun isRainySeason(epochMillis: Long, zoneId: ZoneId): Boolean {
        val month = Instant.ofEpochMilli(epochMillis).atZone(zoneId).monthValue
        return month in RAINY_MONTHS
    }

    /** 1 000 km caminados: la meta dorada del pasaporte. */
    const val A_PURO_PULMON_METERS = 1_000_000
    private const val DAWN_START_HOUR = 4
    private const val DAWN_END_HOUR = 7
    private val RAINY_MONTHS = 5..11
}
