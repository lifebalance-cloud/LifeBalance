package com.example.mylife.lifebalance.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mylife.lifebalance.data.LifeSphere
import com.example.mylife.lifebalance.data.Task
import com.example.mylife.lifebalance.viewmodel.LifeBalanceViewModel

@Composable
fun TaskRecommendationDialog(
    sphere: LifeSphere?,
    tasks: List<Task>,
    onDismiss: () -> Unit,
    onTaskToggle: (Task) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LifeBalanceViewModel = viewModel() // используем viewModel по умолчанию
) {
    var showGoalDialog by remember { mutableStateOf(false) }

    // Кнопка создания главных целей
    Button(onClick = { showGoalDialog = true }) {
        Text("Создать главные цели")
    }

    // Диалог для создания цели
    if (showGoalDialog && sphere != null) {
        GoalsCreateDialog(
            sphere = sphere,
            onDismiss = { showGoalDialog = false },
            onSaveGoal = { text, date ->
                viewModel.addGoal(sphere, text, date)
                showGoalDialog = false
            }
        )
    }

    // Основной диалог с задачами
    if (sphere != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = "Рекомендации: ${sphere.name}") },
            text = {
                if (tasks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Генерируем рекомендации...")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tasks) { task ->
                            TaskItem(
                                task = task,
                                onToggle = { onTaskToggle(task) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Закрыть")
                }
            }
        )
    }
}

@Composable
fun TaskItem(
    task: Task,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
