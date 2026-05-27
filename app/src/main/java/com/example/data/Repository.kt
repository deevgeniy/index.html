package com.example.data

import kotlinx.coroutines.flow.Flow

class GymRepository(private val database: AppDatabase) {
    val userStats: Flow<UserStats?> = database.userStatsDao().getUserStatsFlow()
    val allWorkoutLogs: Flow<List<WorkoutLog>> = database.workoutLogDao().getAllWorkoutLogsFlow()
    val allSetLogs: Flow<List<ExerciseSetLog>> = database.exerciseSetLogDao().getAllSetLogsFlow()
    val allWeightLogs: Flow<List<WeightLog>> = database.weightLogDao().getAllWeightLogsFlow()
    val allExerciseInfo: Flow<List<ExerciseInfo>> = database.exerciseInfoDao().getAllExerciseInfoFlow()
    val allProgressPhotos: Flow<List<ProgressPhoto>> = database.progressPhotoDao().getAllProgressPhotosFlow()

    suspend fun insertProgressPhoto(photo: ProgressPhoto) {
        database.progressPhotoDao().insertProgressPhoto(photo)
    }

    suspend fun deleteProgressPhoto(photo: ProgressPhoto) {
        database.progressPhotoDao().deleteProgressPhoto(photo)
    }

    suspend fun insertOrUpdateExercise(exercise: ExerciseInfo) {
        database.exerciseInfoDao().insertOrUpdateExercise(exercise)
    }

    suspend fun insertAllExercises(exercises: List<ExerciseInfo>) {
        database.exerciseInfoDao().insertAllExercises(exercises)
    }

    suspend fun getExerciseInfo(name: String): ExerciseInfo? {
        return database.exerciseInfoDao().getExerciseInfo(name)
    }

    suspend fun deleteExercise(name: String) {
        database.exerciseInfoDao().deleteExercise(name)
    }

    suspend fun saveWeightLog(weightLog: WeightLog) {
        database.weightLogDao().insertOrUpdateWeight(weightLog)
    }

    suspend fun getUserStatsDirect(): UserStats {
        return database.userStatsDao().getUserStats() ?: UserStats().also {
            database.userStatsDao().insertOrUpdate(it)
        }
    }

    suspend fun saveUserStats(stats: UserStats) {
        database.userStatsDao().insertOrUpdate(stats)
    }

    suspend fun saveWorkout(log: WorkoutLog, sets: List<ExerciseSetLog>) {
        val workoutId = database.workoutLogDao().insertWorkoutLog(log)
        val linkedSets = sets.map { it.copy(workoutLogId = workoutId) }
        database.exerciseSetLogDao().insertSetLog(*linkedSets.toTypedArray())
    }

    fun getExerciseHistory(exerciseName: String): Flow<List<ExerciseSetLog>> {
        return database.exerciseSetLogDao().getHistoryForExerciseFlow(exerciseName)
    }

    suspend fun getExerciseHistoryDirect(exerciseName: String): List<ExerciseSetLog> {
        return database.exerciseSetLogDao().getHistoryForExercise(exerciseName)
    }

    fun getNutritionForDate(dateString: String): Flow<List<NutritionLog>> {
        return database.nutritionLogDao().getNutritionLogsForDate(dateString)
    }

    suspend fun addNutritionLog(log: NutritionLog) {
        database.nutritionLogDao().insertFood(log)
    }

    suspend fun deleteNutritionLog(log: NutritionLog) {
        database.nutritionLogDao().deleteFood(log)
    }

    suspend fun clearNutritionForDate(dateString: String) {
        database.nutritionLogDao().clearNutritionForDate(dateString)
    }
}
