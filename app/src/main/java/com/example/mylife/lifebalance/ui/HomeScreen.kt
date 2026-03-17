package com.example.mylife.lifebalance.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.mylife.lifebalance.data.Task
import com.example.mylife.lifebalance.data.RepeatType
import com.example.mylife.lifebalance.viewmodel.LifeBalanceViewModel
import android.app.TimePickerDialog
import androidx.compose.ui.platform.LocalContext
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.content.ClipData
import android.content.ClipboardManager
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.example.lifebalance.BuildConfig
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import com.example.lifebalance.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.speech.RecognizerIntent
import androidx.compose.ui.res.stringResource
import com.example.mylife.lifebalance.ui.components.AutoResizedText
import com.example.mylife.lifebalance.ui.components.GroupAutoResizedText
import com.example.mylife.lifebalance.ui.components.TextResizeController
import com.example.mylife.lifebalance.ui.getAppLocale


@Composable
fun HomeScreen(
    viewModel: LifeBalanceViewModel,
    onNavigate: (Screen) -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val allTasks by viewModel.allTasks.collectAsState()

    var showAddTaskDialog by remember { mutableStateOf<LocalDate?>(null) }
    var selectedDateForDetails by remember { mutableStateOf<LocalDate?>(null) }
    
    // Проверяем и переносим задачи при загрузке экрана
    LaunchedEffect(Unit) {
        viewModel.checkAndRescheduleTasks()
    }
    
    // Состояние для отслеживания смещения недель
    var weekOffset by remember { mutableStateOf(0) }
    val swipeChannel = remember { Channel<Int>(Channel.CONFLATED) }
    
    // Анимация для переключения недель (плавная, как в Google Calendar)
    val animatedWeekOffset by animateIntAsState(
        targetValue = weekOffset,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "weekOffset"
    )
    
    // Применяем изменение недели при свайпе
    LaunchedEffect(Unit) {
        swipeChannel.receiveAsFlow().collect { direction ->
            weekOffset += direction
        }
    }

    // Находим понедельник текущей недели с учетом смещения
    val today = LocalDate.now()
    val mondayOfCurrentWeek = remember {
        val dayOfWeek = today.dayOfWeek.value // 1 = понедельник, 7 = воскресенье
        val daysToSubtract = if (dayOfWeek == 1) 0 else dayOfWeek - 1
        today.minusDays(daysToSubtract.toLong())
    }
    val mondayOfWeek = mondayOfCurrentWeek.plusWeeks(animatedWeekOffset.toLong())

    // Получаем задачи на текущую неделю (понедельник - воскресенье)
    val startDate = mondayOfWeek
    val endDate = startDate.plusDays(6)
    val tasksByDate = remember(allTasks, startDate, endDate) {
        allTasks
            .filter { it.date >= startDate && it.date <= endDate }
            .groupBy { it.date }
            .toSortedMap()
    }
    
    // Форматирование дат для отображения диапазона недели
    val appLocale = getAppLocale()
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", appLocale)
    val weekRangeText = "${startDate.format(dateFormatter)} - ${endDate.format(dateFormatter)}"

    Scaffold(
        topBar = {
            TopBarWithIcons(
                onNavigate = { screen ->
                    navController.navigate(screen.route)
                }
            )
        },
        content = { paddingValues ->
            val density = LocalDensity.current
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .pointerInput(Unit) {
                        var totalDrag = 0f
                        val threshold = with(density) { 80.dp.toPx() } // Уменьшили порог для более чувствительного свайпа
                        detectDragGestures(
                            onDragEnd = {
                                // Применяем изменение только если общее смещение достаточно большое
                                if (kotlin.math.abs(totalDrag) > threshold) {
                                    swipeChannel.trySend(if (totalDrag > 0) -1 else 1)
                                }
                                totalDrag = 0f
                            }
                        ) { change, dragAmount ->
                            // Фильтруем только горизонтальные движения (более строгий фильтр)
                            val horizontalRatio = kotlin.math.abs(dragAmount.x) / (kotlin.math.abs(dragAmount.x) + kotlin.math.abs(dragAmount.y) + 0.001f)
                            if (horizontalRatio > 0.6f) { // Минимум 60% движения должно быть горизонтальным
                                totalDrag += dragAmount.x
                            }
                        }
                    },
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Анимированное перелистывание недели
                AnimatedContent(
                    targetState = animatedWeekOffset,
                    transitionSpec = {
                        // Определяем направление анимации
                        val slideDirection = if (targetState > initialState) {
                            // Переход к следующей неделе (слайд вправо)
                            1 // Слайд вправо
                        } else {
                            // Переход к предыдущей неделе (слайд влево)
                            -1 // Слайд влево
                        }
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth * slideDirection },
                            animationSpec = tween(
                                durationMillis = 300,
                                easing = FastOutSlowInEasing
                            )
                        ) togetherWith slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth * slideDirection },
                            animationSpec = tween(
                                durationMillis = 300,
                                easing = FastOutSlowInEasing
                            )
                        )
                    },
                    label = "weekTransition"
                ) { currentWeekOffset ->
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Генерируем дни недели начиная с понедельника
                        val currentMondayOfWeek = mondayOfCurrentWeek.plusWeeks(currentWeekOffset.toLong())
                        val currentStartDate = currentMondayOfWeek
                        val currentEndDate = currentStartDate.plusDays(6)
                        val currentTasksByDate = remember(allTasks, currentStartDate, currentEndDate) {
                            allTasks
                                .filter { it.date >= currentStartDate && it.date <= currentEndDate }
                                .groupBy { it.date }
                                .toSortedMap()
                        }
                        
                        repeat(7) { dayOffset ->
                            val date = currentMondayOfWeek.plusDays(dayOffset.toLong())
                            val dayTasks = currentTasksByDate[date] ?: emptyList()
                            val completedCount = dayTasks.count { it.isCompleted }
                            val totalCount = dayTasks.size

                            DayTaskCard(
                                date = date,
                                tasks = dayTasks,
                                completedCount = completedCount,
                                totalCount = totalCount,
                                onAddClick = { showAddTaskDialog = date },
                                onCardClick = {
                                    if (dayTasks.isNotEmpty()) {
                                        selectedDateForDetails = date
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    )

    // Диалог добавления задачи
    showAddTaskDialog?.let { date ->
        AddTaskDialog(
            date = date,
            onDismiss = { showAddTaskDialog = null },
            onSave = { title, description, time, hasNotification, notificationSound, autoReschedule, repeatType, repeatEndDate ->
                viewModel.addTask(title, description, date, time, hasNotification, notificationSound, autoReschedule = autoReschedule, repeatType = repeatType, repeatEndDate = repeatEndDate)
                showAddTaskDialog = null
            }
        )
    }

    // Диалог просмотра и редактирования задач
    selectedDateForDetails?.let { date ->
        TasksDetailsDialog(
            date = date,
            tasks = tasksByDate[date] ?: emptyList(),
            viewModel = viewModel,
            onDismiss = { selectedDateForDetails = null }
        )
    }
}

@Composable
fun WeekHeader(
    startDate: LocalDate,
    endDate: LocalDate,
    weekOffset: Int,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onTodayClick: () -> Unit
) {
    val appLocale = getAppLocale()
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM", appLocale)
    val today = LocalDate.now()
    val isCurrentWeek = weekOffset == 0
    
    // Форматируем диапазон дат
    val startDateText = startDate.format(dateFormatter).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(appLocale) else it.toString()
    }
    val endDateText = endDate.format(dateFormatter).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(appLocale) else it.toString()
    }
    
    // Если неделя в одном месяце, показываем только один месяц
    val weekRangeText = if (startDate.month == endDate.month && startDate.year == endDate.year) {
        "${startDate.dayOfMonth} - ${endDate.dayOfMonth} ${startDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", appLocale)).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(appLocale) else it.toString()
        }}"
    } else {
        "$startDateText - $endDateText"
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Кнопка "Назад"
        IconButton(
            onClick = onPreviousWeek,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Предыдущая неделя",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        // Центральная часть - диапазон дат и кнопка "Сегодня"
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = weekRangeText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            // Кнопка "Сегодня" (показываем только если не текущая неделя)
            if (!isCurrentWeek) {
                TextButton(
                    onClick = onTodayClick,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "Сегодня",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // Кнопка "Вперед"
        IconButton(
            onClick = onNextWeek,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Следующая неделя",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun DayTaskCard(
    date: LocalDate,
    tasks: List<Task>,
    completedCount: Int,
    totalCount: Int,
    onAddClick: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appLocale = getAppLocale()
    val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EE", appLocale)
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM", appLocale)
    val today = LocalDate.now()
    val isToday = date == today
    val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY

    val dayOfWeekText = date.format(dayOfWeekFormatter).uppercase()
    val dayNumber = date.dayOfMonth.toString()
    val monthText = date.format(monthFormatter)


    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .border(
                width = 1.dp,                       // толщина линии
                color = if (isToday) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline, // цвет (можно свой)

                shape = RoundedCornerShape(12.dp)   // та же форма, что у карточки
            )
            .then(
                if (tasks.isNotEmpty()) {
                    Modifier.clickable(onClick = onCardClick)
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isToday)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 0.dp, vertical = 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Левая часть - дата
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        color = if (isToday) 
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = dayOfWeekText,
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        isToday -> MaterialTheme.colorScheme.onPrimary
                        isWeekend -> Color(0xFFE53935) // Красный для выходных
                        else -> MaterialTheme.colorScheme.onSecondaryContainer
                    },
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dayNumber,
                    style = MaterialTheme.typography.titleLarge,
                    color = when {
                        isToday -> MaterialTheme.colorScheme.onPrimary
                        isWeekend -> Color(0xFFE53935)
                        else -> MaterialTheme.colorScheme.onSecondaryContainer
                    },
                    fontWeight = FontWeight.Bold
                )
                val appLocaleForMonth = getAppLocale()
                Text(
                    text = monthText.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(appLocaleForMonth) else it.toString()
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        isToday -> MaterialTheme.colorScheme.onPrimary
                        isWeekend -> Color(0xFFE53935)
                        else -> MaterialTheme.colorScheme.onSecondaryContainer
                    },
                    fontWeight = FontWeight.Normal
                )
            }

            // Правая часть - информация о задачах
            Column(
                modifier = Modifier
                    .weight(2f)
                    .padding(end = 8.dp), // отступ справа
                horizontalAlignment = Alignment.End
            ) {
                val context = LocalContext.current
                // Текст о количестве задач
                Text(
                    text = when {
                        totalCount == 0 -> context.resources.getQuantityString(R.plurals.tasks_count, 0, 0)
                        completedCount == totalCount -> stringResource(R.string.tasks_all_completed)
                        totalCount == 1 && completedCount == 0 -> stringResource(R.string.task_one_unsolved)
                        else -> context.resources.getQuantityString(R.plurals.tasks_count, totalCount, totalCount)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                // Прогресс
                if (totalCount > 0) {
                    Text(
                        text = "$completedCount/$totalCount",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize * 0.85f,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )

                    // Прогресс-бар
                    LinearProgressIndicator(
                        progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f,
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(6.dp)



                            .clip(RoundedCornerShape(3.dp)),
                        color = when {
                            completedCount == totalCount -> MaterialTheme.colorScheme.secondaryContainer // Для выполненных
                            completedCount > 0 -> Color(0xFFFFB74D) // Оранжевый для частично выполненных
                            else -> Color(0xFFE53935) // Красный для невыполненных
                        },
                        trackColor = Color(0xFFE53935)
                    )
               }
            else {
                    LinearProgressIndicator(
                        progress = 0f,
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(6.dp)
                            .border(
                                width = 1.dp,                       // толщина линии
                                color = if (isToday) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline, // цвет (можно свой)
                                shape = RoundedCornerShape(3.dp)   // та же форма, что у карточки
                            )
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color.Transparent,
                        trackColor = MaterialTheme.colorScheme.background
                    )
                }
            }

            // Кнопка добавления
            FloatingActionButton(
                onClick = onAddClick,
                modifier = Modifier
                    .size(60.dp) //размер кнопки
                    .padding(12.dp),
                containerColor = if (isToday) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.secondaryContainer,
                contentColor = if (isToday) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSecondaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Добавить задачу",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

//ДИАЛОГ ДОБАВЛЕНИЯ ЗАДАЧИ
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    date: LocalDate,
    onDismiss: () -> Unit,
    onSave: (String, String, LocalTime?, Boolean, String, Boolean, RepeatType, LocalDate?) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var specifyTime by remember { mutableStateOf(false) }
    var selectedTime by remember { mutableStateOf<LocalTime?>(null) }
    var hasNotification by remember { mutableStateOf(false) }
    var notificationSound by remember { mutableStateOf("default") }
    var selectedRepeatType by remember { mutableStateOf(RepeatType.NONE) }
    var showRepeatDialog by remember { mutableStateOf(false) }
    var autoReschedule by remember { mutableStateOf(false) }
    var hasEndDate by remember { mutableStateOf(false) }
    var endDate by remember { mutableStateOf(LocalDate.now().plusMonths(1)) }
    var showDatePicker by remember { mutableStateOf(false) }

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    val appLocale = getAppLocale()
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", appLocale)
    val formattedDate = date.format(dateFormatter).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(appLocale) else it.toString()
    }

    // День недели
    val dayOfWeek = date.format(
        DateTimeFormatter.ofPattern("EEEE", appLocale)
    ).replaceFirstChar { it.titlecase(appLocale) }

    // FocusRequester для автоматического фокуса на поле ввода
    val focusRequester = remember { FocusRequester() }

    // Launcher для распознавания речи
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            spokenText?.firstOrNull()?.let { text ->
                title = if (title.isBlank()) text else "$title $text"
            }
        }
    }

    // Автоматически устанавливаем фокус при открытии диалога
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        val scrollState = rememberScrollState()
        
        Surface(
            modifier = Modifier
                .fillMaxWidth(1f)
                .fillMaxHeight(1f)
                .imePadding(), // Учитываем клавиатуру
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                // Прокручиваемая часть контента
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Заголовок с датой и кнопкой записи голосом
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Text(
                                text = dayOfWeek,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                        // Кнопка записи текста голосом справа
                        IconButton(
                            onClick = {
                                val appLocale = getAppLocale(context)
                                val speechPrompt = context.resources.getString(R.string.speech_prompt)
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, appLocale.language)
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, speechPrompt)
                                }
                                // Проверяем доступность распознавания речи
                                if (intent.resolveActivity(context.packageManager) != null) {
                                    try {
                                        speechLauncher.launch(intent)
                                    } catch (e: Exception) {
                                        if (BuildConfig.DEBUG) {
                                            Log.e("SpeechRecognition", context.resources.getString(R.string.speech_recognition_error), e)
                                        }
                                    }
                                } else {
                                    if (BuildConfig.DEBUG) {
                                        Log.e("SpeechRecognition", context.resources.getString(R.string.speech_recognition_not_available))
                                    }
                                }
                            },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_micro),
                                    contentDescription = "Запись текста голосом",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Поле ввода дела (активное при открытии)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(text = stringResource(R.string.add_task)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .focusRequester(focusRequester),
                        singleLine = false,
                        maxLines = Int.MAX_VALUE,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.tertiary,
                            unfocusedContainerColor = MaterialTheme.colorScheme.tertiary
                        )
                    )

                    // Кнопка "Повтор дела"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.repeat_task),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(
                            onClick = { showRepeatDialog = true }
                        ) {
                            Text(
                                text = selectedRepeatType.getLocalizedDisplayName(context),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }



                    // Переключатель "Дата окончания повтора дела"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.repeat_end_date),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = hasEndDate,
                            onCheckedChange = { hasEndDate = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,      // цвет кружочка когда включён
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer, // цвет полоски когда включён
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,  // цвет кружочка когда выключен
                                uncheckedTrackColor = MaterialTheme.colorScheme.surface // цвет полоски когда выключен
                            )
                        )
                    }

                    // Выбор даты окончания (если включен)
                    if (hasEndDate) {
                        val appLocale = getAppLocale()
                        val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", appLocale)
                        val formattedEndDate = endDate.format(dateFormatter).replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(appLocale) else it.toString()
                        }
                        
                        TextButton(
                            onClick = { 
                                showDatePicker = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = formattedEndDate,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }


                    // Переключатель "Автоперенос дел"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.automove_tasks),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = autoReschedule,
                            onCheckedChange = { autoReschedule = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,      // цвет кружочка когда включён
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer, // цвет полоски когда включён
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,  // цвет кружочка когда выключен
                                uncheckedTrackColor = MaterialTheme.colorScheme.surface // цвет полоски когда выключен
                            )
                        )
                    }


                }
                
                // Кнопки внизу - фиксированные, всегда видны
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding(), // Учитываем навигационную панель
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = stringResource(R.string.cancel).uppercase(Locale.getDefault()),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSave(title, description, selectedTime, hasNotification, notificationSound, autoReschedule, selectedRepeatType, if (hasEndDate) endDate else null)
                            }
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.save).uppercase(Locale.getDefault()),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
    
    // Диалог выбора типа повторения
    if (showRepeatDialog) {
        AlertDialog(
            onDismissRequest = { showRepeatDialog = false },
            title = { Text(text = stringResource(R.string.repeat_task),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondary) },
            text = {
                Column {
                    RepeatType.values().forEachIndexed { index, repeatType ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedRepeatType = repeatType
                                    showRepeatDialog = false
                                }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = repeatType.getLocalizedDisplayName(context),
                                color = if (selectedRepeatType == repeatType) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurface
                            )
                            if (selectedRepeatType == repeatType) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Выбрано",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (index < RepeatType.values().size - 1) {
                            Divider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRepeatDialog = false }) {
                    Text(text = stringResource(R.string.cancel).uppercase(Locale.getDefault()))
                }
            }
        )
    }
    
    // Диалог выбора даты окончания
    if (showDatePicker) {
        var selectedDate by remember { mutableStateOf(endDate) }
        
        // Синхронизируем selectedDate с endDate при открытии диалога
        LaunchedEffect(showDatePicker) {
            if (showDatePicker) {
                selectedDate = endDate
            }
        }
        
        Dialog(onDismissRequest = { showDatePicker = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Темный заголовок
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // День недели (полностью)
                            val appLocale = getAppLocale()
                            val dayOfWeek = selectedDate.format(
                                DateTimeFormatter.ofPattern("EEEE", appLocale)
                            ).replaceFirstChar { it.titlecase(appLocale) }

                            Text(
                                text = dayOfWeek,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Дата: 09 декабря 2025
                            val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", appLocale)
                            val formattedDate = selectedDate.format(dateFormatter).replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(appLocale) else it.toString()
                            }

                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Календарь
                    DatePickerCalendar(
                        initialDate = selectedDate,
                        onDateSelected = { newDate ->
                            selectedDate = newDate
                        }
                    )

                    // Кнопки внизу
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text(text = stringResource(R.string.cancel).uppercase(Locale.getDefault()))
                        }
                        TextButton(onClick = { 
                            endDate = selectedDate
                            showDatePicker = false 
                        }) {
                            Text(text = stringResource(R.string.select).uppercase(Locale.getDefault()))
                        }
                    }
                }
            }
        }
    }
}

