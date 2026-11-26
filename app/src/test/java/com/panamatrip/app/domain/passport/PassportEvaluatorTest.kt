package com.panamatrip.app.domain.passport

import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PassportEvaluatorTest {

    private val zone = ZoneId.of("America/Panama")

    @Test
    fun `evaluar dos veces produce exactamente el mismo pasaporte`() {
        val hikes = listOf(
            HikeEvidence("Chiriquí", 1_700_000_000_000, 900),
            HikeEvidence("Colón", 1_700_000_000_000, 120),
        )

        val first = PassportEvaluator.evaluate(hikes, odometerMeters = 2_000_000, zoneId = zone)
        val second = PassportEvaluator.evaluate(hikes, odometerMeters = 2_000_000, zoneId = zone)

        assertEquals(first, second)
    }

    @Test
    fun `desbloquea provincias y comarcas por nombre`() {
        val progress = PassportEvaluator.evaluate(
            hikes = listOf(
                HikeEvidence("Chiriquí", 1_700_000_000_000, 300),
                HikeEvidence("Guna Yala", 1_700_000_000_000, 10),
            ),
            odometerMeters = 12_000,
            zoneId = zone,
        )

        assertTrue("PA-CHI" in progress.unlockedRegionCodes)
        assertTrue("PA-GUN" in progress.unlockedRegionCodes)
        assertFalse("PA-DAR" in progress.unlockedRegionCodes)
        assertEquals(2, progress.unlockedRegions)
        assertEquals(16, progress.totalRegions)
    }

    @Test
    fun `mil kilometros desbloquean la insignia dorada`() {
        val progress = PassportEvaluator.evaluate(
            hikes = listOf(HikeEvidence("Panamá", 1_700_000_000_000, 50)),
            odometerMeters = PassportEvaluator.A_PURO_PULMON_METERS,
            zoneId = zone,
        )

        assertTrue(PassportBadge.A_PURO_PULMON in progress.badges)
        assertTrue(PassportBadge.FIRST_STEP in progress.badges)
    }

    @Test
    fun `sin caminatas no hay insignias`() {
        val progress = PassportEvaluator.evaluate(emptyList(), odometerMeters = 0, zoneId = zone)

        assertTrue(progress.badges.isEmpty())
        assertTrue(progress.unlockedRegionCodes.isEmpty())
    }
}
