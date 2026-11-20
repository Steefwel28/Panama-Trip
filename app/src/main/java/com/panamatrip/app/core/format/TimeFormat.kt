package com.panamatrip.app.core.format

import java.util.Locale

/** Duración activa con formato de cronómetro HH:MM:SS. */
fun formatElapsedDuration(millis: Long): String {
    val totalSeconds = (millis.coerceAtLeast(0L) / MILLIS_PER_SECOND)
    val hours = totalSeconds / SECONDS_PER_HOUR
    val minutes = (totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3_600L
