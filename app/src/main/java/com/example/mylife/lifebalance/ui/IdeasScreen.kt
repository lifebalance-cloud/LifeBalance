package com.example.mylife.lifebalance.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.lifebalance.R
import com.example.mylife.lifebalance.data.IdeaFolder
import com.example.mylife.lifebalance.data.IdeaNote
import com.example.mylife.lifebalance.viewmodel.LifeBalanceViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.app.Activity
import android.speech.RecognizerIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import java.util.Locale
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import androidx.compose.ui.window.Dialog
import android.widget.Toast
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.example.mylife.lifebalance.ui.components.GroupAutoResizedText
import com.example.mylife.lifebalance.ui.components.TextResizeController


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeasScreen(
    viewModel: LifeBalanceViewModel,
    onNavigate: (Screen) -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val folders by viewModel.folders.collectAsState()
    val notesWithoutFolder by viewModel.notesWithoutFolder.collectAsState()
    
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchDialogQuery by remember { mutableStateOf("") }
    
    // Собираем результаты поиска когда изменяется запрос
    val searchResultsFlow = remember(searchQuery) {
        if (searchQuery.isNotBlank()) {
            viewModel.searchNotes(searchQuery)
        } else {
            null
        }
    }
    val searchResults by searchResultsFlow?.collectAsState(initial = emptyList()) 
        ?: remember { mutableStateOf(emptyList<IdeaNote>()) }
    
    var selectedFolder by remember { mutableStateOf<IdeaFolder?>(null) }

    Scaffold(
        topBar = {
            IdeasTopAppBar(
                onNavigate = { screen ->
                    navController.navigate(screen.route)
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Строка с названием "ИДЕИ" и иконками
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.ideas).uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.clickable { selectedFolder = null }
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = { showCreateFolderDialog = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_folderadd),
                                contentDescription = "Создать папку",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                        IconButton(onClick = { onNavigate(Screen.Dreams) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_dreams),
                                contentDescription = "Карта желаний",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                        IconButton(onClick = {
                            showSearchDialog = true
                            searchDialogQuery = ""
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_search),
                                contentDescription = "Поиск",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSecondary
                            )
                        }

                    }
                }
                
                if (selectedFolder != null) {
                    // Показываем экран-блокнот для папки
                    FolderNotebookScreen(
                        folder = selectedFolder!!,
                        viewModel = viewModel,
                        onBack = { selectedFolder = null }
                    )
                } else if (showSearchDialog && searchQuery.isNotBlank()) {
                    // Показываем результаты поиска
                    SearchResultsScreen(
                        searchQuery = searchQuery,
                        results = searchResults,
                        viewModel = viewModel,
                        onBack = { 
                            showSearchDialog = false
                            searchQuery = ""
                        }
                    )
                } else {
                    // Основной список: папки сверху, затем заметки
                    var showAddNoteDialog by remember { mutableStateOf(false) }
                    
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Папки
                            items(folders, key = { "folder_${it.id}" }) { folder ->
                                var showEditFolderDialog by remember { mutableStateOf(false) }
                                var showDeleteFolderDialog by remember { mutableStateOf(false) }
                                FolderItem(
                                    folder = folder,
                                    onClick = { selectedFolder = folder },
                                    onEdit = { showEditFolderDialog = true },
                                    onDelete = { showDeleteFolderDialog = true }
                                )
                                
                                if (showEditFolderDialog) {
                                    EditFolderDialog(
                                        folder = folder,
                                        onDismiss = { showEditFolderDialog = false },
                                        onSave = { folderName ->
                                            if (folderName.isNotBlank()) {
                                                viewModel.updateFolder(folder.copy(name = folderName))
                                                showEditFolderDialog = false
                                            }
                                        }
                                    )
                                }
                                
                                if (showDeleteFolderDialog) {
                                    DeleteFolderDialog(
                                        folderName = folder.name,
                                        onDismiss = { showDeleteFolderDialog = false },
                                        onConfirm = {
                                            viewModel.deleteFolder(folder)
                                            showDeleteFolderDialog = false
                                        }
                                    )
                                }
                            }
                            
                            // Заметки без папки
                            items(notesWithoutFolder, key = { "note_${it.id}" }) { note ->
                                NoteItem(
                                    note = note,
                                    onUpdate = { updatedText ->
                                        viewModel.updateNote(note.copy(text = updatedText))
                                    },
                                    onDelete = { viewModel.deleteNote(note) }
                                )
                            }
                        }
                        
                        // Кнопка добавления заметки
                        FloatingActionButton(
                            onClick = { showAddNoteDialog = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                                .size(40.dp),
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp,
                                focusedElevation = 0.dp,
                                hoveredElevation = 0.dp
                            )
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Добавить заметку",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    // Диалог добавления заметки
                    if (showAddNoteDialog) {
                        AddNoteDialog(
                            onDismiss = { showAddNoteDialog = false },
                            onSave = { noteText ->
                                if (noteText.isNotBlank()) {
                                    viewModel.addNote(noteText, null)
                                    showAddNoteDialog = false
                                }
                            }
                        )
                    }
                }
            }
        }
    )
    
    // Диалог создания папки
    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onSave = { folderName ->
                if (folderName.isNotBlank()) {
                    viewModel.addFolder(folderName)
                    showCreateFolderDialog = false
                }
            }
        )
    }
    
    // Диалог поиска (показываем, пока не нажали «Искать» — тогда searchQuery заполнится и отобразятся результаты)
    if (showSearchDialog && searchQuery.isBlank()) {
        SearchDialog(
            query = searchDialogQuery,
            onQueryChange = { searchDialogQuery = it },
            onDismiss = {
                showSearchDialog = false
                searchQuery = ""
                searchDialogQuery = ""
            },
            onSearch = {
                searchQuery = searchDialogQuery
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeasTopAppBar(
    onNavigate: (Screen) -> Unit
) {
    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onNavigate(Screen.Home) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_home),
                        contentDescription = "Дела",
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = { onNavigate(Screen.Goals) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_goals),
                        contentDescription = "Главные цели",
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = { onNavigate(Screen.Balance) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_balance),
                        contentDescription = "Колесо баланса",
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = { onNavigate(Screen.Ideas) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_ideas),
                        contentDescription = "Идеи",
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = { onNavigate(Screen.Calendar) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_calendar),
                        contentDescription = "Календарь",
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = { onNavigate(Screen.Settings) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = "Настройки",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
fun FolderItem(
    folder: IdeaFolder,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_folder),
                    contentDescription = "Папка",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            // Меню с тремя точками
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Меню",
                        modifier = Modifier.size(24.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.edit)) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NoteItem(
    note: IdeaNote,
    onUpdate: (String) -> Unit,
    onDelete: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedText by remember { mutableStateOf(note.text) }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isEditing) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDelete) {
                        Text(
                            text = stringResource(R.string.delete).uppercase(Locale.getDefault()),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(onClick = {
                        isEditing = false
                        editedText = note.text
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isEditing = true }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NoteText(
                    text = note.text
                )

            }
        }
        Divider(
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = stringResource(R.string.create_folder),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSecondary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text(text = stringResource(R.string.folder_name)) },
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
                    Spacer(modifier = Modifier.width(16.dp))
                    TextButton(
                        onClick = {
                            if (folderName.isNotBlank()) {
                                onSave(folderName)
                            }
                        }
                    ) {
                        Text(text = stringResource(R.string.create).uppercase(Locale.getDefault()))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
fun EditFolderDialog(
    folder: IdeaFolder,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var folderName by remember { mutableStateOf(folder.name) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = stringResource(R.string.edit_folder),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSecondary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text(text = stringResource(R.string.folder_name)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.tertiary,
                        unfocusedContainerColor = MaterialTheme.colorScheme.tertiary,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSecondary
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.cancel).uppercase(Locale.getDefault()))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    TextButton(
                        onClick = {
                            if (folderName.isNotBlank()) {
                                onSave(folderName)
                            }
                        }
                    ) {
                        Text(text = stringResource(R.string.save).uppercase(Locale.getDefault()))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
fun DeleteFolderDialog(
    folderName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.delete_folder),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSecondary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.are_you_sure_you_want_to_delete_this_folder),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.cancel).uppercase(Locale.getDefault()))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    TextButton(
                        onClick = onConfirm,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(text = stringResource(R.string.delete).uppercase(Locale.getDefault()))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
fun SearchDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSearch: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.search),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )},
        text = {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text(text = stringResource(R.string.enter_a_word_to_search)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.close).uppercase(Locale.getDefault()))
                }
                TextButton(onClick = onSearch) {
                    Text(text = stringResource(R.string.search).uppercase(Locale.getDefault()))
                }
            }
        }
    )
}