//ФУНКЦИЯ РЕДАКТИРОВАТЬ
@Composable
fun EditTaskDialog(
    task: Task,
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(task.title) }
    var selectedRepeatType by remember { mutableStateOf(task.repeatType) }
    var autoReschedule by remember { mutableStateOf(task.autoReschedule) }
    var hasEndDate by remember { mutableStateOf(task.repeatEndDate != null) }
    var endDate by remember { mutableStateOf(task.repeatEndDate ?: LocalDate.now().plusMonths(1)) }
    var showRepeatDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    // 🟢 1. СОЗДАЁМ FocusRequester
    val focusRequester = remember { FocusRequester() }

    // Launcher для распознавания речи
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            spokenText?.firstOrNull()?.let { text ->
                title = if (title.isBlank()) text else "$title $text"
            }
        }
    }

    // 🟢 2. ДЕЛАЕМ, ЧТОБЫ ПРИ ОТКРЫТИИ ОКНА ПОЛЕ СРАЗУ АКТИВИРОВАЛОСЬ
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        val scrollState = rememberScrollState()

        Surface(
            modifier = Modifier
                .fillMaxWidth(1f)
                .fillMaxHeight(1f)
                .imePadding(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Заголовок с кнопкой записи голосом
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.edit),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.align(Alignment.CenterStart)
                        )
                        // Кнопка записи текста голосом справа
                        IconButton(
                            onClick = {
                                val appLocale = getAppLocale(context)
                                val speechPrompt = context.resources.getString(R.string.speech_prompt)
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, appLocale.language)
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, speechPrompt)
                                }
                                // Проверяем доступность распознавания речи
                                if (intent.resolveActivity(context.packageManager) != null) {
                                    try {
                                        speechLauncher.launch(intent)
                                    } catch (e: Exception) {
                                        if (BuildConfig.DEBUG) {
                                            Log.e("SpeechRecognition", context.resources.getString(R.string.speech_recognition_error), e)
                                        }
                                    }
                                } else {
                                    if (BuildConfig.DEBUG) {
                                        Log.e("SpeechRecognition", context.resources.getString(R.string.speech_recognition_not_available))
                                    }
                                }
                            },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_micro),
                                    contentDescription = "Запись текста голосом",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Твой OutlinedTextField
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(text = stringResource(R.string.edit_task)) },

                        // 🟢 3. ПРИКРЕПЛЯЕМ FocusRequester
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),

                        singleLine = false,
                        maxLines = Int.MAX_VALUE,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.tertiary,
                            unfocusedContainerColor = MaterialTheme.colorScheme.tertiary
                        )
                    )

                    // Кнопка "Повтор дела"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.repeat_task),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(
                            onClick = { showRepeatDialog = true }
                        ) {
                            Text(
                                text = selectedRepeatType.getLocalizedDisplayName(context),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }


                    // Переключатель "Дата окончания повтора дела"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.repeat_end_date),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = hasEndDate,
                            onCheckedChange = { hasEndDate = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,      // цвет кружочка когда включён
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer, // цвет полоски когда включён
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,  // цвет кружочка когда выключен
                                uncheckedTrackColor = MaterialTheme.colorScheme.surface // цвет полоски когда выключен
                            )
                        )
                    }

                    // Выбор даты окончания (если включен)
                    if (hasEndDate) {
                        val appLocale = getAppLocale()
                        val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", appLocale)
                        val formattedEndDate = endDate.format(dateFormatter).replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(appLocale) else it.toString()
                        }
                        
                        TextButton(
                            onClick = { 
                                showDatePicker = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = formattedEndDate,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }


                    // Переключатель "Автоперенос дел"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.automove_tasks),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = autoReschedule,
                            onCheckedChange = { autoReschedule = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,      // цвет кружочка когда включён
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer, // цвет полоски когда включён
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,  // цвет кружочка когда выключен
                                uncheckedTrackColor = MaterialTheme.colorScheme.surface // цвет полоски когда выключен
                            )
                        )
                    }
                }
                
                // Кнопки внизу
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = stringResource(R.string.cancel).uppercase(Locale.getDefault()),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSave(task.copy(
                                    title = title,
                                    repeatType = selectedRepeatType,
                                    autoReschedule = autoReschedule,
                                    repeatEndDate = if (hasEndDate) endDate else null
                                ))
                            }
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.save).uppercase(Locale.getDefault()),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
    
    // Диалог выбора типа повторения
    if (showRepeatDialog) {
        AlertDialog(
            onDismissRequest = { showRepeatDialog = false },
            title = { Text(text = stringResource(R.string.repeat_task),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondary) },
            text = {
                Column {
                    RepeatType.values().forEachIndexed { index, repeatType ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedRepeatType = repeatType
                                    showRepeatDialog = false
                                }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = repeatType.getLocalizedDisplayName(context),
                                color = if (selectedRepeatType == repeatType) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurface
                            )
                            if (selectedRepeatType == repeatType) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Выбрано",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (index < RepeatType.values().size - 1) {
                            Divider(color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRepeatDialog = false }) {
                    Text(text = stringResource(R.string.cancel).uppercase(Locale.getDefault()))
                }
            }
        )
    }
    
    // Диалог выбора даты окончания
    if (showDatePicker) {
        var selectedDate by remember { mutableStateOf(endDate) }
        
        // Синхронизируем selectedDate с endDate при открытии диалога
        LaunchedEffect(showDatePicker) {
            if (showDatePicker) {
                selectedDate = endDate
            }
        }
        
        Dialog(onDismissRequest = { showDatePicker = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Темный заголовок
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // День недели (полностью)
                            val appLocale = getAppLocale()
                            val dayOfWeek = selectedDate.format(
                                DateTimeFormatter.ofPattern("EEEE", appLocale)
                            ).replaceFirstChar { it.titlecase(appLocale) }

                            Text(
                                text = dayOfWeek,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Дата: 09 декабря 2025
                            val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", appLocale)
                            val formattedDate = selectedDate.format(dateFormatter).replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(appLocale) else it.toString()
                            }

                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Календарь
                    DatePickerCalendar(
                        initialDate = selectedDate,
                        onDateSelected = { newDate ->
                            selectedDate = newDate
                        }
                    )

                    // Кнопки внизу
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text(text = stringResource(R.string.cancel).uppercase(Locale.getDefault()))
                        }
                        TextButton(onClick = { 
                            endDate = selectedDate
                            showDatePicker = false 
                        }) {
                            Text(text = stringResource(R.string.select).uppercase(Locale.getDefault()))
                        }
                    }
                }
            }
        }
    }
}

