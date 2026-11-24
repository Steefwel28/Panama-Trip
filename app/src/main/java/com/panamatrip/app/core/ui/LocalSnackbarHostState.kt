package com.panamatrip.app.core.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Host único de avisos, montado en el `Scaffold` raíz para que los snackbars
 * respeten la barra inferior y los insets del sistema.
 */
val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("LocalSnackbarHostState no provisto: envuelve la app con PanamaTripApp")
}
