package com.example.mylife.lifebalance.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.lifebalance.R
import com.example.mylife.lifebalance.data.LifeSphere
import com.example.mylife.lifebalance.data.RepeatType
import com.example.mylife.lifebalance.viewmodel.LifeBalanceViewModel
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import java.util.Locale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign

data class ParsedAnalysis(
    val support: String = "",
    val weakSpheres: List<Pair<String, Int>> = emptyList(),
    val goals: Map<String, List<String>> = emptyMap(),
    val steps: List<String> = emptyList()
)

@Composable
fun BalanceAnalysisDialog(
    aiResponse: String?,
    spheres: List<LifeSphere>,
    viewModel: LifeBalanceViewModel,
    onDismiss: () -> Unit
) {
    val parsed = remember(aiResponse) {
        parseAiResponse(aiResponse ?: "", spheres)
    }
    
    var showGoalDialog by remember { mutableStateOf<Pair<LifeSphere, String>?>(null) }
    var checkedSteps by remember { mutableStateOf<Set<Int>>(emptySet()) }

    Dialog(onDismissRequest = { /* пусто — клик вне диалога не закрывает */ }) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(1f)
                .fillMaxHeight(0.9f)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 24.dp,
                        vertical = 24.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp) //расстояние между item
            ) {
                // Заголовок
                item {
                    Text(text = stringResource(R.string.balance_analysis),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth() // занять всю ширину строки
                            .wrapContentWidth(Alignment.CenterHorizontally) // выровнять содержимое по центру
                    )
                    //Text(text = stringResource(R.string.gentle_overview_of_what_matters_right),
                      //  style = MaterialTheme.typography.bodySmall,
                      //  color = MaterialTheme.colorScheme.primary,
                     //   fontWeight = FontWeight.Normal,
                     //   textAlign = TextAlign.Center,
                     //   modifier = Modifier.padding(bottom = 8.dp)
                   // )
                }

                // Поддержка
                if (parsed.support.isNotBlank()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = parsed.support,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }

                // Просевшие сферы
                if (parsed.weakSpheres.isNotEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp))
                        {
                            Text(
                                text = "⚠️ ${stringResource(R.string.weak_spheres)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            parsed.weakSpheres.forEach { (name, score) ->
                                Text(
                                    text = "• $name ($score/10)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                        }
                    }
                }

                // Предложенные цели
                if (parsed.goals.isNotEmpty()) {
                    item {
                        Text(
                            text = "🎯 ${stringResource(R.string.suggested_goals)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    parsed.goals.forEach { (sphereName, goalsList) ->
                        val sphere = spheres.find { it.name == sphereName }
                        
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Название сферы
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "➤ ${sphereName.uppercase(Locale.getDefault())}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }

                                // Цели для этой сферы
                                goalsList.forEachIndexed { index, goalText ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 1.dp)
                                    ) {
                                        // Текст цели с нумерацией
                                        Text(
                                            text = "${index + 1}. $goalText",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Start
                                        )

                                        // Кнопка "Добавить цель" под текстом
                                        if (sphere != null) {
                                            Button(onClick = { showGoalDialog = Pair(sphere, goalText)},
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 8.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                                    contentColor = MaterialTheme.colorScheme.onPrimary),
                                                elevation = ButtonDefaults.buttonElevation(
                                                    defaultElevation = 0.dp,
                                                    pressedElevation = 0.dp,
                                                    focusedElevation = 0.dp,
                                                    hoveredElevation = 0.dp
                                            ),
                                                shape = RoundedCornerShape(24.dp),
                                                contentPadding = PaddingValues(
                                                    vertical = 6.dp,
                                                    horizontal = 16.dp)
                                                ) {
                                                Text(text = stringResource(R.string.save_goal).uppercase(Locale.getDefault()),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }

                            }
                        }
                    }
                }

                // Маленькие шаги
                if (parsed.steps.isNotEmpty()) {
                    item {
                        val addedFromLifeBalanceStr = stringResource(R.string.added_from_life_balance)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "👣 ${stringResource(R.string.small_steps)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            parsed.steps.forEachIndexed { index, step ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Checkbox(
                                        checked = checkedSteps.contains(index),
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                // Добавляем шаг в ежедневные дела
                                                val today = LocalDate.now()
                                                viewModel.addTask(
                                                    title = step,
                                                    description = addedFromLifeBalanceStr,
                                                    date = today,
                                                    time = null,
                                                    hasNotification = false,
                                                    notificationSound = "default",
                                                    sphereId = null,
                                                    autoReschedule = false,
                                                    repeatType = RepeatType.NONE,
                                                    repeatEndDate = null
                                                )
                                                checkedSteps = checkedSteps + index
                                            } else {
                                                // Убираем из отмеченных, но не удаляем задачу (пользователь может удалить вручную)
                                                checkedSteps = checkedSteps - index
                                            }
                                        }
                                    )
                                    Text(
                                        text = step,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Start
                                    )
                                }
                            }
                        }
                    }
                }

                // Кнопка закрытия
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.close).uppercase(Locale.getDefault()))
                    }
                }
            }
        }
    }

    // Диалог создания цели
    showGoalDialog?.let { (sphere, goalText) ->
        GoalsCreateDialog(
            sphere = sphere,
            onDismiss = { showGoalDialog = null },
            onSaveGoal = { text, date ->
                viewModel.addGoal(sphere, text, date)
                showGoalDialog = null
            },
            initialGoalText = goalText
        )
    }
}