//ФУНКЦИЯ СОЗДАТЬ ЗАДАЧУ
@Composable
fun TasksDetailsDialog(
    date: LocalDate,
    tasks: List<Task>,
    viewModel: LifeBalanceViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    var showActionDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showCopyDialog by remember { mutableStateOf(false) }
    var taskToMove by remember { mutableStateOf<Task?>(null) }
    var taskToCopy by remember { mutableStateOf<Task?>(null) }

    val appLocale = getAppLocale()
    val dayOfWeek = date.format(
        DateTimeFormatter.ofPattern("EEEE", appLocale)
    ).replaceFirstChar { it.titlecase(appLocale) }

    val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", appLocale)
    val formattedDate = date.format(dateFormatter).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(appLocale) else it.toString()
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .wrapContentHeight(),
                    //.heightIn(max = 600.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    // Заголовок
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {

                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterStart)  // всё выравниваем влево
                        ) {
                            Text(
                                text = dayOfWeek,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // Кнопка закрытия справа
                        FloatingActionButton(
                            onClick = onDismiss,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(40.dp), // маленькая круглая кнопка
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp,
                                focusedElevation = 0.dp,
                                hoveredElevation = 0.dp
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Закрыть",
                                modifier = Modifier.size(24.dp)
                            )
                        }

                    }

                    Divider(color = MaterialTheme.colorScheme.outline)

                    // Прокручиваемый список задач
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 500.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        tasks.forEachIndexed { index, task ->
                            TaskItem(
                                task = task,
                                isSelected = selectedTask?.id == task.id,
                                onToggleComplete = {
                                    viewModel.toggleTaskCompletion(task)
                                },
                                onClick = {
                                    selectedTask = if (selectedTask?.id == task.id) null else task
                                    showActionDialog = selectedTask != null
                                }
                            )

                            // Добавляем разделитель, кроме последнего элемента
                            if (index < tasks.lastIndex) {
                                Divider(
                                    modifier = Modifier
                                        .padding(vertical = 0.dp)
                                        .padding(start = 1.dp, end = 1.dp),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
            
            // Нижний диалог с кнопками действий поверх основного
            if (showActionDialog && selectedTask != null) {
                TaskActionDialog(
                    task = selectedTask!!,
                    viewModel = viewModel,
                    onDismiss = {
                        showActionDialog = false
                        selectedTask = null
                    },
                    onEdit = {
                        editingTask = selectedTask
                        showActionDialog = false
                    },
                    onDelete = {
                        viewModel.deleteTask(selectedTask!!)
                        showActionDialog = false
                        selectedTask = null
                    },
                    onCopy = {
                        taskToCopy = selectedTask
                        showCopyDialog = true
                        showActionDialog = false
                    },
                    onMove = {
                        taskToMove = selectedTask
                        showMoveDialog = true
                        showActionDialog = false
                    },
                    onSend = {
                        // Отправка текста задачи в мессенджеры
                        val taskText = buildString {
                            append(selectedTask!!.title)
                            if (selectedTask!!.description.isNotBlank()) {
                                append("\n${selectedTask!!.description}")
                            }
                        }
                        sendTaskToMessengers(context, taskText)
                        showActionDialog = false
                        selectedTask = null
                    }
                )
            }
        }
    }

    // Диалог переноса задачи
    if (showMoveDialog && taskToMove != null) {
        MoveTaskDialog(
            task = taskToMove!!,
            viewModel = viewModel,
            onDismiss = {
                showMoveDialog = false
                taskToMove = null
                selectedTask = null
            },
            onMove = { newDate ->
                viewModel.moveTask(taskToMove!!, newDate)
                showMoveDialog = false
                taskToMove = null
                selectedTask = null
            }
        )
    }

    // Диалог копирования задачи
    if (showCopyDialog && taskToCopy != null) {
        CopyTaskDialog(
            task = taskToCopy!!,
            viewModel = viewModel,
            onDismiss = {
                showCopyDialog = false
                taskToCopy = null
                selectedTask = null
            },
            onCopy = { newDate ->
                viewModel.copyTask(taskToCopy!!, newDate)
                showCopyDialog = false
                taskToCopy = null
                selectedTask = null
            }
        )
    }

    // Диалог редактирования задачи
    editingTask?.let { task ->
        EditTaskDialog(
            task = task,
            onDismiss = { editingTask = null },
            onSave = { updatedTask ->
                viewModel.updateTask(updatedTask)
                editingTask = null
            }
        )
    }
}

