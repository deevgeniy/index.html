package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// 1. User Stats & Settings Entity
@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val id: Int = 1,
    val weight: Double = 75.0,
    val goal: String = "mass", // "mass" (набор) or "loss" (сушка)
    val activity: String = "medium", // "low", "medium", "high"
    val rirEnabled: Boolean = false
)

// 2. Workout Log Header
@Entity(tableName = "workout_logs")
data class WorkoutLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateMillis: Long,
    val programDayName: String,
    val totalVolume: Double,
    val durationMinutes: Int
)

// 3. Recorded Set Metrics
@Entity(tableName = "exercise_set_logs")
data class ExerciseSetLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutLogId: Long, // links to WorkoutLog.id
    val dateMillis: Long,
    val exerciseName: String,
    val setIndex: Int,
    val weight: Double,
    val reps: Int,
    val rir: Int? = null
)

// 4. Daily Nutrition Meal Diary Log
@Entity(tableName = "nutrition_logs")
data class NutritionLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateString: String, // format "YYYY-MM-DD"
    val mealType: String, // "Завтрак", "Обед", "Перед тренировкой", "После тренировкой", "Ужин", "Перед сном"
    val productName: String,
    val weightGrams: Double,
    val calories: Double,
    val proteins: Double,
    val fats: Double,
    val carbs: Double
)

// 5. User Body Weight Logs (Dynamic tracking over time)
@Entity(tableName = "weight_logs")
data class WeightLog(
    @PrimaryKey val dateString: String, // format "yyyy-MM-dd"
    val dateMillis: Long,
    val weight: Double
)

// 6. Exercise Details Library Entity
@Entity(tableName = "exercise_info")
data class ExerciseInfo(
    @PrimaryKey val name: String,
    val technique: String = "",
    val photoUri: String? = null, // URI chosen by user from gallery
    val category: String = "Грудь", // e.g. "Грудь", "Спина", "Ноги", "Плечи", "Руки", "Пресс", "Другое"
    val isCustom: Boolean = false
)

// --- DAOs ---

@Dao
interface UserStatsDao {
    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    fun getUserStatsFlow(): Flow<UserStats?>

    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    suspend fun getUserStats(): UserStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: UserStats)
}

@Dao
interface WorkoutLogDao {
    @Query("SELECT * FROM workout_logs ORDER BY dateMillis DESC")
    fun getAllWorkoutLogsFlow(): Flow<List<WorkoutLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutLog(log: WorkoutLog): Long

    @Query("DELETE FROM workout_logs")
    suspend fun clearAll()
}

@Dao
interface ExerciseSetLogDao {
    @Query("SELECT * FROM exercise_set_logs ORDER BY dateMillis DESC")
    fun getAllSetLogsFlow(): Flow<List<ExerciseSetLog>>

    @Query("SELECT * FROM exercise_set_logs WHERE workoutLogId = :workoutId ORDER BY id ASC")
    suspend fun getSetsForWorkout(workoutId: Long): List<ExerciseSetLog>

    @Query("SELECT * FROM exercise_set_logs WHERE exerciseName = :exerciseName ORDER BY dateMillis DESC")
    fun getHistoryForExerciseFlow(exerciseName: String): Flow<List<ExerciseSetLog>>

    @Query("SELECT * FROM exercise_set_logs WHERE exerciseName = :exerciseName ORDER BY dateMillis DESC")
    suspend fun getHistoryForExercise(exerciseName: String): List<ExerciseSetLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetLog(vararg logs: ExerciseSetLog)

    @Query("DELETE FROM exercise_set_logs WHERE workoutLogId = :workoutId")
    suspend fun deleteSetsForWorkout(workoutId: Long)
}

@Dao
interface NutritionLogDao {
    @Query("SELECT * FROM nutrition_logs WHERE dateString = :date ORDER BY id ASC")
    fun getNutritionLogsForDate(date: String): Flow<List<NutritionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: NutritionLog)

    @Delete
    suspend fun deleteFood(food: NutritionLog)

    @Query("DELETE FROM nutrition_logs WHERE dateString = :date")
    suspend fun clearNutritionForDate(date: String)
}

@Dao
interface WeightLogDao {
    @Query("SELECT * FROM weight_logs ORDER BY dateMillis ASC")
    fun getAllWeightLogsFlow(): Flow<List<WeightLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateWeight(weightLog: WeightLog)

    @Query("DELETE FROM weight_logs")
    suspend fun clearAll()
}

@Dao
interface ExerciseInfoDao {
    @Query("SELECT * FROM exercise_info ORDER BY name ASC")
    fun getAllExerciseInfoFlow(): Flow<List<ExerciseInfo>>

    @Query("SELECT * FROM exercise_info WHERE name = :name LIMIT 1")
    suspend fun getExerciseInfo(name: String): ExerciseInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateExercise(exercise: ExerciseInfo)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllExercises(exercises: List<ExerciseInfo>)

    @Query("DELETE FROM exercise_info WHERE name = :name")
    suspend fun deleteExercise(name: String)
}

// --- AppDatabase class ---

@Database(
    entities = [UserStats::class, WorkoutLog::class, ExerciseSetLog::class, NutritionLog::class, WeightLog::class, ExerciseInfo::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userStatsDao(): UserStatsDao
    abstract fun workoutLogDao(): WorkoutLogDao
    abstract fun exerciseSetLogDao(): ExerciseSetLogDao
    abstract fun nutritionLogDao(): NutritionLogDao
    abstract fun weightLogDao(): WeightLogDao
    abstract fun exerciseInfoDao(): ExerciseInfoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gym_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
