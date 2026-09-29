package org.umn.jalankita.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ReportEntity::class, NotificationEntity::class, ReporterEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun reportDao(): ReportDao

    abstract fun notificationDao(): NotificationDao

    abstract fun reporterDao(): ReporterDao

    companion object {
        private const val DATABASE_NAME = "jalankita.db"

        /**
         * Adds reporter identity and track records.
         *
         * Written out rather than falling back to a destructive migration: this
         * database is the only copy of a report that has not reached the server,
         * so an upgrade must never drop it.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reports ADD COLUMN reporterName TEXT")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS reporters (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        joinedAt INTEGER NOT NULL,
                        totalReports INTEGER NOT NULL DEFAULT 0,
                        confirmedReports INTEGER NOT NULL DEFAULT 0,
                        completedReports INTEGER NOT NULL DEFAULT 0,
                        needsInfoReports INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent(),
                )
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME,
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
    }
}
