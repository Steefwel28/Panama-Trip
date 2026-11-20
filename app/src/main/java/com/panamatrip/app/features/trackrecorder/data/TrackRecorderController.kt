package com.panamatrip.app.features.trackrecorder.data

import android.content.Context
import com.panamatrip.app.features.trackrecorder.service.LocationTrackingService
import com.panamatrip.app.features.trackrecorder.service.RecorderEvent
import com.panamatrip.app.features.trackrecorder.service.RecorderSession
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** Puerta de entrada a la grabación; aísla a la UI del Service de Android. */
interface TrackRecorderController {
    val session: StateFlow<RecorderSession>
    val events: SharedFlow<RecorderEvent>

    fun start(destinationId: String?, destinationName: String?)
    fun pause()
    fun resume()
    fun finish()
    fun discard()
}

class ServiceTrackRecorderController(context: Context) : TrackRecorderController {

    private val appContext = context.applicationContext

    override val session: StateFlow<RecorderSession> = LocationTrackingService.session
    override val events: SharedFlow<RecorderEvent> = LocationTrackingService.events

    override fun start(destinationId: String?, destinationName: String?) {
        LocationTrackingService.start(appContext, destinationId, destinationName)
    }

    override fun pause() = LocationTrackingService.pause(appContext)

    override fun resume() = LocationTrackingService.resume(appContext)

    override fun finish() = LocationTrackingService.finish(appContext)

    override fun discard() = LocationTrackingService.discard(appContext)
}
