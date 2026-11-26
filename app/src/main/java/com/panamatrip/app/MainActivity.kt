package com.panamatrip.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.panamatrip.app.core.designsystem.theme.PanamaTripTheme
import com.panamatrip.app.features.onboarding.ui.OnboardingRoute
import com.panamatrip.app.features.onboarding.ui.PanamaSplash
import com.panamatrip.app.navigation.PanamaTripApp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as PanamaTripApplication).container
        setContent {
            PanamaTripTheme {
                // rememberSaveable: rotar o volver de un diálogo de permisos no
                // repite el splash; solo un arranque nuevo de Activity lo ve.
                var showSplash by rememberSaveable { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    if (showSplash) {
                        delay(SPLASH_MILLIS)
                        showSplash = false
                    }
                }
                val preferences by container.preferencesRepository.preferences
                    .collectAsStateWithLifecycle(initialValue = null)

                when {
                    showSplash -> PanamaSplash()
                    // DataStore aún carga: superficie sólida, sin repetir splash.
                    preferences == null -> Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surface,
                    ) {}
                    preferences?.hasCompletedOnboarding == true -> PanamaTripApp(container)
                    else -> OnboardingRoute(container)
                }
            }
        }
    }

    private companion object {
        const val SPLASH_MILLIS = 1_350L
    }
}
