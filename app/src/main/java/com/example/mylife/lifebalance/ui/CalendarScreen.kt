package com.example.mylife.lifebalance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.example.lifebalance.R
import com.example.mylife.lifebalance.data.Goal
import com.example.mylife.lifebalance.data.LifeSphere
import com.example.mylife.lifebalance.data.Task
import com.example.mylife.lifebalance.viewmodel.LifeBalanceViewModel
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: LifeBalanceViewModel,
    onNavigate: (Screen) -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val spheres by viewModel.spheres.collectAsState()
    val allGoals by viewModel.allGoals.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    Scaffold(
        topBar = {
            TopBarWithIcons(
                onNavigate = { screen ->
                    navController.navigate(screen.route)
                }
            )
        },
        content = { paddingValues ->
            val scrollState = rememberScrollState()
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Заголовок
                Text(
                    text = stringResource(R.string.calendar).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Навигация по месяцам
                MonthNavigation(
                    currentMonth = currentMonth,
                    onPreviousMonth = { currentMonth = currentMonth.minusMonths(1) },
                    onNextMonth = { currentMonth = currentMonth.plusMonths(1) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Календарь
                CalendarView(
                    yearMonth = currentMonth,
                    goals = allGoals,
                    tasks = allTasks,
                    spheres = spheres,
                    selectedDate = selectedDate,
                    onDateClick = { date -> selectedDate = if (selectedDate == date) null else date },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Список целей и дел для выбранной даты
                selectedDate?.let { date ->
                    ItemsForDateCard(
                        date = date,
                        goals = allGoals.filter { it.deadline == date },
                        tasks = allTasks.filter { it.date == date },
                        spheres = spheres,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    )
}

@Composable
fun MonthNavigation(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appLocale = getAppLocale()
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", appLocale)
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Предыдущий месяц",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Text(
            text = currentMonth.format(monthFormatter).replaceFirstChar { 
                if (it.isLowerCase()) it.titlecase(appLocale) else it.toString() 
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        IconButton(onClick = onNextMonth) {
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = "Следующий месяц",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun CalendarView(
    yearMonth: YearMonth,
    goals: List<Goal>,
    tasks: List<Task>,
    spheres: List<LifeSphere>,
    selectedDate: LocalDate?,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value
    val today = LocalDate.now()
    
    // Создаем карту дат с целями
    val goalsByDate = goals
        .filter { 
            val goalDate = it.deadline
            goalDate.year == yearMonth.year && goalDate.monthValue == yearMonth.monthValue
        }
        .groupBy { it.deadline }
    
    // Создаем карту дат с задачами
    val tasksByDate = tasks
        .filter { 
            val taskDate = it.date
            taskDate.year == yearMonth.year && taskDate.monthValue == yearMonth.monthValue
        }
        .groupBy { it.date }
    
    // Создаем карту сфер по ID
    val spheresMap = spheres.associateBy { it.id.toInt() }

    Column(modifier = modifier) {
        // Заголовки дней недели
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val dayNames = listOf(
                stringResource(R.string.day_mon),
                stringResource(R.string.day_tue),
                stringResource(R.string.day_wed),
                stringResource(R.string.day_thu),
                stringResource(R.string.day_fri),
                stringResource(R.string.day_sat),
                stringResource(R.string.day_sun)
            )
            dayNames.forEach { dayName ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Дни месяца
        val totalWeeks = ((firstDayOfWeek - 1 + daysInMonth - 1) / 7) + 1
        val calendarDays = remember(yearMonth) {
            val days = mutableListOf<LocalDate?>()
            // Добавляем пустые дни до начала месяца
            repeat(firstDayOfWeek - 1) { days.add(null) }
            // Добавляем дни месяца
            for (day in 1..daysInMonth) {
                days.add(yearMonth.atDay(day))
            }
            // Добавляем пустые дни до конца недели
            val remainingDays = 7 - (days.size % 7)
            if (remainingDays < 7) {
                repeat(remainingDays) { days.add(null) }
            }
            days
        }

        calendarDays.chunked(7).forEachIndexed { weekIndex, weekDays ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                weekDays.forEach { date ->
                    CalendarDayCell(
                        date = date,
                        isToday = date == today,
                        isSelected = date == selectedDate,
                        goals = date?.let { goalsByDate[it] ?: emptyList() } ?: emptyList(),
                        tasks = date?.let { tasksByDate[it] ?: emptyList() } ?: emptyList(),
                        spheresMap = spheresMap,
                        onClick = { date?.let(onDateClick) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (weekIndex < calendarDays.chunked(7).size - 1) {
                Spacer(modifier = Modifier.height(0.dp))//расстояние между ячейками по вертикали
            }
        }
    }
}
//ВИД КАЛЕНДАРЯ
@Composable
fun CalendarDayCell(
    date: LocalDate?,
    isToday: Boolean,
    isSelected: Boolean,
    goals: List<Goal>,
    tasks: List<Task>,
    spheresMap: Map<Int, LifeSphere>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Объединяем цели и задачи для отображения
    val allItems = goals.size + tasks.size
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(
                when {
                    // ⬜ ПУСТЫЕ ЯЧЕЙКИ (date == null)
                    date == null -> {
                        Modifier
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            )
                    }
                    // 🟢 ВЫБРАННЫЙ ДЕНЬ
                    isSelected -> {
                        Modifier
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                            )
                    }
                    // 🔵 СЕГОДНЯ
                    isToday -> {
                        Modifier
                            .border(
                                2.dp,
                                MaterialTheme.colorScheme.onSecondary,
                                RoundedCornerShape(8.dp)
                            )
                            .background(MaterialTheme.colorScheme.primary)
                    }
                    // 📅 ОБЫЧНЫЙ ДЕНЬ
                    else -> {
                        Modifier
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(8.dp)
                            )
                            .background(MaterialTheme.colorScheme.surface)
                    }
                }
            )
            .clickable(enabled = date != null, onClick = onClick),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (date != null) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) //(isToday || isSelected)
                        MaterialTheme.colorScheme.onSecondary
                    else 
                        MaterialTheme.colorScheme.onPrimary
                )
                
                // Индикаторы целей и задач
                if (allItems > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Показываем цели (кружочки)
                        goals.take(3).forEach { goal ->
                            val sphere = spheresMap[goal.sphereId]
                            val color = sphere?.let { 
                                colorPalette[it.colorIndex % colorPalette.size] 
                            } ?: MaterialTheme.colorScheme.primary
                            
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(color)
                                    .padding(horizontal = 1.dp)
                            )
                        }
                        // Показываем задачи (квадратики)
                        tasks.take(3 - goals.size.coerceAtMost(3)).forEach { task ->
                            val sphere = task.sphereId.let { id -> 
                                spheresMap.values.firstOrNull { it.id == id }
                            }
                            val color = sphere?.let { 
                                colorPalette[it.colorIndex % colorPalette.size]
                            } ?: MaterialTheme.colorScheme.primaryContainer
                            
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(color)
                                    .padding(horizontal = 1.dp)
                            )
                        }
                        if (allItems > 3) {
                            Text(
                                text = "+${allItems - 3}",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = MaterialTheme.typography.bodySmall.fontSize * 0.7f,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemsForDateCard(
    date: LocalDate,
    goals: List<Goal>,
    tasks: List<Task>,
    spheres: List<LifeSphere>,
    viewModel: LifeBalanceViewModel,
    modifier: Modifier = Modifier
) {
    val appLocale = getAppLocale()
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", appLocale)
    val spheresMap = spheres.associateBy { it.id.toInt() }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = date.format(dateFormatter).replaceFirstChar { 
                    if (it.isLowerCase()) it.titlecase(appLocale) else it.toString() 
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            if (goals.isEmpty() && tasks.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_goals_or_tasks_for_this_date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            } else {
                // Показываем цели
                if (goals.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.main_goals).uppercase(Locale.getDefault()) + ":",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    goals.forEach { goal ->
                        val sphere = spheresMap[goal.sphereId]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            sphere?.let {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            colorPalette[it.colorIndex % colorPalette.size]
                                        )
                                )
                            }
                            Text(
                                text = goal.text,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                // Показываем задачи
                if (tasks.isNotEmpty()) {
                    if (goals.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    Text(
                        text = stringResource(R.string.tasks).uppercase(Locale.getDefault()) + ":",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    tasks.forEach { task ->
                        val sphere = task.sphereId.let { id -> 
                            spheresMap.values.firstOrNull { it.id == id }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            sphere?.let {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            colorPalette[it.colorIndex % colorPalette.size]
                                        )
                                )
                            }
                            Column(modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        textDecoration = if (task.isCompleted) 
                                            TextDecoration.LineThrough 
                                        else 
                                            TextDecoration.None
                                    ),
                                    color = if (task.isCompleted) 
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    else 
                                        MaterialTheme.colorScheme.onSurface
                                )
                                if (task.description.isNotBlank()) {
                                    Text(
                                        text = task.description,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            textDecoration = if (task.isCompleted) 
                                                TextDecoration.LineThrough 
                                            else 
                                                TextDecoration.None
                                        ),
                                        color = if (task.isCompleted)
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Checkbox(
                                checked = task.isCompleted,
                                onCheckedChange = {
                                    viewModel.toggleTaskCompletion(task)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}