@Composable
fun TaskItem(
    task: Task,
    isSelected: Boolean = false,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                color = if (isSelected) 
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else 
                    Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete() }
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (task.isCompleted)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) // <- текст светлеет
                    else
                        MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null // <- зачеркивание
                )
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

//КАЛЕНДАРЬ
@Composable
fun DatePickerCalendar(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    showTimeButton: Boolean = true,
    onTimeButtonClick: (() -> Unit)? = null
) {
    var selectedDate by remember { mutableStateOf(initialDate) }
    var currentMonth by remember { mutableStateOf(initialDate.withDayOfMonth(1)) }
    
    // Синхронизируем selectedDate с initialDate, если он изменился извне
    LaunchedEffect(initialDate) {
        selectedDate = initialDate
        currentMonth = initialDate.withDayOfMonth(1)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Навигация по месяцам
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                currentMonth = currentMonth.minusMonths(1)
            }) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Предыдущий месяц"
                )
            }
            
            val appLocale = getAppLocale()
            val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", appLocale)
            Text(
                text = currentMonth.format(monthFormatter).replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(appLocale) else it.toString()
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            IconButton(onClick = {
                currentMonth = currentMonth.plusMonths(1)
            }) {
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = "Следующий месяц"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Дни недели
        val daysOfWeek = listOf(
            stringResource(R.string.day_mon),
            stringResource(R.string.day_tue),
            stringResource(R.string.day_wed),
            stringResource(R.string.day_thu),
            stringResource(R.string.day_fri),
            stringResource(R.string.day_sat),
            stringResource(R.string.day_sun)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Календарная сетка
        val firstDayOfMonth = currentMonth.withDayOfMonth(1)
        val daysInMonth = currentMonth.lengthOfMonth()
        
        // Находим первый день недели месяца (понедельник = 1, воскресенье = 7)
        // Нужно получить смещение для понедельника (0 = понедельник, 6 = воскресенье)
        val firstDayOfWeekValue = firstDayOfMonth.dayOfWeek.value // 1=Mon, 7=Sun
        val offset = if (firstDayOfWeekValue == 7) 6 else firstDayOfWeekValue - 1
        
        // Вычисляем количество недель для отображения
        // Нужно учесть offset (пустые ячейки в начале) + дни месяца
        val totalCells = offset + daysInMonth
        val weeks = (totalCells + 6) / 7  // Округление вверх
        
        Column {
            repeat(weeks) { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(7) { dayOfWeek ->
                        // Вычисляем день месяца: начинаем с 1, учитывая offset
                        val dayIndex = week * 7 + dayOfWeek - offset + 1
                        val date = when {
                            dayIndex < 1 -> {
                                // День предыдущего месяца
                                val prevMonth = currentMonth.minusMonths(1)
                                val daysInPrevMonth = prevMonth.lengthOfMonth()
                                prevMonth.withDayOfMonth(daysInPrevMonth + dayIndex)
                            }
                            dayIndex > daysInMonth -> {
                                // День следующего месяца
                                currentMonth.plusMonths(1).withDayOfMonth(dayIndex - daysInMonth)
                            }
                            else -> {
                                // День текущего месяца
                                currentMonth.withDayOfMonth(dayIndex)
                            }
                        }
                        val isCurrentMonth = dayIndex in 1..daysInMonth
                        
                        val isSelected = date == selectedDate
                        val isToday = date == LocalDate.now()
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(4.dp)
                                .then(
                                    if (isSelected) {
                                        Modifier
                                            .background(
                                                MaterialTheme.colorScheme.primary,
                                                CircleShape
                                            )
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable {
                                    selectedDate = date
                                    onDateSelected(date)
                                    // Обновляем текущий месяц, если выбрана дата из другого месяца
                                    if (!isCurrentMonth) {
                                        currentMonth = date.withDayOfMonth(1)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isCurrentMonth) {
                                    dayIndex.toString()
                                } else {
                                    date.dayOfMonth.toString()
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    isToday -> MaterialTheme.colorScheme.primary
                                    isCurrentMonth -> MaterialTheme.colorScheme.onSurface
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                },
                                fontWeight = if (isSelected || (isToday && isCurrentMonth)) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        }
    }

//ФУНКЦИЯ ПЕРЕНЕСТИ
@Composable
fun MoveTaskDialog(
    task: Task,
    viewModel: LifeBalanceViewModel,
    onDismiss: () -> Unit,
    onMove: (LocalDate) -> Unit
) {
    val context = LocalContext.current
    var selectedDate by remember { mutableStateOf(task.date) }
    var showTimePicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Темный заголовок
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        // День недели (полностью)
                        val appLocale = getAppLocale()
                        val dayOfWeek = selectedDate.format(
                            DateTimeFormatter.ofPattern("EEEE", appLocale)
                        ).replaceFirstChar { it.titlecase(appLocale) }

                        Text(
                            text = dayOfWeek,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Дата: 09 декабря 2025
                        val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", appLocale)
                        val formattedDate = selectedDate.format(dateFormatter).replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(appLocale) else it.toString()
                        }

                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Календарь
                DatePickerCalendar(
                    initialDate = selectedDate,
                    onDateSelected = { newDate ->
                        selectedDate = newDate
                    },
                    showTimeButton = true,
                    onTimeButtonClick = { showTimePicker = true }
                )

                // Кнопки внизу
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.cancel).uppercase(Locale.getDefault()))
                    }
                    TextButton(onClick = { onMove(selectedDate) }) {
                        Text(text = stringResource(R.string.move).uppercase(Locale.getDefault()))
                    }
                }
            }
        }
    }

    // Диалог выбора времени (опционально, если нужно)
    if (showTimePicker) {
        val currentTime = task.time ?: LocalTime.now().plusHours(1)
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                // Здесь можно обновить время задачи, если нужно
                showTimePicker = false
            },
            currentTime.hour,
            currentTime.minute,
            true
        ).show()
    }
}

