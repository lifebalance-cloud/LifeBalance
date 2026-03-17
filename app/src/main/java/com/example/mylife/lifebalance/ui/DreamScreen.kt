package com.example.mylife.lifebalance.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.example.lifebalance.BuildConfig
import com.example.lifebalance.R
import com.example.mylife.lifebalance.utils.ImageStorageHelper
import com.example.mylife.lifebalance.viewmodel.LifeBalanceViewModel
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.combine


// Data classes для секторов и мечтаний
data class DreamSector(
    val id: Int,
    @StringRes val nameRes: Int,
    val name: String,
    val color: Color,
    val photoUris: List<String> = emptyList()
)

data class DreamEntry(
    val id: Int,
    val sectorId: Int,
    val text: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DreamScreen(
    viewModel: LifeBalanceViewModel,
    onNavigate: (Screen) -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Инициализация секторов с цветами из примера (статичные данные)
    val sectorDefinitions = remember {
        listOf(
            DreamSector(0, R.string.sector_wealth, context.resources.getString(R.string.sector_wealth), Color(0xFF9945ba)),
            DreamSector(1, R.string.sector_fame, context.resources.getString(R.string.sector_fame), Color(0xFFE53935)),
            DreamSector(2, R.string.sector_relationships, context.resources.getString(R.string.sector_relationships), Color(0xFFEC407A)),
            DreamSector(3, R.string.sector_home, context.resources.getString(R.string.sector_home), Color(0xFF7CB342)),
            DreamSector(4, R.string.sector_state, context.resources.getString(R.string.sector_state), Color(0xFFe0e0e0)),
            DreamSector(5, R.string.sector_children, context.resources.getString(R.string.sector_children), Color(0xFFa8a8a8)),
            DreamSector(6, R.string.sector_knowledge, context.resources.getString(R.string.sector_knowledge), Color(0xFF8D6E63)),
            DreamSector(7, R.string.sector_career, context.resources.getString(R.string.sector_career), Color(0xFF42A5F5)),
            DreamSector(8, R.string.sector_travel, context.resources.getString(R.string.sector_travel), Color(0xFFFDD835))
        )
    }
    
    // Загружаем фотографии для каждого сектора из репозитория
    val photos0 by viewModel.getPhotosBySectorId(0).collectAsState(initial = emptyList())
    val photos1 by viewModel.getPhotosBySectorId(1).collectAsState(initial = emptyList())
    val photos2 by viewModel.getPhotosBySectorId(2).collectAsState(initial = emptyList())
    val photos3 by viewModel.getPhotosBySectorId(3).collectAsState(initial = emptyList())
    val photos4 by viewModel.getPhotosBySectorId(4).collectAsState(initial = emptyList())
    val photos5 by viewModel.getPhotosBySectorId(5).collectAsState(initial = emptyList())
    val photos6 by viewModel.getPhotosBySectorId(6).collectAsState(initial = emptyList())
    val photos7 by viewModel.getPhotosBySectorId(7).collectAsState(initial = emptyList())
    val photos8 by viewModel.getPhotosBySectorId(8).collectAsState(initial = emptyList())
    
    // Объединяем секторы с фотографиями
    val sectors = remember(sectorDefinitions, photos0, photos1, photos2, photos3, photos4, photos5, photos6, photos7, photos8) {
        listOf(
            sectorDefinitions[0].copy(photoUris = photos0.map { it.photoUri }),
            sectorDefinitions[1].copy(photoUris = photos1.map { it.photoUri }),
            sectorDefinitions[2].copy(photoUris = photos2.map { it.photoUri }),
            sectorDefinitions[3].copy(photoUris = photos3.map { it.photoUri }),
            sectorDefinitions[4].copy(photoUris = photos4.map { it.photoUri }),
            sectorDefinitions[5].copy(photoUris = photos5.map { it.photoUri }),
            sectorDefinitions[6].copy(photoUris = photos6.map { it.photoUri }),
            sectorDefinitions[7].copy(photoUris = photos7.map { it.photoUri }),
            sectorDefinitions[8].copy(photoUris = photos8.map { it.photoUri })
        )
    }
    
    // Загружаем аффирмации из репозитория
    val allAffirmations by viewModel.getAllAffirmations().collectAsState(initial = emptyList())
    val dreamEntries = remember(allAffirmations) {
        allAffirmations.map { affirmation ->
            DreamEntry(
                id = affirmation.id.toInt(),
                sectorId = affirmation.sectorId,
                text = affirmation.text
            )
        }
    }
    var fullScreenImageData by remember { mutableStateOf<Triple<List<String>, Int, Int>?>(null) } // Список фото, начальный индекс, индекс сектора
    var showAddDreamDialog by remember { mutableStateOf(false) }
    var selectedSectorForDream by remember { mutableStateOf<Int?>(null) }
    var showPhotoDialog by remember { mutableStateOf<Int?>(null) }
    

    Scaffold(
        topBar = {
            IdeasTopAppBar(onNavigate = { screen ->
                navController.navigate(screen.route)
            })
        },
        content = { paddingValues ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Заголовок
                Text(
                    text = stringResource(R.string.vision_board).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = stringResource(R.string.visualize_dreams_and_desires).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Сетка 3x3 секторов в общей рамке
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    border = BorderStroke(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                    // Верхний ряд
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        sectors.take(3).forEachIndexed { index, sector ->
                            SectorCard(
                                sector = sector,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showPhotoDialog = index },
                                cornerShape = when (index) {
                                    0 -> RoundedCornerShape(topStart = 12.dp) // Верхний левый
                                    1 -> RoundedCornerShape(0.dp) // Верхний средний
                                    2 -> RoundedCornerShape(topEnd = 12.dp) // Верхний правый
                                    else -> RoundedCornerShape(0.dp)
                                },
                                sectorIndex = index,
                                onPhotoClick = { uri ->
                                    val sector = sectors[index]
                                    val photoIndex = sector.photoUris.indexOf(uri)
                                    if (photoIndex >= 0) {
                                        fullScreenImageData = Triple(sector.photoUris, photoIndex, index)
                                    }
                                }
                            )
                        }
                    }
                    
                    // Средний ряд
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        sectors.drop(3).take(3).forEachIndexed { localIndex, sector ->
                            val index = localIndex + 3
                            SectorCard(
                                sector = sector,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showPhotoDialog = index },
                                cornerShape = RoundedCornerShape(0.dp), // Средний ряд без скругления
                                sectorIndex = index,
                                onPhotoClick = { uri ->
                                    val sector = sectors[index]
                                    val photoIndex = sector.photoUris.indexOf(uri)
                                    if (photoIndex >= 0) {
                                        fullScreenImageData = Triple(sector.photoUris, photoIndex, index)
                                    }
                                }
                            )
                        }
                    }
                    
                    // Нижний ряд
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        sectors.drop(6).take(3).forEachIndexed { localIndex, sector ->
                            val index = localIndex + 6
                            SectorCard(
                                sector = sector,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showPhotoDialog = index },
                                cornerShape = when (index) {
                                    6 -> RoundedCornerShape(bottomStart = 12.dp) // Нижний левый
                                    7 -> RoundedCornerShape(0.dp) // Нижний средний
                                    8 -> RoundedCornerShape(bottomEnd = 12.dp) // Нижний правый
                                    else -> RoundedCornerShape(0.dp) // Остальные без скругления снизу
                                },
                                sectorIndex = index,
                                onPhotoClick = { uri ->
                                    val sector = sectors[index]
                                    val photoIndex = sector.photoUris.indexOf(uri)
                                    if (photoIndex >= 0) {
                                        fullScreenImageData = Triple(sector.photoUris, photoIndex, index)
                                    }
                                }
                            )
                        }
                    }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Список записей мечтаний
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(dreamEntries, key = { it.id }) { entry ->
                        DreamEntryItem(
                            entry = entry,
                            sector = sectors.find { it.id == entry.sectorId },
                            onUpdate = { newText ->
                                val affirmation = allAffirmations.find { it.id.toInt() == entry.id }
                                affirmation?.let {
                                    viewModel.updateAffirmation(it.copy(text = newText))
                                }
                            },
                            onDelete = {
                                val affirmation = allAffirmations.find { it.id.toInt() == entry.id }
                                affirmation?.let {
                                    viewModel.deleteAffirmation(it)
                                }
                            }
                        )
                    }
                    
                    // Кнопка добавления новой записи
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAddDreamDialog = true },
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.add_affirmations).uppercase(Locale.getDefault()),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    )
    
    // Диалог полноэкранного просмотра изображения
    fullScreenImageData?.let { (photoUris, initialIndex, sectorIndex) ->
        var currentPhotoIndex by remember { mutableStateOf(initialIndex) }
        FullScreenImageDialog(
            photoUris = photoUris,
            initialIndex = initialIndex,
            onDismiss = { fullScreenImageData = null },
            onEdit = {
                fullScreenImageData = null
                showPhotoDialog = sectorIndex
            },
            onDelete = {
                if (currentPhotoIndex < photoUris.size) {
                    val photoToDelete = photoUris[currentPhotoIndex]
                    // Находим фото в базе данных по URI
                    val allPhotos = when (sectorIndex) {
                        0 -> photos0
                        1 -> photos1
                        2 -> photos2
                        3 -> photos3
                        4 -> photos4
                        5 -> photos5
                        6 -> photos6
                        7 -> photos7
                        8 -> photos8
                        else -> emptyList()
                    }
                    val photoToDeleteEntity = allPhotos.find { it.photoUri == photoToDelete }
                    photoToDeleteEntity?.let {
                        viewModel.deletePhoto(it)
                    }
                    // Если удалили последнее фото, закрываем диалог
                    val remainingPhotos = allPhotos.filter { it.photoUri != photoToDelete }
                    if (remainingPhotos.isEmpty()) {
                        fullScreenImageData = null
                    } else {
                        // Обновляем данные для диалога
                        val newIndex = if (currentPhotoIndex >= remainingPhotos.size) {
                            remainingPhotos.size - 1
                        } else {
                            currentPhotoIndex
                        }.coerceAtLeast(0)
                        currentPhotoIndex = newIndex
                        fullScreenImageData = Triple(remainingPhotos.map { it.photoUri }, newIndex, sectorIndex)
                    }
                }
            },
            onBack = {
                fullScreenImageData = null
            },
            onPageChanged = { index -> currentPhotoIndex = index }
        )
    }
    
    // Диалог добавления желания
    if (showAddDreamDialog) {
        AddDreamDialog(
            sectors = sectors,
            onDismiss = { 
                showAddDreamDialog = false
                selectedSectorForDream = null
            },
            onSave = { sectorId, text ->
                if (text.isNotBlank()) {
                    viewModel.addAffirmation(sectorId, text)
                    showAddDreamDialog = false
                    selectedSectorForDream = null
                }
            }
        )
    }
    
    // Диалог добавления фото
    showPhotoDialog?.let { sectorIndex ->
        SectorPhotoDialog(
            viewModel = viewModel,
            sector = sectors[sectorIndex],
            sectorId = sectorIndex,
            onDismiss = { showPhotoDialog = null },
            onSave = { photoUris ->
                viewModel.updatePhotosForSector(sectorIndex, photoUris)
                showPhotoDialog = null
            }
        )
    }
}

