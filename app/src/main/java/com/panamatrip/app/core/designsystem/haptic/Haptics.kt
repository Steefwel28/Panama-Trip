package com.panamatrip.app.core.designsystem.haptic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Feedback táctil consistente de la app. Los estados activos usan [ToggleOn] /
 * [ToggleOff] y las acciones que confirman un resultado usan [Confirm].
 */
@Stable
class PanamaHaptics internal constructor(private val feedback: HapticFeedback) {

    fun toggle(on: Boolean) {
        feedback.performHapticFeedback(
            if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff,
        )
    }

    fun confirm() {
        feedback.performHapticFeedback(HapticFeedbackType.Confirm)
    }

    fun segment() {
        feedback.performHapticFeedback(HapticFeedbackType.SegmentTick)
    }
}

@Composable
fun rememberPanamaHaptics(): PanamaHaptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) { PanamaHaptics(feedback) }
}
