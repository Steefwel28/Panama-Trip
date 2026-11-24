package com.panamatrip.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.panamatrip.app.core.database.dao.CollectionDao
import com.panamatrip.app.core.database.dao.DestinationDao
import com.panamatrip.app.core.database.dao.GuideDao
import com.panamatrip.app.core.database.dao.PostDao
import com.panamatrip.app.core.database.dao.ReviewDao
import com.panamatrip.app.core.database.dao.RoutePlanDao
import com.panamatrip.app.core.database.dao.TrackDao
import com.panamatrip.app.core.database.dao.VehicleReportDao
import com.panamatrip.app.core.database.entity.CollectionEntity
import com.panamatrip.app.core.database.entity.CollectionPostEntity
import com.panamatrip.app.core.database.entity.CommentEntity
import com.panamatrip.app.core.database.entity.DestinationEntity
import com.panamatrip.app.core.database.entity.GuideEntity
import com.panamatrip.app.core.database.entity.PostEntity
import com.panamatrip.app.core.database.entity.PostLikeEntity
import com.panamatrip.app.core.database.entity.ReviewEntity
import com.panamatrip.app.core.database.entity.RoutePlanEntity
import com.panamatrip.app.core.database.entity.TrackEntity
import com.panamatrip.app.core.database.entity.TrackPointEntity
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
        RoutePlanEntity::class,
        GuideEntity::class,
        TrackEntity::class,
        TrackPointEntity::class,
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

    abstract fun routePlanDao(): RoutePlanDao

    abstract fun guideDao(): GuideDao

    abstract fun trackDao(): TrackDao

    companion object {
        const val VERSION = 4
        private const val DATABASE_NAME = "panama_trip.db"

        /**
         * v1 → v2: aparecen la planificación de ruta guardada y el directorio de guías.
         * Son tablas nuevas, así que no hay pérdida de datos del usuario.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `route_plans` (" +
                        "`id` TEXT NOT NULL, `destinationId` TEXT NOT NULL, " +
                        "`destinationName` TEXT NOT NULL, `fuelType` TEXT NOT NULL, " +
                        "`vehicleType` TEXT NOT NULL, `mountainSurcharge` INTEGER NOT NULL, " +
                        "`departureMinuteOfDay` INTEGER NOT NULL, `distanceMeters` INTEGER NOT NULL, " +
                        "`driveMinutes` INTEGER NOT NULL, `roundTripKilometers` REAL NOT NULL, " +
                        "`fuelLiters` REAL NOT NULL, `fuelCostCents` INTEGER NOT NULL, " +
                        "`tollCostCents` INTEGER NOT NULL, `cashOnlyCents` INTEGER NOT NULL, " +
                        "`totalCents` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_route_plans_destinationId` " +
                        "ON `route_plans` (`destinationId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_route_plans_createdAt` " +
                        "ON `route_plans` (`createdAt`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `guides` (" +
                        "`id` TEXT NOT NULL, `destinationId` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                        "`headline` TEXT NOT NULL, `languages` TEXT NOT NULL, " +
                        "`rateCents` INTEGER NOT NULL, `verified` INTEGER NOT NULL, " +
                        "`whatsappNumber` TEXT NOT NULL, PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_guides_destinationId` " +
                        "ON `guides` (`destinationId`)",
                )
            }
        }

        /**
         * v2 → v3: `posts` gana la foto verificada y su EXIF, y nacen los tracks GPS.
         * La columna `photoVerified` es NOT NULL sin default en el esquema v3, por lo
         * que se reconstruye la tabla copiando los datos (0 = no verificado) en vez
         * de usar ALTER TABLE con un DEFAULT que Room luego no reconocería.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `posts_new` (" +
                        "`id` TEXT NOT NULL, `destinationId` TEXT NOT NULL, " +
                        "`authorName` TEXT NOT NULL, `authorRole` TEXT NOT NULL, " +
                        "`authorInitials` TEXT NOT NULL, `caption` TEXT NOT NULL, " +
                        "`imageUrl` TEXT NOT NULL, `visitedYear` INTEGER NOT NULL, " +
                        "`visitedMonth` INTEGER NOT NULL, `vehicleUsed` TEXT NOT NULL, " +
                        "`walkDistanceMeters` INTEGER NOT NULL, `walkMinutes` INTEGER NOT NULL, " +
                        "`activityTag` TEXT, `baseLikeCount` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, `photoVerified` INTEGER NOT NULL, " +
                        "`exifCapturedAt` INTEGER, PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "INSERT INTO `posts_new` (`id`, `destinationId`, `authorName`, `authorRole`, " +
                        "`authorInitials`, `caption`, `imageUrl`, `visitedYear`, `visitedMonth`, " +
                        "`vehicleUsed`, `walkDistanceMeters`, `walkMinutes`, `activityTag`, " +
                        "`baseLikeCount`, `createdAt`, `photoVerified`, `exifCapturedAt`) " +
                        "SELECT `id`, `destinationId`, `authorName`, `authorRole`, " +
                        "`authorInitials`, `caption`, `imageUrl`, `visitedYear`, `visitedMonth`, " +
                        "`vehicleUsed`, `walkDistanceMeters`, `walkMinutes`, `activityTag`, " +
                        "`baseLikeCount`, `createdAt`, 0, NULL FROM `posts`",
                )
                db.execSQL("DROP TABLE `posts`")
                db.execSQL("ALTER TABLE `posts_new` RENAME TO `posts`")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_posts_destinationId` " +
                        "ON `posts` (`destinationId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_posts_createdAt` " +
                        "ON `posts` (`createdAt`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tracks` (" +
                        "`id` TEXT NOT NULL, `destinationId` TEXT, `destinationName` TEXT, " +
                        "`startedAt` INTEGER NOT NULL, `finishedAt` INTEGER NOT NULL, " +
                        "`distanceMeters` INTEGER NOT NULL, `activeDurationMillis` INTEGER NOT NULL, " +
                        "`averageSpeedKmh` REAL NOT NULL, `elevationGainMeters` INTEGER NOT NULL, " +
                        "`pointCount` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_tracks_destinationId` " +
                        "ON `tracks` (`destinationId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_tracks_startedAt` " +
                        "ON `tracks` (`startedAt`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `track_points` (" +
                        "`trackId` TEXT NOT NULL, `sequence` INTEGER NOT NULL, " +
                        "`latitude` REAL NOT NULL, `longitude` REAL NOT NULL, " +
                        "`elevationMeters` REAL, `accuracyMeters` REAL, " +
                        "`recordedAt` INTEGER NOT NULL, `activeElapsedMillis` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`trackId`, `sequence`))",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_track_points_trackId` " +
                        "ON `track_points` (`trackId`)",
                )
            }
        }

        /**
         * v3 → v4: estado del recorrido para recuperar tracks huérfanos y el índice
         * de segmento en cada punto. Ambos tienen default, así que el ALTER no
         * pierde filas existentes.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE tracks ADD COLUMN status TEXT NOT NULL DEFAULT 'COMPLETED'",
                )
                db.execSQL(
                    "ALTER TABLE track_points ADD COLUMN segmentIndex INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        fun build(context: Context): PanamaTripDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                PanamaTripDatabase::class.java,
                DATABASE_NAME,
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
    }
}
