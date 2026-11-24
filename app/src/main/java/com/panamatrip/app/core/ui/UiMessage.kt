package com.panamatrip.app.core.ui

/** Errores de recuperación de datos mostrados por las pantallas. */
enum class UiError {
    DATABASE,
    UNKNOWN,
}

/** Avisos efímeros (snackbar) disparados por acciones del invitado. */
enum class UiNotice {
    COMMENT_PUBLISHED,
    SAVED,
    REMOVED,
    COLLECTION_CREATED,
    ROUTE_READY,
    PLAN_SAVED,
    TRACK_SAVED,
    TRACK_DISCARDED,
    LOCATION_DENIED,
    ACTION_FAILED,
}
