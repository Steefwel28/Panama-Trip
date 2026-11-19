package com.panamatrip.app.core.database.seed

import com.panamatrip.app.domain.geo.GeoCalculator
import com.panamatrip.app.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedCatalogTest {

    private val destinationIds = SeedCatalog.destinations.map { it.id }.toSet()
    private val postIds = SeedCatalog.posts.map { it.id }.toSet()

    @Test
    fun `catalog ships exactly the six agreed destinations`() {
        val expected = setOf(
            "cerro-picacho",
            "laguna-san-carlos",
            "los-picachos-de-ola",
            "cerro-azul",
            "cascada-el-salto",
            "la-yeguada",
        )

        assertEquals(6, SeedCatalog.destinations.size)
        assertEquals(expected, destinationIds)
    }

    @Test
    fun `destinations keep a plausible parking and attraction point in Panama`() {
        SeedCatalog.destinations.forEach { destination ->
            val parking = GeoPoint(destination.parkingLatitude, destination.parkingLongitude)
            val attraction = GeoPoint(destination.attractionLatitude, destination.attractionLongitude)

            assertTrue("${destination.id} parking fuera del istmo", GeoCalculator.isPlausiblePanamaPoint(parking))
            assertTrue("${destination.id} attraction fuera del istmo", GeoCalculator.isPlausiblePanamaPoint(attraction))
            assertNotEquals("${destination.id} comparte ambas coordenadas", parking, attraction)
            assertTrue("${destination.id} caminata inválida", destination.walkDistanceMeters > 0)
            assertTrue("${destination.id} minutos inválidos", destination.walkMinutes > 0)
            assertTrue("${destination.id} sin imagen", destination.heroImageUrl.startsWith("https://"))
        }
    }

    @Test
    fun `every destination has community vehicle reports`() {
        val reportsByDestination = SeedCatalog.vehicleReports.groupBy { it.destinationId }

        destinationIds.forEach { id ->
            assertTrue("$id no tiene reportes vehiculares", reportsByDestination[id].orEmpty().isNotEmpty())
        }
        assertTrue(SeedCatalog.vehicleReports.all { it.destinationId in destinationIds })
    }

    @Test
    fun `posts credit the curator role and reference real destinations`() {
        SeedCatalog.posts.forEach { post ->
            assertTrue("${post.id} apunta a destino inexistente", post.destinationId in destinationIds)
            assertEquals(SeedCatalog.CURATOR_ROLE, post.authorRole)
            assertTrue("${post.id} sin autor visible", post.authorName.isNotBlank())
            assertTrue("${post.id} sin iniciales", post.authorInitials.isNotBlank())
            assertTrue("${post.id} sin likes base", post.baseLikeCount > 0)
        }
    }

    @Test
    fun `comments and collections reference seeded posts`() {
        SeedCatalog.comments.forEach { comment ->
            assertTrue("${comment.id} apunta a post inexistente", comment.postId in postIds)
        }
        SeedCatalog.collections.forEach { collection ->
            assertTrue("${collection.id} sin publicaciones", collection.postIds.isNotEmpty())
            collection.postIds.forEach { postId ->
                assertTrue("${collection.id} apunta a post inexistente", postId in postIds)
            }
        }
    }

    @Test
    fun `reviews reference destinations with valid ratings`() {
        SeedCatalog.reviews.forEach { review ->
            assertTrue("${review.id} apunta a destino inexistente", review.destinationId in destinationIds)
            assertTrue("${review.id} rating fuera de rango", review.rating in 1..5)
            assertEquals(SeedCatalog.CURATOR_ROLE, review.authorRole)
        }
        assertEquals(
            "todos los destinos necesitan reseñas iniciales",
            destinationIds,
            SeedCatalog.reviews.map { it.destinationId }.toSet(),
        )
    }

    @Test
    fun `identifiers stay unique across the catalog`() {
        assertEquals(SeedCatalog.destinations.size, destinationIds.size)
        assertEquals(SeedCatalog.posts.size, postIds.size)
        assertEquals(SeedCatalog.comments.size, SeedCatalog.comments.map { it.id }.toSet().size)
        assertEquals(SeedCatalog.collections.size, SeedCatalog.collections.map { it.id }.toSet().size)
        assertEquals(SeedCatalog.reviews.size, SeedCatalog.reviews.map { it.id }.toSet().size)
    }

    @Test
    fun `seed covers all three vehicle profiles`() {
        val vehicles = SeedCatalog.vehicleReports.map { it.vehicle }.toSet()

        assertEquals(3, vehicles.size)
    }
}