// Маркеры секций для всех языков приложения: ru, en, uk, de, fr, es
private val SUPPORT_SECTION_MARKERS = listOf(
    "[ПОДДЕРЖКА]", "[SUPPORT]", "[ПІДТРИМКА]", "[UNTERSTÜTZUNG]", "[UNTERSTUTZUNG]",
    "[SOUTIEN]", "[APOYO]"
)
private val WEAK_SPHERES_SECTION_MARKERS = listOf(
    "[ПРОСЕВШИЕ_СФЕРЫ]", "[ПРОСЕВШИЕ СФЕРЫ]", "[WEAK SPHERES]", "[WEAK_SPHERES]",
    "[ПРОСІДАНІ СФЕРИ]", "[ПРОСІДАНІ_СФЕРИ]", "[SCHWACHE BEREICHE]", "[SCHWACHE_BEREICHE]",
    "[DOMAINES FAIBLES]", "[DOMAINES_FAIBLES]", "[ÁREAS DÉBILES]", "[AREAS DEBILES]"
)
private val GOALS_SECTION_MARKERS = listOf(
    "[ЦЕЛИ]", "[GOALS]", "[ЦІЛІ]", "[ZIELE]", "[OBJECTIFS]", "[OBJETIVOS]"
)
private val STEPS_SECTION_MARKERS = listOf(
    "[ШАГИ]", "[STEPS]", "[КРОКИ]", "[SCHRITTE]", "[ÉTAPES]", "[ETAPES]", "[PASOS]"
)

// Префиксы строки "Сфера: ..." для всех языков
private val SPHERE_LINE_PREFIXES = listOf(
    "Сфера:", "сфера:", "Sphere:", "sphere:", "Сфера:", "Bereich:", "Sphäre:", "Sphère:", "Domaine:", "Esfera:", "Área:", "Area:"
)

// Префиксы строки "Цель 1: ..." для всех языков (без номера — опционально)
private val GOAL_LINE_PREFIX_PATTERNS = listOf(
    "Цель", "цель", "Goal", "goal", "Ціль", "ціль", "Ziel", "ziel", "Objectif", "objectif", "Objetivo", "objetivo"
)

private fun containsAny(trimmed: String, markers: List<String>): Boolean =
    markers.any { trimmed.contains(it, ignoreCase = true) }

private fun getSphereNameFromLine(trimmed: String): String? {
    for (prefix in SPHERE_LINE_PREFIXES) {
        if (trimmed.startsWith(prefix, ignoreCase = true)) {
            return trimmed.substring(prefix.length).trim()
        }
    }
    return null
}

private fun isGoalLine(trimmed: String): Boolean =
    GOAL_LINE_PREFIX_PATTERNS.any { trimmed.startsWith(it, ignoreCase = true) } ||
    trimmed.startsWith("→")

