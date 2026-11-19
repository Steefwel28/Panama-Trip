package com.panamatrip.app.domain.affinity

import com.panamatrip.app.domain.model.Destination
import com.panamatrip.app.domain.model.Ecosystem
import com.panamatrip.app.domain.model.GeoPoint
import com.panamatrip.app.domain.model.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AffinitySnapshotTest {

    @Test
    fun `codec roundtrip preserves scores`() {
        val scores = linkedMapOf("cerro-picacho" to 3.5, "la-yeguada" to 1.25)
        val decoded = AffinityCodec.decode(AffinityCodec.encode(scores))

        assertEquals(scores, decoded)
    }

    @Test
    fun `codec ignores malformed and non positive entries`() {
        val decoded = AffinityCodec.decode("valido:2.00|sinvalor:|negativo:-1.00|sinseparador")

        assertEquals(mapOf("valido" to 2.0), decoded)
    }

    @Test
    fun `codec decodes legacy blank payloads as empty`() {
        assertTrue(AffinityCodec.decode(null).isEmpty())
        assertTrue(AffinityCodec.decode("").isEmpty())
    }

    @Test
    fun `interaction raises destination and ecosystem scores`() {
        val updated = AffinitySnapshot()
            .withInteraction("cerro-picacho", Ecosystem.MOUNTAIN, AffinityWeights.LIKE)

        assertEquals(AffinityWeights.LIKE, updated.destinationScores["cerro-picacho"] ?: 0.0, 0.001)
        assertEquals(AffinityWeights.LIKE, updated.ecosystemScores[Ecosystem.MOUNTAIN.name] ?: 0.0, 0.001)
        assertEquals(1, updated.totalInteractions)
        assertFalse(updated.isColdStart)
    }

    @Test
    fun `scores never exceed the configured ceiling`() {
        var snapshot = AffinitySnapshot()
        repeat(50) {
            snapshot = snapshot.withInteraction("cerro-picacho", Ecosystem.MOUNTAIN, AffinityWeights.SAVE)
        }

        assertEquals(AffinityWeights.MAX_SCORE, snapshot.destinationScores.getValue("cerro-picacho"), 0.001)
        assertTrue(snapshot.normalizedFor(destination("cerro-picacho")) <= 1f)
    }

    @Test
    fun `ecosystem affinity lifts a destination without direct interaction`() {
        val snapshot = AffinitySnapshot(
            ecosystemScores = mapOf(Ecosystem.WATERFALL.name to 4.0),
            totalInteractions = 1,
        )

        assertTrue(snapshot.scoreFor(destination("cascada-el-salto")) > 0.0)
        assertEquals(0.0, snapshot.scoreFor(destination("la-yeguada")), 0.001)
    }

    private fun destination(id: String): Destination = Destination(
        id = id,
        name = id,
        province = "Panamá",
        ecosystem = if (id == "cascada-el-salto") Ecosystem.WATERFALL else Ecosystem.PINE_FOREST,
        summary = "",
        description = "",
        accessNotes = "",
        parking = GeoPoint(8.0, -80.0),
        attraction = GeoPoint(8.1, -80.1),
        heroImageUrl = "",
        walkDistanceMeters = 1000,
        walkMinutes = 20,
        elevationGainMeters = 50,
        recommendedVehicle = VehicleType.SEDAN,
        vehicleAccess = emptyMap(),
        averageRating = 0.0,
        reviewCount = 0,
        featuredOrder = 1,
    )
}
