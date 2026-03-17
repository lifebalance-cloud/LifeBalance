package com.example.mylife.lifebalance.viewmodel

import com.example.lifebalance.BuildConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mylife.lifebalance.data.*
import com.example.mylife.lifebalance.repository.LifeBalanceRepository
import com.example.mylife.lifebalance.data.AppSettingsDataStore
import com.example.mylife.lifebalance.premium.PremiumManager
import com.example.mylife.lifebalance.utils.ImageStorageHelper
import android.content.Context
import android.util.Log
import com.example.mylife.lifebalance.RetrofitProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.DayOfWeek
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject



class LifeBalanceViewModel(
    private val repository: LifeBalanceRepository,
    private val syncService: com.example.mylife.lifebalance.repository.SyncService? = null,
    private val settingsDataStore: AppSettingsDataStore? = null
) : ViewModel() {
    val spheres: StateFlow<List<LifeSphere>> = repository.getAllSpheres()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    val allGoals: StateFlow<List<Goal>> = repository.getAllGoals()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    private val _selectedSphere = MutableStateFlow<LifeSphere?>(null)
    val selectedSphere: StateFlow<LifeSphere?> = _selectedSphere.asStateFlow()

    // ===== AI Balance =====
    private val _aiResult = MutableStateFlow<String?>(null)
    val aiResult: StateFlow<String?> = _aiResult.asStateFlow()
    // ===== AI Notes =====
    private val _notesAiResult = MutableStateFlow<String?>(null)
    val notesAiResult: StateFlow<String?> = _notesAiResult.asStateFlow()

    val isPremium: StateFlow<Boolean> = PremiumManager.isPremium

    /** Согласие пользователя на обработку данных для AI (EEA/UK, GDPR). */
    val hasAiDataProcessingConsent: StateFlow<Boolean> = (settingsDataStore?.aiDataProcessingConsentGranted ?: flowOf(false))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun setAiDataProcessingConsent(granted: Boolean) {
        viewModelScope.launch {
            settingsDataStore?.setAiDataProcessingConsent(granted)
        }
    }

    // Используем flatMapLatest для автоматического обновления при изменении выбранной сферы
    val goals: StateFlow<List<Goal>> = _selectedSphere
        .flatMapLatest { sphere ->
            if (sphere != null) {
                repository.getGoalsForSphere(sphere.id.toInt())
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val tasks: StateFlow<List<Task>> = _selectedSphere
        .flatMapLatest { sphere ->
            if (sphere != null) {
                repository.getTasksBySphereId(sphere.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    init {
        viewModelScope.launch {
            repository.initializeDefaultSpheres()
            checkAndRescheduleTasks()
        }
    }

    // Автоматический перенос невыполненных задач с автопереносом на следующий день
    fun checkAndRescheduleTasks() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val allTasksList = repository.getAllTasks().first()

            allTasksList.forEach { task ->
                if (task.autoReschedule &&
                    !task.isCompleted &&
                    task.date.isBefore(today)) {
                    // Переносим задачу на сегодняшний день
                    repository.updateTask(task.copy(date = today))
                }
            }
        }
    }
    fun selectSphere(sphere: LifeSphere) {
        _selectedSphere.value = sphere
    }
    fun clearSelection() {
        _selectedSphere.value = null
    }
    // Автоматическая синхронизация в фоне
    private suspend fun syncInBackground() {
        syncService?.let { service ->
            try {
                if (BuildConfig.DEBUG) android.util.Log.d("LifeBalanceViewModel", "Starting background sync...")
                // Синхронизируем только отправку данных на сервер (не блокируем UI)
                val result = service.syncUp()
                result.fold(
                    onSuccess = {
                        if (BuildConfig.DEBUG) android.util.Log.d("LifeBalanceViewModel", "Background sync completed successfully")
                    },
                    onFailure = { e ->
                        if (BuildConfig.DEBUG) android.util.Log.w("LifeBalanceViewModel", "Background sync failed", e)
                    }
                )
            } catch (e: Exception) {
                // Игнорируем ошибки синхронизации в фоне, чтобы не мешать работе приложения
                if (BuildConfig.DEBUG) android.util.Log.w("LifeBalanceViewModel", "Background sync error", e)
            }
        } ?: run {
            if (BuildConfig.DEBUG) android.util.Log.d("LifeBalanceViewModel", "SyncService not available, skipping background sync")
        }
    }

    fun updateSphereScore(sphere: LifeSphere, score: Int) {
        viewModelScope.launch {
            repository.updateSphere(sphere.copy(score = score.coerceIn(0, 10)))
            syncInBackground()
        }
    }
    fun addSphere(name: String, colorIndex: Int) {
        viewModelScope.launch {
            val maxSpheres = if (isPremium.value) 12 else 8
            if (repository.getSphereCount() < maxSpheres) {
                repository.insertSphere(
                    LifeSphere(name = name, score = 0, colorIndex = colorIndex)
                )
                syncInBackground()
            }
        }
    }
    fun updateSphere(sphere: LifeSphere) {
        viewModelScope.launch {
            repository.updateSphere(sphere)
            syncInBackground()
        }
    }

    // Обновление названий стандартных сфер при смене языка
    fun updateDefaultSphereNames() {
        viewModelScope.launch {
            repository.updateDefaultSphereNames()
        }
    }
    fun deleteSphere(sphere: LifeSphere) {
        viewModelScope.launch {
            repository.deleteSphere(sphere)
            if (_selectedSphere.value?.id == sphere.id) {
                _selectedSphere.value = null
            }
            syncInBackground()
        }
    }
    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
            syncInBackground()
        }
    }
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
            syncInBackground()
        }
    }

    fun analyzeBalanceWithAi(userContext: String = "Я устаю и не успеваю жить") {
        viewModelScope.launch(Dispatchers.IO) {
            // Проверяем лимит запросов
            if (settingsDataStore != null) {
                val canMakeRequest = settingsDataStore.canMakeAiRequest()
                if (!canMakeRequest) {
                    _aiResult.value = "LIMIT_EXCEEDED" // Специальный маркер для UI
                    return@launch
                }
            }
            
            _aiResult.value = "Загрузка..." // показываем индикатор

            val appLanguage = aiLanguageFromLocale(language = Locale.getDefault().language)

            val textForAi = buildString {
                append(
                    """
        Ты — поддерживающий и бережный коуч по балансу жизни.
        Твоя задача — помочь человеку улучшить качество жизни без давления и тревоги.

        Отвечай СТРОГО на $appLanguage языке.

        Верни ответ в следующем структурированном формате:

        [ПОДДЕРЖКА]
        Текст поддержки (1-2 предложения, мягкий и поддерживающий тон)

        [ПРОСЕВШИЕ_СФЕРЫ]
        Название сферы 1 (оценка/10)
        Название сферы 2 (оценка/10)
        ...

        [ЦЕЛИ]
        Сфера: Название сферы 1
        Цель 1: Текст цели 1
        Цель 2: Текст цели 2
        Цель 3: Текст цели 3

        Сфера: Название сферы 2
        Цель 1: Текст цели 1
        Цель 2: Текст цели 2
        Цель 3: Текст цели 3

        [ШАГИ]
        Текст маленького шага 1
        Текст маленького шага 2
        Текст маленького шага 3

        Правила:
        - Укажи ВСЕ сферы, оценка которых меньше 5
        - Для каждой такой сферы предложи по 3 простые и реалистичные цели
        - Предложи 3-5 маленьких шагов, которые можно сделать прямо сейчас
        - Используй спокойный, поддерживающий тон
        - Избегай резких формулировок и давления
        - Если данные неполные — не делай резких выводов

        """.trimIndent()
                )

                append("Сферы и оценки:\n")
                spheres.value.forEach {
                    append("${it.name}: ${it.score}/10\n")
                }

                append("\nКонтекст пользователя: $userContext")
            }


            try {
                val request = AiAnalyzeRequest(
                    text = textForAi,
                    language = appLanguage
                )
                val response = RetrofitProvider.api.analyzeBalance(request)
                _aiResult.value = response.result
                
                // Регистрируем успешный запрос
                settingsDataStore?.registerAiRequest()
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e("LifeBalanceViewModel", "analyzeBalanceWithAi failed", e)
                }
                val errorMessage = when {
                    e.message?.contains("timeout", ignoreCase = true) == true ->
                        "Превышено время ожидания ответа от AI. Попробуйте позже."
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "Ошибка сети. Проверьте подключение к интернету."
                    else -> "Ошибка получения ответа от AI. Попробуйте позже."
                }
                _aiResult.value = errorMessage
            }
        }
    }

    private fun aiLanguageFromLocale(language: String): String {
        return when (language) {
            "uk" -> "украинском"
            "ru" -> "русском"
            "en" -> "английском"
            "de" -> "немецком"
            "es" -> "испанском"
            "fr" -> "французком"
            else -> "английском"
        }
    }

    // запрос к AI для ЗАМЕТОК (лимит 20 запросов в сутки)
    suspend fun analyzeNotesWithAi(notesText: String): String = withContext(Dispatchers.IO) {
        if (settingsDataStore != null) {
            if (!settingsDataStore.canMakeAiNotesRequest()) {
                return@withContext "LIMIT_EXCEEDED"
            }
        }
        val appLanguage = aiLanguageFromLocale(Locale.getDefault().language)

        val textForAi = buildString {
            append(
                """Ты — AI-помощник для личных заметок.

Определи тип текста (внутренне, не выводи пользователю).

Отвечай ТОЛЬКО в одном формате, соответствующем типу:

• Личный/эмоциональный — 2–3 предложения. Отрази чувства и смысл текста. 
  НЕ давай советов. НЕ задавай вопросов. НЕ интерпретируй причины.

• Информационный — 2–4 предложения. Кратко перескажи факты.
  БЕЗ выводов, оценок и советов.

• Книги / Фильмы / Песни — 2–3 предложения. 
  ТОЛЬКО краткое содержание (кто/что/о чём). 
  НЕ оценивай. НЕ советуй. НЕ объясняй смысл.

• Список / данные — упорядочи как есть. 
  НЕ добавляй пояснений.

• Размышления / идеи — 2–3 предложения. Отрази основную мысль.
  Можно одно мягкое уточнение, БЕЗ советов.

ОБЩИЕ ПРАВИЛА (обязательные):
— Максимум 4 предложения.
— Без советов, рекомендаций, «можно», «стоит», «попробуй».
— Без диагнозов и давления.
— Без списков, шагов и инструкций.
— Отвечай строго на $appLanguage языке.
            """.trimIndent()
            )
            append("\nТекст заметок:\n")
            append(notesText)
        }

        return@withContext try {
            val response = RetrofitProvider.api.analyzeBalance(
                AiAnalyzeRequest(
                    text = textForAi,
                    language = appLanguage,
                    maxTokens = 133
                )
            )
            settingsDataStore?.registerAiNotesRequest()
            response.result
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e("LifeBalanceViewModel", "analyzeNotesWithAi failed", e)
            }
            when {
                e.message?.contains("timeout", ignoreCase = true) == true ->
                    "Превышено время ожидания. Попробуйте позже 🙏"
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Ошибка сети. Проверьте подключение 🙏"
                else -> "Не удалось проанализировать заметки 🙏"
            }
        }
    }





    // Больше не нужна, так как goals обновляется автоматически через flatMapLatest
    // Оставляем для обратной совместимости, но она теперь просто выбирает сферу
    fun loadGoalsForSphere(sphereId: Int) {
        // Просто выбираем сферу, goals обновится автоматически через flatMapLatest
        selectSphereById(sphereId)
    }
    fun addGoal(sphere: LifeSphere, text: String, deadline: LocalDate) {
        viewModelScope.launch {
            val currentGoals = repository.getGoalsForSphere(sphere.id.toInt()).first()
            val maxGoalsPerSphere = if (isPremium.value) Int.MAX_VALUE else 5
            if (currentGoals.size >= maxGoalsPerSphere) return@launch
            val newGoal = Goal(
                sphereId = sphere.id.toInt(),
                text = text,
                deadline = deadline,
                checked = false
            )
            repository.addGoal(newGoal)
            syncInBackground()
        }
    }

    fun updateGoal(goal: Goal) {
        viewModelScope.launch {
            repository.updateGoal(goal)
            // goals обновится автоматически через Room Flow и flatMapLatest
            syncInBackground()
        }
    }

    //Кнопка удаления целей
    fun deleteGoal(goal: Goal) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
            // goals обновится автоматически через Room Flow и flatMapLatest
            syncInBackground()
        }
    }
    fun selectSphereById(id: Int) {
        viewModelScope.launch {
            // Проверяем, не выбрана ли уже нужная сфера
            val currentSelected = _selectedSphere.value
            if (currentSelected != null && currentSelected.id.toInt() == id) {
                // Сфера уже выбрана правильно, ничего не делаем
                return@launch
            }

            // Сначала пробуем найти в уже загруженном списке
            val sphere = spheres.value.firstOrNull { it.id.toInt() == id }
            if (sphere != null) {
                selectSphere(sphere)
            } else {
                // Если не найдено, загружаем напрямую из репозитория
                val loadedSphere = repository.getSphereById(id.toLong())
                loadedSphere?.let { selectSphere(it) }
            }
        }
    }

    fun goToNextSphere(currentId: Int): Int? {
        val list = spheres.value
        val index = list.indexOfFirst { it.id == currentId.toLong() }

        return if (index != -1 && index + 1 < list.size) {
            list[index + 1].id.toInt()  // <- преобразуем Long в Int
        } else null
    }

    //ЧЕКБОКС
    fun isGoalChecked(goalId: Int): Flow<Boolean> = repository.isGoalChecked(goalId)

    fun saveGoalChecked(goalId: Int, checked: Boolean) {
        viewModelScope.launch {
            repository.saveGoalChecked(goalId, checked)
            syncService?.syncUp()
        }
    }

    // ===== Tasks by Date =====
    fun getTasksByDate(date: LocalDate): Flow<List<Task>> = repository.getTasksByDate(date)

    fun getTasksByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Task>> =
        repository.getTasksByDateRange(startDate, endDate)

    val allTasks: StateFlow<List<Task>> = repository.getAllTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addTask(
        title: String,
        description: String,
        date: LocalDate,
        time: org.threeten.bp.LocalTime? = null,
        hasNotification: Boolean = false,
        notificationSound: String = "default",
        sphereId: Long? = null,
        autoReschedule: Boolean = false,
        repeatType: RepeatType = RepeatType.NONE,
        repeatEndDate: LocalDate? = null
    ) {
        viewModelScope.launch {
            val baseTask = Task(
                title = title,
                description = description,
                date = date,
                time = time,
                hasNotification = hasNotification,
                notificationSound = notificationSound,
                sphereId = sphereId ?: 0L,
                isCompleted = false,
                autoReschedule = autoReschedule,
                repeatType = repeatType,
                repeatEndDate = repeatEndDate
            )

            // Определяем дату окончания (если не указана пользователем, используем разумные значения по умолчанию)
            val endDate = repeatEndDate ?: when (repeatType) {
                RepeatType.NONE -> null
                RepeatType.DAILY -> date.plusYears(1)
                RepeatType.WEEKDAYS -> date.plusYears(1)
                RepeatType.WEEKLY -> date.plusYears(1)
                RepeatType.MONTHLY -> date.plusYears(5)
                RepeatType.YEARLY -> date.plusYears(10)
            }

            when (repeatType) {
                RepeatType.NONE -> {
                    repository.insertTask(baseTask.copy(repeatEndDate = endDate))
                }
                RepeatType.DAILY -> {
                    // Создаем задачи на каждый день до даты окончания
                    var currentDate = date
                    val finalEndDate = endDate ?: date.plusYears(1)
                    while (!currentDate.isAfter(finalEndDate)) {
                        repository.insertTask(baseTask.copy(date = currentDate, id = 0, repeatEndDate = endDate))
                        currentDate = currentDate.plusDays(1)
                    }
                }
                RepeatType.WEEKDAYS -> {
                    // Создаем задачи только на рабочие дни (ПН-ПТ) до даты окончания
                    var currentDate = date
                    val finalEndDate = endDate ?: date.plusYears(1)
                    while (!currentDate.isAfter(finalEndDate)) {
                        val dayOfWeek = currentDate.dayOfWeek
                        if (dayOfWeek >= DayOfWeek.MONDAY && dayOfWeek <= DayOfWeek.FRIDAY) {
                            repository.insertTask(baseTask.copy(date = currentDate, id = 0, repeatEndDate = endDate))
                        }
                        currentDate = currentDate.plusDays(1)
                    }
                }
                RepeatType.WEEKLY -> {
                    // Создаем задачи каждую неделю до даты окончания
                    var currentDate = date
                    val finalEndDate = endDate ?: date.plusYears(1)
                    while (!currentDate.isAfter(finalEndDate)) {
                        repository.insertTask(baseTask.copy(date = currentDate, id = 0, repeatEndDate = endDate))
                        currentDate = currentDate.plusWeeks(1)
                    }
                }
                RepeatType.MONTHLY -> {
                    // Создаем задачи каждый месяц до даты окончания
                    var currentDate = date
                    val finalEndDate = endDate ?: date.plusYears(5)
                    while (!currentDate.isAfter(finalEndDate)) {
                        repository.insertTask(baseTask.copy(date = currentDate, id = 0, repeatEndDate = endDate))
                        currentDate = currentDate.plusMonths(1)
                    }
                }
                RepeatType.YEARLY -> {
                    // Создаем задачи каждый год до даты окончания
                    var currentDate = date
                    val finalEndDate = endDate ?: date.plusYears(10)
                    while (!currentDate.isAfter(finalEndDate)) {
                        repository.insertTask(baseTask.copy(date = currentDate, id = 0, repeatEndDate = endDate))
                        currentDate = currentDate.plusYears(1)
                    }
                }
            }

            // Если установлена дата окончания, удаляем все копии задач после этой даты
            endDate?.let { finalEndDate ->
                repository.deleteTasksAfterDate(title, sphereId ?: 0L, finalEndDate)
            }
            syncInBackground()
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task)
            // Если установлена дата окончания, удаляем все копии задач после этой даты
            task.repeatEndDate?.let { endDate ->
                repository.deleteTasksAfterDate(task.title, task.sphereId, endDate)
            }
            syncInBackground()
        }
    }

    fun copyTask(task: Task, newDate: LocalDate) {
        viewModelScope.launch {
            val newTask = task.copy(
                id = 0, // Сброс ID для создания новой записи
                date = newDate,
                isCompleted = false // Сброс статуса выполнения для копии
            )
            repository.insertTask(newTask)
            syncInBackground()
        }
    }

    fun moveTask(taskToMove: Task, newDate: LocalDate) {
        viewModelScope.launch {
            repository.updateTask(taskToMove.copy(date = newDate))
            syncInBackground()
        }
    }
