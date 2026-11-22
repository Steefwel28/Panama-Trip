package com.panamatrip.app.features.trackrecorder.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.panamatrip.app.MainActivity
import com.panamatrip.app.PanamaTripApplication
import com.panamatrip.app.R
import com.panamatrip.app.core.format.formatElapsedDuration
import com.panamatrip.app.domain.model.GeoPoint
import com.panamatrip.app.domain.model.RecorderStatus
import com.panamatrip.app.domain.model.TrackSample
import com.panamatrip.app.domain.model.TrackSnapshot
import com.panamatrip.app.domain.repository.TrackRepository
import com.panamatrip.app.domain.track.TrackRecorderEngine
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Foreground service ligero que graba el recorrido por satélite sin depender
 * de la red. La lógica de métricas vive en [TrackRecorderEngine]; aquí solo
 * se conecta Android (Fused Location, notificación y persistencia).
 */
class LocationTrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var trackRepository: TrackRepository
    private lateinit var notificationManager: NotificationManager

    private var engine: TrackRecorderEngine? = null
    private var locationCallback: LocationCallback? = null
    private var tickerJob: Job? = null
    private var startedAtEpochMillis = 0L
    private var destinationId: String? = null
    private var destinationName: String? = null

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        trackRepository = (application as PanamaTripApplication).container.trackRepository
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording(intent)
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
            ACTION_FINISH -> finishRecording()
            ACTION_DISCARD -> discardRecording()
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopTicker()
        stopLocationUpdates()
        serviceScope.cancel()
        if (_session.value is RecorderSession.Active) {
            _session.value = RecorderSession.Idle
        }
        super.onDestroy()
    }

    private fun startRecording(intent: Intent) {
        if (engine != null) return
        if (!hasLocationPermission()) {
            // startForeground con tipo location lanza SecurityException sin el
            // permiso runtime; el servicio debe cerrarse sin tumbar el proceso.
            runCatching { startForegroundCompat(EMPTY_SNAPSHOT) }
            _events.tryEmit(RecorderEvent.PERMISSION_DENIED)
            stopForegroundCompat()
            stopSelf()
            return
        }

        destinationId = intent.getStringExtra(EXTRA_DESTINATION_ID)
        destinationName = intent.getStringExtra(EXTRA_DESTINATION_NAME)
        startedAtEpochMillis = System.currentTimeMillis()
        engine = TrackRecorderEngine(
            trackId = "track-${UUID.randomUUID()}",
            startedAtEpochMillis = startedAtEpochMillis,
            startedElapsedMillis = SystemClock.elapsedRealtime(),
            destinationId = destinationId,
            destinationName = destinationName,
        )

        val snapshot = snapshot()
        if (runCatching { startForegroundCompat(snapshot) }.isFailure) {
            engine = null
            _events.tryEmit(RecorderEvent.FAILED)
            stopSelf()
            return
        }
        _session.value = RecorderSession.Active(
            status = snapshot.status,
            snapshot = snapshot,
            startedAtEpochMillis = startedAtEpochMillis,
            destinationId = destinationId,
            destinationName = destinationName,
        )
        requestLocationUpdates()
        startTicker()
    }

    private fun pauseRecording() {
        val engine = engine ?: return stopSelf()
        if (engine.pause(SystemClock.elapsedRealtime())) {
            publishSnapshot()
            stopTicker()
        }
    }

    private fun resumeRecording() {
        val engine = engine ?: return stopSelf()
        if (engine.resume(SystemClock.elapsedRealtime())) {
            publishSnapshot()
            startTicker()
        }
    }

    private fun finishRecording() {
        val activeEngine = engine ?: return stopSelf()
        engine = null
        stopTicker()
        stopLocationUpdates()
        val now = SystemClock.elapsedRealtime()
        val track = activeEngine.finish(
            nowElapsedMillis = now,
            finishedAtEpochMillis = System.currentTimeMillis(),
        )
        serviceScope.launch {
            val result = runCatching { trackRepository.save(track) }
            _session.value = RecorderSession.Idle
            _events.tryEmit(if (result.isSuccess) RecorderEvent.SAVED else RecorderEvent.FAILED)
            stopForegroundCompat()
            stopSelf()
        }
    }

    private fun discardRecording() {
        engine = null
        stopTicker()
        stopLocationUpdates()
        _session.value = RecorderSession.Idle
        _events.tryEmit(RecorderEvent.DISCARDED)
        stopForegroundCompat()
        stopSelf()
    }

    private fun publishSnapshot() {
        val activeEngine = engine ?: return
        val snapshot = activeEngine.snapshot(SystemClock.elapsedRealtime())
        _session.value = RecorderSession.Active(
            status = snapshot.status,
            snapshot = snapshot,
            startedAtEpochMillis = startedAtEpochMillis,
            destinationId = destinationId,
            destinationName = destinationName,
        )
        notificationManager.notify(NOTIFICATION_ID, buildNotification(snapshot))
    }

    private fun snapshot(): TrackSnapshot =
        engine?.snapshot(SystemClock.elapsedRealtime()) ?: EMPTY_SNAPSHOT

    /**
     * Ticker independiente de las muestras GPS: mantiene el cronómetro y la
     * notificación avanzando cada segundo aunque el usuario esté detenido y no
     * lleguen coordenadas nuevas.
     */
    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = serviceScope.launch {
            while (isActive) {
                delay(TICK_INTERVAL_MILLIS)
                val activeEngine = engine ?: break
                if (activeEngine.snapshot(SystemClock.elapsedRealtime()).status != RecorderStatus.RECORDING) {
                    break
                }
                publishSnapshot()
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    @Suppress("MissingPermission")
    private fun requestLocationUpdates() {
        if (locationCallback != null) return
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            SAMPLE_INTERVAL_MILLIS,
        )
            .setMinUpdateDistanceMeters(MIN_UPDATE_DISTANCE_METERS)
            .setWaitForAccurateLocation(false)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                addLocation(location)
            }
        }
        locationCallback = callback
        fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
    }

    private fun addLocation(location: android.location.Location) {
        val activeEngine = engine ?: return
        val sample = TrackSample(
            point = GeoPoint(location.latitude, location.longitude),
            elevationMeters = location.altitude.takeIf { location.hasAltitude() },
            accuracyMeters = location.accuracy.takeIf { location.hasAccuracy() },
            capturedAtEpochMillis = location.time.takeIf { it > 0L } ?: System.currentTimeMillis(),
        )
        if (activeEngine.addSample(sample, SystemClock.elapsedRealtime())) {
            publishSnapshot()
        }
    }

    private fun stopLocationUpdates() {
        val callback = locationCallback ?: return
        fusedLocationClient.removeLocationUpdates(callback)
        locationCallback = null
    }

    private fun hasLocationPermission(): Boolean {
        val fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun startForegroundCompat(snapshot: TrackSnapshot) {
        val notification = buildNotification(snapshot)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundCompat() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun buildNotification(snapshot: TrackSnapshot): Notification {
        val recording = snapshot.status == RecorderStatus.RECORDING
        val distance = if (snapshot.distanceMeters < METERS_PER_KILOMETER) {
            getString(R.string.distance_meters, snapshot.distanceMeters)
        } else {
            getString(
                R.string.distance_kilometers,
                snapshot.distanceMeters / METERS_PER_KILOMETER.toDouble(),
            )
        }
        val stats = getString(
            R.string.track_notification_stats,
            distance,
            formatElapsedDuration(snapshot.activeDurationMillis),
        )
        val toggleLabel = getString(
            if (recording) R.string.track_action_pause else R.string.track_action_resume,
        )
        val toggleIntent = commandPendingIntent(if (recording) ACTION_PAUSE else ACTION_RESUME)

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(
                getString(
                    if (recording) {
                        R.string.track_notification_recording
                    } else {
                        R.string.track_notification_paused
                    },
                ),
            )
            .setContentText(stats)
            .setContentIntent(openAppPendingIntent())
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(notificationAction(toggleLabel, toggleIntent))
            .addAction(
                notificationAction(
                    getString(R.string.track_action_finish),
                    commandPendingIntent(ACTION_FINISH),
                ),
            )
            .build()
    }

    private fun notificationAction(label: String, intent: PendingIntent): Notification.Action =
        Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_launcher_monochrome),
            label,
            intent,
        ).build()

    private fun openAppPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            REQUEST_OPEN_APP,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun commandPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, LocationTrackingService::class.java).setAction(action)
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.track_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.track_notification_channel_description)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_START = "com.panamatrip.app.action.TRACK_START"
        const val ACTION_PAUSE = "com.panamatrip.app.action.TRACK_PAUSE"
        const val ACTION_RESUME = "com.panamatrip.app.action.TRACK_RESUME"
        const val ACTION_FINISH = "com.panamatrip.app.action.TRACK_FINISH"
        const val ACTION_DISCARD = "com.panamatrip.app.action.TRACK_DISCARD"

        const val EXTRA_DESTINATION_ID = "destinationId"
        const val EXTRA_DESTINATION_NAME = "destinationName"

        private const val CHANNEL_ID = "track_recording"
        private const val NOTIFICATION_ID = 3001
        private const val REQUEST_OPEN_APP = 3002
        private const val SAMPLE_INTERVAL_MILLIS = 3_000L
        private const val MIN_UPDATE_DISTANCE_METERS = 4f
        private const val TICK_INTERVAL_MILLIS = 1_000L
        private const val METERS_PER_KILOMETER = 1_000

        private val EMPTY_SNAPSHOT = TrackSnapshot(
            status = RecorderStatus.RECORDING,
            distanceMeters = 0,
            activeDurationMillis = 0L,
            averageSpeedKmh = 0.0,
            elevationGainMeters = 0,
            pointCount = 0,
        )

        private val _session = MutableStateFlow<RecorderSession>(RecorderSession.Idle)
        val session: StateFlow<RecorderSession> = _session.asStateFlow()

        private val _events = MutableSharedFlow<RecorderEvent>(
            extraBufferCapacity = 4,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
        val events: SharedFlow<RecorderEvent> = _events.asSharedFlow()

        fun start(context: Context, destinationId: String?, destinationName: String?) {
            val intent = Intent(context, LocationTrackingService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_DESTINATION_ID, destinationId)
                .putExtra(EXTRA_DESTINATION_NAME, destinationName)
            context.startForegroundService(intent)
        }

        fun pause(context: Context) = command(context, ACTION_PAUSE)

        fun resume(context: Context) = command(context, ACTION_RESUME)

        fun finish(context: Context) = command(context, ACTION_FINISH)

        fun discard(context: Context) = command(context, ACTION_DISCARD)

        private fun command(context: Context, action: String) {
            context.startService(
                Intent(context, LocationTrackingService::class.java).setAction(action),
            )
        }
    }
}
