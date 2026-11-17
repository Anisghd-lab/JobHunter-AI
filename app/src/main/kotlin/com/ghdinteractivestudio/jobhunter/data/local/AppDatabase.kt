package com.ghdinteractivestudio.jobhunter.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [JobOfferEntity::class, UserProfileEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jobOfferDao(): JobOfferDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE job_offers ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE job_offers ADD COLUMN regionScope TEXT NOT NULL DEFAULT 'INTERNATIONAL'")
                db.execSQL("ALTER TABLE job_offers ADD COLUMN workMode TEXT NOT NULL DEFAULT 'REMOTE'")
                db.execSQL("ALTER TABLE job_offers ADD COLUMN applicationMethod TEXT NOT NULL DEFAULT 'WEBSITE'")
                db.execSQL("ALTER TABLE job_offers ADD COLUMN salary TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE job_offers ADD COLUMN publishedDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE job_offers ADD COLUMN aiVerdict TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE job_offers ADD COLUMN aiReasoning TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE job_offers ADD COLUMN appliedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE job_offers ADD COLUMN appliedChannel TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE job_offers ADD COLUMN publishedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE job_offers ADD COLUMN deadlineAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE job_offers ADD COLUMN deadlineLabel TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE job_offers ADD COLUMN followUpCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE job_offers ADD COLUMN lastFollowUpAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE job_offers ADD COLUMN lastReplySummary TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE job_offers ADD COLUMN autoApplied INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE job_offers ADD COLUMN lastAutoAttemptAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE job_offers ADD COLUMN lastReplyFrom TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE job_offers ADD COLUMN lastReplySubject TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE job_offers ADD COLUMN lastReplyAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE job_offers ADD COLUMN lastReplyMessageId TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "UPDATE job_offers SET recruiterEmail = '', applicationMethod = 'WEBSITE' WHERE lower(recruiterEmail) LIKE '%accommodations%' OR lower(recruiterEmail) LIKE '%accessibility%' OR lower(recruiterEmail) LIKE '%privacy%' OR lower(recruiterEmail) LIKE '%legal@%' OR lower(recruiterEmail) LIKE '%noreply%'"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jobhunter_database.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