// Копируем цель в ежедневные дела как независимую задачу (не привязанную к сфере)
    fun moveGoalToTasks(goal: Goal, movedFromGoalsDescription: String) {
        viewModelScope.launch {
            val today = LocalDate.now()
            val task = Task(
                title = goal.text,
                description = movedFromGoalsDescription,
                date = today,
                sphereId = 0L, // независимая задача — не удалится при удалении сферы
                isCompleted = false
            )
            repository.insertTask(task)
            syncInBackground()
        }
    }

//Конец кнопки удаления целей

    // ===== Idea Folders =====
    val folders: StateFlow<List<IdeaFolder>> = repository.getAllFolders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addFolder(name: String) {
        viewModelScope.launch {
            repository.insertFolder(IdeaFolder(name = name))
            syncInBackground()
        }
    }

    fun deleteFolder(folder: IdeaFolder) {
        viewModelScope.launch {
            repository.deleteFolder(folder)
            syncInBackground()
        }
    }

    fun updateFolder(folder: IdeaFolder) {
        viewModelScope.launch {
            repository.updateFolder(folder)
            syncInBackground()
        }
    }

    // ===== Idea Notes =====
    val notesWithoutFolder: StateFlow<List<IdeaNote>> = repository.getNotesWithoutFolder()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun getNotesByFolderId(folderId: Long): StateFlow<List<IdeaNote>> = repository.getNotesByFolderId(folderId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun searchNotes(query: String): StateFlow<List<IdeaNote>> = repository.searchNotes(query)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addNote(text: String, folderId: Long? = null) {
        viewModelScope.launch {
            try {
                if (text.isNotBlank()) {
                    val note = IdeaNote(
                        id = 0, // Явно указываем 0 для новой записи
                        text = text.trim(),
                        folderId = folderId,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.insertNote(note)
                    syncInBackground()
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e("LifeBalanceViewModel", "addNote failed", e)
                }
            }
        }
    }

    fun updateNote(note: IdeaNote) {
        viewModelScope.launch {
            repository.updateNote(note)
            syncInBackground()
        }
    }

    fun deleteNote(note: IdeaNote) {
        viewModelScope.launch {
            repository.deleteNote(note)
            syncInBackground()
        }
    }

    // ===== Dream Sector Photos =====
    fun getPhotosBySectorId(sectorId: Int): Flow<List<com.example.mylife.lifebalance.data.DreamSectorPhoto>> =
        repository.getPhotosBySectorId(sectorId)

    suspend fun getPhotosBySectorIdSync(sectorId: Int): List<com.example.mylife.lifebalance.data.DreamSectorPhoto> =
        repository.getPhotosBySectorIdSync(sectorId)

    suspend fun addPhotoToSectorSync(sectorId: Int, sourceUri: android.net.Uri): Result<com.example.mylife.lifebalance.data.DreamSectorPhoto> {
        return repository.addPhotoToSector(sectorId, sourceUri)
    }

    fun addPhotoToSector(sectorId: Int, sourceUri: android.net.Uri) {
        viewModelScope.launch {
            repository.addPhotoToSector(sectorId, sourceUri)
            syncInBackground()
        }
    }

    fun deletePhoto(photo: com.example.mylife.lifebalance.data.DreamSectorPhoto) {
        viewModelScope.launch {
            repository.deletePhoto(photo)
            syncInBackground()
        }
    }

    fun updatePhotosForSector(sectorId: Int, photoUris: List<String>) {
        viewModelScope.launch {
            repository.updatePhotosForSector(sectorId, photoUris)
            syncInBackground()
        }
    }

    // ===== Dream Affirmations =====
    fun getAffirmationsBySectorId(sectorId: Int): Flow<List<com.example.mylife.lifebalance.data.DreamAffirmation>> =
        repository.getAffirmationsBySectorId(sectorId)

    fun getAllAffirmations(): Flow<List<com.example.mylife.lifebalance.data.DreamAffirmation>> =
        repository.getAllAffirmations()

    fun addAffirmation(sectorId: Int, text: String) {
        viewModelScope.launch {
            repository.addAffirmation(sectorId, text)
            syncInBackground()
        }
    }

    fun updateAffirmation(affirmation: com.example.mylife.lifebalance.data.DreamAffirmation) {
        viewModelScope.launch {
            repository.updateAffirmation(affirmation)
            syncInBackground()
        }
    }

    fun deleteAffirmation(affirmation: com.example.mylife.lifebalance.data.DreamAffirmation) {
        viewModelScope.launch {
            repository.deleteAffirmation(affirmation)
            syncInBackground()
        }
    }

    // ===== PDF Export =====
    suspend fun collectDataForPdf(
        startDate: LocalDate?,
        endDate: LocalDate?,
        includeBalanceWheel: Boolean = true,
        includeTasks: Boolean = true,
        includeGoals: Boolean = true,
        includeIdeas: Boolean = true
    ): com.example.mylife.lifebalance.utils.PdfExportData {
        val spheres = repository.getAllSpheres().first()
        val tasks = if (startDate != null && endDate != null) {
            repository.getTasksByDateRange(startDate, endDate).first()
        } else {
            repository.getAllTasks().first()
        }
        val goals = repository.getAllGoals().first()
        val folders = repository.getAllFolders().first()
        val notesWithoutFolder = repository.getNotesWithoutFolder().first()
        val notesByFolder = folders.associate { folder ->
            folder.id to repository.getNotesByFolderId(folder.id).first()
        }
        return com.example.mylife.lifebalance.utils.PdfExportData(
            spheres = spheres,
            tasks = tasks,
            goals = goals,
            folders = folders,
            notesWithoutFolder = notesWithoutFolder,
            notesByFolder = notesByFolder,
            startDate = startDate,
            endDate = endDate,
            includeBalanceWheel = includeBalanceWheel,
            includeTasks = includeTasks,
            includeGoals = includeGoals,
            includeIdeas = includeIdeas
        )
    }

}
class LifeBalanceViewModelFactory(
    private val repository: LifeBalanceRepository,
    private val syncService: com.example.mylife.lifebalance.repository.SyncService? = null,
    private val settingsDataStore: AppSettingsDataStore? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LifeBalanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LifeBalanceViewModel(repository, syncService, settingsDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}
