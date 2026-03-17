package com.example.mylife.lifebalance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.mylife.lifebalance.data.LifeSphere
import com.example.mylife.lifebalance.viewmodel.LifeBalanceViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.lifebalance.R
import java.util.Locale

data class GoalCount(val total: Int, val remaining: Int)

@Composable
fun rememberGoalCount(
    sphereId: Int,
    allGoals: List<com.example.mylife.lifebalance.data.Goal>,
    viewModel: LifeBalanceViewModel
): GoalCount {
    val sphereGoals = allGoals.filter { it.sphereId == sphereId }
    val total = sphereGoals.size
    
    // Если целей нет, возвращаем 0/0
    if (sphereGoals.isEmpty()) {
        return GoalCount(total = 0, remaining = 0)
    }
    
    // Используем checked напрямую из объектов goals, так как Room Flow автоматически обновляет список
    // Подсчитываем количество незачеркнутых целей
    val remaining = sphereGoals.count { !it.checked }
    
    return GoalCount(total = total, remaining = remaining)
}

@Composable
fun GoalsScreen(
    viewModel: LifeBalanceViewModel,
    onNavigate: (Screen) -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val spheres by viewModel.spheres.collectAsState()
    val allGoals by viewModel.allGoals.collectAsState()

    Scaffold(
        topBar = {
            TopBarWithIcons { screen ->
                navController.navigate(screen.route)
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally  // горизонтальный центр
            ) {

            Text(text = stringResource(R.string.main_goals).uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(text = stringResource(R.string.by_area_of_life).uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Spacer(Modifier.height(12.dp))

            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(spheres) { sphere ->
                    val goalCount = rememberGoalCount(sphere.id.toInt(), allGoals, viewModel)
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .border(
                                width = 1.dp,                       // толщина линии
                                color = MaterialTheme.colorScheme.outline, // цвет (можно свой)
                                shape = MaterialTheme.shapes.small   // та же форма, что у карточки
                            )
                            .clickable {
                                navController.navigate(
                                    Screen.GoalsDetails.createRoute(sphere.id.toInt())
                                )},

                        //цвет добавили
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),

                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        color = colorPalette[sphere.colorIndex % colorPalette.size],
                                        shape = MaterialTheme.shapes.small
                                    )
                            )
                            Text(
                                sphere.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onTertiary
                            )
                            Text(
                                text = "${goalCount.remaining}/${goalCount.total}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}