private fun extractGoalText(trimmed: String): String {
    return when {
        trimmed.startsWith("→") -> trimmed.removePrefix("→").trim()
        trimmed.contains(":") -> trimmed.substringAfter(":").trim()
        else -> {
            val prefix = GOAL_LINE_PREFIX_PATTERNS.firstOrNull { trimmed.startsWith(it, ignoreCase = true) }
            if (prefix != null) trimmed.removePrefix(prefix).trim() else trimmed
        }
    }
}

fun parseAiResponse(response: String, spheres: List<LifeSphere>): ParsedAnalysis {
    if (response.isBlank() || response == "Загрузка...") {
        return ParsedAnalysis()
    }

    var support = ""
    val weakSpheres = mutableListOf<Pair<String, Int>>()
    val goals = mutableMapOf<String, MutableList<String>>()
    val steps = mutableListOf<String>()

    val lines = response.lines()
    var currentSection = ""
    var currentSphere = ""

    for (line in lines) {
        val trimmed = line.trim()

        when {
            SUPPORT_SECTION_MARKERS.any { trimmed.contains(it, ignoreCase = true) } -> {
                currentSection = "SUPPORT"
                continue
            }
            WEAK_SPHERES_SECTION_MARKERS.any { trimmed.contains(it, ignoreCase = true) } -> {
                currentSection = "WEAK_SPHERES"
                continue
            }
            GOALS_SECTION_MARKERS.any { trimmed.contains(it, ignoreCase = true) } -> {
                currentSection = "GOALS"
                continue
            }
            STEPS_SECTION_MARKERS.any { trimmed.contains(it, ignoreCase = true) } -> {
                currentSection = "STEPS"
                continue
            }
            getSphereNameFromLine(trimmed) != null -> {
                currentSphere = getSphereNameFromLine(trimmed) ?: ""
                if (currentSphere.isNotBlank() && !goals.containsKey(currentSphere)) {
                    goals[currentSphere] = mutableListOf()
                }
                continue
            }
            (isGoalLine(trimmed) || trimmed.startsWith("→")) &&
                currentSection == "GOALS" && currentSphere.isNotBlank() -> {
                val goalText = extractGoalText(trimmed)
                if (goalText.isNotBlank()) {
                    goals[currentSphere]?.add(goalText)
                }
                continue
            }
            currentSection == "SUPPORT" && trimmed.isNotBlank() &&
                !trimmed.startsWith("[") && !containsAny(trimmed, SUPPORT_SECTION_MARKERS) -> {
                if (support.isBlank()) {
                    support = trimmed
                } else {
                    support += " $trimmed"
                }
            }
            currentSection == "WEAK_SPHERES" && trimmed.isNotBlank() &&
                !trimmed.startsWith("[") && !containsAny(trimmed, WEAK_SPHERES_SECTION_MARKERS) -> {
                val match1 = Regex("""(.+?)\s*\((\d+)/10\)""").find(trimmed)
                val match2 = Regex("""(.+?):\s*(\d+)/10""").find(trimmed)
                val match = match1 ?: match2
                if (match != null) {
                    val name = match.groupValues[1].trim()
                    val score = match.groupValues[2].toIntOrNull() ?: 0
                    weakSpheres.add(Pair(name, score))
                } else if (trimmed.contains("/10")) {
                    val parts = trimmed.split("/10")
                    if (parts.isNotEmpty()) {
                        val beforeScore = parts[0].trim()
                        val scoreMatch = Regex("""(\d+)""").find(beforeScore)
                        val name = beforeScore.replace(Regex("""\d+"""), "").trim()
                        val score = scoreMatch?.value?.toIntOrNull() ?: 0
                        if (name.isNotBlank() && score > 0) {
                            weakSpheres.add(Pair(name, score))
                        }
                    }
                }
            }
            currentSection == "STEPS" && trimmed.isNotBlank() &&
                !trimmed.startsWith("[") && !containsAny(trimmed, STEPS_SECTION_MARKERS) &&
                getSphereNameFromLine(trimmed) == null &&
                !isGoalLine(trimmed) &&
                !trimmed.startsWith("→") -> {
                val cleanStep = trimmed.removePrefix("☐").removePrefix("☑").trim()
                if (cleanStep.isNotBlank()) {
                    steps.add(cleanStep)
                }
            }
        }
    }

    return ParsedAnalysis(
        support = support,
        weakSpheres = weakSpheres,
        goals = goals,
        steps = steps
    )
}