@Composable
fun AddNoteDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val context = LocalContext.current
    val appLocale = getAppLocale(context)
    val initialDateText = remember {
        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yy", appLocale)
        "${LocalDate.now().format(dateFormatter)} "
    }
    var noteText by remember { mutableStateOf(initialDateText) }

    // Launcher для распознавания речи
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            spokenText?.firstOrNull()?.let { text ->
                // Только первая буква заглавная, остальные строчные
                val normalizedText = if (text.isNotBlank()) {
                    text.lowercase().replaceFirstChar { it.uppercaseChar() }
                } else {
                    text
                }
                noteText = if (noteText.isBlank() || noteText.trim() == initialDateText.trim()) {
                    "$initialDateText$normalizedText"
                } else {
                    "$noteText $normalizedText"
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.add_note),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontWeight = FontWeight.Bold
                )
                // Кнопка записи текста голосом
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
                            speechLauncher.launch(intent)
                        }
                    }
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
                            contentDescription = stringResource(R.string.voice_record_description),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { newValue ->
                            // Только первая буква после даты и пробела должна быть заглавной
                            if (newValue.length > noteText.length) {
                                val dateLength = initialDateText.trim().length
                                val addedChar = newValue.lastOrNull()
                                // Проверяем, что это первый символ после даты и пробела
                                if (newValue.length == dateLength + 2 && addedChar?.isLetter() == true) {
                                    // Первая буква - заглавная
                                    noteText = newValue.dropLast(1) + addedChar.uppercaseChar()
                                } else if (newValue.length > dateLength + 2 && addedChar?.isLetter() == true && addedChar.isUpperCase()) {
                                    // Все остальные буквы - строчные
                                    noteText = newValue.dropLast(1) + addedChar.lowercaseChar()
                                } else {
                                    noteText = newValue
                                }
                            } else {
                                noteText = newValue
                            }
                        },
                    label = { Text(text = stringResource(R.string.write_down_your_idea)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.tertiary,   // фон когда поле в фокусе
                        unfocusedContainerColor = MaterialTheme.colorScheme.tertiary, // фон когда поле не в фокусе
                        focusedBorderColor = MaterialTheme.colorScheme.primary,   // цвет рамки в фокусе
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary, // цвет рамки без фокуса
                        cursorColor = MaterialTheme.colorScheme.primary,          // цвет курсора
                        focusedLabelColor = MaterialTheme.colorScheme.primary,    // цвет label в фокусе
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSecondary // цвет label без фокуса
                    ),
                    maxLines = 5,
                    minLines = 3
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.cancel).uppercase(Locale.getDefault()))
                    }
                    TextButton(
                        onClick = {
                            if (noteText.isNotBlank()) {
                                onSave(noteText)
                            }
                        }
                    ) {
                        Text(text = stringResource(R.string.save).uppercase(Locale.getDefault()))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
fun SearchResultsScreen(
    searchQuery: String,
    results: List<IdeaNote>,
    viewModel: LifeBalanceViewModel,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.search_results, searchQuery),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.back))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (results.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.nothing_found))
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(results, key = { it.id }) { note ->
                    NoteItem(
                        note = note,
                        onUpdate = { updatedText ->
                            viewModel.updateNote(note.copy(text = updatedText))
                        },
                        onDelete = { viewModel.deleteNote(note) }
                    )
                }
            }
        }
    }
}
//ЗАМЕТКИ В ПАПКЕ
@Composable
fun FolderNotebookScreen(
    folder: IdeaFolder,
    viewModel: LifeBalanceViewModel,
    onBack: () -> Unit
) {
    val notesFlow = remember(folder.id) { viewModel.getNotesByFolderId(folder.id) }
    val notes by notesFlow.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val appLocale = getAppLocale(context)
    val initialDateText = remember {
        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yy", appLocale)
        "${LocalDate.now().format(dateFormatter)} "
    }
    var newNoteText by remember { mutableStateOf("") }
    var isAddingNote by remember { mutableStateOf(false) }

    // Для AI диалога
    var showAIDialog by remember { mutableStateOf(false) }
    var aiResponse by remember { mutableStateOf<String?>(null) }
    var noteBeingAnalyzed by remember { mutableStateOf<IdeaNote?>(null) }
    var showPremiumDialog by remember { mutableStateOf<PremiumFeature?>(null) }
    var showAiConsentDialog by remember { mutableStateOf(false) }
    var pendingNotesTextForAi by remember { mutableStateOf<String?>(null) }

    val isPremium by viewModel.isPremium.collectAsState()
    val hasAiConsent by viewModel.hasAiDataProcessingConsent.collectAsState()

    // CoroutineScope для AI
    val coroutineScope = rememberCoroutineScope()

    // Launcher для распознавания речи
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            spokenText?.firstOrNull()?.let { text ->
                // Только первая буква заглавная, остальные строчные
                val normalizedText = if (text.isNotBlank()) {
                    text.lowercase().replaceFirstChar { it.uppercaseChar() }
                } else {
                    text
                }
                newNoteText = if (newNoteText.isBlank() || newNoteText.trim() == initialDateText.trim()) {
                    "$initialDateText$normalizedText"
                } else {
                    "$newNoteText $normalizedText"
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Заголовок
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_folder),
                    contentDescription = "Папка",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопка добавления новой заметки
        if (!isAddingNote) {
            FloatingActionButton(
                onClick = { 
                    isAddingNote = true
                    newNoteText = initialDateText
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.extraLarge,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp
                )
            ) {
                Text(stringResource(R.string.add_note).uppercase(Locale.getDefault()),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Поле ввода новой заметки
        if (isAddingNote) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    width = 1.dp, // толщина границы
                    color = MaterialTheme.colorScheme.outline // цвет границы
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newNoteText,
                        onValueChange = { newValue ->
                            // Только первая буква после даты и пробела должна быть заглавной
                            if (newValue.length > newNoteText.length) {
                                val dateLength = initialDateText.trim().length
                                val addedChar = newValue.lastOrNull()
                                // Проверяем, что это первый символ после даты и пробела
                                if (newValue.length == dateLength + 2 && addedChar?.isLetter() == true) {
                                    // Первая буква - заглавная
                                    newNoteText = newValue.dropLast(1) + addedChar.uppercaseChar()
                                } else if (newValue.length > dateLength + 2 && addedChar?.isLetter() == true && addedChar.isUpperCase()) {
                                    // Все остальные буквы - строчные
                                    newNoteText = newValue.dropLast(1) + addedChar.lowercaseChar()
                                } else {
                                    newNoteText = newValue
                                }
                            } else {
                                newNoteText = newValue
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.tertiary,   // фон когда поле в фокусе
                            unfocusedContainerColor = MaterialTheme.colorScheme.tertiary, // фон когда поле не в фокусе
                            focusedBorderColor = MaterialTheme.colorScheme.primary,   // цвет рамки в фокусе
                            unfocusedBorderColor = MaterialTheme.colorScheme.primary, // цвет рамки без фокуса
                            cursorColor = MaterialTheme.colorScheme.primary,          // цвет курсора
                            focusedLabelColor = MaterialTheme.colorScheme.primary,    // цвет label в фокусе
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSecondary // цвет label без фокуса
                        ),
                        placeholder = { Text(stringResource(R.string.write_down_your_idea)) },
                        maxLines = 10,
                        minLines = 3
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Кнопка записи текста голосом
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
                                    speechLauncher.launch(intent)
                                }
                            }
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
                                    contentDescription = stringResource(R.string.voice_record_description),
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Row {
                            TextButton(onClick = {
                                isAddingNote = false
                                newNoteText = initialDateText
                            }) {
                                Text(stringResource(R.string.cancel_uppercase))
                            }
                            TextButton(onClick = {
                                if (newNoteText.isNotBlank()) {
                                    viewModel.addNote(newNoteText, folder.id)
                                    newNoteText = initialDateText
                                    isAddingNote = false
                                }
                            }) {
                                Text(stringResource(R.string.save_uppercase))
                            }
                        }
                    }
                }
            }
        }

        // Список заметок в папке (как в блокноте - строки одна за другой)
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_notes_in_folder),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notes, key = { it.id }) { note ->

                    NotebookNoteItem(
                        note = note,
                        isPremium = isPremium,
                        onAiAnalyze = { text ->
                            if (!isPremium) {
                                showPremiumDialog = PremiumFeature.AI_NOTES
                            } else if (!hasAiConsent) {
                                noteBeingAnalyzed = note
                                pendingNotesTextForAi = text
                                showAiConsentDialog = true
                            } else {
                                noteBeingAnalyzed = note
                                showAIDialog = true
                                aiResponse = "Загрузка..."
                                coroutineScope.launch {
                                    aiResponse = viewModel.analyzeNotesWithAi(text)
                                }
                            }
                        },
                        onUpdate = { updatedText -> viewModel.updateNote(note.copy(text = updatedText)) },
                        onDelete = { viewModel.deleteNote(note) }
                    )
                }
            }
        }
        // Диалог AI
        if (showAIDialog) {
            Dialog(onDismissRequest = { showAIDialog = false }) {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).heightIn(min = 200.dp, max = 400.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.ai_assistant),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        when {
                            aiResponse == null || aiResponse == "Загрузка..." -> {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(text = stringResource(R.string.getting_ai_response))
                            }
                            aiResponse == "LIMIT_EXCEEDED" -> {
                                Text(
                                    text = stringResource(R.string.ai_notes_request_limit_exceeded),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                            else -> {
                                val text = aiResponse ?: return@Column
                                LazyColumn(
                                    modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp)
                                ) {
                                    items(text.split("\n")) { line ->
                                        Text(
                                            text = line,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (aiResponse != "LIMIT_EXCEEDED") {
                                Button(onClick = {
                                    val note = noteBeingAnalyzed
                                    val response = aiResponse
                                    if (note != null && response != null && response != "Загрузка...") {
                                        val newText = note.text.trimEnd() + "\n\nAI:\n" + response
                                        viewModel.updateNote(note.copy(text = newText))
                                        Toast.makeText(context, context.getString(R.string.saved), Toast.LENGTH_SHORT).show()
                                        showAIDialog = false
                                        noteBeingAnalyzed = null
                                        aiResponse = null
                                    }
                                }) {
                                    Text(text = stringResource(R.string.save).uppercase(Locale.getDefault()))
                                }
                            }
                            Button(onClick = {
                                showAIDialog = false
                                noteBeingAnalyzed = null
                                aiResponse = null
                            }) {
                                Text(text = stringResource(R.string.close).uppercase(Locale.getDefault()))
                            }
                        }
                    }
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
                    val textToSend = pendingNotesTextForAi
                    pendingNotesTextForAi = null
                    if (textToSend != null) {
                        showAIDialog = true
                        aiResponse = "Загрузка..."
                        coroutineScope.launch {
                            aiResponse = viewModel.analyzeNotesWithAi(textToSend)
                        }
                    }
                },
                onDecline = {
                    showAiConsentDialog = false
                    pendingNotesTextForAi = null
                    noteBeingAnalyzed = null
                }
            )
        }
    }
}
@Composable
fun NoteText(text: String) {
    val annotatedText = buildAnnotatedString {
        val aiIndex = text.indexOf("AI:")

        if (aiIndex == -1) {
            append(text)
        } else {
            // Обычный текст
            append(text.substring(0, aiIndex))

            // AI-блок
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontStyle = FontStyle.Italic
                )
            ) {
                append("“")
                append(text.substring(aiIndex))
                append("”")
            }
        }
    }

    Text(
        text = annotatedText,
        style = MaterialTheme.typography.bodyMedium
    )
}