//ФУНКЦИЯ КОПИРОВАТЬ
@Composable
fun CopyTaskDialog(
    task: Task,
    viewModel: LifeBalanceViewModel,
    onDismiss: () -> Unit,
    onCopy: (LocalDate) -> Unit
) {
    val context = LocalContext.current
    var selectedDate by remember { mutableStateOf(task.date) }
    var showTimePicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Темный заголовок
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        // День недели (полностью)
                        val appLocale = getAppLocale()
                        val dayOfWeek = selectedDate.format(
                            DateTimeFormatter.ofPattern("EEEE", appLocale)
                        ).replaceFirstChar { it.titlecase(appLocale) }

                        Text(
                            text = dayOfWeek,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Дата: 09 декабря 2025
                        val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", appLocale)
                        val formattedDate = selectedDate.format(dateFormatter).replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(appLocale) else it.toString()
                        }

                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                // Календарь
                DatePickerCalendar(
                    initialDate = selectedDate,
                    onDateSelected = { newDate ->
                        selectedDate = newDate
                    },
                    showTimeButton = true,
                    onTimeButtonClick = { showTimePicker = true }
                )

                // Кнопки внизу
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.cancel).uppercase(Locale.getDefault()))
                    }
                    TextButton(onClick = { onCopy(selectedDate) }) {
                        Text(text = stringResource(R.string.copy).uppercase(Locale.getDefault()))
                    }
                }
            }
        }
    }

    // Диалог выбора времени (опционально, если нужно)
    if (showTimePicker) {
        val currentTime = task.time ?: LocalTime.now().plusHours(1)
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                // Здесь можно обновить время задачи, если нужно
                showTimePicker = false
            },
            currentTime.hour,
            currentTime.minute,
            true
        ).show()
    }
}

