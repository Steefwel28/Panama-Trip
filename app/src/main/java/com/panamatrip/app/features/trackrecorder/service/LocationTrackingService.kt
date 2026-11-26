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
import com.panamatrip.app.domain.model.RecordedTrack
import com.panamatrip.app.domain.model.RecorderStatus
import com.panamatrip.app.domain.model.TrackPoint
import com.panamatrip.app.domain.model.TrackProgress
import com.panamatrip.app.domain.model.TrackSample
import com.panamatrip.app.domain.model.TrackSnapshot
import com.panamatrip.app.domain.model.TrackStatus
import com.panamatrip.app.domain.repository.TrackRepository
import com.panamatrip.app.domain.track.TrackRecorderEngine
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
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

    /** Identificador persistido: sobrevive aunque el servicio se recree. */
    private var trackId: String? = null

    /**
     * Puntos aún no escritos en Room. Solo se mutan en el hilo principal
     * ([addLocation], ticker y cierres); el escritor recibe copias inmutables.
     */
    private val pendingPoints = mutableListOf<TrackPoint>()
    private var lastFlushElapsedMillis = 0L

    /** startId del último comando: `stopSelf(lastStartId)` no mata un START nuevo. */
    private var lastStartId = 0

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        trackRepository = (application as PanamaTripApplication).container.trackRepository
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
        ensurePersistenceWriter(trackRepository)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastStartId = startId
        when (intent?.action) {
            ACTION_START -> startRecording(intent)
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
            ACTION_FINISH -> finishRecording()
            ACTION_DISCARD -> discardRecording()
            else -> stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        flushPendingPoints(force = true)
        stopTicker()
        stopLocationUpdates()
        serviceScope.cancel()
        // Un finish/discard ya encolado deja `engine == null`: el escritor es
        // quien publica Idle al terminar la escritura, no este callback.
        if (engine != null && _session.value is RecorderSession.Active) {
            _session.value = RecorderSession.Idle
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // El usuario barrió la app: asegura el lote y deja el track IN_PROGRESS.
        flushPendingPoints(force = true)
        super.onTaskRemoved(rootIntent)
    }

    private fun startRecording(intent: Intent) {
        if (engine != null) return
        if (!hasLocationPermission()) {
            // startForeground con tipo location lanza SecurityException sin el
            // permiso runtime; el servicio debe cerrarse sin tumbar el proceso.
            runCatching { startForegroundCompat(EMPTY_SNAPSHOT) }
            _events.tryEmit(RecorderEvent.PERMISSION_DENIED)
            stopForegroundCompat()
            stopSelf(lastStartId)
            return
        }

        destinationId = intent.getStringExtra(EXTRA_DESTINATION_ID)
        destinationName = intent.getStringExtra(EXTRA_DESTINATION_NAME)
        startedAtEpochMillis = System.currentTimeMillis()
        val newTrackId = "track-${UUID.randomUUID()}"
        trackId = newTrackId
        pendingPoints.clear()
        lastFlushElapsedMillis = SystemClock.elapsedRealtime()
        engine = TrackRecorderEngine(
            trackId = newTrackId,
            startedAtEpochMillis = startedAtEpochMillis,
            startedElapsedMillis = SystemClock.elapsedRealtime(),
            destinationId = destinationId,
            destinationName = destinationName,
        )

        val snapshot = snapshot()
        // El foreground se levanta antes de crear la cabecera: si falla, no
        // queda un track huérfano de 0 puntos en Room.
        if (runCatching { startForegroundCompat(snapshot) }.isFailure) {
            engine = null
            trackId = null
            _events.tryEmit(RecorderEvent.FAILED)
            stopSelf(lastStartId)
            return
        }

        // La cabecera nace IN_PROGRESS: si Android mata el proceso, al volver a
        // abrir la app el recorrido queda recuperable en lugar de perderse.
        enqueue(
            PersistenceCommand.StartTrack(
                RecordedTrack(
                    id = newTrackId,
                    destinationId = destinationId,
                    destinationName = destinationName,
                    startedAtEpochMillis = startedAtEpochMillis,
                    finishedAtEpochMillis = 0L,
                    distanceMeters = 0,
                    activeDurationMillis = 0L,
                    averageSpeedKmh = 0.0,
                    elevationGainMeters = 0,
                    pointCount = 0,
                    status = TrackStatus.IN_PROGRESS,
                ),
            ),
        )

        _session.value = RecorderSession.Active(
            trackId = newTrackId,
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
        val engine = engine ?: return stopSelf(lastStartId)
        if (engine.pause(SystemClock.elapsedRealtime())) {
            publishSnapshot()
            flushPendingPoints(force = true)
            stopTicker()
        }
    }

    private fun resumeRecording() {
        val engine = engine ?: return stopSelf(lastStartId)
        if (engine.resume(SystemClock.elapsedRealtime())) {
            publishSnapshot()
            flushPendingPoints(force = true)
            startTicker()
        }
    }

    private fun finishRecording() {
        val activeEngine = engine ?: return stopSelf(lastStartId)
        engine = null
        stopTicker()
        stopLocationUpdates()
        val now = SystemClock.elapsedRealtime()
        val track = activeEngine.finish(
            nowElapsedMillis = now,
            finishedAtEpochMillis = System.currentTimeMillis(),
        )
        val finishedId = trackId
        pendingPoints.clear()
        if (finishedId != null) {
            // El guardado y el Idle los completa el escritor serializado.
            enqueue(PersistenceCommand.FinishTrack(track))
            // Solo limpia si sigue siendo la misma grabación: un START
            // inmediatamente posterior conserva su propio trackId.
            if (trackId == finishedId) trackId = null
        } else {
            _session.value = RecorderSession.Idle
        }
        stopIfIdle()
    }

    private fun discardRecording() {
        val discardedId = trackId
        engine = null
        pendingPoints.clear()
        stopTicker()
        stopLocationUpdates()
        if (discardedId != null) {
            enqueue(PersistenceCommand.DiscardTrack(discardedId))
            if (trackId == discardedId) trackId = null
        } else {
            _session.value = RecorderSession.Idle
            _events.tryEmit(RecorderEvent.DISCARDED)
        }
        stopIfIdle()
    }

    /** Apaga el servicio solo si no hay motor: nunca corta un START nuevo. */
    private fun stopIfIdle() {
        if (engine != null) return
        stopForegroundCompat()
        stopSelf(lastStartId)
    }

    private fun publishSnapshot() {
        val activeEngine = engine ?: return
        val activeTrackId = trackId ?: return
        val snapshot = activeEngine.snapshot(SystemClock.elapsedRealtime())
        _session.value = RecorderSession.Active(
            trackId = activeTrackId,
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
                // El ticker es el reloj del flush por tiempo: 30 s sin lote lleno.
                flushPendingPoints(force = false)
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
            elevationMeters = elevationMetersOf(location),
            accuracyMeters = location.accuracy.takeIf { location.hasAccuracy() },
            capturedAtEpochMillis = location.time.takeIf { it > 0L } ?: System.currentTimeMillis(),
        )
        if (activeEngine.addSample(sample, SystemClock.elapsedRealtime())) {
            activeEngine.lastRecordedPoint()?.let { point ->
                pendingPoints += point
            }
            publishSnapshot()
            flushPendingPoints(force = false)
        }
    }

    /**
     * API 34+ expone la altitud sobre el nivel del mar: es la métrica estable
     * para desniveles. En versiones previas se usa la altitud elipsoidal.
     */
    private fun elevationMetersOf(location: android.location.Location): Double? =
        when {
            Build.VERSION.SDK_INT >= 34 && location.hasMslAltitude() -> location.mslAltitudeMeters
            location.hasAltitude() -> location.altitude
            else -> null
        }

    /**
     * Encola el buffer en el escritor serializado cuando hay 10 puntos o
     * pasaron 30 s. El escritor corre en [persistenceScope] y reintenta el lote
     * completo sin devolverlo a esta lista (la `sequence` ya es definitiva).
     */
    private fun flushPendingPoints(force: Boolean) {
        val id = trackId ?: return
        val activeEngine = engine
        val now = SystemClock.elapsedRealtime()
        val dueByCount = pendingPoints.size >= BATCH_SIZE
        val dueByTime = now - lastFlushElapsedMillis >= FLUSH_INTERVAL_MILLIS
        if (!force && !dueByCount && !dueByTime) return
        if (pendingPoints.isEmpty() && activeEngine == null) return

        val batch = pendingPoints.toList()
        pendingPoints.clear()
        lastFlushElapsedMillis = now
        val snapshot = activeEngine?.snapshot(now)
        val progress = TrackProgress(
            status = if (snapshot?.status == RecorderStatus.PAUSED) {
                TrackStatus.PAUSED
            } else {
                TrackStatus.IN_PROGRESS
            },
            distanceMeters = snapshot?.distanceMeters ?: 0,
            activeDurationMillis = snapshot?.activeDurationMillis ?: 0L,
            averageSpeedKmh = snapshot?.averageSpeedKmh ?: 0.0,
            elevationGainMeters = snapshot?.elevationGainMeters ?: 0,
            pointCount = snapshot?.pointCount ?: 0,
            finishedAtEpochMillis = 0L,
        )
        enqueue(PersistenceCommand.PersistBatch(id, batch, progress))
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
        private const val BATCH_SIZE = 10
        private const val FLUSH_INTERVAL_MILLIS = 30_000L

        /**
         * Scope de proceso para escrituras críticas: no se cancela con el
         * servicio ni con la Activity, así un lote en vuelo siempre termina.
         */
        private val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /**
         * Comandos de persistencia procesados por un único consumidor: el orden
         * de llegada es el orden de escritura (cabecera → lotes → cierre).
         */
        private val persistenceCommands = Channel<PersistenceCommand>(Channel.UNLIMITED)

        @Volatile
        private var persistenceWriter: Job? = null

        private const val PERSIST_ATTEMPTS = 3
        private const val PERSIST_RETRY_DELAY_MILLIS = 250L

        private sealed interface PersistenceCommand {
            data class StartTrack(val track: RecordedTrack) : PersistenceCommand

            data class PersistBatch(
                val trackId: String,
                val points: List<TrackPoint>,
                val progress: TrackProgress,
            ) : PersistenceCommand

            data class FinishTrack(val track: RecordedTrack) : PersistenceCommand

            data class DiscardTrack(val trackId: String) : PersistenceCommand
        }

        @Synchronized
        private fun ensurePersistenceWriter(repository: TrackRepository) {
            if (persistenceWriter?.isActive == true) return
            persistenceWriter = persistenceScope.launch {
                // Puntos que fallaron al persistir: se reenvían desde dentro del
                // propio escritor con el próximo comando, nunca a una lista
                // compartida con el hilo principal.
                var carryOver = emptyList<TrackPoint>()
                for (command in persistenceCommands) {
                    carryOver = when (command) {
                        is PersistenceCommand.StartTrack -> {
                            retryIo { repository.startTrack(command.track) }
                            // Un lote fallido de otra grabación no debe mezclarse
                            // con las secuencias del track nuevo.
                            emptyList()
                        }

                        is PersistenceCommand.PersistBatch -> {
                            val batch = (carryOver + command.points).distinctBy { it.sequence }
                            val result = retryIo {
                                repository.persistBatch(command.trackId, batch, command.progress)
                            }
                            if (result.isSuccess) emptyList() else batch
                        }

                        is PersistenceCommand.FinishTrack -> {
                            val points = (carryOver + command.track.points)
                                .distinctBy { it.sequence }
                            val result = retryIo {
                                repository.save(command.track.copy(points = points))
                            }
                            completeSession(command.track.id)
                            _events.tryEmit(
                                if (result.isSuccess) RecorderEvent.SAVED else RecorderEvent.FAILED,
                            )
                            emptyList()
                        }

                        is PersistenceCommand.DiscardTrack -> {
                            retryIo { repository.discard(command.trackId) }
                            completeSession(command.trackId)
                            _events.tryEmit(RecorderEvent.DISCARDED)
                            emptyList()
                        }
                    }
                }
            }
        }

        private fun enqueue(command: PersistenceCommand) {
            persistenceCommands.trySend(command)
        }

        /** Publica Idle solo si la sesión sigue siendo la grabación cerrada. */
        private fun completeSession(finishedTrackId: String) {
            val current = _session.value
            if (current is RecorderSession.Active && current.trackId == finishedTrackId) {
                _session.value = RecorderSession.Idle
            }
        }

        private suspend fun <T> retryIo(block: suspend () -> T): Result<T> {
            var lastFailure: Throwable? = null
            repeat(PERSIST_ATTEMPTS) { attempt ->
                val result = runCatching { block() }
                if (result.isSuccess) return result
                lastFailure = result.exceptionOrNull()
                if (attempt < PERSIST_ATTEMPTS - 1) delay(PERSIST_RETRY_DELAY_MILLIS)
            }
            return Result.failure(lastFailure ?: IllegalStateException("persistencia fallida"))
        }

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
