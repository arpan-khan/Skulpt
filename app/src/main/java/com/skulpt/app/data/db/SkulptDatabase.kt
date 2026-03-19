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
    version = 5,
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
                    .addMigrations(MIGRATION_4_5)
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

            // Insert default settings
            settingsDao.upsertSettings(AppSettings())

            // Insert one default workout day
            val dayId = dayDao.insertDay(
                WorkoutDay(dayIndex = 0, name = "My Workout", colorHex = "#6750A4")
            )

            // Seed it with sample exercises
            val sampleExercises = listOf(
                Exercise(dayId = dayId, name = "Pushups", sets = 3, reps = 15, orderIndex = 0),
                Exercise(dayId = dayId, name = "Pullups", sets = 3, reps = 8, orderIndex = 1),
                Exercise(dayId = dayId, name = "Squats", sets = 4, reps = 12, orderIndex = 2)
            )
            exerciseDao.insertExercises(sampleExercises)
        }
    }
}
