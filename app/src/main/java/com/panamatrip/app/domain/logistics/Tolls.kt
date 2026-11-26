package com.panamatrip.app.domain.logistics

import com.panamatrip.app.domain.model.GeoPoint

/** Medio de pago aceptado en un tramo de peaje panameño. */
enum class TollPayment {
    PANAPASS,
    CASH_ONLY,
}

/** Tramo de peaje referencial, con su tarifa por sentido. */
data class TollSegment(
    val id: String,
    val name: String,
    val payment: TollPayment,
    val oneWayCents: Int,
) {
    val isCashOnly: Boolean get() = payment == TollPayment.CASH_ONLY
}

/**
 * Catálogo de peajes del istmo usado por el planificador. Las tarifas son
 * referenciales y se muestran como estimado: el monto final lo fija cada
 * concesionaria al pasar la caseta.
 */
object TollCatalog {

    private val PANAMA_COLON = TollSegment(
        id = "autopista-panama-colon",
        name = "Autopista Panamá–Colón",
        payment = TollPayment.CASH_ONLY,
        oneWayCents = 250,
    )

    private val ARRAIJAN_LA_CHORRERA = TollSegment(
        id = "arraijan-la-chorrera",
        name = "Autopista Arraiján–La Chorrera",
        payment = TollPayment.PANAPASS,
        oneWayCents = 135,
    )

    /**
     * Corredores que conectan la Ciudad de Panamá con cada provincia del
     * catálogo. Si la provincia no exige peaje, la lista queda vacía.
     */
    fun segmentsFor(province: String): List<TollSegment> = when (province) {
        "Colón" -> listOf(PANAMA_COLON)
        "Panamá Oeste", "Coclé", "Veraguas", "Chiriquí", "Herrera", "Los Santos" ->
            listOf(ARRAIJAN_LA_CHORRERA)
        else -> emptyList()
    }

    /**
     * Filtro por corredor real: los accesos al este de la capital (Cerro Azul)
     * no cruzan casetas, aunque compartan la provincia de Panamá.
     */
    fun segmentsFor(province: String, parking: GeoPoint): List<TollSegment> =
        if (CorridorCatalog.of(province, parking) == PanamanianCorridor.EAST_HILLS) {
            emptyList()
        } else {
            segmentsFor(province)
        }

    fun roundTripCents(segments: List<TollSegment>): Int =
        segments.sumOf { it.oneWayCents } * DIRECTIONS_PER_TRIP

    fun cashOnlyRoundTripCents(segments: List<TollSegment>): Int =
        segments.filter { it.isCashOnly }.sumOf { it.oneWayCents } * DIRECTIONS_PER_TRIP

    private const val DIRECTIONS_PER_TRIP = 2
}
