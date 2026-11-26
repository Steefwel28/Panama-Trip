package com.panamatrip.app.domain.model

/**
 * Vehículo declarado por el usuario. El acceso real siempre lo decide la
 * comunidad: cada destino guarda un semáforo por tipo de vehículo.
 */
enum class VehicleType {
    SEDAN,
    SUV,
    FOUR_BY_FOUR,
}

/** Ecosistema natural del destino, usado por el motor silencioso de afinidad. */
enum class Ecosystem {
    MOUNTAIN,
    CLOUD_FOREST,
    WATERFALL,
    LAGOON,
    PINE_FOREST,
}

/** Operadoras con cobertura registrada en el destino. */
enum class Carrier {
    MOVIL,
    TIGO,
}

/** Nivel de señal móvil reportado, de sin señal a cobertura plena. */
enum class SignalLevel {
    NONE,
    LOW,
    MEDIUM,
    HIGH,
}

/** Veredicto comunitario de acceso vehicular. */
enum class AccessStatus {
    CLEAR,
    CAUTION,
    BLOCKED,
    CLOSED,
}

/** Mensaje rápido de estado de vía disponible al comentar una publicación. */
enum class TerrainStatus {
    ROAD_CLEAR,
    HEAVY_MUD,
    RIVER_HIGH,
    LOW_HANGING_BRANCHES,
}
