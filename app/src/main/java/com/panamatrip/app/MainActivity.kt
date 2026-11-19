package com.panamatrip.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.panamatrip.app.core.designsystem.theme.PanamaTripTheme
import com.panamatrip.app.navigation.PanamaTripApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as PanamaTripApplication).container
        setContent {
            PanamaTripTheme {
                PanamaTripApp(container = container)
            }
        }
    }
}
