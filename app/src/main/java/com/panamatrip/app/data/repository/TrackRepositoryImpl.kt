package com.panamatrip.app.data.repository

import androidx.room.withTransaction
import com.panamatrip.app.core.database.PanamaTripDatabase
import com.panamatrip.app.data.mapper.toDomain
import com.panamatrip.app.data.mapper.toEntity
import com.panamatrip.app.domain.model.RecordedTrack
import com.panamatrip.app.domain.model.TrackPoint
import com.panamatrip.app.domain.model.TrackProgress
import com.panamatrip.app.domain.model.TrackStatus
import com.panamatrip.app.domain.repository.TrackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persistencia de recorridos GPS. Todas las escrituras que tocan cabecera y
 * puntos pasan por `withTransaction` para que un cierre abrupto nunca deje un
 * track a medias con métricas que no correspondan a sus puntos.
 */
class TrackRepositoryImpl(
    private val database: PanamaTripDatabase,
) : TrackRepository {

    private val trackDao = database.trackDao()

    override fun observeTracks(): Flow<List<RecordedTrack>> =
        trackDao.observeAll().map { tracks -> tracks.map { it.toDomain() } }

    override fun observeTrackPoints(trackId: String): Flow<List<TrackPoint>> =
        trackDao.observePoints(trackId).map { points -> points.map { it.toDomain() } }

    override suspend fun save(track: RecordedTrack) {
        database.withTransaction {
            trackDao.insertTrack(track.toEntity())
            insertPoints(track.id, track.points, startSequence = 0)
        }
    }

    override suspend fun startTrack(track: RecordedTrack) {
        database.withTransaction {
            trackDao.insertTrack(track.copy(status = TrackStatus.IN_PROGRESS).toEntity())
            insertPoints(track.id, track.points, startSequence = 0)
        }
    }

    override suspend fun persistBatch(
        trackId: String,
        points: List<TrackPoint>,
        progress: TrackProgress,
    ) {
        database.withTransaction {
            // Los lotes se agregan al final: el GPS solo entrega puntos nuevos.
            val startSequence = trackDao.countPoints(trackId)
            insertPoints(trackId, points, startSequence)
            trackDao.updateProgress(
                trackId = trackId,
                status = progress.status.name,
                finishedAt = progress.finishedAtEpochMillis,
                distanceMeters = progress.distanceMeters,
                activeDurationMillis = progress.activeDurationMillis,
                averageSpeedKmh = progress.averageSpeedKmh,
                elevationGainMeters = progress.elevationGainMeters,
                pointCount = progress.pointCount,
            )
        }
    }

    override suspend fun updateProgress(trackId: String, progress: TrackProgress) {
        database.withTransaction {
            trackDao.updateProgress(
                trackId = trackId,
                status = progress.status.name,
                finishedAt = progress.finishedAtEpochMillis,
                distanceMeters = progress.distanceMeters,
                activeDurationMillis = progress.activeDurationMillis,
                averageSpeedKmh = progress.averageSpeedKmh,
                elevationGainMeters = progress.elevationGainMeters,
                pointCount = progress.pointCount,
            )
        }
    }

    override suspend fun findResumable(): List<RecordedTrack> =
        database.withTransaction {
            trackDao.findResumable().map { entity ->
                entity.toDomain(trackDao.getPoints(entity.id))
            }
        }

    override suspend fun discard(trackId: String) {
        database.withTransaction {
            trackDao.deletePoints(trackId)
            trackDao.deleteTrack(trackId)
        }
    }

    private suspend fun insertPoints(
        trackId: String,
        points: List<TrackPoint>,
        startSequence: Int,
    ) {
        if (points.isEmpty()) return
        trackDao.insertPoints(
            points.mapIndexed { index, point -> point.toEntity(trackId, startSequence + index) },
        )
    }
}
