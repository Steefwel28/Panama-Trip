package com.panamatrip.app.data.repository

import com.panamatrip.app.core.database.dao.TrackDao
import com.panamatrip.app.data.mapper.toDomain
import com.panamatrip.app.data.mapper.toEntity
import com.panamatrip.app.domain.model.RecordedTrack
import com.panamatrip.app.domain.model.TrackPoint
import com.panamatrip.app.domain.repository.TrackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TrackRepositoryImpl(
    private val trackDao: TrackDao,
) : TrackRepository {

    override fun observeTracks(): Flow<List<RecordedTrack>> =
        trackDao.observeAll().map { tracks -> tracks.map { it.toDomain() } }

    override fun observeTrackPoints(trackId: String): Flow<List<TrackPoint>> =
        trackDao.observePoints(trackId).map { points -> points.map { it.toDomain() } }

    override suspend fun save(track: RecordedTrack) {
        trackDao.insertTrack(track.toEntity())
        if (track.points.isNotEmpty()) {
            trackDao.insertPoints(
                track.points.mapIndexed { index, point -> point.toEntity(track.id, index) },
            )
        }
    }
}
