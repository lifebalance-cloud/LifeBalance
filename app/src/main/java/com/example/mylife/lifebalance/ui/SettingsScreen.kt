package com.example.mylife.lifebalance.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.example.lifebalance.BuildConfig
import com.example.lifebalance.R
import com.example.mylife.lifebalance.ui.theme.*
import com.example.mylife.lifebalance.data.AppSettingsDataStore
import com.example.mylife.lifebalance.data.User
import com.example.mylife.lifebalance.repository.AuthRepository
import com.example.mylife.lifebalance.repository.SyncService
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import java.util.Locale
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import com.example.mylife.lifebalance.utils.PdfExportHelper
import android.content.Intent
import android.net.Uri
import java.io.File
import androidx.compose.foundation.Image

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigate: (Screen) -> Unit,
    navController: NavController,
    authRepository: AuthRepository,
    syncService: SyncService,
    viewModel: com.example.mylife.lifebalance.viewmodel.LifeBalanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settingsDataStore = remember { AppSettingsDataStore(context) }
    
    var selectedTheme by remember { mutableStateOf("purple") }
    // Инициализируем язык с двухступенчатой логикой: сначала сохраненный, потом системный
    var selectedLanguage by remember { mutableStateOf(settingsDataStore.getLanguageSync()) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showPdfDialog by remember { mutableStateOf(false) }
    var showPdfResultDialog by remember { mutableStateOf(false) }
    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    var pdfFile by remember { mutableStateOf<File?>(null) }
    var isPdfGenerating by remember { mutableStateOf(false) }
    var pdfErrorMessage by remember { mutableStateOf<String?>(null) }
    var user by remember { mutableStateOf<User?>(null) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var showPremiumDialog by remember { mutableStateOf<PremiumFeature?>(null) }

    val isPremium by viewModel.isPremium.collectAsState()

    // Используем scope, не привязанный к композиции, чтобы избежать LeftCompositionCancellationException
    val syncScope = remember { CoroutineScope(Dispatchers.IO + SupervisorJob()) }
    val uiScope = rememberCoroutineScope()
    
    // Загружаем данные пользователя
    LaunchedEffect(Unit) {
        authRepository.getLocalUserFlow().collect { localUser ->
            user = localUser
        }
    }
    
    // Загружаем сохранённые настройки
    LaunchedEffect(Unit) {
        settingsDataStore.selectedTheme.collect { theme ->
            selectedTheme = theme
        }
    }
    
    LaunchedEffect(Unit) {
        settingsDataStore.selectedLanguage.collect { lang ->
            selectedLanguage = lang
        }
    }
    
    Scaffold(
        topBar = {
            TopBarWithIcons(
                onNavigate = { screen ->
                    navController.navigate(screen.route)
                }
            )
        },
        content = { paddingValues ->
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.settings).uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }

                item {
                    // Профиль пользователя
                    Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showProfileDialog = true },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user?.displayName ?: user?.email ?: stringResource(R.string.profile),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = user?.email ?: stringResource(R.string.not_authorized),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            user?.let { currentUser ->
                                Text(
                                    text = stringResource(R.string.last_sync, formatSyncTime(context, currentUser.lastSyncTimestamp)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                        Text(
                            text = ">",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                }

                item {
                // Кнопка синхронизации
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (!isSyncing) {
                            isSyncing = true
                            syncMessage = null
                            // Используем syncScope, который не зависит от композиции
                            syncScope.launch {
                                try {
                                    if (BuildConfig.DEBUG) android.util.Log.d("SettingsScreen", "Manual sync started")
                                    val result = syncService.syncAllData()
                                    result.fold(
                                        onSuccess = {
                                            // Обновляем UI в главном потоке
                                            uiScope.launch {
                                                syncMessage = context.resources.getString(R.string.sync_completed)
                                                user = authRepository.getLocalUser()
                                            }
                                            if (BuildConfig.DEBUG) android.util.Log.d("SettingsScreen", "Manual sync completed successfully")
                                        },
                                        onFailure = { e ->
                                            // Обновляем UI в главном потоке
                                            uiScope.launch {
                                                syncMessage = context.resources.getString(R.string.sync_error, context.resources.getString(R.string.error_try_again))
                                            }
                                            if (BuildConfig.DEBUG) android.util.Log.e("SettingsScreen", "Manual sync failed", e)
                                        }
                                    )
                                } catch (e: Exception) {
                                    // Обновляем UI в главном потоке
                                    uiScope.launch {
                                        syncMessage = context.resources.getString(R.string.error, context.resources.getString(R.string.error_try_again))
                                    }
                                    if (BuildConfig.DEBUG) android.util.Log.e("SettingsScreen", "Manual sync error", e)
                                } finally {
                                    uiScope.launch {
                                        isSyncing = false
                                    }
                                }
                            }
                        }
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.sync),
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (syncMessage != null) {
                                Text(
                                    text = syncMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (syncMessage?.let { msg ->
                                        val errorStr = context.resources.getString(R.string.error).lowercase()
                                        val syncErrorStr = context.resources.getString(R.string.sync_error).lowercase()
                                        msg.lowercase().contains(errorStr.substringBefore(":")) || 
                                        msg.lowercase().contains(syncErrorStr.substringBefore(":"))
                                    } == true) 
                                        MaterialTheme.colorScheme.error 
                                    else 
                                        MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = ">",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                }

                item {
                // Кнопка выбора цветовой схемы
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showThemeDialog = true },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.color_scheme),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = getThemeName(selectedTheme),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Text(
                            text = ">",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                }

                item {
                // Кнопка выбора языка
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showLanguageDialog = true },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.language),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = getLanguageName(selectedLanguage),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Text(
                            text = ">",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                }

                item {
                // Кнопка "Сформировать PDF" (премиум)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (!isPdfGenerating) {
                            if (!isPremium) {
                                showPremiumDialog = PremiumFeature.PDF_REPORT
                            } else {
                                pdfErrorMessage = null
                                showPdfDialog = true
                            }
                        }
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.generate_pdf),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.pdf_export_period),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        if (isPdfGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!isPremium) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.size(8.dp))
                                }
                                Text(
                                    text = ">",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
                }

                item {
                // Кнопка "Обратная связь"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showFeedbackDialog = true },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.feedback),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = ">",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                }
            }
        }
    )
    
    // Диалог выбора темы
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = selectedTheme,
            onThemeSelected = { theme ->
                selectedTheme = theme
                uiScope.launch {
                    settingsDataStore.saveTheme(theme)
                }
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }
    
    // Диалог выбора языка
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = selectedLanguage,
            onLanguageSelected = { language ->
                selectedLanguage = language
                uiScope.launch {
                    settingsDataStore.saveLanguage(language)
                    // Перезапускаем Activity для применения языка
                    // После перезапуска initializeDefaultSpheres() автоматически обновит названия сфер
                    (context as? Activity)?.recreate()
                }
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    // Диалог обратной связи
    if (showFeedbackDialog) {
        FeedbackDialog(
            onDismiss = { showFeedbackDialog = false }
        )
    }

    showPremiumDialog?.let { feature ->
        PremiumRequiredDialog(
            feature = feature,
            onDismiss = { showPremiumDialog = null }
        )
    }

    // Диалог экспорта PDF
    if (showPdfDialog) {
        PdfExportDialog(
            isGenerating = isPdfGenerating,
            errorMessage = pdfErrorMessage,
            onExport = { startDate, endDate, includeBalanceWheel, includeTasks, includeGoals, includeIdeas ->
                uiScope.launch {
                    isPdfGenerating = true
                    pdfErrorMessage = null
                    val result = withContext(Dispatchers.IO) {
                        val data = viewModel.collectDataForPdf(
                            startDate, endDate,
                            includeBalanceWheel, includeTasks, includeGoals, includeIdeas
                        )
                        PdfExportHelper.generatePdf(context, data)
                    }
                    isPdfGenerating = false
                    result.fold(
                        onSuccess = { result ->
                            pdfUri = result.uri
                            pdfFile = result.file
                            showPdfDialog = false
                            showPdfResultDialog = true
                        },
                        onFailure = { e ->
                            pdfErrorMessage = e.message ?: context.getString(R.string.error, "")
                        }
                    )
                }
            },
            onDismiss = {
                if (!isPdfGenerating) {
                    showPdfDialog = false
                    pdfErrorMessage = null
                }
            }
        )
    }

    // Диалог результата PDF (поделиться / открыть). При закрытии удаляем файл из кэша.
    if (showPdfResultDialog && pdfUri != null) {
        fun deletePdfAndClose() {
            pdfFile?.let { file ->
                try {
                    if (file.exists()) file.delete()
                } catch (_: Exception) {}
            }
            pdfFile = null
            pdfUri = null
            showPdfResultDialog = false
        }
        fun closeDialogWithoutDeletingFile() {
            pdfFile = null
            pdfUri = null
            showPdfResultDialog = false
        }
        PdfResultDialog(
            onShare = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, pdfUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    context.startActivity(Intent.createChooser(shareIntent, null))
                } catch (_: Exception) {}
                closeDialogWithoutDeletingFile()
            },
            onOpen = {
                val openIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(pdfUri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    context.startActivity(openIntent)
                } catch (_: Exception) {}
                closeDialogWithoutDeletingFile()
            },
            onDismiss = { deletePdfAndClose() }
        )
    }

    // Диалог профиля
    if (showProfileDialog) {
        ProfileDialog(
            user = user,
            onSignOut = {
                uiScope.launch {
                    authRepository.signOut()
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                showProfileDialog = false
            },
            onDismiss = { showProfileDialog = false }
        )
    }
}
//ДИАЛОГ ОБРАТНАЯ СВЯЗЬ
@Composable
fun FeedbackDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val email = BuildConfig.CONTACT_EMAIL
    val telegram = BuildConfig.CONTACT_TELEGRAM
    val whatsappNumber = BuildConfig.CONTACT_WHATSAPP // номер в формате: +380123456789

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.feedback_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.feedback_dialog_title2),
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // EMAIL
                ContactRow(
                    icon = Icons.Default.Email,
                    text = stringResource(R.string.feedback_contact_email_label)
                ) {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:$email")
                    }
                    try {
                        context.startActivity(Intent.createChooser(intent, null))
                    } catch (_: Exception) {}
                }


                // TELEGRAM
                ContactRow(
                    icon = Icons.Default.Send,
                    text = stringResource(R.string.feedback_contact_telegram_label)
                ) {
                    val username = telegram.removePrefix("@").trim()
                    val uri = Uri.parse(
                        if (username.startsWith("http")) username
                        else "https://t.me/$username"
                    )
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }

                // WHATSAPP
                ContactRowWithImage(
                    imageRes = R.drawable.ic_whatsapp,
                    text = stringResource(R.string.feedback_contact_whatsapp_label)
                ) {
                    val uri = Uri.parse("https://wa.me/${whatsappNumber.replace("+", "")}")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close).uppercase())
            }
        }
    )
}
@Composable
fun ContactRowWithImage(
    imageRes: Int,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun ContactRow(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}


@Composable
fun ProfileDialog(
    user: User?,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Заголовок
                Text(
                    text = stringResource(R.string.profile),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                // Контент
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    user?.let { currentUser ->
                        Text(
                            text = currentUser.displayName ?: stringResource(R.string.user),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = currentUser.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        if (currentUser.lastSyncTimestamp > 0) {
                            Text(
                                text = stringResource(R.string.last_sync, formatSyncTime(LocalContext.current, currentUser.lastSyncTimestamp)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } ?: run {
                        Text(
                            text = stringResource(R.string.user_not_authorized),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                // Кнопки в разных углах
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Кнопка "Закрыть" слева
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.close).uppercase(Locale.getDefault()))
                    }
                    
                    // Кнопка "Выйти" справа
                    user?.let {
                        Button(
                            onClick = onSignOut,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(stringResource(R.string.sign_out).uppercase(Locale.getDefault()))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun formatSyncTime(context: android.content.Context, timestamp: Long): String {
    if (timestamp == 0L) return context.resources.getString(R.string.never)
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    
    return when {
        days > 0 -> context.resources.getString(R.string.days_ago, days)
        hours > 0 -> context.resources.getString(R.string.hours_ago, hours)
        minutes > 0 -> context.resources.getString(R.string.minutes_ago, minutes)
        else -> context.resources.getString(R.string.just_now)
    }
}

@Composable
fun getThemePrimaryContainerColor(themeKey: String): Color {
    return when (themeKey) {
        "purple" -> PurpleSecondary
        "blue" -> BlueSecondary
        "green" -> Mint
        "orange" -> PeachSecondary
        "dark" -> DarkSecondary
        "system" -> PurpleSecondary // Используем фиолетовый как дефолтный для системной темы
        else -> PurpleSecondary
    }
}

@Composable
fun ThemeSelectionDialog(
    currentTheme: String,
    onThemeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val themes = listOf(
        "purple" to R.string.theme_purple,
        "blue" to R.string.theme_blue,
        "green" to R.string.theme_green,
        "orange" to R.string.theme_orange,
        "dark" to R.string.theme_dark,
        "system" to R.string.theme_system
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.color_scheme),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        },
        text = {
            Column {
                themes.forEach { (themeKey, themeNameRes) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = currentTheme == themeKey,
                                onValueChange = { if (it) onThemeSelected(themeKey) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTheme == themeKey,
                            onClick = { onThemeSelected(themeKey) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Цветной квадратик
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (themeKey == "system") {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        getThemePrimaryContainerColor(themeKey)
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(themeNameRes))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.save).uppercase(Locale.getDefault()))
            }
        }
    )
}

@Composable
fun LanguageSelectionDialog(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val languages = listOf(
        "ru" to R.string.language_russian,
        "uk" to R.string.language_ukrainian,
        "en" to R.string.language_english,
        "de" to R.string.language_german,
        "fr" to R.string.language_french,
        "es" to R.string.language_spanish
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer) },
        text = {
            Column {
                languages.forEach { (langKey, langNameRes) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = currentLanguage == langKey,
                                onValueChange = { if (it) onLanguageSelected(langKey) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentLanguage == langKey,
                            onClick = { onLanguageSelected(langKey) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(langNameRes))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.save).uppercase(Locale.getDefault()))
            }
        }
    )
}

@Composable
fun getThemeName(theme: String): String {
    return when (theme) {
        "purple" -> stringResource(R.string.theme_purple)
        "blue" -> stringResource(R.string.theme_blue)
        "green" -> stringResource(R.string.theme_green)
        "orange" -> stringResource(R.string.theme_orange)
        "dark" -> stringResource(R.string.theme_dark)
        "system" -> stringResource(R.string.theme_system)
        else -> stringResource(R.string.theme_purple)
    }
}

@Composable
fun getLanguageName(language: String): String {
    return when (language) {
        "ru" -> stringResource(R.string.language_russian)
        "uk" -> stringResource(R.string.language_ukrainian)
        "en" -> stringResource(R.string.language_english)
        "de" -> stringResource(R.string.language_german)
        "fr" -> stringResource(R.string.language_french)
        "es" -> stringResource(R.string.language_spanish)
        else -> stringResource(R.string.language_russian)
    }
}

@Composable
fun PdfExportDialog(
    isGenerating: Boolean,
    errorMessage: String?,
    onExport: (
        startDate: LocalDate?,
        endDate: LocalDate?,
        includeBalanceWheel: Boolean,
        includeTasks: Boolean,
        includeGoals: Boolean,
        includeIdeas: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var periodType by remember { mutableStateOf("all") }
    val today = LocalDate.now()
    var startDate by remember { mutableStateOf(today.minusDays(30)) }
    var endDate by remember { mutableStateOf(today) }
    var includeBalanceWheel by remember { mutableStateOf(true) }
    var includeTasks by remember { mutableStateOf(true) }
    var includeGoals by remember { mutableStateOf(true) }
    var includeIdeas by remember { mutableStateOf(true) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault())

    // Диалог выбора даты начала
    if (showStartDatePicker) {
        var selectedDate by remember { mutableStateOf(startDate) }
        LaunchedEffect(showStartDatePicker) { selectedDate = startDate }
        Dialog(onDismissRequest = { showStartDatePicker = false }) {
            Surface(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        stringResource(R.string.pdf_start_date),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    DatePickerCalendar(
                        initialDate = selectedDate,
                        onDateSelected = { selectedDate = it },
                        showTimeButton = false
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { showStartDatePicker = false }) {
                            Text(stringResource(R.string.cancel).uppercase(Locale.getDefault()))
                        }
                        TextButton(onClick = {
                            startDate = selectedDate
                            if (startDate.isAfter(endDate)) endDate = startDate
                            showStartDatePicker = false
                        }) {
                            Text(stringResource(R.string.save).uppercase(Locale.getDefault()))
                        }
                    }
                }
            }
        }
    }

    // Диалог выбора даты окончания
    if (showEndDatePicker) {
        var selectedDate by remember { mutableStateOf(endDate) }
        LaunchedEffect(showEndDatePicker) { selectedDate = endDate }
        Dialog(onDismissRequest = { showEndDatePicker = false }) {
            Surface(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        stringResource(R.string.pdf_end_date),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    DatePickerCalendar(
                        initialDate = selectedDate,
                        onDateSelected = { selectedDate = it },
                        showTimeButton = false
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { showEndDatePicker = false }) {
                            Text(stringResource(R.string.cancel).uppercase(Locale.getDefault()))
                        }
                        TextButton(onClick = {
                            endDate = selectedDate
                            if (endDate.isBefore(startDate)) startDate = endDate
                            showEndDatePicker = false
                        }) {
                            Text(stringResource(R.string.save).uppercase(Locale.getDefault()))
                        }
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.pdf_export_dialog_title),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.pdf_export_period),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = periodType == "all", onValueChange = { if (it) periodType = "all" }, role = Role.RadioButton)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = periodType == "all", onClick = { periodType = "all" })
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.pdf_period_all))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = periodType == "custom", onValueChange = { if (it) periodType = "custom" }, role = Role.RadioButton)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = periodType == "custom", onClick = { periodType = "custom" })
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.pdf_period_custom))
                }

                if (periodType == "custom") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStartDatePicker = true }
                            .padding(start = 48.dp, top = 8.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${stringResource(R.string.pdf_start_date)}: ${startDate.format(dateFormatter)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEndDatePicker = true }
                            .padding(start = 48.dp, top = 4.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${stringResource(R.string.pdf_end_date)}: ${endDate.format(dateFormatter)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.pdf_sections_include),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().toggleable(value = includeBalanceWheel, onValueChange = { includeBalanceWheel = it }, role = Role.Checkbox).padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = includeBalanceWheel, onCheckedChange = { includeBalanceWheel = it })
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.wheel_of_life_balance))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().toggleable(value = includeGoals, onValueChange = { includeGoals = it }, role = Role.Checkbox).padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = includeGoals, onCheckedChange = { includeGoals = it })
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.main_goals))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().toggleable(value = includeTasks, onValueChange = { includeTasks = it }, role = Role.Checkbox).padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = includeTasks, onCheckedChange = { includeTasks = it })
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.tasks))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().toggleable(value = includeIdeas, onValueChange = { includeIdeas = it }, role = Role.Checkbox).padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = includeIdeas, onCheckedChange = { includeIdeas = it })
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.ideas))
                }

                if (errorMessage != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (isGenerating) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.pdf_generating), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (!isGenerating) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.cancel).uppercase(Locale.getDefault()))
                        }
                        TextButton(
                            onClick = {
                                val start = if (periodType == "custom") startDate else null
                                val end = if (periodType == "custom") endDate else null
                                onExport(start, end, includeBalanceWheel, includeTasks, includeGoals, includeIdeas)
                            }
                        ) {
                            Text(stringResource(R.string.generate_pdf).uppercase(Locale.getDefault()))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
fun PdfResultDialog(
    onShare: () -> Unit,
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.pdf_success),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.pdf_share),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                ) {
                    Button(onClick = onShare) {
                        Text(stringResource(R.string.pdf_share))
                    }
                    Button(onClick = onOpen) {
                        Text(stringResource(R.string.pdf_open))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close).uppercase(Locale.getDefault()))
            }
        }
    )
}
