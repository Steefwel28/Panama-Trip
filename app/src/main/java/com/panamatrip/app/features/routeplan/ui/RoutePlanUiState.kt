package com.panamatrip.app.features.routeplan.ui

import com.panamatrip.app.core.ui.UiError
import com.panamatrip.app.domain.logistics.FuelType
import com.panamatrip.app.domain.logistics.LogisticsPlan
import com.panamatrip.app.domain.model.Destination
import com.panamatrip.app.domain.model.GeoPoint
import com.panamatrip.app.domain.model.VehicleType

/** Franjas de salida rápidas que alimentan el cálculo de paradas por horario. */
enum class DepartureWindow(val minuteOfDay: Int) {
    DAWN(5 * 60 + 30),
    MORNING(8 * 60),
    NOON(12 * 60),
}

/** Origen elegido para el cálculo logístico. */
enum class OriginMode {
    CURRENT_LOCATION,
    CUSTOM,
}

/** Fila compacta del histórico de planes guardados en Room. */
data class SavedPlanUi(
    val id: String,
    val fuelType: FuelType,
    val mountainSurcharge: Boolean,
    val departureMinuteOfDay: Int,
    val totalCents: Int,
    val cashOnlyCents: Int,
    val createdAtEpochMillis: Long,
)

sealed interface RoutePlanUiState {

    data object Loading : RoutePlanUiState

    data class Content(
        val destination: Destination,
        val origin: GeoPoint,
        val originMode: OriginMode,
        val isLocatingOrigin: Boolean = false,
        val vehicle: VehicleType,
        val fuelType: FuelType,
        val surchargeEnabled: Boolean,
        val departureWindow: DepartureWindow,
        val cashAlertsEnabled: Boolean,
        val plan: LogisticsPlan,
        val savedPlans: List<SavedPlanUi>,
    ) : RoutePlanUiState

    data object Empty : RoutePlanUiState

    data class Error(val error: UiError) : RoutePlanUiState
}
