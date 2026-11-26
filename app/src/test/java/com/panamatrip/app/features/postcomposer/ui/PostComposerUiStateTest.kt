package com.panamatrip.app.features.postcomposer.ui

import com.panamatrip.app.domain.model.Destination
import com.panamatrip.app.domain.model.Ecosystem
import com.panamatrip.app.domain.model.GeoPoint
import com.panamatrip.app.domain.model.PostMediaType
import com.panamatrip.app.domain.model.VehicleType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PostComposerUiStateTest {

    private val destination = Destination(
        id = "cerro-picacho",
        name = "Cerro Picacho",
        province = "Panamá",
        ecosystem = Ecosystem.MOUNTAIN,
        summary = "",
        description = "",
        accessNotes = "",
        parking = GeoPoint(8.9, -79.5),
        attraction = GeoPoint(8.91, -79.51),
        heroImageUrl = "",
        walkDistanceMeters = 2_400,
        walkMinutes = 75,
        elevationGainMeters = 520,
        recommendedVehicle = VehicleType.SUV,
        vehicleAccess = emptyMap(),
        averageRating = 4.5,
        reviewCount = 10,
        featuredOrder = 1,
    )

    private val photo = ComposerMediaUi(
        id = "media-1",
        fileUri = "file:///foto.jpg",
        mimeType = "image/jpeg",
        type = PostMediaType.PHOTO,
        width = 1080,
        height = 1440,
    )

    private fun content(
        destination: Destination? = this.destination,
        media: List<ComposerMediaUi> = listOf(photo),
        caption: String = "Subida al Picacho",
        busy: Boolean = false,
        linkedTrackId: String? = null,
    ) = PostComposerUiState.Content(
        destinations = listOfNotNull(destination),
        selectedDestination = destination,
        caption = caption,
        media = media,
        searchQuery = "",
        nearby = null,
        tracks = emptyList(),
        linkedTrackId = linkedTrackId,
        isBusy = busy,
    )

    @Test
    fun `publica con destino media y leyenda`() {
        assertTrue(content().canPublish)
    }

    @Test
    fun `no publica sin destino`() {
        assertFalse(content(destination = null).canPublish)
    }

    @Test
    fun `no publica sin media`() {
        assertFalse(content(media = emptyList()).canPublish)
    }

    @Test
    fun `no publica sin leyenda`() {
        assertFalse(content(caption = "   ").canPublish)
    }

    @Test
    fun `no publica mientras hay trabajo en curso`() {
        assertFalse(content(busy = true).canPublish)
    }

    @Test
    fun `solo una ruta puede quedar enlazada`() {
        // El estado guarda un único id nullable: reemplazar es asignar de nuevo.
        val first = content(linkedTrackId = "track-1")
        val replaced = first.copy(linkedTrackId = "track-2")

        assertTrue(first.linkedTrackId != replaced.linkedTrackId)
        assertTrue(replaced.canPublish)
    }
}
