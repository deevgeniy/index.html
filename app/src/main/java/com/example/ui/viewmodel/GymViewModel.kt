package com.example.ui.viewmodel

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Representation of products in database
data class FoodPreset(
    val name: String,
    val proteins: Double, // per 100g
    val fats: Double,     // per 100g
    val carbs: Double,    // per 100g
    val calories: Double  // per 100g
)

// Active performance models
data class WorkoutSetState(
    val setIndex: Int,
    val weight: String = "",
    val reps: String = "",
    val rir: String = "",
    val isDone: Boolean = false
)

data class ActiveExerciseState(
    val name: String,
    val targetSets: Int,
    val targetReps: String,
    val defaultRestSec: Int,
    val technique: String,
    val sets: List<WorkoutSetState>
)

data class WorkoutDayPreset(
    val dayName: String,
    val description: String,
    val exercises: List<PredefinedExercise>
)

data class PredefinedExercise(
    val name: String,
    val sets: Int,
    val reps: String,
    val restMin: Double,
    val technique: String
)

class GymViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: GymRepository
    
    // UI Screen state
    val userStats: StateFlow<UserStats>
    val workoutLogs: StateFlow<List<WorkoutLog>>
    val allSetLogs: StateFlow<List<ExerciseSetLog>>
    val allWeightLogs: StateFlow<List<WeightLog>>
    val allExerciseInfo: StateFlow<List<ExerciseInfo>>
    
    // Active date string: YYYY-MM-DD
    private val _selectedDate = MutableStateFlow("")
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()
    
    // Nutrition logs for active date
    val todayNutrition: StateFlow<List<NutritionLog>>

    // Food presets
    val foodPresets = listOf(
        FoodPreset("Куриное филе (сырое)", 23.6, 1.9, 0.0, 113.0),
        FoodPreset("Куриное бедро (без кожи)", 20.0, 5.6, 0.0, 130.0),
        FoodPreset("Филе индейки", 21.6, 3.2, 0.0, 115.0),
        FoodPreset("Говядина постная", 22.0, 7.0, 0.0, 158.0),
        FoodPreset("Свинина постная", 20.0, 8.0, 0.0, 160.0),
        FoodPreset("Минтай филе", 16.0, 0.9, 0.0, 72.0),
        FoodPreset("Горбуша филе", 20.0, 6.0, 0.0, 140.0),
        FoodPreset("Тунец консервированный", 24.0, 1.0, 0.0, 105.0),
        FoodPreset("Яйцо куриное (целое)", 13.0, 11.0, 1.1, 155.0),
        FoodPreset("Белок яичный", 11.0, 0.2, 0.7, 48.0),
        FoodPreset("Творог 5%", 17.2, 5.0, 1.8, 121.0),
        FoodPreset("Творог 9%", 16.0, 9.0, 2.0, 157.0),
        FoodPreset("Творог обезжиренный", 18.0, 0.5, 3.3, 90.0),
        FoodPreset("Гречка (крупа сухая)", 12.6, 3.3, 64.2, 343.0),
        FoodPreset("Рис белый (сухой)", 6.7, 0.7, 78.9, 345.0),
        FoodPreset("Рис бурый (сухой)", 7.5, 1.8, 72.0, 330.0),
        FoodPreset("Овсяные хлопья (сухие)", 13.0, 6.5, 68.0, 379.0),
        FoodPreset("Макароны тв. сортов", 12.5, 1.5, 71.0, 350.0),
        FoodPreset("Перловка (сухая)", 9.3, 1.1, 73.0, 340.0),
        FoodPreset("Картофель запеченный", 2.0, 0.1, 19.0, 82.0),
        FoodPreset("Батат (сладкий картофель)", 2.0, 0.1, 20.0, 86.0),
        FoodPreset("Банан", 1.5, 0.1, 21.8, 89.0),
        FoodPreset("Яблоко", 0.3, 0.2, 14.0, 52.0),
        FoodPreset("Груша", 0.4, 0.3, 11.0, 47.0),
        FoodPreset("Апельсин", 0.9, 0.2, 8.0, 38.0),
        FoodPreset("Огурец свежий", 0.8, 0.1, 3.0, 15.0),
        FoodPreset("Помидор свежий", 1.1, 0.2, 3.8, 20.0),
        FoodPreset("Капуста белокочанная", 1.8, 0.1, 4.7, 27.0),
        FoodPreset("Квашеная капуста", 0.9, 0.1, 4.3, 19.0),
        FoodPreset("Салат листовой", 1.2, 0.3, 1.3, 12.0),
        FoodPreset("Овощи тушеные", 1.5, 2.0, 7.0, 50.0),
        FoodPreset("Брокколи свежая", 3.0, 0.4, 5.2, 34.0),
        FoodPreset("Масло оливковое", 0.0, 100.0, 0.0, 884.0),
        FoodPreset("Масло подсолнечное", 0.0, 100.0, 0.0, 884.0),
        FoodPreset("Масло сливочное 82.5%", 0.8, 82.5, 0.8, 717.0),
        FoodPreset("Арахисовая паста", 25.0, 50.0, 20.0, 588.0),
        FoodPreset("Орехи грецкие", 15.0, 65.0, 7.0, 654.0),
        FoodPreset("Миндаль", 21.0, 50.0, 13.0, 579.0),
        FoodPreset("Арахис", 26.0, 45.0, 10.0, 550.0),
        FoodPreset("Кешью", 18.0, 48.0, 22.0, 553.0),
        FoodPreset("Мед пчелиный", 0.3, 0.0, 82.4, 304.0),
        FoodPreset("Финики сушеные", 2.5, 0.5, 69.0, 292.0),
        FoodPreset("Изюм", 2.9, 0.6, 66.0, 264.0),
        FoodPreset("Сывороточный протеин", 80.0, 5.0, 10.0, 400.0),
        FoodPreset("Сметана 10%", 3.0, 10.0, 2.9, 115.0),
        FoodPreset("Сметана 15%", 2.6, 15.0, 3.0, 160.0),
        FoodPreset("Кефир 1%", 3.0, 1.0, 4.0, 37.0),
        FoodPreset("Кефир 3.2%", 3.0, 3.2, 4.0, 56.0),
        FoodPreset("Молоко 2.5%", 2.8, 2.5, 4.7, 52.0),
        FoodPreset("Сок вишневый", 0.5, 0.0, 12.0, 50.0)
    )

    // --- Active Workout State ---
    private val _selectedDayIndex = MutableStateFlow(0) // 0: Day 1, 1: Day 2, 2: Day 3
    val selectedDayIndex = _selectedDayIndex.asStateFlow()

    private val _isWorkoutActive = MutableStateFlow(false)
    val isWorkoutActive = _isWorkoutActive.asStateFlow()

    private val _activeExercises = MutableStateFlow<List<ActiveExerciseState>>(emptyList())
    val activeExercises = _activeExercises.asStateFlow()

    private val _currentExerciseIndex = MutableStateFlow(0)
    val currentExerciseIndex = _currentExerciseIndex.asStateFlow()

    // Dynamic timer inside workout
    private val _workoutDurationSeconds = MutableStateFlow(0)
    val workoutDurationSeconds = _workoutDurationSeconds.asStateFlow()
    private var workoutTimerJob: Job? = null

    // Rest countdown timer state
    private val _restTimeRemaining = MutableStateFlow(0)
    val restTimeRemaining = _restTimeRemaining.asStateFlow()

    private val _restTimeTarget = MutableStateFlow(120)
    val restTimeTarget = _restTimeTarget.asStateFlow()

    private val _restTimerRunning = MutableStateFlow(false)
    val restTimerRunning = _restTimerRunning.asStateFlow()
    private var restTimerJob: Job? = null
    private var expectedTimerEndMillis: Long = 0L

    // For historical sets display on current active exercise
    private val _activeExerciseHistory = MutableStateFlow<List<ExerciseSetLog>>(emptyList())
    val activeExerciseHistory = _activeExerciseHistory.asStateFlow()

    // Preloaded program days
    val programDays = listOf(
        WorkoutDayPreset(
            dayName = "День 1. ЖИМОВЫЙ",
            description = "Грудь, плечи, трицепс",
            exercises = listOf(
                PredefinedExercise("Жим в рычажном тренажере или Смите", 4, "8-10", 2.5, "Сядьте в тренажер, спина и затылок плотно прижаты к опорам. Хват на ширине плеч или чуть шире. На выдохе выжмите вес вперед, не отрывая таз и спину от сиденья. На вдохе плавно верните в исходное положение, чувствуя растяжение грудных мышц."),
                PredefinedExercise("Жим гантелей лежа под углом 30°", 3, "10-12", 2.0, "Установите скамью под углом 30°. Лягте, лопатки сведены, поясница сохраняет естественный прогиб. Гантели на согнутых руках над грудью. На вдохе опустите гантели к верху груди (ключицам), локти под углом 45–60° к корпусу. На выдохе выжмите вверх, сводя гантели в верхней точке."),
                PredefinedExercise("Сведение рук в кроссовере (верхние блоки)", 3, "12-15", 2.0, "Встаньте между двумя верхними блоками, ноги на ширине плеч. Корпус чуть наклонен вперед, локти слегка согнуты и зафиксированы. На выдохе сведите руки перед собой, сводя грудные мышцы. На вдохе плавно вернитесь в исходное положение, растягивая грудь."),
                PredefinedExercise("Жим гантелей сидя (ладони друг к другу)", 4, "8-10", 2.0, "Сядьте на скамью с вертикальной спинкой, спина и поясница плотно прижаты. Гантели у плеч, ладони обращены друг к другу (нейтральный хват). На выдохе выжмите гантели вверх по дуге, не выпрямляя локти до конца. На вдохе опустите до уровня ушей — не ниже."),
                PredefinedExercise("Разведение гантелей в стороны стоя", 3, "12-15", 1.5, "Встаньте прямо, корпус слегка наклонен вперед (разгрузка поясницы). Гантели в опущенных руках, локти чуть согнуты. На выдохе поднимите руки через стороны до уровня плеч (не выше). Кисти чуть наклонены вперед. На вдохе плавно опустите. Без рывков и помощи корпусом."),
                PredefinedExercise("Французский жим с гантелью сидя", 3, "10-12", 1.5, "Сядьте на скамью с опорой спины. Возьмите гантель в руки, поднимите над головой, локоть смотрит в потолок, предплечье вертикально. На вдохе согните руку, опуская гантель за голову (до комфортной глубины). На выдохе разогните руки, не выпрямляя локоть до щелчка."),
                PredefinedExercise("Разгибание рук на блоке (канат)", 3, "12-15", 1.0, "Встаньте лицом к верхнему блоку, возьмите канат. Корпус чуть наклонен, локти прижаты к корпусу под 90°. На выдохе разогните руки вниз, в нижней точке разворачивайте кисти (ладони вниз) для пикового сокращения. На вдохе вернитесь под контролем. Локти неподвижны.")
            )
        ),
        WorkoutDayPreset(
            dayName = "День 2. ТЯГОВЫЙ",
            description = "Спина, бицепс, брахиалис, задняя дельта",
            exercises = listOf(
                PredefinedExercise("Тяга штанги в наклоне", 4, "8-10", 2.5, "Возьмите штангу хватом чуть шире плеч. Наклоните корпус вперед на 45–60° (не ниже). Спина жестко прямая, поясница нейтральна. Колени чуть согнуты. На выдохе тяните штангу к нижней части живота, сводя лопатки. Локти идут вдоль корпуса. На вдохе плавно верните, не округляя спину."),
                PredefinedExercise("Тяга верхнего блока к груди широким хватом", 4, "10-12", 2.0, "Сядьте на тренажер, бедра зафиксированы. Хват широкий (на изгибах грифа). На выдохе тяните гриф к ключицам, одновременно отклоняя корпус назад на 10–15°. Локти тянутся вниз и назад. Не заводите гриф за голову. На вдохе плавно верните, растягивая широчайшие."),
                PredefinedExercise("Тяга гантели к поясу (с упором в скамью)", 3, "10-12", 1.75, "Упритесь одной рукой и коленом в горизонтальную скамью. Спина параллельна полу, поясница прямая. Гантель в опущенной руке. На выдохе тяните гантель к поясу (к тазу), локоть идет вверх и назад. В верхней точке сведите лопатку. На вдохе опустите под контролем, растягивая мышцу."),
                PredefinedExercise("Тяга нижнего блока к поясу (узкий хват)", 3, "10-12", 1.75, "Сядьте на тренажер, ноги в упор. Хват узкий (ладони друг к другу). На выдохе тяните рукоять к поясу, отклоняя корпус назад на 10–15°. Локти скользят вдоль корпуса. В конце амплитуды сведите лопатки. На вдохе плавно верните, не округляя спину."),
                PredefinedExercise("Разведение гантелей в наклоне (задняя дельта)", 3, "12-15", 1.5, "Наклоните корпус вперед до параллели с полом (или используйте наклонную скамью грудью вниз). Гантели в опущенных руках, локти чуть согнуты. На выдохе разведите руки в стороны до уровня плеч (не выше), локти смотрят в потолок. На вдохе плавно опустите. Вес легкий, работайте задней дельтой."),
                PredefinedExercise("Сгибание рук со штангой EZ стоя", 3, "10-12", 1.5, "Встаньте прямо, возьмите EZ-гриф обратным хватом на ширине плеч. Локти прижаты к корпусу. На выдохе согните руки, поднимая гриф к плечам, не отрывая локтей. На вдохе плавно опустите, не выпрямляя руки до конца (сохраняйте напряжение)."),
                PredefinedExercise("Сгибание рук с гантелями (молотки) стоя", 3, "10-12", 1.5, "Гантели в опущенных руках, ладони обращены друг к другу (нейтральный хват). Локти прижаты к корпусу. На выдохе согните руки, поднимая гантели к плечам. На вдохе опустите под контролем."),
                PredefinedExercise("Сгибание рук с канатом на нижнем блоке (обратный хват)", 3, "12-15", 1.5, "Встаньте лицом к нижнему блоку, возьмите канат обратным хватом (ладони вниз). Локти прижаты к корпусу. На выдохе согните руки, поднимая канат к плечам. На вдохе плавно опустите.")
            )
        ),
        WorkoutDayPreset(
            dayName = "День 3. НОГИ + КОР",
            description = "Квадрицепс, бицепс бедра, пресс, кор",
            exercises = listOf(
                PredefinedExercise("Жим ногами платформы", 4, "10-12", 2.5, "Сядьте в тренажер, поясница плотно прижата к спинке. Ноги поставьте на середину платформы на ширине плеч. На вдохе опустите платформу под контролем до угла 90° в коленях. На выдохе выжмите вверх, не выпрямляя колени до конца."),
                PredefinedExercise("Приседания со штангой или гантелью перед собой (Goblet squat)", 4, "10-12", 2.5, "Возьмите одну гантель вертикально за верхний диск, прижмите к груди. Ноги на ширине плеч, носки чуть в стороны. На вдохе приседайте, отводя таз назад, спина прямая, грудная клетка поднята. Глубина — до параллели бедра с полом. На выдохе мощно встаньте, опираясь на пятки."),
                PredefinedExercise("Румынская тяга с гантелями", 4, "10-12", 2.5, "Возьмите гантели в опущенные руки. Ноги на ширине таза, колени чуть согнуты. Спина жестко прямая. На вдохе отведите таз назад и опустите корпус вперед, скользя гантелями вдоль голеней. Чувствуете растяжение бицепса бедра. На выдохе вернитесь вверх, сжимая ягодицы. Не округляйте поясницу."),
                PredefinedExercise("Сгибание ног лежа", 3, "12-15", 1.75, "Лягте на тренажер лицом вниз, валики на задней поверхности голеней (под пятками). На выдохе согните ноги, приводя пятки к ягодицам. На вдохе плавно верните под контролем, не бросайте вес."),
                PredefinedExercise("Разгибание ног сидя", 3, "12-15", 1.75, "Сядьте в тренажер, валики на передней поверхности голеней. На выдохе разогните ноги, не выпрямляя колени до щелчка. На вдохе опустите под контролем."),
                PredefinedExercise("Скручивания на римском стуле", 3, "15-20", 1.5, "Римский стул: сядьте, зафиксируйте ноги. Руки за головой или скрещены на груди. На выдохе скручивайте корпус, поднимая грудную клетку к тазу. Поясница прижата. На вдохе плавно опуститесь."),
                PredefinedExercise("Гиперэкстензия с округлением спины", 3, "15-20", 1.5, "Лягте на тренажер для гиперэкстензии бедрами на подушках. На вдохе опуститесь вниз, округлив спину (этого безопасно и разгружает межпозвонковые диски). На выдохе поднимитесь вверх до прямой линии корпуса — без переразгибания в пояснице. Сжимайте ягодицы в верхней точке.")
            )
        )
    )

    init {
        val database = AppDatabase.getDatabase(application)
        repository = GymRepository(database)

        // Setup Date
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        _selectedDate.value = sdf.format(Date())

        // Collect flows
        userStats = repository.userStats
            .map { it ?: UserStats(id = 1) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, UserStats(id = 1))

        workoutLogs = repository.allWorkoutLogs
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allSetLogs = repository.allSetLogs
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allWeightLogs = repository.allWeightLogs
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allExerciseInfo = repository.allExerciseInfo
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        todayNutrition = _selectedDate.flatMapLatest { date ->
            repository.getNutritionForDate(date)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Initialize UserStats state in Db if empty
        viewModelScope.launch {
            val stats = repository.getUserStatsDirect()
            if (repository.allWeightLogs.first().isEmpty()) {
                val cal = Calendar.getInstance()
                val sdfFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val baseWeight = stats.weight
                
                val points = listOf(
                    Pair(-21, baseWeight - 1.8),
                    Pair(-18, baseWeight - 1.5),
                    Pair(-14, baseWeight - 1.0),
                    Pair(-11, baseWeight - 1.2),
                    Pair(-7, baseWeight - 0.7),
                    Pair(-4, baseWeight - 0.4),
                    Pair(0, baseWeight)
                )
                
                points.forEach { (daysOffset, weightVal) ->
                    val tempCal = cal.clone() as Calendar
                    tempCal.add(Calendar.DAY_OF_YEAR, daysOffset)
                    val dStr = sdfFormat.format(tempCal.time)
                    val dMillis = tempCal.timeInMillis
                    repository.saveWeightLog(WeightLog(dateString = dStr, dateMillis = dMillis, weight = weightVal))
                }
            }

            // Pre-populate exercise library if empty
            if (repository.getExerciseInfo("Жим в рычажном тренажере или Смите") == null) {
                val allDefaultExercises = programDays.flatMap { day ->
                    day.exercises.map { pe ->
                        val category = when {
                            day.dayName.contains("ЖИМ", ignoreCase = true) -> {
                                if (pe.name.contains("трицепс", ignoreCase = true) || pe.name.contains("разгибание", ignoreCase = true)) "Руки"
                                else if (pe.name.contains("плеч", ignoreCase = true) || pe.name.contains("разведение", ignoreCase = true)) "Плечи"
                                else "Грудь"
                            }
                            day.dayName.contains("ТЯГ", ignoreCase = true) -> {
                                if (pe.name.contains("бицепс", ignoreCase = true) || pe.name.contains("сгибание", ignoreCase = true) || pe.name.contains("молот", ignoreCase = true)) "Руки"
                                else "Спина"
                            }
                            else -> {
                                if (pe.name.contains("пресс", ignoreCase = true) || pe.name.contains("скруч", ignoreCase = true)) "Пресс"
                                else "Ноги"
                            }
                        }
                        ExerciseInfo(
                            name = pe.name,
                            technique = pe.technique,
                            category = category,
                            photoUri = null,
                            isCustom = false
                        )
                    }
                }.distinctBy { it.name }
                repository.insertAllExercises(allDefaultExercises)
            }
        }
    }

    // --- User Stats / Settings Operations ---
    fun updateWeight(weight: Double) {
        viewModelScope.launch {
            val current = userStats.value
            repository.saveUserStats(current.copy(weight = weight))
            
            // Also save/update weight log for today:
            val sdfStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = sdfStr.format(Date())
            repository.saveWeightLog(
                WeightLog(
                    dateString = todayStr,
                    dateMillis = System.currentTimeMillis(),
                    weight = weight
                )
            )
        }
    }

    fun updateGoal(goal: String) {
        viewModelScope.launch {
            val current = userStats.value
            repository.saveUserStats(current.copy(goal = goal))
        }
    }

    fun updateActivity(activity: String) {
        viewModelScope.launch {
            val current = userStats.value
            repository.saveUserStats(current.copy(activity = activity))
        }
    }

    fun updateRirEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = userStats.value
            repository.saveUserStats(current.copy(rirEnabled = enabled))
        }
    }

    // --- Workout Operations ---
    fun selectDayIndex(index: Int) {
        _selectedDayIndex.value = index
    }

    fun startWorkout() {
        if (_isWorkoutActive.value) return
        val preset = programDays.getOrNull(_selectedDayIndex.value) ?: return
        
        // Initialize active exercises and pre-fill sets based on preset counts
        val initialActiveExercises = preset.exercises.map { pred ->
            ActiveExerciseState(
                name = pred.name,
                targetSets = pred.sets,
                targetReps = pred.reps,
                defaultRestSec = (pred.restMin * 60).toInt(),
                technique = pred.technique,
                sets = List(pred.sets) { idx ->
                    WorkoutSetState(setIndex = idx + 1)
                }
            )
        }

        _activeExercises.value = initialActiveExercises
        _currentExerciseIndex.value = 0
        _workoutDurationSeconds.value = 0
        _isWorkoutActive.value = true
        
        // Load history for first exercise
        loadHistoryForActiveExercise(initialActiveExercises.firstOrNull()?.name ?: "")

        // Start session duration timer
        workoutTimerJob?.cancel()
        workoutTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _workoutDurationSeconds.value += 1
            }
        }
    }

    fun cancelActiveWorkout() {
        _isWorkoutActive.value = false
        _activeExercises.value = emptyList()
        workoutTimerJob?.cancel()
        stopRestTimer()
    }

    fun changeActiveExerciseIndex(index: Int) {
        if (index in _activeExercises.value.indices) {
            _currentExerciseIndex.value = index
            loadHistoryForActiveExercise(_activeExercises.value[index].name)
        }
    }

    fun updateSetMetrics(exerciseIndex: Int, setIdx0: Int, weight: String, reps: String, rir: String) {
        val exercises = _activeExercises.value.toMutableList()
        if (exerciseIndex in exercises.indices) {
            val exercise = exercises[exerciseIndex]
            val sets = exercise.sets.toMutableList()
            if (setIdx0 in sets.indices) {
                // Keep rir empty if empty, or filter to digits and keep in range 0..5
                val cleanRir = rir.trim().filter { it.isDigit() }
                val finalRir = if (cleanRir.isNotEmpty()) {
                    val rVal = cleanRir.toIntOrNull() ?: 0
                    rVal.coerceIn(0, 5).toString()
                } else {
                    ""
                }
                sets[setIdx0] = sets[setIdx0].copy(
                    weight = weight,
                    reps = reps,
                    rir = finalRir
                )
                exercises[exerciseIndex] = exercise.copy(sets = sets)
                _activeExercises.value = exercises
            }
        }
    }

    fun toggleSetDone(exerciseIndex: Int, setIdx0: Int) {
        val exercises = _activeExercises.value.toMutableList()
        if (exerciseIndex in exercises.indices) {
            val exercise = exercises[exerciseIndex]
            val sets = exercise.sets.toMutableList()
            if (setIdx0 in sets.indices) {
                val currentDone = sets[setIdx0].isDone
                sets[setIdx0] = sets[setIdx0].copy(isDone = !currentDone)
                exercises[exerciseIndex] = exercise.copy(sets = sets)
                _activeExercises.value = exercises
                
                // If set was checked, automatically pre-fill/prepare rest timer!
                if (!currentDone) {
                    startRestTimer(exercise.defaultRestSec)
                }
            }
        }
    }

    private fun loadHistoryForActiveExercise(name: String) {
        viewModelScope.launch {
            _activeExerciseHistory.value = repository.getExerciseHistoryDirect(name)
        }
    }

    // --- Rest Count Down Timer ---
    fun startRestTimer(seconds: Int) {
        _restTimeTarget.value = seconds
        _restTimeRemaining.value = seconds
        _restTimerRunning.value = true
        expectedTimerEndMillis = System.currentTimeMillis() + (seconds * 1000)

        restTimerJob?.cancel()
        restTimerJob = viewModelScope.launch {
            while (_restTimeRemaining.value > 0) {
                delay(100)
                // Evaluate accurate delta using clock to persist background minimization ticks
                val msLeft = expectedTimerEndMillis - System.currentTimeMillis()
                val secLeft = (msLeft / 1000).toInt() + 1
                if (secLeft <= 0) {
                    _restTimeRemaining.value = 0
                } else if (secLeft < _restTimeRemaining.value) {
                    _restTimeRemaining.value = secLeft
                }
            }
            _restTimerRunning.value = false
            playAlertTone()
        }
    }

    fun stopRestTimer() {
        restTimerJob?.cancel()
        _restTimerRunning.value = false
        _restTimeRemaining.value = 0
    }

    fun skipRestTimer() {
        stopRestTimer()
    }

    private fun playAlertTone() {
        // Bypassed ToneGenerator to avoid native audio subsystem crashes in virtual/headless cloud emulators.
        android.util.Log.d("GymViewModel", "Rest timer ended successfully. Bypanned audible beep for emulator compatibility.")
    }

    // --- Save Active Session to Database ---
    fun finishWorkout() {
        val exercises = _activeExercises.value
        val durationMins = _workoutDurationSeconds.value / 60
        val dayName = programDays.getOrNull(_selectedDayIndex.value)?.dayName ?: "Тренировка"

        viewModelScope.launch {
            var totalTonnage = 0.0
            val doneSetsToInsert = mutableListOf<ExerciseSetLog>()
            val now = System.currentTimeMillis()

            exercises.forEach { ex ->
                ex.sets.forEach { sState ->
                    if (sState.isDone) {
                        val w = sState.weight.toDoubleOrNull() ?: 0.0
                        val r = sState.reps.toIntOrNull() ?: 0
                        totalTonnage += w * r
                        
                        doneSetsToInsert.add(
                            ExerciseSetLog(
                                workoutLogId = 0, // Filled during repository save path
                                dateMillis = now,
                                exerciseName = ex.name,
                                setIndex = sState.setIndex,
                                weight = w,
                                reps = r,
                                rir = sState.rir.toIntOrNull()
                            )
                        )
                    }
                }
            }

            if (doneSetsToInsert.isNotEmpty()) {
                val header = WorkoutLog(
                    dateMillis = now,
                    programDayName = dayName,
                    totalVolume = totalTonnage,
                    durationMinutes = if (durationMins > 0) durationMins else 1
                )
                repository.saveWorkout(header, doneSetsToInsert)
            }

            // Cleanup
            _isWorkoutActive.value = false
            _activeExercises.value = emptyList()
            workoutTimerJob?.cancel()
            stopRestTimer()
        }
    }

    // --- Nutrition Day Log Operations ---
    fun selectDateString(date: String) {
        _selectedDate.value = date
    }

    fun loadPredefinedMenuFromPdf(dayOrdinal: Int) {
        val dateStr = _selectedDate.value
        viewModelScope.launch {
            // First clear nutrition for today to avoid overlapping inserts
            repository.clearNutritionForDate(dateStr)

            // Compile menu data exactly matching PDF contents
            val mealPlans = when (dayOrdinal) {
                1 -> listOf(
                    // Овсянка 80г (сухая) на молоке (150мл), 2 яйца вареных (~100г), 1 банан (~120г)
                    NutritionLog(0, dateStr, "Завтрак", "Овсяные хлопья (сухие)", 80.0, 280.0, 10.4, 5.2, 54.4),
                    NutritionLog(0, dateStr, "Завтрак", "Молоко 2.5%", 150.0, 78.0, 4.2, 3.75, 7.05),
                    NutritionLog(0, dateStr, "Завтрак", "Яйцо куриное (целое)", 100.0, 155.0, 13.0, 11.0, 1.1),
                    NutritionLog(0, dateStr, "Завтрак", "Банан", 120.0, 106.8, 1.8, 0.12, 26.16),
                    // Рис белый 150г, филе курицы 200г, оливковое масло 10г, огурец 100г
                    NutritionLog(0, dateStr, "Обед", "Рис белый (сухой)", 150.0, 517.5, 10.05, 1.05, 118.35),
                    NutritionLog(0, dateStr, "Обед", "Куриное филе (сырое)", 200.0, 226.0, 47.2, 3.8, 0.0),
                    NutritionLog(0, dateStr, "Обед", "Масло оливковое", 10.0, 88.4, 0.0, 10.0, 0.0),
                    NutritionLog(0, dateStr, "Обед", "Огурец свежий", 100.0, 15.0, 0.8, 0.1, 3.0),
                    // Рис белый 150г, куриное филе 200г, оливковое масло 10г, огурец 100г
                    NutritionLog(0, dateStr, "Перед тренировкой", "Рис белый (сухой)", 150.0, 517.5, 10.05, 1.05, 118.35),
                    NutritionLog(0, dateStr, "Перед тренировкой", "Куриное филе (сырое)", 200.0, 226.0, 47.2, 3.8, 0.0),
                    NutritionLog(0, dateStr, "Перед тренировкой", "Масло оливковое", 10.0, 88.4, 0.0, 10.0, 0.0),
                    NutritionLog(0, dateStr, "Перед тренировкой", "Огурец свежий", 100.0, 15.0, 0.8, 0.1, 3.0),
                    // Протеин - 1 порция (30г), банан 120г
                    NutritionLog(0, dateStr, "После тренировки", "Сывороточный протеин", 30.0, 120.0, 24.0, 1.5, 3.0),
                    NutritionLog(0, dateStr, "После тренировки", "Банан", 120.0, 106.8, 1.8, 0.12, 26.16),
                    // Гречка 120г, индейка 200г, овощи тушеные 150г
                    NutritionLog(0, dateStr, "Ужин", "Гречка (крупа сухая)", 120.0, 411.6, 15.12, 3.96, 77.04),
                    NutritionLog(0, dateStr, "Ужин", "Филе индейки", 200.0, 230.0, 43.2, 6.4, 0.0),
                    NutritionLog(0, dateStr, "Ужин", "Овощи тушеные", 150.0, 75.0, 2.25, 3.0, 10.5),
                    // Творог 5% 200г
                    NutritionLog(0, dateStr, "Перед сном", "Творог 5%", 200.0, 242.0, 34.4, 10.0, 3.6)
                )
                2 -> listOf(
                    // Завтрак: Омлет из 3 яиц (150г) + 2 белка (60г), гречка 100г, масло сливочное 10г
                    NutritionLog(0, dateStr, "Завтрак", "Яйцо куриное (целое)", 150.0, 232.5, 19.5, 16.5, 1.65),
                    NutritionLog(0, dateStr, "Завтрак", "Белок яичный", 60.0, 28.8, 6.6, 0.12, 0.42),
                    NutritionLog(0, dateStr, "Завтрак", "Гречка (крупа сухая)", 100.0, 343.0, 12.6, 3.3, 64.2),
                    NutritionLog(0, dateStr, "Завтрак", "Масло сливочное 82.5%", 10.0, 71.7, 0.08, 8.25, 0.08),
                    // Обед: Макароны тв. сортов 150г, говядина постная 180г, томатный соус 50г
                    NutritionLog(0, dateStr, "Обед", "Макароны тв. сортов", 150.0, 525.0, 18.75, 2.25, 106.5),
                    NutritionLog(0, dateStr, "Обед", "Говядина постная", 180.0, 284.4, 39.6, 12.6, 0.0),
                    NutritionLog(0, dateStr, "Обед", "Сок вишневый", 50.0, 25.0, 0.25, 0.0, 6.0), // Используем сок как заменитель соуса
                    // Перед тренировкой: Творог 5% 150г, мед 20г
                    NutritionLog(0, dateStr, "Перед тренировкой", "Творог 5%", 150.0, 181.5, 25.8, 7.5, 2.7),
                    NutritionLog(0, dateStr, "Перед тренировкой", "Мед пчелиный", 20.0, 60.8, 0.06, 0.0, 16.48),
                    // После тренировки: Протеин 1 порция (30г), финики 4шт (~30г)
                    NutritionLog(0, dateStr, "После тренировки", "Сывороточный протеин", 30.0, 120.0, 24.0, 1.5, 3.0),
                    NutritionLog(0, dateStr, "После тренировки", "Финики сушеные", 30.0, 87.6, 0.75, 0.15, 20.7),
                    // Ужин: Рис 120г, минтай 200г, оливковое масло 10г
                    NutritionLog(0, dateStr, "Ужин", "Рис белый (сухой)", 120.0, 414.0, 8.04, 0.84, 94.68),
                    NutritionLog(0, dateStr, "Ужин", "Минтай филе", 200.0, 144.0, 32.0, 1.8, 0.0),
                    NutritionLog(0, dateStr, "Ужин", "Масло оливковое", 10.0, 88.4, 0.0, 10.0, 0.0),
                    // Перед сном: Творог 5% 200г
                    NutritionLog(0, dateStr, "Перед сном", "Творог 5%", 200.0, 242.0, 34.4, 10.0, 3.6)
                )
                else -> listOf(
                    // Завтрак: Овсянка 80г, протеин 12г (0.5п), арахисовая паста 20г, яблоко 150г
                    NutritionLog(0, dateStr, "Завтрак", "Овсяные хлопья (сухие)", 80.0, 280.0, 10.4, 5.2, 54.4),
                    NutritionLog(0, dateStr, "Завтрак", "Сывороточный протеин", 15.0, 60.0, 12.0, 0.75, 1.5),
                    NutritionLog(0, dateStr, "Завтрак", "Арахисовая паста", 20.0, 117.6, 5.0, 10.0, 4.0),
                    NutritionLog(0, dateStr, "Завтрак", "Яблоко", 150.0, 78.0, 0.45, 0.3, 21.0),
                    // Обед: Картофель запеченный 300г, куриное бедро 250г, сметана 10% 30г
                    NutritionLog(0, dateStr, "Обед", "Картофель запеченный", 300.0, 246.0, 6.0, 0.3, 57.0),
                    NutritionLog(0, dateStr, "Обед", "Куриное бедро (без кожи)", 250.0, 325.0, 50.0, 14.0, 0.0),
                    NutritionLog(0, dateStr, "Обед", "Сметана 10%", 30.0, 34.5, 0.9, 3.0, 0.87),
                    // Перед тренировкой: 2 банана (~240г), протеин 15г
                    NutritionLog(0, dateStr, "Перед тренировкой", "Банан", 240.0, 213.6, 3.6, 0.24, 52.32),
                    NutritionLog(0, dateStr, "Перед тренировкой", "Сывороточный протеин", 15.0, 60.0, 12.0, 0.75, 1.5),
                    // После тренировки: протеин 30г, сок вишневый 200мл (200г)
                    NutritionLog(0, dateStr, "После тренировки", "Сывороточный протеин", 30.0, 120.0, 24.0, 1.5, 3.0),
                    NutritionLog(0, dateStr, "После тренировки", "Сок вишневый", 200.0, 100.0, 1.0, 0.0, 24.0),
                    // Ужин: Гречка 120г, свинина постная 180г, квашеная капуста 150г
                    NutritionLog(0, dateStr, "Ужин", "Гречка (крупа сухая)", 120.0, 411.6, 15.12, 3.96, 77.04),
                    NutritionLog(0, dateStr, "Ужин", "Свинина постная", 180.0, 288.0, 36.0, 14.4, 0.0),
                    NutritionLog(0, dateStr, "Ужин", "Квашеная капуста", 150.0, 28.5, 1.35, 0.15, 6.45),
                    // Перед сном: Творог 5% 200г
                    NutritionLog(0, dateStr, "Перед сном", "Творог 5%", 200.0, 242.0, 34.4, 10.0, 3.6)
                )
            }

            mealPlans.forEach { log ->
                repository.addNutritionLog(log)
            }
        }
    }

    fun addCustomNutrition(meal: String, name: String, grams: Double, p: Double, f: Double, c: Double, kcal: Double) {
        val dateStr = _selectedDate.value
        viewModelScope.launch {
            val ratio = grams / 100.0
            val computedP = p * ratio
            val computedF = f * ratio
            val computedC = c * ratio
            val computedKcal = kcal * ratio

            repository.addNutritionLog(
                NutritionLog(
                    dateString = dateStr,
                    mealType = meal,
                    productName = name,
                    weightGrams = grams,
                    proteins = computedP,
                    fats = computedF,
                    carbs = computedC,
                    calories = computedKcal
                )
            )
        }
    }

    fun deleteNutrition(log: NutritionLog) {
        viewModelScope.launch {
            repository.deleteNutritionLog(log)
        }
    }

    fun clearNutritionToday() {
        val dateStr = _selectedDate.value
        viewModelScope.launch {
            repository.clearNutritionForDate(dateStr)
        }
    }

    // --- Exercise Library Operations ---
    fun addCustomExercise(name: String, technique: String, category: String, photoUri: String? = null) {
        viewModelScope.launch {
            repository.insertOrUpdateExercise(
                ExerciseInfo(
                    name = name.trim(),
                    technique = technique.trim(),
                    category = category,
                    photoUri = photoUri,
                    isCustom = true
                )
            )
        }
    }

    fun updateExercisePhoto(name: String, photoUri: String?) {
        viewModelScope.launch {
            val existing = repository.getExerciseInfo(name)
            if (existing != null) {
                repository.insertOrUpdateExercise(existing.copy(photoUri = photoUri))
            } else {
                repository.insertOrUpdateExercise(
                    ExerciseInfo(
                        name = name,
                        technique = "",
                        photoUri = photoUri,
                        category = "Другое",
                        isCustom = false
                    )
                )
            }
        }
    }

    fun deleteExercise(name: String) {
        viewModelScope.launch {
            repository.deleteExercise(name)
        }
    }

    fun addExerciseToActiveWorkout(name: String, setsCount: Int, reps: String, restSec: Int, technique: String) {
        val current = _activeExercises.value.toMutableList()
        val extra = ActiveExerciseState(
            name = name,
            targetSets = setsCount,
            targetReps = reps,
            defaultRestSec = restSec,
            technique = technique,
            sets = List(setsCount) { idx ->
                WorkoutSetState(setIndex = idx + 1)
            }
        )
        current.add(extra)
        _activeExercises.value = current
        
        // Also ensure this exercise exists in ExerciseInfo table!
        viewModelScope.launch {
            val existing = repository.getExerciseInfo(name)
            if (existing == null) {
                repository.insertOrUpdateExercise(
                    ExerciseInfo(
                        name = name,
                        technique = technique,
                        category = "Другое",
                        photoUri = null,
                        isCustom = true
                    )
                )
            }
        }
    }
}
