package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.viewmodel.*
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import java.text.SimpleDateFormat
import java.util.*

// Sophisticated Dark theme colors (Zinc & Lime-400)
val GymDarkBackground = Color(0xFF09090B)
val GymSurface = Color(0xFF18181B)
val GymOrangeAccent = Color(0xFFA3E635) // Elegant Lime Accent
val GymGreenAccent = Color(0xFF10B981)  // Smooth Green Success Indicator
val GymSlateBorder = Color(0xFF27272A)  // Dark Zinc lines
val GymTextSecondary = Color(0xFFA1A1AA) // Muted slate text

fun Modifier.dashedBorder(width: androidx.compose.ui.unit.Dp, color: Color, cornerRadius: androidx.compose.ui.unit.Dp) = drawBehind {
    val stroke = Stroke(
        width = width.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
    )
    drawRoundRect(
        color = color,
        style = stroke,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
    )
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GymAppContent(viewModel: GymViewModel) {
    val userStats by viewModel.userStats.collectAsStateWithLifecycle()
    val isWorkoutActive by viewModel.isWorkoutActive.collectAsStateWithLifecycle()
    val workoutLogs by viewModel.workoutLogs.collectAsStateWithLifecycle()
    
    var currentTab by remember { mutableStateOf("workout") } // "workout", "nutrition", "progress", "settings"
    
    // Summary popup at workout finish
    var showWorkoutSummary by remember { mutableStateOf<Pair<Double, Int>?>(null) } // pair of (total tonnage, duration)

    Scaffold(
        containerColor = GymDarkBackground,
        bottomBar = {
            if (!isWorkoutActive) {
                NavigationBar(
                    containerColor = GymSurface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    val tabs = listOf(
                        Triple("workout", "Тренировки", Icons.Default.FitnessCenter),
                        Triple("nutrition", "Питание", Icons.Default.Restaurant),
                        Triple("progress", "Прогресс", Icons.Default.TrendingUp),
                        Triple("settings", "Настройки", Icons.Default.Settings)
                    )
                    
                    tabs.forEach { (tabId, label, icon) ->
                        val selected = currentTab == tabId
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentTab = tabId },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = GymOrangeAccent,
                                selectedTextColor = GymOrangeAccent,
                                indicatorColor = GymSlateBorder,
                                unselectedIconColor = GymTextSecondary,
                                unselectedTextColor = GymTextSecondary
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Screen contents
            when (currentTab) {
                "workout" -> {
                    if (isWorkoutActive) {
                        ActiveWorkoutScreen(
                            viewModel = viewModel,
                            onFinish = { tonnage, duration ->
                                showWorkoutSummary = Pair(tonnage, duration)
                            }
                        )
                    } else {
                        WorkoutPresetSelectionScreen(viewModel)
                    }
                }
                "nutrition" -> NutritionDiaryScreen(viewModel)
                "progress" -> ProgressDashboardScreen(viewModel)
                "settings" -> SettingsScreen(viewModel)
            }
            
            // Finish summary overlay dialog
            showWorkoutSummary?.let { (tonnage, duration) ->
                WorkoutFinishedDialog(
                    tonnage = tonnage,
                    durationMins = duration,
                    onDismiss = { showWorkoutSummary = null }
                )
            }
        }
    }
}

// 1. --- WORKOUT BLOCK ---

@Composable
fun WorkoutPresetSelectionScreen(viewModel: GymViewModel) {
    val selectedDayIndex by viewModel.selectedDayIndex.collectAsStateWithLifecycle()
    val programDays = viewModel.programDays

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Тренировочный дневник",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Выберите день из актуальной программы:",
            fontSize = 15.sp,
            color = GymTextSecondary,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Day Selector Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(GymSurface)
                .padding(4.dp)
        ) {
            programDays.forEachIndexed { index, preset ->
                val isSelected = selectedDayIndex == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) GymOrangeAccent else Color.Transparent)
                        .clickable { viewModel.selectDayIndex(index) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "День ${index + 1}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else GymTextSecondary
                    )
                }
            }
        }

        // Selected Day info card
        val selectedPreset = programDays.getOrNull(selectedDayIndex) ?: programDays.first()
        Card(
            colors = CardDefaults.cardColors(containerColor = GymSurface),
            border = BorderStroke(1.dp, GymSlateBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.SportsGymnastics, 
                        contentDescription = null, 
                        tint = GymOrangeAccent,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                    Text(
                        text = selectedPreset.dayName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    text = selectedPreset.description,
                    fontSize = 14.sp,
                    color = GymTextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Divider(color = GymSlateBorder, thickness = 1.dp)

                Text(
                    text = "План упражнений:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                )

                selectedPreset.exercises.forEachIndexed { exIdx, ex ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${exIdx + 1}. ${ex.name}",
                            fontSize = 13.sp,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${ex.sets}x${ex.reps}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GymOrangeAccent
                        )
                    }
                }
            }
        }

        Button(
            onClick = { viewModel.startWorkout() },
            colors = ButtonDefaults.buttonColors(containerColor = GymOrangeAccent),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Начать тренировку", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ActiveWorkoutScreen(viewModel: GymViewModel, onFinish: (Double, Int) -> Unit) {
    val activeExercises by viewModel.activeExercises.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentExerciseIndex.collectAsStateWithLifecycle()
    val durationSec by viewModel.workoutDurationSeconds.collectAsStateWithLifecycle()
    val userStats by viewModel.userStats.collectAsStateWithLifecycle()
    
    // Rest Timer state
    val restRemaining by viewModel.restTimeRemaining.collectAsStateWithLifecycle()
    val restRunning by viewModel.restTimerRunning.collectAsStateWithLifecycle()
    val restTarget by viewModel.restTimeTarget.collectAsStateWithLifecycle()

    var showTechniqueDialog by remember { mutableStateOf(false) }
    var showCancelConfirmDialog by remember { mutableStateOf(false) }
    var showAddExerciseToWorkout by remember { mutableStateOf(false) }
    val allExercisesForAdding by viewModel.allExerciseInfo.collectAsStateWithLifecycle()

    if (activeExercises.isEmpty()) return
    val currentExercise = activeExercises.getOrNull(currentIndex) ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // TOP Header Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { showCancelConfirmDialog = true }) {
                Icon(Icons.Default.Close, contentDescription = "Закончить", tint = Color.Red)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val formattedDuration = String.format("%02d:%02d", durationSec / 60, durationSec % 60)
                Text("Длительность", fontSize = 11.sp, color = GymTextSecondary)
                Text(formattedDuration, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Button(
                onClick = {
                    // Compute finish statistics
                    var tonnage = 0.0
                    activeExercises.forEach { ex ->
                        ex.sets.forEach { s ->
                            if (s.isDone) {
                                val w = s.weight.toDoubleOrNull() ?: 0.0
                                val r = s.reps.toIntOrNull() ?: 0
                                tonnage += w * r
                            }
                        }
                    }
                    val durationMins = durationSec / 60
                    viewModel.finishWorkout()
                    onFinish(tonnage, if (durationMins > 0) durationMins else 1)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GymGreenAccent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Готово", fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Exercises scrollable dots/row indicator
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(activeExercises) { idx, ex ->
                val isActive = idx == currentIndex
                val isAnySetDone = ex.sets.any { it.isDone }
                val isAllCompleted = ex.sets.all { it.isDone }
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isActive) GymOrangeAccent 
                            else if (isAllCompleted) GymGreenAccent.copy(alpha = 0.25f)
                            else GymSurface
                        )
                        .border(
                            1.dp, 
                            if (isActive) GymOrangeAccent 
                            else if (isAnySetDone) GymGreenAccent.copy(alpha = 0.5f) 
                            else GymSlateBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { viewModel.changeActiveExerciseIndex(idx) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Упр. ${idx + 1}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) Color.White else if (isAllCompleted) GymGreenAccent else GymTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Active Exercise Card Title
        Card(
            colors = CardDefaults.cardColors(containerColor = GymSurface),
            border = BorderStroke(1.dp, GymSlateBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = currentExercise.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "План: ${currentExercise.targetSets} подходов по ${currentExercise.targetReps} повт.",
                        fontSize = 12.sp,
                        color = GymTextSecondary
                    )

                    Row {
                        IconButton(
                            onClick = { showTechniqueDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "Инфо", tint = GymOrangeAccent)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // List of Sets
        Text(
            text = "Подходы",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(currentExercise.sets) { sIdx, sState ->
                SetRecordingRow(
                    sIdx = sIdx,
                    sState = sState,
                    rirEnabled = userStats.rirEnabled,
                    onMetricsChanged = { weight, reps, rir ->
                        viewModel.updateSetMetrics(currentIndex, sIdx, weight, reps, rir)
                    },
                    onToggleDone = {
                        viewModel.toggleSetDone(currentIndex, sIdx)
                    }
                )
            }

            // Add Exercise on-the-fly option
            item {
                Button(
                    onClick = { showAddExerciseToWorkout = true },
                    colors = ButtonDefaults.buttonColors(containerColor = GymSurface),
                    border = BorderStroke(1.dp, GymSlateBorder),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = GymOrangeAccent)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Добавить упражнение в сессию", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // Exercise History section inside scroll
            item {
                HistorySection(viewModel = viewModel)
            }
        }

        // Rest countdown timer bar (sticky bottom)
        if (restRemaining > 0 || restRunning) {
            RestTimerCard(
                remaining = restRemaining,
                target = restTarget,
                onStop = { viewModel.stopRestTimer() },
                onSkip = { viewModel.skipRestTimer() },
                onAddInterval = { sec -> viewModel.startRestTimer(restRemaining + sec) }
            )
        }

        // Navigation Next / Prev Workout buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { viewModel.changeActiveExerciseIndex(currentIndex - 1) },
                enabled = currentIndex > 0,
                colors = ButtonDefaults.buttonColors(containerColor = GymSurface),
                border = BorderStroke(1.dp, GymSlateBorder),
                modifier = Modifier.weight(1f).padding(end = 4.dp)
            ) {
                Text("Назад", color = if (currentIndex > 0) Color.White else GymTextSecondary)
            }

            Button(
                onClick = { viewModel.changeActiveExerciseIndex(currentIndex + 1) },
                enabled = currentIndex < activeExercises.size - 1,
                colors = ButtonDefaults.buttonColors(containerColor = GymSurface),
                border = BorderStroke(1.dp, GymSlateBorder),
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            ) {
                Text("Вперед", color = if (currentIndex < activeExercises.size - 1) Color.White else GymTextSecondary)
            }
        }
    }

    if (showAddExerciseToWorkout) {
        AddExerciseToWorkoutDialog(
            allExercises = allExercisesForAdding,
            onDismiss = { showAddExerciseToWorkout = false },
            onAdd = { name, setsCount, reps, restSec, technique ->
                viewModel.addExerciseToActiveWorkout(name, setsCount, reps, restSec, technique)
            }
        )
    }

    // Interactive custom Technique sheet / Alert Dialog
    if (showTechniqueDialog) {
        val currentExerciseInfo = allExercisesForAdding.find { it.name == currentExercise.name }
        val context = LocalContext.current
        val photoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                try {
                    val takeFlags: Int = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (e: Exception) {}
                viewModel.updateExercisePhoto(currentExercise.name, uri.toString())
            }
        }

        Dialog(onDismissRequest = { showTechniqueDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = GymSurface),
                border = BorderStroke(1.dp, GymSlateBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Техника выполнения",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = GymOrangeAccent,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = currentExercise.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Photo Rendering Frame
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .padding(bottom = 12.dp)
                    ) {
                        if (currentExerciseInfo?.photoUri != null) {
                            AsyncImage(
                                model = currentExerciseInfo.photoUri,
                                contentDescription = currentExercise.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            ExerciseIllustrationPlaceholder(category = currentExerciseInfo?.category ?: "Грудь", modifier = Modifier.fillMaxSize())
                        }
                    }

                    // Upload/Setup photo Row
                    var showQuickPhotos by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = GymSlateBorder),
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = GymOrangeAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Своё фото", fontSize = 11.sp, color = Color.White)
                        }

                        Button(
                            onClick = { showQuickPhotos = true },
                            colors = ButtonDefaults.buttonColors(containerColor = GymSlateBorder),
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = GymOrangeAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Выбрать готовое", fontSize = 11.sp, color = Color.White)
                        }

                        if (showQuickPhotos) {
                            PredefinedPhotoSelectorDialog(
                                exerciseName = currentExercise.name,
                                viewModel = viewModel,
                                onDismiss = { showQuickPhotos = false }
                            )
                        }
                    }

                    Text(
                        text = currentExercise.technique,
                        fontSize = 12.sp,
                        color = GymTextSecondary,
                        lineHeight = 16.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { showTechniqueDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = GymOrangeAccent),
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Понятно", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Cancel active confirmation dialog
    if (showCancelConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmDialog = false },
            title = { Text("Сбросить тренировку?", color = Color.White) },
            text = { Text("Все несохраненные подходы активного сеанса будут утеряны.", color = GymTextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cancelActiveWorkout()
                    showCancelConfirmDialog = false
                }) {
                    Text("Да, сбросить", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmDialog = false }) {
                    Text("Продолжить", color = Color.White)
                }
            },
            containerColor = GymSurface
        )
    }
}

@Composable
fun SetRecordingRow(
    sIdx: Int,
    sState: WorkoutSetState,
    rirEnabled: Boolean,
    onMetricsChanged: (rawWeight: String, rawReps: String, rawRir: String) -> Unit,
    onToggleDone: () -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current
    
    // Large gym friendly input fields for rapid weights and reps insertion
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (sState.isDone) GymGreenAccent.copy(alpha = 0.08f) else GymSurface
        ),
        border = BorderStroke(1.dp, if (sState.isDone) GymGreenAccent.copy(alpha = 0.4f) else GymSlateBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Index number
            Text(
                text = "${sIdx + 1}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (sState.isDone) GymGreenAccent else Color.White,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Weight Column & Stepper Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1.3f)
            ) {
                // Stepper Dec
                IconButton(
                    onClick = {
                        val curr = sState.weight.toDoubleOrNull() ?: 0.0
                        val res = if (curr >= 2.5) curr - 2.5 else 0.0
                        onMetricsChanged(if (res > 0) res.toString() else "", sState.reps, sState.rir)
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = null, tint = GymTextSecondary, modifier = Modifier.size(16.dp))
                }

                // Field
                OutlinedTextField(
                    value = sState.weight,
                    onValueChange = { onMetricsChanged(it, sState.reps, sState.rir) },
                    placeholder = { Text("0", color = GymTextSecondary, fontSize = 13.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GymOrangeAccent,
                        unfocusedBorderColor = GymSlateBorder,
                        focusedContainerColor = GymDarkBackground,
                        unfocusedContainerColor = GymDarkBackground
                    ),
                    modifier = Modifier
                        .width(62.dp)
                        .height(48.dp)
                )

                Text(
                    text = "кг",
                    fontSize = 11.sp,
                    color = GymTextSecondary,
                    modifier = Modifier.padding(start = 2.dp)
                )

                // Stepper Inc
                IconButton(
                    onClick = {
                        val curr = sState.weight.toDoubleOrNull() ?: 0.0
                        val res = curr + 2.5
                        onMetricsChanged(res.toString(), sState.reps, sState.rir)
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = GymOrangeAccent, modifier = Modifier.size(16.dp))
                }
            }

            // Reps Column & Stepper Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1.2f)
            ) {
                // Stepper Dec
                IconButton(
                    onClick = {
                        val curr = sState.reps.toIntOrNull() ?: 0
                        val res = if (curr >= 1) curr - 1 else 0
                        onMetricsChanged(sState.weight, if (res > 0) res.toString() else "", sState.rir)
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = null, tint = GymTextSecondary, modifier = Modifier.size(16.dp))
                }

                // Field
                OutlinedTextField(
                    value = sState.reps,
                    onValueChange = { onMetricsChanged(sState.weight, it, sState.rir) },
                    placeholder = { Text("0", color = GymTextSecondary, fontSize = 13.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GymOrangeAccent,
                        unfocusedBorderColor = GymSlateBorder,
                        focusedContainerColor = GymDarkBackground,
                        unfocusedContainerColor = GymDarkBackground
                    ),
                    modifier = Modifier
                        .width(52.dp)
                        .height(48.dp)
                )

                Text(
                    text = "пов",
                    fontSize = 11.sp,
                    color = GymTextSecondary,
                    modifier = Modifier.padding(start = 2.dp)
                )

                // Stepper Inc
                IconButton(
                    onClick = {
                        val curr = sState.reps.toIntOrNull() ?: 0
                        val res = curr + 1
                        onMetricsChanged(sState.weight, res.toString(), sState.rir)
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = GymOrangeAccent, modifier = Modifier.size(16.dp))
                }
            }

            // Optional RIR Column
            if (rirEnabled) {
                OutlinedTextField(
                    value = sState.rir,
                    onValueChange = { onMetricsChanged(sState.weight, sState.reps, it) },
                    placeholder = { Text("RIR", color = GymTextSecondary, fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GymOrangeAccent,
                        unfocusedBorderColor = GymSlateBorder,
                        focusedContainerColor = GymDarkBackground,
                        unfocusedContainerColor = GymDarkBackground
                    ),
                    modifier = Modifier
                        .width(44.dp)
                        .height(48.dp)
                        .padding(horizontal = 2.dp)
                )
            }

            // Complete Toggle Button (Large click target)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (sState.isDone) GymGreenAccent else GymSlateBorder)
                    .clickable {
                        keyboard?.hide()
                        onToggleDone()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (sState.isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Состояние",
                    tint = if (sState.isDone) Color.Black else Color.White
                )
            }
        }
    }
}

@Composable
fun RestTimerCard(
    remaining: Int,
    target: Int,
    onStop: () -> Unit,
    onSkip: () -> Unit,
    onAddInterval: (Int) -> Unit
) {
    val mins = remaining / 60
    val secs = remaining % 60
    val textTime = String.format("%02d:%02d", mins, secs)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(GymSurface.copy(alpha = 0.3f))
            .dashedBorder(1.5.dp, GymSlateBorder, 24.dp)
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "ОТДЫХ МЕЖДУ ПОДХОДАМИ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = GymTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            
            Text(
                text = textTime,
                fontSize = 42.sp,
                fontWeight = FontWeight.Light,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .border(1.dp, GymSlateBorder, CircleShape)
                        .clickable { onAddInterval(15) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("+15с", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
                }
                
                Box(
                    modifier = Modifier
                        .border(1.dp, GymSlateBorder, CircleShape)
                        .clickable { onAddInterval(30) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("+30с", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF7F1D1D).copy(alpha = 0.3f))
                        .clickable { onStop() }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("СТОП", fontSize = 11.sp, color = Color(0xFFF87171), fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .border(1.dp, GymOrangeAccent.copy(alpha = 0.5f), CircleShape)
                        .clickable { onSkip() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("ПРОПУСТИТЬ", fontSize = 11.sp, color = GymOrangeAccent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun HistorySection(viewModel: GymViewModel) {
    val history by viewModel.activeExerciseHistory.collectAsStateWithLifecycle()
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(GymSurface)
            .border(1.dp, GymSlateBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, contentDescription = "История", tint = GymOrangeAccent, modifier = Modifier.size(18.dp))
                Text(
                    "История выполненных весов",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = GymTextSecondary
            )
        }

        if (isExpanded) {
            Spacer(modifier = Modifier.height(8.dp))
            
            if (history.isEmpty()) {
                Text(
                    text = "В этом упражнении пока нет записей.",
                    fontSize = 12.sp,
                    color = GymTextSecondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                // Group by workout header or timestamp
                val grouped = history.groupBy { it.dateMillis }
                val keys = grouped.keys.sortedDescending().take(4) // Show up to latest 4 sessions
                
                keys.forEach { dateMs ->
                    val dateStr = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(dateMs))
                    val sets = grouped[dateMs] ?: emptyList()
                    
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "Сессия от $dateStr:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GymOrangeAccent
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            sets.forEach { setLog ->
                                val rirText = if (setLog.rir != null) " (RIR:${setLog.rir})" else ""
                                Text(
                                    text = "${setLog.setIndex}: ${setLog.weight}кг x ${setLog.reps}$rirText",
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                        }
                        Divider(color = GymSlateBorder.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutFinishedDialog(tonnage: Double, durationMins: Int, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = GymSurface),
            border = BorderStroke(2.dp, GymGreenAccent),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.EmojiEvents, 
                    contentDescription = null, 
                    tint = GymGreenAccent,
                    modifier = Modifier.size(56.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Тренировка завершена!",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Отличная работа! Все подходы записаны в историю прогресса.",
                    fontSize = 13.sp,
                    color = GymTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Общий объем", fontSize = 11.sp, color = GymTextSecondary)
                        Text(String.format("%.1f кг", tonnage), fontSize = 16.sp, fontWeight = FontWeight.Black, color = GymGreenAccent)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Длительность", fontSize = 11.sp, color = GymTextSecondary)
                        Text("$durationMins мин", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = GymGreenAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Продолжить и закрыть", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 2. --- NUTRITION BLOCK ---

@Composable
fun NutritionDiaryScreen(viewModel: GymViewModel) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val rawNutritionList by viewModel.todayNutrition.collectAsStateWithLifecycle()
    val stats by viewModel.userStats.collectAsStateWithLifecycle()

    var showAddFoodDialog by remember { mutableStateOf(false) }

    // Computations based on current settings
    val bmr = stats.weight * 22
    val mult = when (stats.activity) {
        "low" -> 1.2
        "high" -> 1.6
        else -> 1.4
    }
    val targetTdee = bmr * mult
    val targetCalories = (if (stats.goal == "mass") targetTdee + 300.0 else targetTdee - 400.0).coerceAtLeast(500.0)
    val targetProt = (if (stats.goal == "mass") stats.weight * 2.0 else stats.weight * 2.2).coerceAtLeast(0.0)
    val targetFat = (if (stats.goal == "mass") stats.weight * 1.0 else stats.weight * 0.8).coerceAtLeast(0.0)
    val targetCarbs = ((targetCalories - (targetProt * 4.0) - (targetFat * 9.0)) / 4.0).coerceAtLeast(0.0)

    // Raw sums consumed today
    val sumP = rawNutritionList.sumOf { it.proteins }
    val sumF = rawNutritionList.sumOf { it.fats }
    val sumC = rawNutritionList.sumOf { it.carbs }
    val sumKcal = rawNutritionList.sumOf { it.calories }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Date switcher header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { changeDate(viewModel, selectedDate, -1) }) {
                Icon(Icons.Default.ArrowBackIos, contentDescription = "Назад", tint = GymOrangeAccent)
            }
            Text(
                text = formatDateRussian(selectedDate),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            IconButton(onClick = { changeDate(viewModel, selectedDate, 1) }) {
                Icon(Icons.Default.ArrowForwardIos, contentDescription = "Вперед", tint = GymOrangeAccent)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Large Calories Progress Bar Info card
        Card(
            colors = CardDefaults.cardColors(containerColor = GymSurface),
            border = BorderStroke(1.dp, GymSlateBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Потреблено калорий", fontSize = 12.sp, color = GymTextSecondary)
                        Text(
                            text = "${sumKcal.toInt()} / ${targetCalories.toInt()} ккал",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    val pct = if (targetCalories > 0.0) (sumKcal / targetCalories).coerceIn(0.0, 2.0) else 0.0
                    Text(
                        text = String.format("%.0f%%", pct * 100),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = if (pct <= 1.0) GymGreenAccent else GymOrangeAccent
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Bar Linear
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(GymSlateBorder)
                ) {
                    val progressFraction = if (targetCalories > 0.0) (sumKcal / targetCalories).toFloat().coerceIn(0f, 1f) else 0f
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressFraction)
                            .background(GymOrangeAccent)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // BJU Indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MacroBar(label = "Белки (Б)", consumed = sumP, target = targetProt, color = GymOrangeAccent)
                    MacroBar(label = "Жиры (Ж)", consumed = sumF, target = targetFat, color = Color(0xFFFFD54F))
                    MacroBar(label = "Углеводы (У)", consumed = sumC, target = targetCarbs, color = GymGreenAccent)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // PDF Menu actions and Add actions line
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Приёмы пищи", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            
            Row {
                IconButton(onClick = { showAddFoodDialog = true }) {
                    Icon(Icons.Default.AddCircle, contentDescription = "Добавить продукт", tint = GymGreenAccent, modifier = Modifier.size(28.dp))
                }
            }
        }

        // PDF Menu templates dropdown / presets row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Загрузить меню День 1", "Загрузить День 2", "Загрузить День 3").forEachIndexed { index, title ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(GymSurface)
                        .border(0.5.dp, GymSlateBorder, RoundedCornerShape(8.dp))
                        .clickable { viewModel.loadPredefinedMenuFromPdf(index + 1) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        title.replace("Загрузить ", ""),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GymTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // Clear Meal button
            Box(
                modifier = Modifier
                    .size(width = 44.dp, height = 32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Red.copy(alpha = 0.15f))
                    .clickable { viewModel.clearNutritionToday() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = "Очистить", tint = Color.Red, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Meal segments list
        val mealBuckets = listOf("Завтрак", "Обед", "Перед тренировкой", "После тренировки", "Ужин", "Перед сном")
        
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(mealBuckets) { bucketName ->
                val bucketLogs = rawNutritionList.filter { it.mealType == bucketName }
                MealBucketSection(
                    name = bucketName,
                    items = bucketLogs,
                    onDeleteLog = { viewModel.deleteNutrition(it) }
                )
            }
        }
    }

    // Interactive product picker dialog (built-in 50 base bodybuilding products database)
    if (showAddFoodDialog) {
        AddFoodDialog(
            foodPresets = viewModel.foodPresets,
            onDismiss = { showAddFoodDialog = false },
            onAdd = { mealType, name, grams, p, f, c, kcal ->
                viewModel.addCustomNutrition(mealType, name, grams, p, f, c, kcal)
                showAddFoodDialog = false
            }
        )
    }
}

@Composable
fun MacroBar(label: String, consumed: Double, target: Double, color: Color) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.width(96.dp)
    ) {
        Text(label, fontSize = 11.sp, color = GymTextSecondary)
        Text("${consumed.toInt()} / ${target.toInt()} г", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(GymSlateBorder)
        ) {
            val ratio = if (target > 0) (consumed / target).toFloat().coerceIn(0f, 1f) else 0f
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(ratio)
                    .background(color)
            )
        }
    }
}

@Composable
fun MealBucketSection(
    name: String,
    items: List<NutritionLog>,
    onDeleteLog: (NutritionLog) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }

    Card(
        colors = CardDefaults.cardColors(containerColor = GymSurface),
        border = BorderStroke(1.dp, GymSlateBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (items.isNotEmpty()) GymOrangeAccent else GymTextSecondary)
                    )
                    Text(
                        text = name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    if (items.isNotEmpty()) {
                        val sumK = items.sumOf { it.calories }.toInt()
                        Text(
                            text = " ($sumK ккал)",
                            fontSize = 12.sp,
                            color = GymOrangeAccent
                        )
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = GymTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (isExpanded) {
                if (items.isEmpty()) {
                    Text(
                        "Пусто. Добавьте блюдо.",
                        fontSize = 11.sp,
                        color = GymTextSecondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    items.forEach { log ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(log.productName, fontSize = 13.sp, color = Color.White)
                                Text(
                                    text = "${log.weightGrams.toInt()}г | Б: ${log.proteins.toInt()} Ж: ${log.fats.toInt()} У: ${log.carbs.toInt()}",
                                    fontSize = 11.sp,
                                    color = GymTextSecondary
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${log.calories.toInt()} ккал",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                IconButton(onClick = { onDeleteLog(log) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Удалить", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddFoodDialog(
    foodPresets: List<FoodPreset>,
    onDismiss: () -> Unit,
    onAdd: (mealType: String, name: String, grams: Double, p: Double, f: Double, c: Double, kcal: Double) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf<FoodPreset?>(null) }
    var customName by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("100") }
    
    var manualP by remember { mutableStateOf("") }
    var manualF by remember { mutableStateOf("") }
    var manualC by remember { mutableStateOf("") }
    var manualKcal by remember { mutableStateOf("") }

    var selectedMealType by remember { mutableStateOf("Завтрак") }
    val mealTypes = listOf("Завтрак", "Обед", "Перед тренировкой", "После тренировки", "Ужин", "Перед сном")

    val filteredPresets = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            foodPresets.take(6)
        } else {
            foodPresets.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = GymSurface),
            border = BorderStroke(1.dp, GymSlateBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Добавить приём пищи", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GymOrangeAccent)
                
                Spacer(modifier = Modifier.height(10.dp))

                // Meal Category picker
                Text("Раздел:", fontSize = 11.sp, color = GymTextSecondary)
                Box(modifier = Modifier.fillMaxWidth()) {
                    var expandedMealDropdown by remember { mutableStateOf(false) }
                    Button(
                        onClick = { expandedMealDropdown = true },
                        colors = ButtonDefaults.buttonColors(containerColor = GymSlateBorder),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedMealType, color = Color.White)
                    }
                    DropdownMenu(
                        expanded = expandedMealDropdown,
                        onDismissRequest = { expandedMealDropdown = false },
                        modifier = Modifier.background(GymSurface)
                    ) {
                        mealTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type, color = Color.White) },
                                onClick = {
                                    selectedMealType = type
                                    expandedMealDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        if (selectedPreset != null && selectedPreset?.name != it) {
                            selectedPreset = null
                        }
                    },
                    label = { Text("Поиск в базе (50 продуктов)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GymOrangeAccent,
                        unfocusedBorderColor = GymSlateBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Render preset suggestion results
                if (selectedPreset == null && searchQuery.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = GymDarkBackground),
                        border = BorderStroke(0.5.dp, GymSlateBorder)
                    ) {
                        Column {
                            filteredPresets.forEach { preset ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedPreset = preset
                                            customName = preset.name
                                            searchQuery = preset.name
                                            manualP = preset.proteins.toString()
                                            manualF = preset.fats.toString()
                                            manualC = preset.carbs.toString()
                                            manualKcal = preset.calories.toString()
                                        }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(preset.name, fontSize = 13.sp, color = Color.White)
                                    Text(
                                        "Ккал: ${preset.calories.toInt()}",
                                        fontSize = 11.sp, 
                                        color = GymOrangeAccent
                                    )
                                }
                            }
                            if (filteredPresets.isEmpty()) {
                                Text(
                                    "Не найдено. Можно ввести вручную.",
                                    fontSize = 11.sp,
                                    color = GymTextSecondary,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Name of product if custom
                if (selectedPreset == null) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Название продукта вручную") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GymOrangeAccent,
                            unfocusedBorderColor = GymSlateBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(GymOrangeAccent.copy(alpha = 0.12f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.RestaurantMenu, contentDescription = null, tint = GymOrangeAccent)
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text("Выбран шаблон:", fontSize = 10.sp, color = GymOrangeAccent)
                            Text(selectedPreset!!.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(
                                "Б: ${selectedPreset!!.proteins}г Ж: ${selectedPreset!!.fats}г У: ${selectedPreset!!.carbs}г (на 100г)",
                                fontSize = 11.sp,
                                color = GymTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Weight Grams
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text("Вес порции (в граммах)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GymOrangeAccent,
                        unfocusedBorderColor = GymSlateBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Custom nutrition values if typed manually
                if (selectedPreset == null) {
                    Text("БЖУ на 100г:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = manualP,
                            onValueChange = { manualP = it },
                            label = { Text("Белки", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GymOrangeAccent, unfocusedBorderColor = GymSlateBorder),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = manualF,
                            onValueChange = { manualF = it },
                            label = { Text("Жиры", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GymOrangeAccent, unfocusedBorderColor = GymSlateBorder),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = manualC,
                            onValueChange = { manualC = it },
                            label = { Text("Углев", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GymOrangeAccent, unfocusedBorderColor = GymSlateBorder),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = manualKcal,
                        onValueChange = { manualKcal = it },
                        label = { Text("Калории (на 100г)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GymOrangeAccent, unfocusedBorderColor = GymSlateBorder),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена", color = GymTextSecondary)
                    }
                    Button(
                        onClick = {
                            val name = if (selectedPreset != null) selectedPreset!!.name else customName
                            val grams = weightInput.toDoubleOrNull() ?: 100.0
                            
                            val p = if (selectedPreset != null) selectedPreset!!.proteins else manualP.toDoubleOrNull() ?: 0.0
                            val f = if (selectedPreset != null) selectedPreset!!.fats else manualF.toDoubleOrNull() ?: 0.0
                            val c = if (selectedPreset != null) selectedPreset!!.carbs else manualC.toDoubleOrNull() ?: 0.0
                            val kcal = if (selectedPreset != null) selectedPreset!!.calories else manualKcal.toDoubleOrNull() ?: 0.0

                            if (name.isNotBlank()) {
                                onAdd(selectedMealType, name, grams, p, f, c, kcal)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GymOrangeAccent),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("Добавить", color = Color.White)
                    }
                }
            }
        }
    }
}

private fun changeDate(viewModel: GymViewModel, currentDate: String, daysOffset: Int) {
    try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(currentDate) ?: Date()
        val cal = Calendar.getInstance()
        cal.time = date
        cal.add(Calendar.DATE, daysOffset)
        viewModel.selectDateString(sdf.format(cal.time))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun formatDateRussian(dateStr: String): String {
    return try {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr) ?: Date()
        val russianSdf = SimpleDateFormat("EEEE, d MMMM", Locale("ru"))
        russianSdf.format(date).replaceFirstChar { it.uppercase() }
    } catch (e: Exception) {
        dateStr
    }
}

// 3. --- PROGRESS / ANALYTICS BLOCK (CUSTOM CANVAS CHARTS) ---

@Composable
fun ProgressDashboardScreen(viewModel: GymViewModel) {
    val allSetLogs by viewModel.allSetLogs.collectAsStateWithLifecycle()
    val workoutLogs by viewModel.workoutLogs.collectAsStateWithLifecycle()
    val allWeightLogs by viewModel.allWeightLogs.collectAsStateWithLifecycle()

    var selectedExerciseForChart by remember { mutableStateOf("Жим в рычажном тренажере или Смите") }
    val candidateExercises = listOf(
        "Жим в рычажном тренажере или Смите",
        "Тяга штанги в наклоне",
        "Приседания со штангой или гантелью перед собой (Goblet squat)"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Аналитика прогресса", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Графики рабочих весов и объёмов:", fontSize = 13.sp, color = GymTextSecondary, modifier = Modifier.padding(bottom = 16.dp))

        // --- 1. PERSONAL RECORDS (PR) SECTION ---
        Card(
            colors = CardDefaults.cardColors(containerColor = GymSurface),
            border = BorderStroke(1.dp, GymSlateBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Рекорды",
                        tint = GymOrangeAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        "Личные рекорды (PR) в упражнениях",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                val keyExercises = listOf(
                    "Жим в рычажном тренажере или Смите",
                    "Жим гантелей лежа под углом 30°",
                    "Тяга штанги в наклоне",
                    "Тяга верхнего блока к груди широким хватом",
                    "Жим ногами платформы",
                    "Приседания со штангой или гантелью перед собой (Goblet squat)"
                )

                // Extract PRs from set logs
                val prMap = remember(allSetLogs) {
                    allSetLogs.filter { it.weight > 0.0 && it.reps > 0 }
                        .groupBy { it.exerciseName }
                        .mapValues { (_, sets) ->
                            sets.maxWithOrNull(compareBy<ExerciseSetLog> { it.weight }.thenBy { it.reps })
                        }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    keyExercises.forEach { exName ->
                        val pr = prMap[exName]
                        val displayExName = exName
                            .replace(" в рычажном тренажере или Смите", "")
                            .replace(" со штангой или гантелью перед собой", "")
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GymDarkBackground, RoundedCornerShape(12.dp))
                                .border(1.dp, GymSlateBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = displayExName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                if (pr != null) {
                                    val dateStr = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(pr.dateMillis))
                                    Text(
                                        text = "Достигнут: $dateStr",
                                        fontSize = 11.sp,
                                        color = GymTextSecondary
                                    )
                                } else {
                                    Text(
                                        text = "Нет записей",
                                        fontSize = 11.sp,
                                        color = GymTextSecondary
                                    )
                                }
                            }

                            if (pr != null) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${pr.weight} кг × ${pr.reps}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = GymOrangeAccent
                                    )
                                    // 1RM Calculation using Epley: Weight * (1 + Reps / 30.0)
                                    val oneRepMax = if (pr.reps > 1) {
                                        pr.weight * (1.0 + pr.reps / 30.0)
                                    } else {
                                        pr.weight
                                    }
                                    Text(
                                        text = String.format("1ПМ ≈ %.1f кг", oneRepMax),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = GymGreenAccent
                                    )
                                }
                            } else {
                                Text(
                                    text = "—",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GymSlateBorder
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 2. WEEKLY VOLUME SUMMARY CARD & GRAPH ---
        Card(
            colors = CardDefaults.cardColors(containerColor = GymSurface),
            border = BorderStroke(1.dp, GymSlateBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Еженедельный объём нагрузок (кг)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Группировка тренировочной нагрузки по неделям",
                    fontSize = 11.sp,
                    color = GymTextSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val weeklyVolumeLogs = remember(workoutLogs) {
                    if (workoutLogs.isEmpty()) {
                        emptyList()
                    } else {
                        val grouped = workoutLogs.groupBy {
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = it.dateMillis
                            cal.set(Calendar.HOUR_OF_DAY, 0)
                            cal.set(Calendar.MINUTE, 0)
                            cal.set(Calendar.SECOND, 0)
                            cal.set(Calendar.MILLISECOND, 0)
                            
                            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                            val daysToSubtract = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
                            cal.add(Calendar.DAY_OF_YEAR, -daysToSubtract)
                            cal.timeInMillis
                        }
                        
                        grouped.map { (mondayMillis, logs) ->
                            Pair(mondayMillis, logs.sumOf { it.totalVolume })
                        }.sortedBy { it.first }
                    }
                }

                if (weeklyVolumeLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Ещё нет завершённых тренировок за неделю.",
                            fontSize = 12.sp,
                            color = GymTextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    WeeklyVolumeBarChart(data = weeklyVolumeLogs)
                }
            }
        }

        // Exercise Chart Selector
        Text("Макс. вес в упражнении:", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            candidateExercises.forEach { ex ->
                val isSelected = selectedExerciseForChart == ex
                val displayLabel = ex.replace(" в рычажном тренажере или Смите", "").replace(" со штангой или гантелью перед собой", "")
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) GymOrangeAccent else GymSurface)
                        .border(1.dp, if (isSelected) GymOrangeAccent else GymSlateBorder, RoundedCornerShape(8.dp))
                        .clickable { selectedExerciseForChart = ex }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(displayLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else GymTextSecondary)
                }
            }
        }

        // 3. Plot Max Weight
        val maxWeightsData = remember(allSetLogs, selectedExerciseForChart) {
            val fitLogs = allSetLogs.filter { it.exerciseName == selectedExerciseForChart }
            fitLogs.groupBy { it.dateMillis }
                .map { (date, sets) -> Pair(date, sets.maxOfOrNull { it.weight } ?: 0.0) }
                .sortedBy { it.first }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = GymSurface),
            border = BorderStroke(1.dp, GymSlateBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Максимальный вес (кг)", 
                    fontSize = 15.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = Color.White
                )
                Text(
                    "Упражнение: $selectedExerciseForChart", 
                    fontSize = 11.sp, 
                    color = GymTextSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                if (maxWeightsData.size < 2) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Недостаточно данных. Запишите не менее 2 разных тренировок.",
                            fontSize = 12.sp,
                            color = GymTextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LineChartCanvas(data = maxWeightsData)
                }
            }
        }

        // 4. Plot Workout Volumes (Tonnage Tonnage)
        Card(
            colors = CardDefaults.cardColors(containerColor = GymSurface),
            border = BorderStroke(1.dp, GymSlateBorder),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Объём нагрузки за тренировку (кг)", 
                    fontSize = 15.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    "Сумма: рабочий вес × повторения × подходы", 
                    fontSize = 11.sp, 
                    color = GymTextSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val volumeLogs = remember(workoutLogs) {
                    workoutLogs.sortedBy { it.dateMillis }
                        .map { Pair(it.dateMillis, it.totalVolume) }
                }

                if (volumeLogs.size < 2) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Тут пока пусто. Завершите первую полноценную тренировку.",
                            fontSize = 12.sp,
                            color = GymTextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LineChartCanvas(data = volumeLogs, isVolume = true)
                }
            }
        }

        // 5. Body Weight Dynamics Card
        Card(
            colors = CardDefaults.cardColors(containerColor = GymSurface),
            border = BorderStroke(1.dp, GymSlateBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "Вес тела",
                        tint = GymOrangeAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Динамика веса тела (кг)", 
                        fontSize = 15.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = Color.White
                    )
                }
                Text(
                    "На основе ваших сохранений из раздела настроек профиля", 
                    fontSize = 11.sp, 
                    color = GymTextSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val weightChartData = remember(allWeightLogs) {
                    allWeightLogs.sortedBy { it.dateMillis }
                        .map { Pair(it.dateMillis, it.weight) }
                }

                if (weightChartData.size < 2) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Недостаточно записей. Измените свой вес в настройках для фиксации истории.",
                            fontSize = 12.sp,
                            color = GymTextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LineChartCanvas(data = weightChartData, isVolume = false, isWeight = true)
                }
            }
        }
    }
}

@Composable
fun WeeklyVolumeBarChart(data: List<Pair<Long, Double>>) {
    if (data.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Нет данных о тренировках за неделю", color = GymTextSecondary, fontSize = 12.sp)
        }
        return
    }

    val maxVal = data.maxOf { it.second }
    val maxValForLabel = if (maxVal == 0.0) 1000.0 else maxVal
    
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(GymDarkBackground, RoundedCornerShape(12.dp))
            .padding(top = 16.dp, bottom = 28.dp, start = 48.dp, end = 16.dp)
    ) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        // Horizontal gridlines
        val gridCount = 4
        for (i in 0 until gridCount) {
            val factor = i.toFloat() / (gridCount - 1)
            val yPos = height * factor
            val v = maxValForLabel - (factor * maxValForLabel)
            
            drawLine(
                color = GymSlateBorder.copy(alpha = 0.5f),
                start = Offset(0f, yPos),
                end = Offset(width, yPos),
                strokeWidth = 1f
            )
            
            // Text label
            val p = android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 24f
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText(
                String.format("%.0fкг", v),
                -115f,
                yPos + 8f,
                p
            )
        }

        // Draw Bars
        val barCount = data.size
        val gapRatio = 0.3f // 30% gap
        val totalBarSpace = width
        val singleGroupWidth = totalBarSpace / barCount
        val barWidth = singleGroupWidth * (1f - gapRatio)
        val barGap = singleGroupWidth * gapRatio

        data.forEachIndexed { i, pair ->
            val left = (i * singleGroupWidth) + (barGap / 2f)
            val ratio = if (maxValForLabel > 0) (pair.second / maxValForLabel).toFloat() else 0f
            val barHeight = height * ratio
            val top = height - barHeight

            // Draw rounded bar
            val rectPath = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = left,
                        top = top,
                        right = left + barWidth,
                        bottom = height,
                        topLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
                        topRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
                        bottomLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f),
                        bottomRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                    )
                )
            }
            drawPath(
                path = rectPath,
                color = GymOrangeAccent
            )

            // Draw value above bar
            if (pair.second > 0) {
                val valPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 22f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                drawContext.canvas.nativeCanvas.drawText(
                    String.format("%.0f", pair.second),
                    left + (barWidth / 2f),
                    top - 10f,
                    valPaint
                )
            }

            // Label at bottom
            val cal = Calendar.getInstance()
            cal.timeInMillis = pair.first
            val startLabel = SimpleDateFormat("dd.MM", Locale.getDefault()).format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 6)
            val endLabel = SimpleDateFormat("dd.MM", Locale.getDefault()).format(cal.time)
            val weekLabel = "$startLabel-$endLabel"

            val lblPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 20f
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText(
                weekLabel,
                left + (barWidth / 2f),
                height + 30f,
                lblPaint
            )
        }
    }
}

@Composable
fun LineChartCanvas(data: List<Pair<Long, Double>>, isVolume: Boolean = false, isWeight: Boolean = false) {
    if (data.size < 2) return
    val maxVal = data.maxOf { it.second }
    val minVal = data.minOf { it.second }
    val spread = if (maxVal == minVal) 10.0 else (maxVal - minVal)

    // Boundaries cushion
    val topBound = maxVal + (spread * 0.15)
    val bottomBound = if (minVal - (spread * 0.15) > 0) minVal - (spread * 0.15) else 0.0

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(GymDarkBackground, RoundedCornerShape(8.dp))
            .padding(top = 16.dp, bottom = 24.dp, start = 44.dp, end = 24.dp)
    ) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        // 1. Draw horizontal gridlines and axis labels manually
        val gridLinesCount = 4
        for (i in 0 until gridLinesCount) {
            val yFactor = i.toFloat() / (gridLinesCount - 1)
            val yPos = height * yFactor
            val value = topBound - (yFactor * (topBound - bottomBound))

            // Grid Line
            drawLine(
                color = GymSlateBorder.copy(alpha = 0.5f),
                start = Offset(0f, yPos),
                end = Offset(width, yPos),
                strokeWidth = 1f
            )

            // Numeric Label
            // Using DrawScope drawContext to draw text safely
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 28f
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText(
                if (isVolume) String.format("%.0fкг", value) else if (isWeight) String.format("%.1f кг", value) else String.format("%.1f", value),
                -110f, 
                yPos + 10f, 
                paint
            )
        }

        // 2. Draw Points & Line
        val totalPoints = data.size
        val pointsList = mutableListOf<Offset>()

        data.forEachIndexed { index, pair ->
            val divisor = if (totalPoints > 1) totalPoints - 1 else 1
            val xFactor = index.toFloat() / divisor
            val xPos = width * xFactor

            val yDivisor = if (topBound != bottomBound) topBound - bottomBound else 1.0
            val yHeightRatio = (pair.second - bottomBound) / yDivisor
            val yPos = (height - (height * yHeightRatio)).toFloat()

            pointsList.add(Offset(xPos, yPos))
            
            // Render dates below graph
            val dateLabel = SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date(pair.first))
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 24f
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText(
                dateLabel,
                xPos,
                height + 36f,
                paint
            )
        }

        // Draw connecting line of graph
        val linePath = Path()
        if (pointsList.isNotEmpty()) {
            linePath.moveTo(pointsList.first().x, pointsList.first().y)
            for (i in 1 until pointsList.size) {
                linePath.lineTo(pointsList[i].x, pointsList[i].y)
            }
            
            drawPath(
                path = linePath,
                color = if (isWeight) GymOrangeAccent else if (isVolume) GymGreenAccent else GymOrangeAccent,
                style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Draw circular dot markers
            pointsList.forEachIndexed { pIdx, offset ->
                drawCircle(
                    color = Color.White,
                    radius = 8f,
                    center = offset
                )
                drawCircle(
                    color = if (isWeight) GymOrangeAccent else if (isVolume) GymGreenAccent else GymOrangeAccent,
                    radius = 4f,
                    center = offset
                )
                
                // Overlay text value on top of dot
                val valPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 26f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                val rawVal = data[pIdx].second
                val hoverText = if (isVolume) String.format("%.0f", rawVal) else if (isWeight) String.format("%.1f кг", rawVal) else String.format("%.1f", rawVal)
                drawContext.canvas.nativeCanvas.drawText(
                    hoverText,
                    offset.x,
                    offset.y - 14f,
                    valPaint
                )
            }
        }
    }
}

// 4. --- SETTINGS BLOCK ---

@Composable
fun SettingsScreen(viewModel: GymViewModel) {
    val stats by viewModel.userStats.collectAsStateWithLifecycle()
    val workoutCount by viewModel.workoutLogs.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Настройки", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Задайте ваши параметры и цели для расчета БЖУ:", fontSize = 13.sp, color = GymTextSecondary, modifier = Modifier.padding(bottom = 20.dp))

        // Weight numeric selection
        Card(
            colors = CardDefaults.cardColors(containerColor = GymSurface),
            border = BorderStroke(1.dp, GymSlateBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ваш рабочий вес:", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text(String.format("%.1f кг", stats.weight), fontSize = 17.sp, fontWeight = FontWeight.Black, color = GymOrangeAccent)
                }
                
                Slider(
                    value = stats.weight.toFloat(),
                    onValueChange = { viewModel.updateWeight(it.toDouble()) },
                    valueRange = 40f..150f,
                    colors = SliderDefaults.colors(
                        thumbColor = GymOrangeAccent,
                        activeTrackColor = GymOrangeAccent,
                        inactiveTrackColor = GymSlateBorder
                    ),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }

        // Training Goal Selector
        Card(
            colors = CardDefaults.cardColors(containerColor = GymSurface),
            border = BorderStroke(1.dp, GymSlateBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Основная фитнес-цель:", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    val isMass = stats.goal == "mass"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isMass) GymOrangeAccent else GymSlateBorder)
                            .clickable { viewModel.updateGoal("mass") }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Набор массы (+профицит)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!isMass) GymOrangeAccent else GymSlateBorder)
                            .clickable { viewModel.updateGoal("loss") }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Сушка (дефицит ккал)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Activity Multiplier Selector
        Card(
            colors = CardDefaults.cardColors(containerColor = GymSurface),
            border = BorderStroke(1.dp, GymSlateBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Уровень физической активности:", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(modifier = Modifier.height(10.dp))

                val modes = listOf(
                    Triple("low", "Низкая", "Сидячая работа, 1-2 тр/нед"),
                    Triple("medium", "Средняя", "Активная работа, 3-4 тр/нед"),
                    Triple("high", "Высокая", "Тяжелый труд, 5+ тр/нед")
                )

                modes.forEach { (id, title, desc) ->
                    val isSelected = stats.activity == id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) GymOrangeAccent.copy(alpha = 0.15f) else Color.Transparent)
                            .border(1.dp, if (isSelected) GymOrangeAccent else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { viewModel.updateActivity(id) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { viewModel.updateActivity(id) },
                            colors = RadioButtonDefaults.colors(selectedColor = GymOrangeAccent, unselectedColor = GymTextSecondary)
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(desc, fontSize = 11.sp, color = GymTextSecondary)
                        }
                    }
                }
            }
        }

        // RIR (reps in reserve) checkpoint
        Card(
            colors = CardDefaults.cardColors(containerColor = GymSurface),
            border = BorderStroke(1.dp, GymSlateBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.updateRirEnabled(!stats.rirEnabled) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Включить учёт RIR (повторы в запасе)", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text("Добавит поле ввода RIR для каждого тренировочного подхода.", fontSize = 11.sp, color = GymTextSecondary)
                }
                Checkbox(
                    checked = stats.rirEnabled,
                    onCheckedChange = { viewModel.updateRirEnabled(it) },
                    colors = CheckboxDefaults.colors(checkedColor = GymOrangeAccent)
                )
            }
        }

        // Summary calculated calories formula card
        val bmr = stats.weight * 22
        val mult = when (stats.activity) {
            "low" -> 1.2
            "high" -> 1.6
            else -> 1.4
        }
        val targetTdee = bmr * mult
        val targetCalories = (if (stats.goal == "mass") targetTdee + 300.0 else targetTdee - 400.0).coerceAtLeast(500.0)
        val targetProt = (if (stats.goal == "mass") stats.weight * 2.0 else stats.weight * 2.2).coerceAtLeast(0.0)
        val targetFat = (if (stats.goal == "mass") stats.weight * 1.0 else stats.weight * 0.8).coerceAtLeast(0.0)
        val targetCarbs = ((targetCalories - (targetProt * 4.0) - (targetFat * 9.0)) / 4.0).coerceAtLeast(0.0)

        Card(
            colors = CardDefaults.cardColors(containerColor = GymDarkBackground),
            border = BorderStroke(1.dp, GymOrangeAccent),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Рекомендуемая норма БЖУ", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GymOrangeAccent)
                Text(
                    "Расчёт БЖУ выполнен на основе вашего рабочего веса (${stats.weight} кг), цели (${if (stats.goal == "mass") "набор массы" else "похудение"}) и активности.",
                    fontSize = 11.sp,
                    color = GymTextSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Калории", fontSize = 11.sp, color = GymTextSecondary)
                        Text("${targetCalories.toInt()} ккал", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Column {
                        Text("Белки (Б)", fontSize = 11.sp, color = GymTextSecondary)
                        Text("${targetProt.toInt()} г", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GymOrangeAccent)
                    }
                    Column {
                        Text("Жиры (Ж)", fontSize = 11.sp, color = GymTextSecondary)
                        Text("${targetFat.toInt()} г", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
                    }
                    Column {
                        Text("Углеводы (У)", fontSize = 11.sp, color = GymTextSecondary)
                        Text("${targetCarbs.toInt()} г", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GymGreenAccent)
                    }
                }
            }
        }
        
        // App Version
        Text(
            text = "Дневник тренировок v1.2 (Vanilla SQLite + Room DB)\nВсего завершено сессий: ${workoutCount.size}",
            fontSize = 11.sp,
            color = GymTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
    }
}

// -------------------------------------------------------------
// NEWLY ADDED COHESIVE CUSTOM COMPOSABLES (CALENDAR & CUSTOM IMAGES LIBRARY)
// -------------------------------------------------------------

@Composable
fun GymCalendarView(viewModel: GymViewModel, modifier: Modifier = Modifier) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val workoutLogs by viewModel.workoutLogs.collectAsStateWithLifecycle()
    
    var isExpanded by remember { mutableStateOf(false) }
    var calendarMonth by remember { mutableStateOf(Calendar.getInstance()) }
    
    // Parse current selectedDate into calendar instance
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    
    // Map of dates when workouts are completed
    val workoutDates = remember(workoutLogs) {
        workoutLogs.map { log ->
            val inst = Calendar.getInstance()
            inst.timeInMillis = log.dateMillis
            sdf.format(inst.time)
        }.toSet()
    }
    
    Card(
        colors = CardDefaults.cardColors(containerColor = GymSurface),
        border = BorderStroke(1.dp, GymSlateBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Selected Date Clickable to Expand
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Календарь",
                        tint = GymOrangeAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = formatDateRussian(selectedDate),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isExpanded) "Свернуть календарь" else "Развернуть календарь",
                            fontSize = 11.sp,
                            color = GymTextSecondary
                        )
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Индикатор",
                    tint = GymOrangeAccent
                )
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = GymSlateBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))
                
                // Month Navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val next = calendarMonth.clone() as Calendar
                            next.add(Calendar.MONTH, -1)
                            calendarMonth = next
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ArrowBackIos, contentDescription = "Пред. месяц", tint = GymOrangeAccent, modifier = Modifier.size(16.dp))
                    }
                    
                    val monthNameFormat = remember { SimpleDateFormat("LLLL yyyy", Locale("ru")) }
                    Text(
                        text = monthNameFormat.format(calendarMonth.time).replaceFirstChar { it.uppercase() },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    IconButton(
                        onClick = {
                            val next = calendarMonth.clone() as Calendar
                            next.add(Calendar.MONTH, 1)
                            calendarMonth = next
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = "След. месяц", tint = GymOrangeAccent, modifier = Modifier.size(16.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Days of week header
                Row(modifier = Modifier.fillMaxWidth()) {
                    val daysHeader = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
                    daysHeader.forEach { dayName ->
                        Text(
                            text = dayName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GymTextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Generate days grid
                val daysList = remember(calendarMonth) {
                    val list = mutableListOf<Calendar?>()
                    val c = calendarMonth.clone() as Calendar
                    c.set(Calendar.DAY_OF_MONTH, 1)
                    val firstDay = c.get(Calendar.DAY_OF_WEEK) // Sunday = 1, Monday = 2
                    val prependCount = if (firstDay == Calendar.SUNDAY) 6 else firstDay - Calendar.MONDAY
                    
                    for (i in 0 until prependCount) {
                        list.add(null)
                    }
                    
                    val maxDays = c.getActualMaximum(Calendar.DAY_OF_MONTH)
                    for (i in 1..maxDays) {
                        val dayCal = c.clone() as Calendar
                        dayCal.set(Calendar.DAY_OF_MONTH, i)
                        list.add(dayCal)
                    }
                    list
                }
                
                // Chunk into weeks (rows of 7)
                daysList.chunked(7).forEach { weekDays ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        weekDays.forEach { dayCal ->
                            if (dayCal == null) {
                                Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                            } else {
                                val dayStr = sdf.format(dayCal.time)
                                val isSelected = dayStr == selectedDate
                                val hasWorkout = workoutDates.contains(dayStr)
                                val dayNum = dayCal.get(Calendar.DAY_OF_MONTH)
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) GymOrangeAccent 
                                            else if (hasWorkout) GymGreenAccent.copy(alpha = 0.15f)
                                            else Color.Transparent
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) GymOrangeAccent 
                                                    else if (hasWorkout) GymGreenAccent.copy(alpha = 0.4f)
                                                    else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            viewModel.selectDateString(dayStr)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = dayNum.toString(),
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected || hasWorkout) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.Black 
                                                    else if (hasWorkout) GymGreenAccent 
                                                    else Color.White
                                        )
                                        if (hasWorkout && !isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(GymGreenAccent)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        // Pad the remaining items in row if week is incomplete
                        if (weekDays.size < 7) {
                            for (j in 0 until (7 - weekDays.size)) {
                                Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseIllustrationPlaceholder(category: String, modifier: Modifier = Modifier) {
    val gradient = when (category) {
        "Грудь" -> Brush.verticalGradient(listOf(Color(0xFF881337), GymSurface))
        "Спина" -> Brush.verticalGradient(listOf(Color(0xFF1E3A8A), GymSurface))
        "Ноги" -> Brush.verticalGradient(listOf(Color(0xFF065F46), GymSurface))
        "Плечи" -> Brush.verticalGradient(listOf(Color(0xFF581C87), GymSurface))
        "Руки" -> Brush.verticalGradient(listOf(Color(0xFF7C2D12), GymSurface))
        "Пресс" -> Brush.verticalGradient(listOf(Color(0xFF78350F), GymSurface))
        else -> Brush.verticalGradient(listOf(Color(0xFF3F3F46), GymSurface))
    }
    
    val categoryLabelEn = when (category) {
        "Грудь" -> "CHEST"
        "Спина" -> "BACK"
        "Ноги" -> "LEGS"
        "Плечи" -> "SHOULDERS"
        "Руки" -> "ARMS"
        "Пресс" -> "ABS"
        else -> "CORE"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(gradient)
            .border(1.dp, GymSlateBorder, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = GymOrangeAccent.copy(alpha = 0.8f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = categoryLabelEn,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = 0.15f),
                letterSpacing = 4.sp
            )
            Text(
                text = category,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = GymTextSecondary
            )
        }
    }
}

@Composable
fun ExerciseLibraryDialog(
    viewModel: GymViewModel,
    onDismiss: () -> Unit
) {
    val allExercises by viewModel.allExerciseInfo.collectAsStateWithLifecycle()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Все") }
    
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var expandedExerciseName by remember { mutableStateOf<String?>(null) }
    
    val categories = listOf("Все", "Грудь", "Спина", "Ноги", "Плечи", "Руки", "Пресс", "Другое")
    
    val filteredExercises = remember(allExercises, searchQuery, selectedCategory) {
        allExercises.filter { ex ->
            val matchesQuery = ex.name.contains(searchQuery, ignoreCase = true) || ex.technique.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == "Все" || ex.category.equals(selectedCategory, ignoreCase = true)
            matchesQuery && matchesCategory
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = GymDarkBackground),
            border = BorderStroke(1.dp, GymSlateBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = GymOrangeAccent)
                        Text(
                            text = "Справочник техники",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = Color.White)
                    }
                }
                
                // Add Custom Exercise trigger
                Button(
                    onClick = { showAddCustomDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = GymOrangeAccent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Создать упражнение", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 13.sp)
                }
                
                // Search bar
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Поиск упр. или техники...", color = GymTextSecondary) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = GymSurface,
                        unfocusedContainerColor = GymSurface,
                        disabledContainerColor = GymSurface,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = GymOrangeAccent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )
                
                // Muscle categories selector scroll
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    items(categories) { cat ->
                        val isSel = cat == selectedCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) GymOrangeAccent else GymSurface)
                                .border(1.dp, if (isSel) GymOrangeAccent else GymSlateBorder, RoundedCornerShape(8.dp))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.Black else Color.White
                            )
                        }
                    }
                }
                
                Divider(color = GymSlateBorder, thickness = 1.dp, modifier = Modifier.padding(bottom = 8.dp))
                
                // Exercises list
                if (filteredExercises.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Упражнения не найдены", color = GymTextSecondary, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredExercises) { exercise ->
                            val isExpanded = expandedExerciseName == exercise.name
                            ExerciseLibraryItemRow(
                                exercise = exercise,
                                isExpanded = isExpanded,
                                onToggleExpand = {
                                    expandedExerciseName = if (isExpanded) null else exercise.name
                                },
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (showAddCustomDialog) {
        AddCustomExerciseDialog(
            viewModel = viewModel,
            onDismiss = { showAddCustomDialog = false }
        )
    }
}

@Composable
fun ExerciseLibraryItemRow(
    exercise: ExerciseInfo,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    viewModel: GymViewModel
) {
    val context = LocalContext.current
    var showPredefinedPhotoSelector by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags: Int = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {}
            viewModel.updateExercisePhoto(exercise.name, uri.toString())
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = GymSurface),
        border = BorderStroke(1.dp, if (isExpanded) GymOrangeAccent else GymSlateBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(GymSlateBorder)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(exercise.category, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GymOrangeAccent)
                        }
                        if (exercise.isCustom) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(GymOrangeAccent.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Своё", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GymOrangeAccent)
                            }
                        }
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = GymOrangeAccent
                )
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Photo Illustration Frame
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    if (exercise.photoUri != null) {
                        AsyncImage(
                            model = exercise.photoUri,
                            contentDescription = exercise.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        ExerciseIllustrationPlaceholder(category = exercise.category, modifier = Modifier.fillMaxSize())
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Change Photo action row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { launcher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = GymSlateBorder),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(38.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = GymOrangeAccent, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Из галереи", fontSize = 11.sp, color = Color.White)
                    }

                    Button(
                        onClick = { showPredefinedPhotoSelector = true },
                        colors = ButtonDefaults.buttonColors(containerColor = GymSlateBorder),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(38.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = GymOrangeAccent, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Из заготовок", fontSize = 11.sp, color = Color.White)
                    }
                }
                
                if (exercise.technique.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Техника выполнения:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GymOrangeAccent
                    )
                    Text(
                        text = exercise.technique,
                        fontSize = 12.sp,
                        color = GymTextSecondary,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (exercise.isCustom) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.deleteExercise(exercise.name) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.15f)),
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Удалить", color = Color.Red, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    if (showPredefinedPhotoSelector) {
        PredefinedPhotoSelectorDialog(
            exerciseName = exercise.name,
            viewModel = viewModel,
            onDismiss = { showPredefinedPhotoSelector = false }
        )
    }
}

@Composable
fun PredefinedPhotoSelectorDialog(
    exerciseName: String,
    viewModel: GymViewModel,
    onDismiss: () -> Unit
) {
    val options = listOf(
        Triple("Жим лежа / Грудные", "Грудь", "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?auto=format&fit=crop&q=80&w=600"),
        Triple("Тяга блока / Спина", "Спина", "https://images.unsplash.com/photo-1605296867304-46d5465a25f1?auto=format&fit=crop&q=80&w=600"),
        Triple("Приседания / Ноги", "Ноги", "https://images.unsplash.com/photo-1574680096145-d05b474e2155?auto=format&fit=crop&q=80&w=600"),
        Triple("Армейский жим / Плечи", "Плечи", "https://images.unsplash.com/photo-1581009146145-b5ef050c2e1e?auto=format&fit=crop&q=80&w=600"),
        Triple("Подъем на бицепс / Руки", "Руки", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&q=80&w=600"),
        Triple("Скручивания / Пресс", "Пресс", "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?auto=format&fit=crop&q=80&w=600"),
        Triple("Беговая дорожка / Кардио", "Кардио", "https://images.unsplash.com/photo-1476480862126-209bfaa8edc8?auto=format&fit=crop&q=80&w=600")
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = GymSurface),
            border = BorderStroke(1.dp, GymSlateBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Выберите заготовку фото",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(options) { (label, group, url) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(GymDarkBackground)
                                .clickable {
                                    viewModel.updateExercisePhoto(exerciseName, url)
                                    onDismiss()
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AsyncImage(
                                model = url,
                                contentDescription = label,
                                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Column {
                                Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                Text(group, fontSize = 11.sp, color = GymTextSecondary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = GymSlateBorder),
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Отмена", color = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddCustomExerciseDialog(
    viewModel: GymViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var technique by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Грудь") }
    
    val categories = listOf("Грудь", "Спина", "Ноги", "Плечи", "Руки", "Пресс", "Другое")
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = GymSurface),
            border = BorderStroke(1.dp, GymSlateBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Новое упражнение",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = GymOrangeAccent,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GymOrangeAccent,
                        unfocusedBorderColor = GymSlateBorder,
                        focusedLabelColor = GymOrangeAccent,
                        unfocusedLabelColor = GymTextSecondary,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )
                
                OutlinedTextField(
                    value = technique,
                    onValueChange = { technique = it },
                    label = { Text("Описание техники") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GymOrangeAccent,
                        unfocusedBorderColor = GymSlateBorder,
                        focusedLabelColor = GymOrangeAccent,
                        unfocusedLabelColor = GymTextSecondary,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )
                
                Text(
                    "Группа мышц",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GymTextSecondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = cat == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) GymOrangeAccent else GymDarkBackground)
                                .border(1.dp, if (isSelected) GymOrangeAccent else GymSlateBorder, RoundedCornerShape(6.dp))
                                .clickable { category = cat }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else Color.White
                            )
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена", color = GymTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                viewModel.addCustomExercise(name, technique, category)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GymOrangeAccent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Создать", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddExerciseToWorkoutDialog(
    allExercises: List<ExerciseInfo>,
    onDismiss: () -> Unit,
    onAdd: (name: String, sets: Int, reps: String, restSec: Int, technique: String) -> Unit
) {
    var selectedExerciseName by remember { mutableStateOf("") }
    var customExerciseName by remember { mutableStateOf("") }
    var useSelectedFromLibrary by remember { mutableStateOf(true) }
    
    var setsVal by remember { mutableStateOf("3") }
    var repsVal by remember { mutableStateOf("10-12") }
    var restVal by remember { mutableStateOf("120") }
    var techniqueVal by remember { mutableStateOf("") }
    
    var filterText by remember { mutableStateOf("") }
    val filteredNames = remember(allExercises, filterText) {
        allExercises.map { it.name }.filter { it.contains(filterText, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = GymSurface),
            border = BorderStroke(1.dp, GymSlateBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Добавить упражнение в сессию",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = GymOrangeAccent,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(GymDarkBackground)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (useSelectedFromLibrary) GymSurface else Color.Transparent)
                            .clickable { useSelectedFromLibrary = true }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Из справочника", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (useSelectedFromLibrary) Color.White else GymTextSecondary)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!useSelectedFromLibrary) GymSurface else Color.Transparent)
                            .clickable { useSelectedFromLibrary = false }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Вручную", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (!useSelectedFromLibrary) Color.White else GymTextSecondary)
                    }
                }
                
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    if (useSelectedFromLibrary) {
                        OutlinedTextField(
                            value = filterText,
                            onValueChange = { filterText = it },
                            placeholder = { Text("Быстрый поиск...", color = GymTextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GymOrangeAccent,
                                unfocusedBorderColor = GymSlateBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        
                        Box(
                            modifier = Modifier
                                .height(120.dp)
                                .fillMaxWidth()
                                .border(1.dp, GymSlateBorder, RoundedCornerShape(8.dp))
                                .background(GymDarkBackground)
                        ) {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(filteredNames) { name ->
                                    val isSelected = name == selectedExerciseName
                                    Text(
                                        text = name,
                                        fontSize = 12.sp,
                                        color = if (isSelected) Color.Black else Color.White,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (isSelected) GymOrangeAccent else Color.Transparent)
                                            .clickable {
                                                selectedExerciseName = name
                                                val infoObj = allExercises.find { it.name == name }
                                                techniqueVal = infoObj?.technique ?: ""
                                            }
                                            .padding(8.dp)
                                    )
                                }
                            }
                        }
                        
                        if (selectedExerciseName.isNotEmpty()) {
                            Text(
                                "Выбрано: $selectedExerciseName",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GymOrangeAccent,
                                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                            )
                        } else {
                            Text(
                                "Выберите упражнение из списка",
                                fontSize = 11.sp,
                                color = GymTextSecondary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = customExerciseName,
                            onValueChange = { customExerciseName = it },
                            label = { Text("Название упражнения") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GymOrangeAccent,
                                unfocusedBorderColor = GymSlateBorder,
                                focusedLabelColor = GymOrangeAccent,
                                unfocusedLabelColor = GymTextSecondary,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        )
                        
                        OutlinedTextField(
                            value = techniqueVal,
                            onValueChange = { techniqueVal = it },
                            label = { Text("Техника / описание захода") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GymOrangeAccent,
                                unfocusedBorderColor = GymSlateBorder,
                                focusedLabelColor = GymOrangeAccent,
                                unfocusedLabelColor = GymTextSecondary,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        )
                    }
                    
                    Text("Параметры:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(vertical = 4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = setsVal,
                            onValueChange = { setsVal = it.filter { c -> c.isDigit() } },
                            label = { Text("Подходы") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GymOrangeAccent,
                                unfocusedBorderColor = GymSlateBorder,
                                focusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = repsVal,
                            onValueChange = { repsVal = it },
                            label = { Text("Повторы") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GymOrangeAccent,
                                unfocusedBorderColor = GymSlateBorder,
                                focusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1.5f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = restVal,
                        onValueChange = { restVal = it.filter { c -> c.isDigit() } },
                        label = { Text("Отдых в секундах") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GymOrangeAccent,
                            unfocusedBorderColor = GymSlateBorder,
                            focusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена", color = GymTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val finalName = if (useSelectedFromLibrary) selectedExerciseName else customExerciseName
                            if (finalName.isNotBlank()) {
                                onAdd(
                                    finalName,
                                    setsVal.toIntOrNull() ?: 3,
                                    repsVal.ifBlank { "10" },
                                    restVal.toIntOrNull() ?: 120,
                                    techniqueVal
                                )
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GymOrangeAccent),
                        shape = RoundedCornerShape(8.dp),
                        enabled = (useSelectedFromLibrary && selectedExerciseName.isNotBlank()) || (!useSelectedFromLibrary && customExerciseName.isNotBlank())
                    ) {
                        Text("Добавить", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
