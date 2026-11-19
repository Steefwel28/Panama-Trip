package com.panamatrip.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.panamatrip.app.core.database.dao.CollectionDao
import com.panamatrip.app.core.database.dao.DestinationDao
import com.panamatrip.app.core.database.dao.PostDao
import com.panamatrip.app.core.database.dao.ReviewDao
import com.panamatrip.app.core.database.dao.VehicleReportDao
import com.panamatrip.app.core.database.entity.CollectionEntity
import com.panamatrip.app.core.database.entity.CollectionPostEntity
import com.panamatrip.app.core.database.entity.CommentEntity
import com.panamatrip.app.core.database.entity.DestinationEntity
import com.panamatrip.app.core.database.entity.PostEntity
import com.panamatrip.app.core.database.entity.PostLikeEntity
import com.panamatrip.app.core.database.entity.ReviewEntity
import com.panamatrip.app.core.database.entity.VehicleReportEntity

@Database(
    entities = [
        DestinationEntity::class,
        VehicleReportEntity::class,
        PostEntity::class,
        PostLikeEntity::class,
        CommentEntity::class,
        CollectionEntity::class,
        CollectionPostEntity::class,
        ReviewEntity::class,
    ],
    version = PanamaTripDatabase.VERSION,
    exportSchema = true,
)
abstract class PanamaTripDatabase : RoomDatabase() {

    abstract fun destinationDao(): DestinationDao

    abstract fun vehicleReportDao(): VehicleReportDao

    abstract fun postDao(): PostDao

    abstract fun collectionDao(): CollectionDao

    abstract fun reviewDao(): ReviewDao

    companion object {
        const val VERSION = 1
        private const val DATABASE_NAME = "panama_trip.db"

        fun build(context: Context): PanamaTripDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                PanamaTripDatabase::class.java,
                DATABASE_NAME,
            )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