/**
 * Приблизительный подсчет токенов в тексте.
 * Использует формулу: примерно 1 токен = 2.5 символа для кириллицы/латиницы.
 */
fun approximateTokens(text: String): Int {
    if (text.isBlank()) return 0
    // Приблизительная оценка: 1 токен ≈ 2.5 символа
    // Это консервативная оценка для смешанного текста (русский/английский)
    return (text.length / 2.5).toInt()
}

@Composable
fun NotebookNoteItem(
    note: IdeaNote,
    isPremium: Boolean,
    onAiAnalyze: (String) -> Unit,
    onUpdate: (String) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var isEditing by remember(note.id) { mutableStateOf(false) }
    var editedText by remember(note.id, note.text) { mutableStateOf(note.text) }
    
    // Общий контроллер для синхронного уменьшения размера текста во всех кнопках
    val initialFontSize = MaterialTheme.typography.bodySmall.fontSize
    val resizeController = remember(initialFontSize) {
        TextResizeController(initialFontSize)
    }
    
    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isEditing) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 10,
                    minLines = 3,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            if (editedText.isNotBlank()) {
                                val maxTokens = 700
                                if (approximateTokens(editedText) > maxTokens) {
                                    showToast(context.getString(R.string.text_too_long_for_analysis))
                                } else {
                                    onAiAnalyze(editedText)
                                }
                            }
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!isPremium) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.size(2.dp))
                            }
                            GroupAutoResizedText(
                                text = "AI",
                                controller = resizeController,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error // красный цвет для удаления
                        )
                    ) {
                        GroupAutoResizedText(
                            text = stringResource(R.string.delete_uppercase),
                            controller = resizeController,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    TextButton(onClick = {
                        isEditing = false
                        editedText = note.text
                    }) {
                        GroupAutoResizedText(
                            text = stringResource(R.string.cancel_uppercase),
                            controller = resizeController,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(onClick = {
                        if (editedText.isNotBlank()) {
                            onUpdate(editedText)
                            isEditing = false
                        }
                    }) {
                        GroupAutoResizedText(
                            text = stringResource(R.string.save_uppercase),
                            controller = resizeController,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isEditing = true }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top
            ) {
                NoteText(
                    text = note.text
                )

            }
        }
        Divider(
            color = MaterialTheme.colorScheme.outline
        )
    }
}