// Маленькая функция для вертикальной линии-разделителя
@Composable
fun DividerLine() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(MaterialTheme.colorScheme.outline)
    )
}

//КНОПКИ ВНИЗУ РЕДАКТИРОВАТЬ КОПИРОВАТЬ ПЕРЕНЕСТИ ОТПРАВИТЬ УДАЛИТЬ
@Composable
fun TaskActionDialog(
    task: Task,
    viewModel: LifeBalanceViewModel,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onSend: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        // ❗ Один общий контроллер для всех надписей кнопок
        val typography = MaterialTheme.typography
        val textController = remember(typography) {
            TextResizeController(typography.bodySmall.fontSize)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // Кнопка РЕДАКТИРОВАТЬ
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .clickable(onClick = onEdit)
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit),
                            contentDescription = "Редактировать",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        GroupAutoResizedText(
                            text = stringResource(R.string.edit),
                            controller = textController,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    DividerLine()

                    // Кнопка ПЕРЕНЕСТИ
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onMove)
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_ok),
                            contentDescription = "Перенести",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        GroupAutoResizedText(
                            text = stringResource(R.string.move),
                            controller = textController,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    DividerLine()

                    // Копировать
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onCopy)
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_copy),
                            contentDescription = "Копировать",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        GroupAutoResizedText(
                            text = stringResource(R.string.copy),
                            controller = textController,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    DividerLine()

                    // Кнопка ОТПРАВИТЬ
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onSend)
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_go),
                            contentDescription = "Отправить",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        GroupAutoResizedText(
                            text = stringResource(R.string.send),
                            controller = textController,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    DividerLine()

                    // Кнопка УДАЛИТЬ
                    Column(
                        modifier = Modifier
                            .weight(0.9f)
                            .clickable(onClick = onDelete)
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_bin),
                            contentDescription = "Удалить",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        GroupAutoResizedText(
                            text = stringResource(R.string.delete),
                            controller = textController,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

/**
 * Отправляет текст задачи в мессенджеры через Intent
 */
fun sendTaskToMessengers(context: Context, taskText: String) {
    if (taskText.isBlank()) {
        if (BuildConfig.DEBUG) Log.w("SEND_ERROR", "Task text is blank")
        return
    }

    // Получаем Activity контекст, если возможно
    val activityContext: Context = if (context is Activity) {
        context
    } else {
        // Пытаемся получить Activity из контекста
        var ctx: Context? = context
        var foundActivity: Activity? = null
        while (ctx != null) {
            if (ctx is Activity) {
                foundActivity = ctx
                break
            }
            ctx = if (ctx is android.content.ContextWrapper) {
                ctx.baseContext
            } else {
                null
            }
        }
        foundActivity ?: context
    }

    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, taskText)
        }

        // Проверка — есть ли приложения, которые могут обработать именно sendIntent
        val resolveInfo = sendIntent.resolveActivity(activityContext.packageManager)
        if (resolveInfo == null) {
            if (BuildConfig.DEBUG) Log.e("SEND_ERROR", activityContext.resources.getString(R.string.no_apps_for_sending))
            return
        }

        // Создаем chooser с локализованным заголовком
        val chooserTitle = try {
            activityContext.resources.getString(R.string.send_task_chooser_title)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w("SEND_ERROR", "Failed to get chooser title", e)
            "Send task to:" // Fallback на английский
        }
        
        val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
            // Добавляем флаг для запуска из не-Activity контекста
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }

        // Проверяем, что chooser может быть запущен
        val chooserResolveInfo = chooser.resolveActivity(activityContext.packageManager)
        if (chooserResolveInfo == null) {
            if (BuildConfig.DEBUG) Log.e("SEND_ERROR", activityContext.resources.getString(R.string.apps_not_found))
            return
        }

        // Используем ContextCompat для безопасного запуска Activity
        try {
            ContextCompat.startActivity(activityContext, chooser, null)
            if (BuildConfig.DEBUG) Log.d("SEND_SUCCESS", "Chooser started successfully")
        } catch (e: Exception) {
            // Если ContextCompat не сработал, пробуем обычный startActivity
            if (BuildConfig.DEBUG) Log.w("SEND_ERROR", "ContextCompat.startActivity failed, trying startActivity", e)
            activityContext.startActivity(chooser)
        }

    } catch (e: ActivityNotFoundException) {
        if (BuildConfig.DEBUG) Log.e("SEND_ERROR", "Apps not found", e)
    } catch (e: SecurityException) {
        if (BuildConfig.DEBUG) Log.e("SEND_ERROR", "Security exception", e)
    } catch (e: Exception) {
        if (BuildConfig.DEBUG) Log.e("SEND_ERROR", activityContext.resources.getString(R.string.error_opening_messengers), e)
    }
}


