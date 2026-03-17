package com.example.mylife.lifebalance.repository

import android.content.Context
import com.example.lifebalance.BuildConfig
import com.example.mylife.lifebalance.data.*
import com.example.mylife.lifebalance.utils.TaskGenerator
import com.example.mylife.lifebalance.utils.ImageStorageHelper
import com.example.mylife.lifebalance.utils.FirebaseStorageHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.lifebalance.R
import com.google.firebase.auth.FirebaseAuth
import android.net.Uri
import kotlinx.coroutines.tasks.await
import com.example.mylife.lifebalance.RetrofitProvider
import com.example.mylife.lifebalance.data.AiAnalyzeRequest



class LifeBalanceRepository(
    private val context: Context,          // ← добавили context
    private val lifeSphereDao: LifeSphereDao,
    private val taskDao: TaskDao,
    private val goalDao: GoalDao,
    private val ideaFolderDao: IdeaFolderDao,
    private val ideaNoteDao: IdeaNoteDao,
    private val dreamSectorPhotoDao: DreamSectorPhotoDao,
    private val dreamAffirmationDao: DreamAffirmationDao
) {

    // ====== ЧЕКБОКСЫ ======
    // Теперь используем поле checked из базы данных вместо DataStore
    
    fun isGoalChecked(goalId: Int): Flow<Boolean> =
        goalDao.getGoalById(goalId).map { goal -> goal?.checked ?: false }

    suspend fun saveGoalChecked(goalId: Int, checked: Boolean) {
        val goal = goalDao.getGoalById(goalId).first()
        goal?.let {
            goalDao.updateGoal(it.copy(checked = checked))
        }
    }

    // ===== LifeSphere =====
    fun getAllSpheres(): Flow<List<LifeSphere>> = lifeSphereDao.getAllSpheres()

    suspend fun getSphereById(id: Long): LifeSphere? = lifeSphereDao.getSphereById(id)

    suspend fun insertSphere(sphere: LifeSphere): Long {
        val currentCount = lifeSphereDao.getSphereCount()
        val sphereWithOrder = sphere.copy(order = currentCount)
        return lifeSphereDao.insertSphere(sphereWithOrder)
    }

    // Вставка сферы с сохранением оригинального order (для синхронизации из Firebase)
    suspend fun insertSphereFromSync(sphere: LifeSphere): Long {
        return lifeSphereDao.insertSphere(sphere)
    }

    suspend fun updateSphere(sphere: LifeSphere) {
        lifeSphereDao.updateSphere(sphere)
        // regenerateTasks(sphere) // Отключено: автоматическая генерация рекомендаций при изменении оценок
    }

    //Удаление ЦЕЛЕЙ сфер,которых нет
    suspend fun deleteSphere(sphere: LifeSphere) {
        goalDao.getGoalsForSphere(sphere.id.toInt()).first().forEach { goal -> //ЭТО ДОБАВИЛИ ДЛЯ УДАЛЕНИЯ ЦЕЛЕЙ
            deleteGoal(goal)
        }
        taskDao.deleteTasksBySphereId(sphere.id)
        lifeSphereDao.deleteSphere(sphere)
    }

    suspend fun getSphereCount(): Int = lifeSphereDao.getSphereCount()

    // ===== Tasks =====
    fun getTasksBySphereId(sphereId: Long): Flow<List<Task>> =
        taskDao.getTasksBySphereId(sphereId)

    fun getTasksByDate(date: org.threeten.bp.LocalDate): Flow<List<Task>> =
        taskDao.getTasksByDate(date)

    fun getTasksByDateRange(startDate: org.threeten.bp.LocalDate, endDate: org.threeten.bp.LocalDate): Flow<List<Task>> =
        taskDao.getTasksByDateRange(startDate, endDate)

    fun getAllTasks(): Flow<List<Task>> =
        taskDao.getAllTasks()

    suspend fun insertTask(task: Task) = taskDao.insertTask(task)
    suspend fun updateTask(task: Task) = taskDao.updateTask(task)
    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)
    suspend fun deleteTasksAfterDate(title: String, sphereId: Long, endDate: org.threeten.bp.LocalDate) =
        taskDao.deleteTasksAfterDate(title, sphereId, endDate)

    private suspend fun regenerateTasks(sphere: LifeSphere) {
        taskDao.deleteTasksBySphereId(sphere.id)
        val tasks = TaskGenerator.generateTasks(sphere)
        tasks.forEach { taskDao.insertTask(it) }
    }

    // ===== Goals =====
    fun getGoalsForSphere(sphereId: Int): Flow<List<Goal>> =
        goalDao.getGoalsForSphere(sphereId)

    fun getAllGoals(): Flow<List<Goal>> =
        goalDao.getAllGoals()

    suspend fun addGoal(goal: Goal): Int {
        // Сохраняем цель с её текущим значением checked (для синхронизации из Firebase)
        // Если checked не указан явно, будет использоваться значение по умолчанию false
        val insertedId = goalDao.insertGoal(goal)
        return insertedId.toInt()
    }

    suspend fun updateGoal(goal: Goal) {
        goalDao.updateGoal(goal)
    }

    suspend fun deleteGoal(goal: Goal) {
        // Удаляем связанные фото перед удалением цели
        val photoUris = goal.getPhotoUrisList()
        withContext(Dispatchers.IO) {
            photoUris.forEach { uri ->
                ImageStorageHelper.deleteImageFromStorage(context, uri)
            }
        }
        goalDao.deleteGoal(goal)
    }
    
    // Миграция старых URI в приватное хранилище
    suspend fun migrateGoalPhotoToPrivateStorage(goal: Goal): Goal? = withContext(Dispatchers.IO) {
        val photoUris = goal.getPhotoUrisList()
        if (photoUris.isEmpty()) return@withContext null
        
        val migratedUris = mutableListOf<String>()
        var needsMigration = false
        
        photoUris.forEach { uriString ->
            // Проверяем, является ли URI уже сохраненным в приватном хранилище
            if (ImageStorageHelper.isPrivateStorageUri(context, uriString)) {
                migratedUris.add(uriString)
            } else {
                // Пытаемся мигрировать старый URI
                try {
                    val uri = android.net.Uri.parse(uriString)
                    val migratedUri = ImageStorageHelper.copyImageToPrivateStorage(context, uri)
                    migratedUri?.let {
                        migratedUris.add(it.toString())
                        needsMigration = true
                    } ?: run {
                        // Если миграция не удалась, оставляем старый URI
                        migratedUris.add(uriString)
                    }
                } catch (e: Exception) {
                    // Если ошибка, оставляем старый URI
                    migratedUris.add(uriString)
                }
            }
        }
        
        if (needsMigration && migratedUris.isNotEmpty()) {
            goal.copy(photoUris = migratedUris.toPhotoUrisString())
        } else {
            null
        }
    }

    // ===== Инициализация стандартных сфер =====
    suspend fun initializeDefaultSpheres() {
        val count = lifeSphereDao.getSphereCount()
        if (count == 0) {
            // Только при самом первом запуске создаем стандартные сферы с примерами названий
            val defaultSpheres = listOf(
                LifeSphere(name = context.getString(R.string.sphere_health), score = 0, colorIndex = 0, order = 0),
                LifeSphere(name = context.getString(R.string.sphere_family), score = 0, colorIndex = 1, order = 1),
                LifeSphere(name = context.getString(R.string.sphere_work), score = 0, colorIndex = 2, order = 2),
                LifeSphere(name = context.getString(R.string.sphere_friends), score = 0, colorIndex = 3, order = 3),
                LifeSphere(name = context.getString(R.string.sphere_finance), score = 0, colorIndex = 4, order = 4),
                LifeSphere(name = context.getString(R.string.sphere_rest), score = 0, colorIndex = 5, order = 5),
                LifeSphere(name = context.getString(R.string.sphere_spirituality), score = 0, colorIndex = 6, order = 6),
                LifeSphere(name = context.getString(R.string.sphere_self_dev), score = 0, colorIndex = 7, order = 7)
            )
            defaultSpheres.forEach { insertSphere(it) }
            if (BuildConfig.DEBUG) android.util.Log.d("LifeBalanceRepository", "Created default spheres for first launch")
        }
        // НЕ обновляем названия при последующих запусках - пользователь может их изменить
        // Стандартные названия служат только как пример при первом запуске
    }
    
    // Обновление названий стандартных сфер в соответствии с текущим языком
    // ВАЖНО: Эта функция больше не вызывается автоматически
    // Используется только для ручного обновления при смене языка, если пользователь не менял названия
    suspend fun updateDefaultSphereNames() {
        val allSpheres = lifeSphereDao.getAllSpheres().first()
        
        // Получаем актуальный язык (читаем из того же хранилища, куда пишет saveLanguage)
        val prefs = getSyncPrefsForAttachment(context)
        val savedLanguage = prefs.getString("language", null)
        val language = if (savedLanguage != null && savedLanguage.isNotEmpty()) {
            savedLanguage
        } else {
            val systemLanguage = context.resources.configuration.locales[0].language
            val supportedLanguages = listOf("ru", "uk", "en", "de", "fr", "es")
            if (systemLanguage in supportedLanguages) systemLanguage else "en"
        }
        
        // Создаем конфигурацию с правильной локализацией
        val config = android.content.res.Configuration(context.resources.configuration)
        val locale = java.util.Locale(language)
        config.setLocale(locale)
        
        // Создаем локализованный контекст
        val localizedContext = context.createConfigurationContext(config)
        
        // Получаем стандартные названия на текущем языке
        val defaultSphereNames = mapOf(
            0 to localizedContext.resources.getString(R.string.sphere_health),
            1 to localizedContext.resources.getString(R.string.sphere_family),
            2 to localizedContext.resources.getString(R.string.sphere_work),
            3 to localizedContext.resources.getString(R.string.sphere_friends),
            4 to localizedContext.resources.getString(R.string.sphere_finance),
            5 to localizedContext.resources.getString(R.string.sphere_rest),
            6 to localizedContext.resources.getString(R.string.sphere_spirituality),
            7 to localizedContext.resources.getString(R.string.sphere_self_dev)
        )
        
        // Получаем стандартные названия на всех поддерживаемых языках для проверки
        // Если текущее название совпадает с любым стандартным на любом языке, значит это стандартное название
        val supportedLanguages = listOf("ru", "uk", "en", "de", "fr", "es")
        val allDefaultNamesByOrder = mutableMapOf<Int, MutableSet<String>>()
        
        supportedLanguages.forEach { lang ->
            try {
                val langConfig = android.content.res.Configuration(context.resources.configuration)
                langConfig.setLocale(java.util.Locale(lang))
                val langContext = context.createConfigurationContext(langConfig)
                
                for (order in 0..7) {
                    val defaultName = when (order) {
                        0 -> langContext.resources.getString(R.string.sphere_health)
                        1 -> langContext.resources.getString(R.string.sphere_family)
                        2 -> langContext.resources.getString(R.string.sphere_work)
                        3 -> langContext.resources.getString(R.string.sphere_friends)
                        4 -> langContext.resources.getString(R.string.sphere_finance)
                        5 -> langContext.resources.getString(R.string.sphere_rest)
                        6 -> langContext.resources.getString(R.string.sphere_spirituality)
                        7 -> langContext.resources.getString(R.string.sphere_self_dev)
                        else -> ""
                    }
                    if (defaultName.isNotEmpty()) {
                        allDefaultNamesByOrder.getOrPut(order) { mutableSetOf() }.add(defaultName)
                    }
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) android.util.Log.w("LifeBalanceRepository", "Error loading default names for language", e)
            }
        }
        
        var updatedCount = 0
        allSpheres.forEach { sphere ->
            // Обновляем только стандартные сферы (order от 0 до 7)
            if (sphere.order in 0..7) {
                val newName = defaultSphereNames[sphere.order]
                val defaultNamesForThisOrder = allDefaultNamesByOrder[sphere.order] ?: emptySet()
                
                // Обновляем название ТОЛЬКО если:
                // 1. Текущее название совпадает с любым стандартным названием на любом языке
                // 2. И новое название отличается от текущего
                // Это означает, что пользователь еще не менял название, и мы просто обновляем его на текущий язык
                if (newName != null && 
                    sphere.name in defaultNamesForThisOrder && 
                    sphere.name != newName) {
                    lifeSphereDao.updateSphere(sphere.copy(name = newName))
                    updatedCount++
                    if (BuildConfig.DEBUG) android.util.Log.d("LifeBalanceRepository", "Updated sphere order for language")
                } else if (sphere.name !in defaultNamesForThisOrder) {
                    if (BuildConfig.DEBUG) android.util.Log.d("LifeBalanceRepository", "Skipping sphere: name was changed by user")
                }
            }
        }
        if (updatedCount > 0) {
            if (BuildConfig.DEBUG) android.util.Log.d("LifeBalanceRepository", "Updated sphere names for language")
        } else {
            if (BuildConfig.DEBUG) android.util.Log.d("LifeBalanceRepository", "No sphere names updated")
        }
    }

    // ===== Idea Folders =====
    fun getAllFolders(): Flow<List<IdeaFolder>> = ideaFolderDao.getAllFolders()

    suspend fun getFolderById(id: Long): IdeaFolder? = ideaFolderDao.getFolderById(id)

    suspend fun insertFolder(folder: IdeaFolder): Long = ideaFolderDao.insertFolder(folder)

    suspend fun updateFolder(folder: IdeaFolder) = ideaFolderDao.updateFolder(folder)

    suspend fun deleteFolder(folder: IdeaFolder) {
        ideaNoteDao.deleteNotesByFolderId(folder.id)
        ideaFolderDao.deleteFolder(folder)
    }

    // ===== Idea Notes =====
    fun getNotesWithoutFolder(): Flow<List<IdeaNote>> = ideaNoteDao.getNotesWithoutFolder()

    fun getNotesByFolderId(folderId: Long): Flow<List<IdeaNote>> = ideaNoteDao.getNotesByFolderId(folderId)

    fun searchNotes(query: String): Flow<List<IdeaNote>> = ideaNoteDao.searchNotes(query)

    suspend fun getNoteById(id: Long): IdeaNote? = ideaNoteDao.getNoteById(id)

    suspend fun insertNote(note: IdeaNote): Long = ideaNoteDao.insertNote(note)

    suspend fun updateNote(note: IdeaNote) = ideaNoteDao.updateNote(note.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteNote(note: IdeaNote) = ideaNoteDao.deleteNote(note)

    // ===== Dream Sector Photos =====
    fun getPhotosBySectorId(sectorId: Int): Flow<List<DreamSectorPhoto>> = 
        dreamSectorPhotoDao.getPhotosBySectorId(sectorId)
    
    suspend fun getPhotosBySectorIdSync(sectorId: Int): List<DreamSectorPhoto> = 
        dreamSectorPhotoDao.getPhotosBySectorIdSync(sectorId)
    
    suspend fun addPhotoToSector(sectorId: Int, sourceUri: Uri): Result<DreamSectorPhoto> = withContext(Dispatchers.IO) {
        try {
            // Копируем фото в приватное хранилище
            val savedUri = ImageStorageHelper.copyImageToPrivateStorage(context, sourceUri)
                ?: return@withContext Result.failure(Exception("Failed to save photo to local storage"))
            
            // Получаем текущие фото сектора
            val existingPhotos = dreamSectorPhotoDao.getPhotosBySectorIdSync(sectorId)
            
            // Проверяем лимит (максимум 2 фото)
            if (existingPhotos.size >= 2) {
                return@withContext Result.failure(Exception("Maximum 2 photos per sector"))
            }
            
            // Определяем порядок нового фото
            val photoOrder = existingPhotos.size
            
            // Загружаем в Firebase Storage (если пользователь авторизован)
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val firebaseStoragePath = userId?.let { uid ->
                val path = FirebaseStorageHelper.generateStoragePath(uid, sectorId, photoOrder)
                FirebaseStorageHelper.uploadPhoto(context, savedUri, path)
                path
            }
            
            // Создаем запись в базе данных
            val photo = DreamSectorPhoto(
                sectorId = sectorId,
                photoUri = savedUri.toString(),
                firebaseStoragePath = firebaseStoragePath,
                order = photoOrder
            )
            
            val photoId = dreamSectorPhotoDao.insertPhoto(photo)
            val savedPhoto = photo.copy(id = photoId)
            
            Result.success(savedPhoto)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.e("LifeBalanceRepository", "Error adding photo", e)
            Result.failure(e)
        }
    }
    
    suspend fun deletePhoto(photo: DreamSectorPhoto) = withContext(Dispatchers.IO) {
        try {
            // Удаляем из локального хранилища
            ImageStorageHelper.deleteImageFromStorage(context, photo.photoUri)
            
            // Удаляем из Firebase Storage
            photo.firebaseStoragePath?.let { path ->
                FirebaseStorageHelper.deletePhoto(context, path)
            }
            
            // Удаляем из базы данных
            dreamSectorPhotoDao.deletePhoto(photo)
            
            // Обновляем порядок оставшихся фото
            val remainingPhotos = dreamSectorPhotoDao.getPhotosBySectorIdSync(photo.sectorId)
            remainingPhotos.forEachIndexed { index, remainingPhoto ->
                if (remainingPhoto.order != index) {
                    dreamSectorPhotoDao.updatePhoto(remainingPhoto.copy(order = index))
                }
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.e("LifeBalanceRepository", "Error deleting photo", e)
        }
    }
    
    suspend fun updatePhotosForSector(sectorId: Int, photoUris: List<String>) = withContext(Dispatchers.IO) {
        try {
            val existingPhotos = dreamSectorPhotoDao.getPhotosBySectorIdSync(sectorId)
            
            // Удаляем фото, которых больше нет в списке
            existingPhotos.forEach { existingPhoto ->
                if (!photoUris.contains(existingPhoto.photoUri)) {
                    deletePhoto(existingPhoto)
                }
            }
            
            // Обновляем порядок существующих фото
            photoUris.forEachIndexed { index, uri ->
                val existingPhoto = existingPhotos.find { it.photoUri == uri }
                if (existingPhoto != null && existingPhoto.order != index) {
                    dreamSectorPhotoDao.updatePhoto(existingPhoto.copy(order = index))
                }
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.e("LifeBalanceRepository", "Error updating photos", e)
        }
    }
    
    // ===== Dream Affirmations =====
    fun getAffirmationsBySectorId(sectorId: Int): Flow<List<DreamAffirmation>> = 
        dreamAffirmationDao.getAffirmationsBySectorId(sectorId)
    
    fun getAllAffirmations(): Flow<List<DreamAffirmation>> = 
        dreamAffirmationDao.getAllAffirmations()
    
    suspend fun addAffirmation(sectorId: Int, text: String): Result<DreamAffirmation> = withContext(Dispatchers.IO) {
        try {
            if (text.isBlank()) {
                return@withContext Result.failure(Exception("Affirmation text cannot be blank"))
            }
            
            val affirmation = DreamAffirmation(
                sectorId = sectorId,
                text = text.trim(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            val id = dreamAffirmationDao.insertAffirmation(affirmation)
            val savedAffirmation = affirmation.copy(id = id)
            
            // Синхронизируем с Firebase Firestore
            syncAffirmationToFirestore(savedAffirmation)
            
            Result.success(savedAffirmation)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.e("LifeBalanceRepository", "Error adding affirmation", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateAffirmation(affirmation: DreamAffirmation) = withContext(Dispatchers.IO) {
        try {
            val updated = affirmation.copy(updatedAt = System.currentTimeMillis())
            dreamAffirmationDao.updateAffirmation(updated)
            
            // Синхронизируем с Firebase Firestore
            syncAffirmationToFirestore(updated)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.e("LifeBalanceRepository", "Error updating affirmation", e)
        }
    }
    
    suspend fun deleteAffirmation(affirmation: DreamAffirmation) = withContext(Dispatchers.IO) {
        try {
            dreamAffirmationDao.deleteAffirmation(affirmation)
            
            // Удаляем из Firebase Firestore
            deleteAffirmationFromFirestore(affirmation.id)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.e("LifeBalanceRepository", "Error deleting affirmation", e)
        }
    }
    
    // Метод для синхронизации аффирмации из Firebase (без синхронизации обратно в Firebase)
    suspend fun insertAffirmationFromSync(affirmation: DreamAffirmation) = withContext(Dispatchers.IO) {
        try {
            val existing = dreamAffirmationDao.getAffirmationById(affirmation.id)
            if (existing != null) {
                dreamAffirmationDao.updateAffirmation(affirmation)
            } else {
                dreamAffirmationDao.insertAffirmation(affirmation)
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.e("LifeBalanceRepository", "Error inserting affirmation from sync", e)
        }
    }
    
    private suspend fun syncAffirmationToFirestore(affirmation: DreamAffirmation) = withContext(Dispatchers.IO) {
        try {
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser ?: return@withContext
            
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val userId = user.uid
            
            val affirmationData = hashMapOf(
                "sectorId" to affirmation.sectorId,
                "text" to affirmation.text,
                "createdAt" to affirmation.createdAt,
                "updatedAt" to affirmation.updatedAt
            )
            
            firestore.collection("users")
                .document(userId)
                .collection("dreamAffirmations")
                .document(affirmation.id.toString())
                .set(affirmationData)
                .await()
            
            if (BuildConfig.DEBUG) android.util.Log.d("LifeBalanceRepository", "Affirmation synced to Firestore")
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.w("LifeBalanceRepository", "Failed to sync affirmation to Firestore", e)
        }
    }
    
    private suspend fun deleteAffirmationFromFirestore(affirmationId: Long) = withContext(Dispatchers.IO) {
        try {
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser ?: return@withContext
            
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val userId = user.uid
            
            firestore.collection("users")
                .document(userId)
                .collection("dreamAffirmations")
                .document(affirmationId.toString())
                .delete()
                .await()
            
            if (BuildConfig.DEBUG) android.util.Log.d("LifeBalanceRepository", "Affirmation deleted from Firestore")
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.w("LifeBalanceRepository", "Failed to delete affirmation from Firestore", e)
        }
    }




    suspend fun analyzeLifeBalance(text: String, language: String): String {
        return try {
            val response = RetrofitProvider.api.analyzeBalance(
                AiAnalyzeRequest(
                    text = text,
                    language = language
                )
            )
            response.result
        } catch (e: Exception) {
            "Ошибка анализа: ${e.message}"
        }
    }

}
