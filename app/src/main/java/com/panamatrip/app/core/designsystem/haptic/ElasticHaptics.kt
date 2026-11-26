package com.panamatrip.app.core.designsystem.haptic

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Crescendo táctil del splash: composición de primitivas en API 30+ cuando el
 * hardware las soporta y forma de onda compatible en API 26–29. El trabajo se
 * cancela al salir de composición para no dejar vibraciones huérfanas.
 */
@Stable
class ElasticSplashHaptics internal constructor(private val vibrator: Vibrator?) {

    private var job: Job? = null

    fun play(scope: CoroutineScope) {
        val device = vibrator ?: return
        if (!device.hasVibrator()) return
        job?.cancel()
        job = scope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && supportsComposition(device)) {
                playComposition(device)
            } else {
                playWaveform(device)
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        vibrator?.cancel()
    }

    private suspend fun playComposition(device: Vibrator) {
        // Primer golpe: el mosaico entra.
        device.vibrate(
            VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 1f)
                .compose(),
        )
        delay(260)
        // Segundo tile: subida corta + tick.
        device.vibrate(
            VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.7f)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.8f)
                .compose(),
        )
        delay(300)
        // Tercer tile: giro ascendente.
        device.vibrate(
            VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_SPIN, 0.8f)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.9f)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 1f)
                .compose(),
        )
        delay(280)
        // Micro-pulsos del cuarto tile.
        repeat(3) {
            device.vibrate(VibrationEffect.createOneShot(16L, 90))
            delay(90)
        }
        // Cierre con el wordmark.
        delay(80)
        device.vibrate(
            VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 1f)
                .compose(),
        )
    }

    private suspend fun playWaveform(device: Vibrator) {
        device.vibrate(
            VibrationEffect.createWaveform(
                longArrayOf(0, 30, 70, 45, 60, 55, 50, 70, 40, 110),
                intArrayOf(0, 55, 0, 90, 0, 130, 0, 175, 0, 255),
                -1,
            ),
        )
        delay(520)
        device.vibrate(
            VibrationEffect.createWaveform(
                longArrayOf(0, 20, 70, 20, 70, 20),
                intArrayOf(0, 100, 0, 150, 0, 210),
                -1,
            ),
        )
    }

    private fun supportsComposition(device: Vibrator): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        val primitives = intArrayOf(
            VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
            VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
            VibrationEffect.Composition.PRIMITIVE_TICK,
            VibrationEffect.Composition.PRIMITIVE_SPIN,
            VibrationEffect.Composition.PRIMITIVE_THUD,
        )
        return device.areAllPrimitivesSupported(*primitives)
    }
}

@Composable
fun rememberElasticSplashHaptics(): ElasticSplashHaptics {
    val context = LocalContext.current
    val haptics = remember(context) {
        ElasticSplashHaptics(vibratorOf(context))
    }
    DisposableEffect(haptics) {
        onDispose { haptics.cancel() }
    }
    return haptics
}

@Composable
fun rememberElasticSplashPlayer(): Pair<ElasticSplashHaptics, CoroutineScope> {
    val haptics = rememberElasticSplashHaptics()
    val scope = rememberCoroutineScope()
    return haptics to scope
}

private fun vibratorOf(context: Context): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
            ?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