@Composable
fun SectorCard(
    sector: DreamSector,
    modifier: Modifier = Modifier,
    cornerShape: RoundedCornerShape = RoundedCornerShape(12.dp),
    sectorIndex: Int = 0,
    onPhotoClick: (String) -> Unit
) {
    val aspectRatio = 1f
    
    Card(
        modifier = modifier
            .aspectRatio(aspectRatio),
        shape = cornerShape,
        colors = CardDefaults.cardColors(
            containerColor = sector.color
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Фотографии (горизонтальное расположение для двух фото)
            when (sector.photoUris.size) {
                0 -> {
                    // Нет фото - показываем только текст
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (sector.name.isNotBlank()) {
                            Text(
                                text = sector.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                1 -> {
                    // Одно фото - на весь сектор
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Фото без текста
                        LoadImageFromUri(
                            uriString = sector.photoUris[0],
                            contentDescription = "Фото сектора",
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onPhotoClick(sector.photoUris[0]) }
                                .clip(cornerShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                2 -> {
                    // Два фото - горизонтально
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Фото без текста
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 0.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Левое фото
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                // Определяем скругления для левого фото на основе позиции сектора
                                val leftPhotoShape = when (sectorIndex) {
                                    0 -> RoundedCornerShape(topStart = 12.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
                                    3 -> RoundedCornerShape(0.dp)
                                    6 -> RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 12.dp, bottomEnd = 0.dp)
                                    else -> RoundedCornerShape(0.dp)
                                }
                                LoadImageFromUri(
                                    uriString = sector.photoUris[0],
                                    contentDescription = "Фото 1",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { onPhotoClick(sector.photoUris[0]) }
                                        .clip(leftPhotoShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            // Правое фото
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                // Определяем скругления для правого фото на основе позиции сектора
                                val rightPhotoShape = when (sectorIndex) {
                                    2 -> RoundedCornerShape(topStart = 0.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
                                    5 -> RoundedCornerShape(0.dp)
                                    8 -> RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 12.dp)
                                    else -> RoundedCornerShape(0.dp)
                                }
                                LoadImageFromUri(
                                    uriString = sector.photoUris[1],
                                    contentDescription = "Фото 2",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { onPhotoClick(sector.photoUris[1]) }
                                        .clip(rightPhotoShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DreamEntryItem(
    entry: DreamEntry,
    sector: DreamSector?,
    onUpdate: (String) -> Unit,
    onDelete: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedText by remember { mutableStateOf(entry.text) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Цветной индикатор сектора
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        sector?.color ?: Color.Gray,
                        RoundedCornerShape(4.dp)
                    )
            )
            
            if (isEditing) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = editedText,
                        onValueChange = { editedText = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5,
                        minLines = 1,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    //Линия перед удалить
                    Divider(
                        color = MaterialTheme.colorScheme.outline,
                        thickness = 1.dp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDelete) {
                            Text(text = stringResource(R.string.delete).uppercase(Locale.getDefault()),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = {
                            isEditing = false
                            editedText = entry.text
                        }) {
                            Text(text = stringResource(R.string.cancel).uppercase(Locale.getDefault()),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = {
                            if (editedText.isNotBlank()) {
                                onUpdate(editedText)
                                isEditing = false
                            }
                        }) {
                            Text(text = stringResource(R.string.save).uppercase(Locale.getDefault()),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isEditing = true }
                ) {
                    if (sector?.name?.isNotBlank() == true) {
                        Text(
                            text = sector.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Text(
                        text = entry.text,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun AddDreamDialog(
    sectors: List<DreamSector>,
    onDismiss: () -> Unit,
    onSave: (Int, String) -> Unit
) {
    var selectedSectorId by remember { mutableStateOf<Int?>(null) }
    var dreamTexts by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    
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
                // Заголовок
                Text(
                    text = stringResource(R.string.add_affirmations),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontWeight = FontWeight.Bold
                )
                
                // Список секторов с возможностью ввода текста
                LazyColumn(
                    modifier = Modifier.height(400.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sectors.filter { it.name.isNotBlank() }) { sector ->
                        val isSelected = selectedSectorId == sector.id
                        val sectorText = dreamTexts[sector.id] ?: ""
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    selectedSectorId = if (isSelected) null else sector.id
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) 
                                    sector.color.copy(alpha = 0.1f)
                                else 
                                    MaterialTheme.colorScheme.surface
                            ),
                            border = if (isSelected)
                                null // Границы нет, когда выбран
                            else 
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Заголовок сектора
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(
                                                sector.color,
                                                RoundedCornerShape(4.dp)
                                            )
                                    )
                                    Text(
                                        text = sector.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                
                                // Поле ввода текста (показывается только для активного сектора)
                                if (isSelected) {
                                    OutlinedTextField(
                                        value = sectorText,
                                        onValueChange = { newText ->
                                            dreamTexts = dreamTexts + (sector.id to newText)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text(stringResource(R.string.write_down_your_affirmation)) },
                                        maxLines = 5,
                                        minLines = 3,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = sector.color,
                                            unfocusedBorderColor = sector.color.copy(alpha = 0.5f),
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Кнопки в разных углах
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.cancel).uppercase(Locale.getDefault()))
                    }
                    TextButton(
                        onClick = {
                            selectedSectorId?.let { sectorId ->
                                val text = dreamTexts[sectorId] ?: ""
                                if (text.isNotBlank()) {
                                    onSave(sectorId, text)
                                }
                            }
                        },
                        enabled = selectedSectorId != null && 
                                  !dreamTexts[selectedSectorId!!].isNullOrBlank()
                    ) {
                        Text(text = stringResource(R.string.save).uppercase(Locale.getDefault()))
                    }
                }
            }
        }
    }
}

private const val MAX_SECTOR_PHOTOS = 2

@Composable
fun SectorPhotoDialog(
    viewModel: LifeBalanceViewModel,
    sector: DreamSector,
    sectorId: Int,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var photoUris by remember { mutableStateOf(sector.photoUris) }
    var pendingUris by remember { mutableStateOf<Map<String, Uri>>(emptyMap()) } // URI для новых фото: savedUri -> sourceUri
    var isSaving by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { sourceUri ->
            if (photoUris.size < MAX_SECTOR_PHOTOS) {
                scope.launch {
                    // Сохраняем фото в приватное хранилище для предпросмотра
                    val savedUri = ImageStorageHelper.copyImageToPrivateStorage(context, sourceUri)
                    savedUri?.let { saved ->
                        val savedUriString = saved.toString()
                        if (!photoUris.contains(savedUriString)) {
                            photoUris = photoUris + savedUriString
                            // Сохраняем исходный URI для последующего сохранения в базу
                            pendingUris = pendingUris + (savedUriString to sourceUri)
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

                Text(
                    text = stringResource(
                        id = R.string.photos_added,
                        photoUris.size,
                        MAX_SECTOR_PHOTOS),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (photoUris.size >= MAX_SECTOR_PHOTOS) Color(0xFFE53935)
                    else MaterialTheme.colorScheme.onSurface
                )

                Button(
                    onClick = {
                        if (photoUris.size < MAX_SECTOR_PHOTOS) {
                            imagePickerLauncher.launch("image/*")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = photoUris.size < MAX_SECTOR_PHOTOS,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = if (photoUris.size < MAX_SECTOR_PHOTOS)
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
                            containerColor = MaterialTheme.colorScheme.surface
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
                                    // Удаляем из pendingUris, если фото было удалено
                                    pendingUris = pendingUris.filterKeys { it != uri }
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
                                    try {
                                        // Получаем текущие фото из базы данных
                                        val existingPhotos = viewModel.getPhotosBySectorIdSync(sectorId)
                                        val existingUris = existingPhotos.map { it.photoUri }.toSet()
                                        
                                        // Добавляем новые фото, которых еще нет в базе
                                        // Для новых фото используем исходный URI из pendingUris
                                        // Репозиторий создаст новый файл, но мы обновим список после сохранения
                                        val newPhotos = mutableListOf<String>()
                                        photoUris.forEach { savedUriString ->
                                            if (!existingUris.contains(savedUriString)) {
                                                // Используем исходный URI, если он есть в pendingUris
                                                val sourceUri = pendingUris[savedUriString]
                                                if (sourceUri != null) {
                                                    // Используем исходный URI для сохранения через репозиторий
                                                    val result = viewModel.addPhotoToSectorSync(sectorId, sourceUri)
                                                    result.onSuccess { photo ->
                                                        if (BuildConfig.DEBUG) android.util.Log.d("SectorPhotoDialog", "Photo added successfully")
                                                        newPhotos.add(photo.photoUri)
                                                    }
                                                    result.onFailure { error ->
                                                        if (BuildConfig.DEBUG) android.util.Log.e("SectorPhotoDialog", "Failed to add photo", error)
                                                    }
                                                } else {
                                                    // Если исходного URI нет, используем сохраненный URI
                                                    try {
                                                        val uriObj = Uri.parse(savedUriString)
                                                        val result = viewModel.addPhotoToSectorSync(sectorId, uriObj)
                                                        result.onSuccess { photo ->
                                                            newPhotos.add(photo.photoUri)
                                                        }
                                                        result.onFailure { error ->
                                                            if (BuildConfig.DEBUG) android.util.Log.e("SectorPhotoDialog", "Failed to add photo", error)
                                                        }
                                                    } catch (e: Exception) {
                                                        if (BuildConfig.DEBUG) android.util.Log.e("SectorPhotoDialog", "Error parsing URI", e)
                                                    }
                                                }
                                            } else {
                                                // Фото уже есть в базе, используем его URI
                                                newPhotos.add(savedUriString)
                                            }
                                        }
                                        
                                        // Обновляем photoUris с правильными URI из базы данных
                                        photoUris = newPhotos
                                        
                                        // Удаляем фото, которые были удалены из списка
                                        existingPhotos.forEach { existingPhoto ->
                                            if (!photoUris.contains(existingPhoto.photoUri)) {
                                                viewModel.deletePhoto(existingPhoto)
                                            }
                                        }
                                        
                                        // После добавления всех новых фото, получаем актуальный список из базы
                                        val updatedPhotos = viewModel.getPhotosBySectorIdSync(sectorId)
                                        val updatedPhotoUris = updatedPhotos.sortedBy { it.order }.map { it.photoUri }
                                        
                                        // Обновляем порядок фото в базе согласно текущему списку
                                        viewModel.updatePhotosForSector(sectorId, updatedPhotoUris)
                                        
                                        // Очищаем pendingUris после сохранения
                                        pendingUris = emptyMap()
                                        
                                        // Вызываем callback для обновления UI с актуальными URI из базы
                                        onSave(updatedPhotoUris)
                                    } catch (e: Exception) {
                                        if (BuildConfig.DEBUG) android.util.Log.e("SectorPhotoDialog", "Error saving photos", e)
                                    } finally {
                                        isSaving = false
                                    }
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


