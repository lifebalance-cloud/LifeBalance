package com.example.mylife.lifebalance.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.mylife.lifebalance.data.*
import com.example.mylife.lifebalance.viewmodel.LifeBalanceViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration.Companion.Underline
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.DisposableEffect
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.lifebalance.BuildConfig
import com.example.lifebalance.R
import com.example.mylife.lifebalance.utils.ImageStorageHelper
import android.util.Log

// Константы для работы с фото
private const val MAX_PHOTOS = 3

@Composable
fun LoadImageFromUri(
    uriString: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(uriString) {
        if (uriString != null && uriString.isNotBlank()) {
            imageBitmap = withContext(Dispatchers.IO) {
                try {
                    val uri = Uri.parse(uriString)
                    // Попытка открыть InputStream с разными способами
                    var inputStream: InputStream? = null
                    
                    // Сначала пробуем через contentResolver (для content:// и file:// URI)
                    try {
                        inputStream = context.contentResolver.openInputStream(uri)
                    } catch (e: Exception) {
                        // Если не получилось, пробуем как file:// URI напрямую
                        if (uri.scheme == "file") {
                            try {
                                val file = java.io.File(uri.path ?: "")
                                if (file.exists()) {
                                    inputStream = java.io.FileInputStream(file)
                                }
                            } catch (e2: Exception) {
                                if (BuildConfig.DEBUG) {
                                    Log.e("GoalsDetailsScreen", "Failed to open file stream for goal image", e2)
                                }
                            }
                        }
                    }
                    
                    if (inputStream != null) {
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream.close()
                        bitmap?.asImageBitmap()
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) {
                        Log.e("GoalsDetailsScreen", "Failed to load goal image", e)
                    }
                    null
                }
            }
        } else {
            imageBitmap = null
        }
    }

    imageBitmap?.let { bitmap ->
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsDetailsScreen(
    sphereId: Int,
    viewModel: LifeBalanceViewModel,
    navController: NavController
) {
    val selectedSphere by viewModel.selectedSphere.collectAsState()
    val goals by viewModel.goals.collectAsState()

    var showCreateGoalDialog by remember { mutableStateOf(false) }
    var expandedMenuGoalId by remember { mutableStateOf<Int?>(null) }
    var showEditDialog by remember { mutableStateOf<Goal?>(null) }
    var showPhotoDialog by remember { mutableStateOf<Goal?>(null) }
    var showLinkDialog by remember { mutableStateOf<Goal?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Goal?>(null) }
    var showFullScreenImage by remember { mutableStateOf<Pair<Goal, Int>?>(null) }
    var showPremiumDialog by remember { mutableStateOf<PremiumFeature?>(null) }

    val isPremium by viewModel.isPremium.collectAsState()

    // Выбираем сферу, goals обновится автоматически через flatMapLatest
    LaunchedEffect(sphereId) {
        viewModel.selectSphereById(sphereId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.main_goals).uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(
                                R.string.for_area,
                                selectedSphere?.name ?: ""
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.goToNextSphere(sphereId)?.let { nextId ->
                            navController.navigate("goals_details/$nextId")
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Вперёд",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            val goalsLimitReached = goals.size >= 5 && !isPremium
            FloatingActionButton(
                onClick = {
                    if (goalsLimitReached) {
                        showPremiumDialog = PremiumFeature.GOALS_EXTRA
                    } else {
                        showCreateGoalDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(16.dp)
                    .size(40.dp),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Добавить цель",
                        modifier = Modifier.size(24.dp)
                    )
                    if (goalsLimitReached) {
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
                                contentDescription = "Премиум",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (goals.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = stringResource(R.string.no_goals_added_yet))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(
                        items = goals,
                        key = { _, goal -> goal.id }
                    ) { index, goal ->
                        val movedFromGoalsStr = stringResource(R.string.moved_from_goals)
                        GoalItem(
                            goal = goal,
                            index = index,
                            viewModel = viewModel,
                            expandedMenuGoalId = expandedMenuGoalId,
                            onMenuExpandedChange = { expandedMenuGoalId = it },
                            onEditClick = { showEditDialog = goal },
                            onPhotoClick = { showPhotoDialog = goal },
                            onLinkClick = { showLinkDialog = goal },
                            onMoveToTasksClick = {
                                viewModel.moveGoalToTasks(goal, movedFromGoalsStr)
                            },
                            onDeleteClick = { showDeleteDialog = goal },
                            onImageClick = { photoUri ->
                                val photoIndex = goal.getPhotoUrisList().indexOf(photoUri)
                                if (photoIndex >= 0) {
                                    showFullScreenImage = Pair(goal, photoIndex)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

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

    showPremiumDialog?.let { feature ->
        PremiumRequiredDialog(
            feature = feature,
            onDismiss = { showPremiumDialog = null }
        )
    }

    showEditDialog?.let { goal ->
        GoalEditDialog(
            goal = goal,
            onDismiss = { showEditDialog = null },
            onSave = { text, date, link ->
                viewModel.updateGoal(goal.copy(text = text, deadline = date, link = link))
                showEditDialog = null
            }
        )
    }

    showPhotoDialog?.let { goal ->
        GoalPhotoDialog(
            goal = goal,
            onDismiss = { showPhotoDialog = null },
            onSave = { photoUris ->
                viewModel.updateGoal(goal.copy(photoUris = photoUris))
                showPhotoDialog = null
            }
        )
    }

    showLinkDialog?.let { goal ->
        GoalLinkDialog(
            goal = goal,
            onDismiss = { showLinkDialog = null },
            onSave = { link ->
                viewModel.updateGoal(goal.copy(link = link))
                showLinkDialog = null
            }
        )
    }

    showDeleteDialog?.let { goal ->
        GoalDeleteDialog(
            goal = goal,
            onDismiss = { showDeleteDialog = null },
            onConfirm = {
                viewModel.deleteGoal(goal)
                showDeleteDialog = null
            }
        )
    }

    showFullScreenImage?.let { (goal, photoIndex) ->
        var currentPhotoIndex by remember(goal.id) { mutableStateOf(photoIndex) }
        val photoUris = goal.getPhotoUrisList()
        FullScreenImageDialog(
            photoUris = photoUris,
            initialIndex = photoIndex,
            onDismiss = { showFullScreenImage = null },
            onEdit = {
                showFullScreenImage = null
                showPhotoDialog = goal
            },
            onDelete = {
                if (currentPhotoIndex < photoUris.size) {
                    val photoToDelete = photoUris[currentPhotoIndex]
                    val updatedPhotoUris = photoUris.filter { it != photoToDelete }
                    viewModel.updateGoal(goal.copy(photoUris = updatedPhotoUris.toPhotoUrisString()))
                    // Закрываем диалог после удаления
                    showFullScreenImage = null
                }
            },
            onBack = {
                showFullScreenImage = null
            },
            onPageChanged = { index -> currentPhotoIndex = index }
        )
    }
}

@Composable
fun GoalItem(
    goal: Goal,
    index: Int,
    viewModel: LifeBalanceViewModel,
    expandedMenuGoalId: Int?,
    onMenuExpandedChange: (Int?) -> Unit,
    onEditClick: () -> Unit,
    onPhotoClick: () -> Unit,
    onLinkClick: () -> Unit,
    onMoveToTasksClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onImageClick: (String) -> Unit
) {
    // Используем checked напрямую из объекта goal, так как Room Flow автоматически обновляет список
    val checked = goal.checked
    val uriHandler = LocalUriHandler.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.small
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { isChecked ->
                        viewModel.saveGoalChecked(goal.id, isChecked)
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Текст цели
                    Text(
                        "${index + 1}. ${goal.text}",
                        fontWeight = FontWeight.Normal,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            textDecoration = if (checked) TextDecoration.LineThrough
                            else TextDecoration.None,
                            color = if (checked)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    )

                    // Ссылка под текстом цели, над датой
                    goal.link?.takeIf { it.isNotBlank() }?.let { link ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = link,
                            style = MaterialTheme.typography.bodySmall.copy(
                                textDecoration = if (checked) TextDecoration.LineThrough else Underline,
                                color = if (checked)
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                else
                                    Color(0xFF1976D2)
                            ),
                            modifier = Modifier.clickable {
                                if (!checked) {
                                    uriHandler.openUri(link)
                                }
                            }
                        )
                    }

                    val context = LocalContext.current
                    val locale = context.resources.configuration.locales[0]
                    val dateFormatter = DateTimeFormatter.ofPattern(
                        "dd MMMM yyyy",
                        locale
                    )
                    val formattedDeadline = goal.deadline.format(dateFormatter)
                        .replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(locale) else it.toString()
                        }
                    Text(
                        text = "${stringResource(R.string.deadline_prefix)} $formattedDeadline",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textDecoration = if (checked) TextDecoration.LineThrough
                            else TextDecoration.None,
                            color = if (checked)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            else
                                Color(0xFFE53935)
                        )
                    )
                }

                // Меню с тремя точками
                Box {
                    IconButton(onClick = {
                        onMenuExpandedChange(if (expandedMenuGoalId == goal.id) null else goal.id)
                    }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Меню",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = expandedMenuGoalId == goal.id,
                        onDismissRequest = { onMenuExpandedChange(null) }
                    ) {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.edit)) },
                            onClick = {
                                onMenuExpandedChange(null)
                                onEditClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.add_photo)) },
                            onClick = {
                                onMenuExpandedChange(null)
                                onPhotoClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.add_link)) },
                            onClick = {
                                onMenuExpandedChange(null)
                                onLinkClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.move_to_tasks)) },
                            onClick = {
                                onMenuExpandedChange(null)
                                onMoveToTasksClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                onMenuExpandedChange(null)
                                onDeleteClick()
                            }
                        )
                    }
                }
            }

            // Фото сбоку, если есть (до 3 фото)
            val photoUris = goal.getPhotoUrisList()
            if (photoUris.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Spacer(modifier = Modifier.width(48.dp)) // Отступ под чекбокс
                    photoUris.forEachIndexed { index, photoUri ->
                        if (index > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Box {
                            LoadImageFromUri(
                                uriString = photoUri,
                                contentDescription = "Фото цели ${index + 1}",
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onImageClick(photoUri) },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoalEditDialog(
    goal: Goal,
    onDismiss: () -> Unit,
    onSave: (String, LocalDate, String?) -> Unit
) {
    var goalText by remember { mutableStateOf(goal.text) }
    var linkText by remember { mutableStateOf(goal.link ?: "") }
    var deadline by remember { mutableStateOf(goal.deadline) }
    var showDatePicker by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.edit_goal),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = goalText,
                    onValueChange = { goalText = it },
                    label = { Text(text = stringResource(R.string.goal_text)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.tertiary,   // фон когда поле в фокусе
                        unfocusedContainerColor = MaterialTheme.colorScheme.tertiary, // фон когда поле не в фокусе
                        focusedBorderColor = MaterialTheme.colorScheme.primary,   // цвет рамки в фокусе
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary, // цвет рамки без фокуса
                        cursorColor = MaterialTheme.colorScheme.primary,          // цвет курсора
                        focusedLabelColor = MaterialTheme.colorScheme.primary,    // цвет label в фокусе
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSecondary // цвет label без фокуса
                    )
                )

                // Поле для редактирования ссылки
                OutlinedTextField(
                    value = linkText,
                    onValueChange = { linkText = it },
                    label = { Text(text = stringResource(R.string.link_url)) },
                    placeholder = { Text("https://example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.tertiary,   // фон когда поле в фокусе
                        unfocusedContainerColor = MaterialTheme.colorScheme.tertiary, // фон когда поле не в фокусе
                        focusedBorderColor = MaterialTheme.colorScheme.primary,   // цвет рамки в фокусе
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary, // цвет рамки без фокуса
                        cursorColor = MaterialTheme.colorScheme.primary,          // цвет курсора
                        focusedLabelColor = MaterialTheme.colorScheme.primary,    // цвет label в фокусе
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSecondary // цвет label без фокуса
                    )
                )

                val currentLocale = Locale.getDefault()  // получаем текущую локаль устройства
                val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", currentLocale)

                val formattedDeadline = deadline.format(dateFormatter).replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(currentLocale) else it.toString()
                }

                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(text = stringResource(R.string.change_date).uppercase(Locale.getDefault()))
                }

                Text(
                    text = stringResource(R.string.deadline_text, formattedDeadline),
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.cancel).uppercase(Locale.getDefault()))
                    }
                    TextButton(onClick = {
                        if (goalText.isNotBlank()) {
                            onSave(goalText, deadline, linkText.ifBlank { null })
                        }
                    }) {
                        Text(text = stringResource(R.string.save).uppercase(Locale.getDefault()))
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        var selectedDate by remember { mutableStateOf(deadline) }

        LaunchedEffect(showDatePicker) {
            if (showDatePicker) {
                selectedDate = deadline
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
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Например, получаем текущую локаль
                            val currentLocale = Locale.getDefault()

                            val dayOfWeek = selectedDate.format(
                                DateTimeFormatter.ofPattern("EEEE", currentLocale)
                            ).replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(currentLocale) else it.toString()
                            }

                            Text(
                                text = dayOfWeek,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )


                            Spacer(modifier = Modifier.height(4.dp))


                            val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", currentLocale)
                            val formattedDate = selectedDate.format(dateFormatter).replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(currentLocale) else it.toString()
                            }
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    DatePickerCalendar(
                        initialDate = selectedDate,
                        onDateSelected = { newDate ->
                            selectedDate = newDate
                        }
                    )

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
                            deadline = selectedDate
                            showDatePicker = false
                        }) {
                            Text(text = stringResource(R.string.save).uppercase(Locale.getDefault()))
                        }
                    }
                }
            }
        }
    }
}

