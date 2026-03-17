package com.example.mylife.lifebalance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.lifebalance.R
import com.example.mylife.lifebalance.data.LifeSphere
import com.example.mylife.lifebalance.viewmodel.LifeBalanceViewModel
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: LifeBalanceViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val spheres by viewModel.spheres.collectAsState()
    val selectedSphere by viewModel.selectedSphere.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showAIDialog by remember { mutableStateOf(false) }

    var editingSphere by remember { mutableStateOf<LifeSphere?>(null) }
    var showCreateGoalDialog by remember { mutableStateOf(false) }
    var sphereToDelete by remember { mutableStateOf<LifeSphere?>(null) }
    var showPremiumDialog by remember { mutableStateOf<PremiumFeature?>(null) }
    var showAiConsentDialog by remember { mutableStateOf(false) }

    val isPremium by viewModel.isPremium.collectAsState()
    val hasAiConsent by viewModel.hasAiDataProcessingConsent.collectAsState()
    val aiResponse by viewModel.aiResult.collectAsState()
    var isLoadingAI by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Обновляем названия сфер при каждом появлении экрана
    LaunchedEffect(Unit) {
        viewModel.updateDefaultSphereNames()
    }

    Scaffold(
        topBar = {
            TopBarWithIcons(onNavigate = { screen -> navController.navigate(screen.route) })
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Заголовок
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.wheel_of_life_balance).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }

            // Дата
            val formattedDate = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault()))
            Text(
                text = formattedDate,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
            )

            // Box для колеса + кнопки
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                BalanceWheel(
                    spheres = spheres,
                    onSectorClick = { sphere ->
                        editingSphere = sphere
                        viewModel.selectSphere(sphere)
                        showCreateGoalDialog = true
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Кнопка AI (премиум + согласие на обработку данных)
                FloatingActionButton(
                    onClick = {
                        if (!isPremium) {
                            showPremiumDialog = PremiumFeature.AI_BALANCE
                        } else if (!hasAiConsent) {
                            showAiConsentDialog = true
                        } else {
                            showAIDialog = true
                            viewModel.analyzeBalanceWithAi()
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {

                        // Текст AI всегда по центру
                        Text(
                            text = "AI",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )

                        // 🔒 Маленький замочек поверх, если не Premium
                        if (!isPremium) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 9.dp, y = (-7).dp)
                                    .size(14.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surface , //Color.Transparent
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }


                // Кнопка добавления сферы (бесплатно до 8, премиум до 12)
                val maxSpheres = if (isPremium) 12 else 8
                val addSphereLocked = spheres.size >= 8 && !isPremium
                if (spheres.size < maxSpheres || addSphereLocked) {
                    FloatingActionButton(
                        onClick = {
                            if (addSphereLocked) {
                                showPremiumDialog = PremiumFeature.SPHERES_EXTRA
                            } else {
                                editingSphere = null
                                showAddDialog = true
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            focusedElevation = 0.dp,
                            hoveredElevation = 0.dp
                        ),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = stringResource(R.string.add_area),
                                modifier = Modifier.size(24.dp)
                            )
                            if (addSphereLocked) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 5.dp, y = (-4).dp)
                                        .size(14.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = stringResource(R.string.premium_required_title),
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Список сфер
            SphereList(
                spheres = spheres,
                onSphereClick = { sphere ->
                    editingSphere = sphere
                    showEditDialog = true
                },
                onScoreChange = { sphere, score ->
                    viewModel.updateSphereScore(sphere, score)
                },
                onDeleteSphere = { sphere -> sphereToDelete = sphere },
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Диалоги добавления/редактирования
    if (showAddDialog || showEditDialog) {
        SphereEditDialog(
            sphere = editingSphere,
            onDismiss = {
                showAddDialog = false
                showEditDialog = false
                editingSphere = null
            },
            onSave = { name, colorIndex ->
                if (editingSphere == null)
                    viewModel.addSphere(name, colorIndex)
                else
                    viewModel.updateSphere(editingSphere!!.copy(name = name, colorIndex = colorIndex))

                showAddDialog = false
                showEditDialog = false
                editingSphere = null
            }
        )
    }

    // Диалог создания целей
    if (showCreateGoalDialog && selectedSphere != null) {
        GoalsCreateDialog(
            sphere = selectedSphere!!,
            onDismiss = { showCreateGoalDialog = false },
            onSaveGoal = { text, date ->
                viewModel.addGoal(selectedSphere!!, text, date)
                showCreateGoalDialog = false
            }
        )
    }

    // Диалог удаления сферы
    if (sphereToDelete != null) {
        Dialog(onDismissRequest = { sphereToDelete = null }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.delete_the_sphere),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.are_you_sure_you_want_to_delete_this_sphere),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(onClick = { sphereToDelete = null }) {
                            Text(stringResource(R.string.cancel).uppercase(Locale.getDefault()))
                        }
                        TextButton(onClick = {
                            viewModel.deleteSphere(sphereToDelete!!)
                            sphereToDelete = null
                        }) {
                            Text(stringResource(R.string.delete).uppercase(Locale.getDefault()), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showAIDialog) {
        when {
            aiResponse == null || aiResponse == "Загрузка..." -> {
                Dialog(onDismissRequest = { showAIDialog = false }) {
                    Card(
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.ai_assistant),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            CircularProgressIndicator()
                            Text(text = stringResource(R.string.getting_ai_response))
                        }
                    }
                }
            }
            aiResponse == "LIMIT_EXCEEDED" -> {
                Dialog(onDismissRequest = { showAIDialog = false }) {
                    Card(
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.ai_assistant),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.ai_request_limit_exceeded),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Button(onClick = { showAIDialog = false }) {
                                Text(text = stringResource(R.string.close).uppercase(Locale.getDefault()))
                            }
                        }
                    }
                }
            }
            else -> {
                BalanceAnalysisDialog(
                    aiResponse = aiResponse,
                    spheres = spheres,
                    viewModel = viewModel,
                    onDismiss = { showAIDialog = false }
                )
            }
        }
    }
    showPremiumDialog?.let { feature ->
        PremiumRequiredDialog(
            feature = feature,
            onDismiss = { showPremiumDialog = null }
        )
    }
    if (showAiConsentDialog) {
        AiConsentDialog(
            onAccept = {
                viewModel.setAiDataProcessingConsent(true)
                showAiConsentDialog = false
                showAIDialog = true
                viewModel.analyzeBalanceWithAi()
            },
            onDecline = { showAiConsentDialog = false }
        )
    }
}
