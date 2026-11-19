package com.panamatrip.app.core.ui

/** Errores de recuperación de datos mostrados por las pantallas. */
enum class UiError {
    DATABASE,
    UNKNOWN,
}

/** Avisos efímeros (snackbar) disparados por acciones del invitado. */
enum class UiNotice {
    LIKE_UPDATED,
    COMMENT_PUBLISHED,
    SAVED,
    REMOVED,
    COLLECTION_CREATED,
    ROUTE_READY,
    ACTION_FAILED,
}