fun getAppLocale(languageCode: String): Locale {
    return Locale(languageCode)
}



@Composable
fun GoalPhotoDialog(
    goal: Goal,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var photoUris by remember { mutableStateOf(goal.getPhotoUrisList()) }
    var isSaving by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { sourceUri ->
            if (photoUris.size < MAX_PHOTOS) {
                scope.launch {
                    // Копируем фото в приватное хранилище
                    val savedUri = ImageStorageHelper.copyImageToPrivateStorage(context, sourceUri)
                    savedUri?.let { saved ->
                        val savedUriString = saved.toString()
                        if (!photoUris.contains(savedUriString)) {
                            photoUris = photoUris + savedUriString
                        }
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.add_photo),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontWeight = FontWeight.Bold
                )

                Text(text = stringResource(R.string.photos_added, photoUris.size, MAX_PHOTOS),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (photoUris.size >= MAX_PHOTOS) Color(0xFFE53935)
                    else MaterialTheme.colorScheme.onSurface
                )

                Button(
                    onClick = {
                        if (photoUris.size < MAX_PHOTOS) {
                            imagePickerLauncher.launch("image/*")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = photoUris.size < MAX_PHOTOS,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = if (photoUris.size < MAX_PHOTOS)
                            stringResource(R.string.add_photo)
                        else
                            stringResource(R.string.limit_reached)
                    )
                }

                // Отображение всех добавленных фото
                photoUris.forEachIndexed { index, uri ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box {
                            LoadImageFromUri(
                                uriString = uri,
                                contentDescription = "Фото ${index + 1}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = {
                                    photoUris = photoUris.filter { it != uri }
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .background(
                                        Color.Black.copy(alpha = 0.6f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Удалить фото",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.cancel).uppercase(Locale.getDefault()))
                    }
                    TextButton(
                        onClick = {
                            if (!isSaving) {
                                isSaving = true
                                scope.launch {
                                    // Удаляем старые фото, которые были удалены из списка
                                    val oldUris = goal.getPhotoUrisList()
                                    oldUris.forEach { oldUri ->
                                        if (!photoUris.contains(oldUri)) {
                                            ImageStorageHelper.deleteImageFromStorage(context, oldUri)
                                        }
                                    }
                                    // Сохраняем новые URI
                                    onSave(photoUris.toPhotoUrisString())
                                    isSaving = false
                                }
                            }
                        },
                        enabled = !isSaving
                    ) {
                        Text(
                            text = if (isSaving) stringResource(R.string.saving).uppercase(Locale.getDefault())
                            else stringResource(R.string.save).uppercase(Locale.getDefault())
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GoalLinkDialog(
    goal: Goal,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit
) {
    var linkText by remember { mutableStateOf(goal.link ?: "") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.add_link),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = linkText,
                    onValueChange = { linkText = it },
                    label = { Text(text = stringResource(R.string.link_url)) },
                    placeholder = { Text("https://example.com") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.tertiary,   // фон когда поле в фокусе
                        unfocusedContainerColor = MaterialTheme.colorScheme.tertiary, // фон когда поле не в фокусе
                        focusedBorderColor = MaterialTheme.colorScheme.primary,   // цвет рамки в фокусе
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary, // цвет рамки без фокуса
                        cursorColor = MaterialTheme.colorScheme.primary,          // цвет курсора
                        focusedLabelColor = MaterialTheme.colorScheme.primary,    // цвет label в фокусе
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSecondary // цвет label без фокуса
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.cancel).uppercase(Locale.getDefault()))
                    }
                    TextButton(onClick = {
                        onSave(linkText.ifBlank { null })
                    }) {
                        Text(text = stringResource(R.string.save).uppercase(Locale.getDefault()))
                    }
                }
            }
        }
    }
}

@Composable
fun GoalDeleteDialog(
    goal: Goal,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.delete_goal),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSecondary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = stringResource(R.string.Are_you_sure_you_want_to_delete_this_goal),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.cancel).uppercase(Locale.getDefault()))
                    }
                    
                    TextButton(onClick = onConfirm) {
                        Text(text = stringResource(R.string.delete).uppercase(Locale.getDefault()),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FullScreenImageDialog(
    imageUri: String,
    onDismiss: () -> Unit
) {
    FullScreenImageDialog(
        photoUris = listOf(imageUri),
        initialIndex = 0,
        onDismiss = onDismiss
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullScreenImageDialog(
    photoUris: List<String>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onPageChanged: ((Int) -> Unit)? = null
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, photoUris.size - 1),
        pageCount = { photoUris.size }
    )
    var showMenu by remember { mutableStateOf(false) }
    
    // Отслеживаем изменение страницы
    LaunchedEffect(pagerState.currentPage) {
        onPageChanged?.invoke(pagerState.currentPage)
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                LoadImageFromUri(
                    uriString = photoUris[page],
                    contentDescription = "Полноэкранное фото ${page + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            
            // Меню с тремя точками в правом верхнем углу
            if (onEdit != null || onDelete != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    IconButton(
                        onClick = { showMenu = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Меню",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(0.dp)
                                .size(24.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        onEdit?.let {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.edit)) },
                                onClick = {
                                    showMenu = false
                                    it()
                                }
                            )
                        }
                        onDelete?.let {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    it()
                                }
                            )
                        }
                    }
                }
            }
            
            // Кнопка "Назад" в нижнем правом углу
            onBack?.let {
                IconButton(
                    onClick = it,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Назад",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
