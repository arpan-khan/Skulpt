package com.skulpt.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.skulpt.app.data.dao.AppSettingsDao
import com.skulpt.app.data.dao.ExerciseDao
import com.skulpt.app.data.dao.WorkoutDayDao
import com.skulpt.app.data.dao.WorkoutSessionDao
import com.skulpt.app.data.model.AppSettings
import com.skulpt.app.data.model.Exercise
import com.skulpt.app.data.model.WorkoutDay
import com.skulpt.app.data.model.WorkoutSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [WorkoutDay::class, Exercise::class, WorkoutSession::class, AppSettings::class],
    version = 6,
    exportSchema = false
)
abstract class SkulptDatabase : RoomDatabase() {

    abstract fun workoutDayDao(): WorkoutDayDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: SkulptDatabase? = null

        fun getInstance(context: Context): SkulptDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SkulptDatabase::class.java,
                    "skulpt_database"
                )
                    .addCallback(DatabaseCallback())
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN hexcolor TEXT NOT NULL DEFAULT '#6750A4'")
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {

                try { db.execSQL("ALTER TABLE exercises ADD COLUMN completedSets INTEGER NOT NULL DEFAULT 0") } catch(e: Exception){}
                try { db.execSQL("ALTER TABLE exercises ADD COLUMN lastTrackedSets INTEGER NOT NULL DEFAULT 0") } catch(e: Exception){}
                try { db.execSQL("ALTER TABLE exercises ADD COLUMN timerSeconds INTEGER NOT NULL DEFAULT 0") } catch(e: Exception){}
                try { db.execSQL("ALTER TABLE exercises ADD COLUMN notes TEXT NOT NULL DEFAULT ''") } catch(e: Exception){}

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `workout_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `dayId` INTEGER NOT NULL,
                        `dayName` TEXT NOT NULL,
                        `dateMillis` INTEGER NOT NULL,
                        `totalExercises` INTEGER NOT NULL,
                        `completedExercises` INTEGER NOT NULL,
                        `totalSets` INTEGER NOT NULL DEFAULT 0,
                        `completedSets` INTEGER NOT NULL DEFAULT 0,
                        `totalReps` INTEGER NOT NULL DEFAULT 0,
                        `completedReps` INTEGER NOT NULL DEFAULT 0,
                        `durationSeconds` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateDefaultData(database)
                }
            }
        }

        suspend fun populateDefaultData(database: SkulptDatabase) {
            val dayDao = database.workoutDayDao()
            val exerciseDao = database.exerciseDao()
            val settingsDao = database.appSettingsDao()

            settingsDao.upsertSettings(AppSettings())

            val dayId = dayDao.insertDay(
                WorkoutDay(dayIndex = 0, name = "My Workout", colorHex = "#6750A4")
            )

            val sampleExercises = listOf(
                Exercise(dayId = dayId, name = "Pushups", sets = 3, reps = 15, orderIndex = 0),
                Exercise(dayId = dayId, name = "Pullups", sets = 3, reps = 8, orderIndex = 1),
                Exercise(dayId = dayId, name = "Squats", sets = 4, reps = 12, orderIndex = 2)
            )
            exerciseDao.insertExercises(sampleExercises)
        }
    }
}
