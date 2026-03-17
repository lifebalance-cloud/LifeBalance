package com.example.mylife.lifebalance.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.lifebalance.BuildConfig
import com.example.mylife.lifebalance.data.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

class SyncService(
    private val context: Context,
    private val repository: LifeBalanceRepository,
    private val authRepository: AuthRepository
) {
    private val firestore: FirebaseFirestore? by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseFirestore.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.w("SyncService", "Firestore not available", e)
            null
        }
    }
    
    private val auth: FirebaseAuth? by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseAuth.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.w("SyncService", "FirebaseAuth not available", e)
            null
        }
    }
    
    private fun isFirebaseAvailable(): Boolean {
        return try {
            firestore != null && auth != null && FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    suspend fun syncAllData(): Result<Unit> {
        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Starting syncAllData...")
        if (!isFirebaseAvailable()) {
            if (BuildConfig.DEBUG) android.util.Log.e("SyncService", "Firebase not available")
            return Result.failure(Exception("Firebase is not initialized. Please configure Firebase to enable sync."))
        }
        
        if (!isNetworkAvailable()) {
            if (BuildConfig.DEBUG) android.util.Log.e("SyncService", "Network not available")
            return Result.failure(Exception("No internet connection"))
        }

        val user = auth?.currentUser
        if (user == null) {
            if (BuildConfig.DEBUG) android.util.Log.e("SyncService", "User not authenticated")
            return Result.failure(Exception("User not authenticated"))
        }

        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Sync started")

        return try {
            if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Syncing spheres...")
            // Синхронизация сфер
            syncSpheres(user.uid)
            
            if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Syncing tasks...")
            // Синхронизация задач
            syncTasks(user.uid)
            
            if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Syncing goals...")
            // Синхронизация целей
            syncGoals(user.uid)
            
            if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Syncing idea folders...")
            // Синхронизация папок идей
            syncIdeaFolders(user.uid)
            
            if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Syncing idea notes...")
            // Синхронизация заметок
            syncIdeaNotes(user.uid)
            
            if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Syncing dream affirmations...")
            // Синхронизация аффирмаций
            syncDreamAffirmations(user.uid)
            
            // Обновляем timestamp последней синхронизации
            val localUser = authRepository.getLocalUser()
            localUser?.let {
                authRepository.updateLocalUser(it.copy(lastSyncTimestamp = System.currentTimeMillis()))
            }
            
            if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Sync completed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.e("SyncService", "Sync failed", e)
            Result.failure(e)
        }
    }

    private suspend fun syncSpheres(userId: String) {
        val firestoreInstance = firestore ?: return
        val localSpheres = repository.getAllSpheres().first()
        val remoteRef = firestoreInstance.collection("users").document(userId).collection("spheres")
        
        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Syncing spheres: local=${localSpheres.size}")
        
        // Загружаем удаленные сферы
        val remoteSpheresSnapshot = remoteRef.get().await()
        val remoteSpheres = remoteSpheresSnapshot.documents.mapNotNull { doc ->
            try {
                val dto = doc.toObject(LifeSphereDTO::class.java)
                dto?.copy(id = doc.id.toLongOrNull() ?: dto.id)?.toLifeSphere()
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) android.util.Log.w("SyncService", "Failed to parse remote sphere ${doc.id}", e)
                null
            }
        }
        
        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Loaded ${remoteSpheres.size} remote spheres")
        
        // Проверяем, являются ли локальные данные дефолтными (после переустановки)
        // Дефолтные данные - это когда все сферы имеют score=0 И соответствуют стандартным сферам (order 0-7)
        // И нет пользовательских сфер (order > 7)
        val hasUserSpheres = localSpheres.any { it.order > 7 }
        val isLocalDataDefault = localSpheres.isNotEmpty() && 
                                 localSpheres.all { it.score == 0 } && 
                                 localSpheres.all { it.order in 0..7 } &&
                                 !hasUserSpheres &&
                                 remoteSpheres.isNotEmpty()
        
        if (isLocalDataDefault) {
            if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Local data appears to be default, replacing with remote data")
            // После переустановки полностью заменяем локальные сферы данными из Firebase
            // Это гарантирует, что все изменения (название, цвет) будут сохранены
            localSpheres.forEach { localSphere ->
                try {
                    repository.deleteSphere(localSphere)
                    if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Deleted local default sphere to replace with Firebase data")
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) android.util.Log.w("SyncService", "Error deleting local default sphere", e)
                }
            }
        }
        
        // Получаем ID всех удаленных сфер из Firebase
        val remoteSphereIds = remoteSpheres.map { it.id }.toSet()
        val localSphereIds = localSpheres.map { it.id }.toSet()
        
        // Определяем сферы, которые были удалены локально (есть в Firebase, но нет локально)
        // ВАЖНО: Если локальные данные дефолтные (после переустановки), НЕ считаем сферы удаленными
        val deletedLocally = if (isLocalDataDefault) {
            emptySet<Long>() // После переустановки не считаем сферы удаленными
        } else {
            remoteSphereIds - localSphereIds // Только если есть пользовательские данные, определяем удаленные
        }
        
        // Удаляем локально сферы, которые были удалены в Firebase (только если не дефолтные данные)
        if (!isLocalDataDefault) {
            val deletedInRemote = localSphereIds - remoteSphereIds
            deletedInRemote.forEach { deletedId ->
                try {
                    val sphereToDelete = localSpheres.find { it.id == deletedId }
                    if (sphereToDelete != null) {
                        repository.deleteSphere(sphereToDelete)
                        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Deleted local sphere (was deleted in Firebase)")
                    }
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) android.util.Log.w("SyncService", "Error deleting local sphere", e)
                }
            }
        }
        
        // Обновляем локальную БД с данными из Firebase
        // После переустановки загружаем все сферы из Firebase с их оригинальными ID
        val spheresToLoad = if (isLocalDataDefault) {
            remoteSpheres // После переустановки загружаем все сферы
        } else {
            remoteSpheres.filter { it.id !in deletedLocally } // Иначе пропускаем удаленные
        }
        
        spheresToLoad.forEach { remoteSphere ->
            try {
                // После переустановки все сферы были удалены, поэтому всегда вставляем
                // В обычном режиме проверяем, существует ли сфера локально
                if (isLocalDataDefault) {
                    // После переустановки вставляем все сферы из Firebase с их оригинальными ID и order
                    // Используем insertSphereFromSync, чтобы сохранить оригинальный order из Firebase
                    repository.insertSphereFromSync(remoteSphere)
                    if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Inserted sphere from Firebase")
                } else {
                    val localSphere = localSpheres.find { it.id == remoteSphere.id }
                    if (localSphere == null) {
                        // Новая сфера из облака - вставляем с сохранением оригинального order из Firebase
                        repository.insertSphereFromSync(remoteSphere)
                        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Inserted new sphere from Firebase")
                    } else {
                        // Сфера существует локально - приоритет локальным изменениям (название, цвет, порядок)
                        // Но обновляем score из удаленных данных, если он изменился
                        val mergedSphere = if (localSphere.name != remoteSphere.name || 
                                               localSphere.colorIndex != remoteSphere.colorIndex ||
                                               localSphere.order != remoteSphere.order) {
                            // Локальные изменения в названии/цвете/порядке - сохраняем их
                            localSphere
                        } else {
                            // Если нет локальных изменений, используем удаленную версию (может быть обновлен score)
                            remoteSphere
                        }
                        repository.updateSphere(mergedSphere)
                        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Merged sphere: local changes preserved")
                    }
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) android.util.Log.w("SyncService", "Error saving sphere", e)
            }
        }
        
        // Получаем актуальный список локальных сфер после синхронизации
        val currentLocalSpheres = repository.getAllSpheres().first()
        val currentLocalSphereIds = currentLocalSpheres.map { it.id }.toSet()
        
        // Пересчитываем удаленные сферы ПОСЛЕ загрузки из Firebase
        val finalDeletedLocally = if (isLocalDataDefault) {
            // После переустановки не удаляем сферы из Firebase
            emptySet<Long>()
        } else {
            // Определяем сферы, которые есть в Firebase, но нет в локальной БД после синхронизации
            remoteSphereIds - currentLocalSphereIds
        }
        
        // Отправляем локальные сферы на сервер (кроме случая, когда локальные данные были дефолтными изначально)
        if (!isLocalDataDefault || currentLocalSpheres.isNotEmpty()) {
            // Отправляем все текущие локальные сферы
            currentLocalSpheres.forEach { sphere ->
                try {
                    val dto = LifeSphereDTO.fromLifeSphere(sphere)
                    remoteRef.document(sphere.id.toString()).set(dto).await()
                    if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Uploaded sphere")
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) android.util.Log.e("SyncService", "Failed to upload sphere", e)
                }
            }
            
            // Удаляем из Firebase сферы, которые были удалены локально
            finalDeletedLocally.forEach { deletedId ->
                try {
                    remoteRef.document(deletedId.toString()).delete().await()
                    if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Deleted sphere from Firebase (was deleted locally)")
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) android.util.Log.w("SyncService", "Error deleting sphere from Firebase", e)
                }
            }
        } else {
            if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Skipping upload of default spheres (using remote data instead)")
        }
        
        // Если после синхронизации сфер нет (новый пользователь) — создаём дефолтные 8 сфер
        val finalCount = repository.getSphereCount()
        if (finalCount == 0) {
            if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "No spheres after sync, initializing default spheres")
            repository.initializeDefaultSpheres()
        }
        
        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Spheres sync completed")
    }

    private suspend fun syncTasks(userId: String) {
        val firestoreInstance = firestore ?: return
        val localTasks = repository.getAllTasks().first()
        val remoteRef = firestoreInstance.collection("users").document(userId).collection("tasks")
        
        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Syncing tasks: local=${localTasks.size}")
        
        // Загружаем удаленные задачи
        val remoteTasksSnapshot = remoteRef.get().await()
        val remoteTasks = remoteTasksSnapshot.documents.mapNotNull { doc ->
            try {
                val dto = doc.toObject(TaskDTO::class.java)
                dto?.copy(id = doc.id.toLongOrNull() ?: dto.id)?.toTask()
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) android.util.Log.w("SyncService", "Failed to parse remote task", e)
                null
            }
        }
        
        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Loaded ${remoteTasks.size} remote tasks")
        
        // Проверяем, являются ли локальные данные дефолтными (после переустановки)
        // Если локальных задач нет, но есть удаленные, используем удаленные
        val isLocalTasksEmpty = localTasks.isEmpty() && remoteTasks.isNotEmpty()
        
        if (isLocalTasksEmpty) {
            if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Local tasks are empty, using remote tasks")
        }
        
        // Получаем ID всех удаленных задач из Firebase
        val remoteTaskIds = remoteTasks.map { it.id }.toSet()
        val localTaskIds = localTasks.map { it.id }.toSet()
        
        // Определяем задачи, которые были удалены локально (есть в Firebase, но нет локально)
        // ВАЖНО: Если локальные задачи пусты (после переустановки), НЕ считаем задачи удаленными
        // В этом случае все задачи из Firebase должны быть загружены
        val deletedLocally = if (isLocalTasksEmpty) {
            emptySet<Long>() // После переустановки не считаем задачи удаленными
        } else {
            remoteTaskIds - localTaskIds // Только если есть локальные задачи, определяем удаленные
        }
        
        // Не удаляем локально задачи из множества (localTaskIds - remoteTaskIds): они могут быть
        // только что добавленными (например из анализа баланса), ещё не попавшими в Firebase.
        // Удаление по признаку «нет в remote» приводило к потере новых задач при синхронизации.
        
        // Обновляем локальную БД с данными из Firebase
        // Если локальные задачи пусты (после переустановки), загружаем ВСЕ задачи из Firebase
        // Иначе пропускаем задачи, которые были удалены локально
        val tasksToLoad = if (isLocalTasksEmpty) {
            remoteTasks // После переустановки загружаем все задачи
        } else {
            remoteTasks.filter { it.id !in deletedLocally } // Иначе пропускаем удаленные
        }
        
        tasksToLoad.forEach { remoteTask ->
            try {
                val localTask = localTasks.find { it.id == remoteTask.id }
                if (localTask == null) {
                    // Новая задача из облака - вставляем
                    repository.insertTask(remoteTask)
                    if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Inserted new task from Firebase")
                } else {
                    // Задача существует локально - используем локальную версию (приоритет локальным изменениям)
                    // Но если локальные задачи пусты, используем удаленную версию
                    if (isLocalTasksEmpty) {
                        repository.updateTask(remoteTask)
                        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Updated task with remote data (local was empty)")
                    } else {
                        repository.updateTask(localTask)
                        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Updated task with local data")
                    }
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) android.util.Log.w("SyncService", "Error saving task", e)
            }
        }
        
        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Tasks sync completed")
        
        // Получаем актуальный список локальных задач после синхронизации
        val currentLocalTasks = repository.getAllTasks().first()
        val currentLocalTaskIds = currentLocalTasks.map { it.id }.toSet()
        
        // Пересчитываем удаленные задачи ПОСЛЕ загрузки из Firebase
        // Это важно, чтобы правильно определить задачи, которые были удалены пользователем
        val finalDeletedLocally = if (isLocalTasksEmpty) {
            // После переустановки не удаляем задачи из Firebase
            emptySet<Long>()
        } else {
            // Определяем задачи, которые есть в Firebase, но нет в локальной БД после синхронизации
            remoteTaskIds - currentLocalTaskIds
        }
        
        // Отправляем локальные задачи на сервер (кроме случая, когда локальные задачи были пусты изначально)
        if (!isLocalTasksEmpty || currentLocalTasks.isNotEmpty()) {
            // Отправляем все текущие локальные задачи
            currentLocalTasks.forEach { task ->
                try {
                    val dto = TaskDTO.fromTask(task)
                    remoteRef.document(task.id.toString()).set(dto).await()
                    if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Uploaded task")
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) android.util.Log.e("SyncService", "Failed to upload task", e)
                }
            }
            
            // Удаляем из Firebase задачи, которые были удалены локально
            finalDeletedLocally.forEach { deletedId ->
                try {
                    remoteRef.document(deletedId.toString()).delete().await()
                    if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Deleted task from Firebase (was deleted locally)")
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) android.util.Log.w("SyncService", "Error deleting task from Firebase", e)
                }
            }
        } else {
            if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Skipping upload of tasks (local tasks empty, using remote data)")
        }
    }

    private suspend fun syncGoals(userId: String) {
        val firestoreInstance = firestore ?: return
        val localGoals = repository.getAllGoals().first()
        val remoteRef = firestoreInstance.collection("users").document(userId).collection("goals")
        
        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Syncing goals: local=${localGoals.size}")
        
        val remoteGoals = remoteRef.get().await().documents.mapNotNull { doc ->
            try {
                val dto = doc.toObject(GoalDTO::class.java)
                dto?.copy(id = doc.id.toIntOrNull() ?: dto.id)?.toGoal()
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) android.util.Log.w("SyncService", "Failed to parse remote goal", e)
                null
            }
        }
        
        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Loaded ${remoteGoals.size} remote goals")
        
        // Проверяем, являются ли локальные данные пустыми (после переустановки)
        // Если локальных целей нет, но есть удаленные, используем удаленные
        val isLocalGoalsEmpty = localGoals.isEmpty() && remoteGoals.isNotEmpty()
        
        if (isLocalGoalsEmpty) {
            if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Local goals are empty, using remote goals")
        }
        
        val allGoals = (localGoals + remoteGoals).distinctBy { it.id }
        
        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Merged ${allGoals.size} total goals")
        
        allGoals.forEach { goal ->
            try {
                repository.addGoal(goal)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Goal already exists, skipping")
            }
        }
        
        // Отправляем локальные цели на сервер
        // НЕ отправляем, если локальных целей нет (после переустановки)
        if (!isLocalGoalsEmpty) {
            localGoals.forEach { goal ->
                try {
                    val dto = GoalDTO.fromGoal(goal)
                    remoteRef.document(goal.id.toString()).set(dto).await()
                    if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Uploaded goal")
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) android.util.Log.w("SyncService", "Failed to upload goal", e)
                }
            }
        } else {
            if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Skipping upload of goals (local goals empty, using remote data)")
        }
        
        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Goals sync completed")
    }

    private suspend fun syncIdeaFolders(userId: String) {
        val firestoreInstance = firestore ?: return
        val localFolders = repository.getAllFolders().first()
        val remoteRef = firestoreInstance.collection("users").document(userId).collection("ideaFolders")
        
        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Syncing idea folders: local=${localFolders.size}")
        
        val remoteFolders = remoteRef.get().await().documents.mapNotNull { doc ->
            try {
                val dto = doc.toObject(IdeaFolderDTO::class.java)
                dto?.copy(id = doc.id.toLongOrNull() ?: dto.id)?.toIdeaFolder()
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) android.util.Log.w("SyncService", "Failed to parse remote folder", e)
                null
            }
        }
        
        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Loaded ${remoteFolders.size} remote folders")
        
        val allFolders = (localFolders + remoteFolders).distinctBy { it.id }
        
        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Merged ${allFolders.size} total folders")
        
        allFolders.forEach { folder ->
            try {
                repository.insertFolder(folder)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Folder already exists, skipping")
            }
        }
        
        localFolders.forEach { folder ->
            try {
                val dto = IdeaFolderDTO.fromIdeaFolder(folder)
                remoteRef.document(folder.id.toString()).set(dto).await()
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) android.util.Log.w("SyncService", "Failed to upload folder", e)
            }
        }
        
        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Idea folders sync completed")
    }

    private suspend fun syncIdeaNotes(userId: String) {
        // Получаем все заметки из всех папок
        val localFolders = repository.getAllFolders().first()
        val allLocalNotes = mutableListOf<IdeaNote>()
        
        // Заметки без папки
        allLocalNotes.addAll(repository.getNotesWithoutFolder().first())
        
        // Заметки из папок
        localFolders.forEach { folder ->
            allLocalNotes.addAll(repository.getNotesByFolderId(folder.id).first())
        }
        
        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Syncing idea notes: local=${allLocalNotes.size}")
        
        val firestoreInstance = firestore ?: return
        val remoteRef = firestoreInstance.collection("users").document(userId).collection("ideaNotes")
        
        val remoteNotes = remoteRef.get().await().documents.mapNotNull { doc ->
            try {
                val dto = doc.toObject(IdeaNoteDTO::class.java)
                dto?.copy(id = doc.id.toLongOrNull() ?: dto.id)?.toIdeaNote()
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) android.util.Log.w("SyncService", "Failed to parse remote note", e)
                null
            }
        }
        
        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Loaded ${remoteNotes.size} remote notes")
        
        val allNotes = (allLocalNotes + remoteNotes).distinctBy { it.id }
        
        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Merged ${allNotes.size} total notes")
        
        allNotes.forEach { note ->
            try {
                repository.insertNote(note)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Note already exists, skipping")
            }
        }
        
        allLocalNotes.forEach { note ->
            try {
                val dto = IdeaNoteDTO.fromIdeaNote(note)
                remoteRef.document(note.id.toString()).set(dto).await()
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) android.util.Log.w("SyncService", "Failed to upload note", e)
            }
        }
        
        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Idea notes sync completed")
    }

    private suspend fun syncDreamAffirmations(userId: String) {
        val localAffirmations = repository.getAllAffirmations().first()
        
        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Syncing dream affirmations: local=${localAffirmations.size}")
        
        val firestoreInstance = firestore ?: return
        val remoteRef = firestoreInstance.collection("users").document(userId).collection("dreamAffirmations")
        
        val remoteAffirmations = remoteRef.get().await().documents.mapNotNull { doc ->
            try {
                val dto = doc.toObject(DreamAffirmationDTO::class.java)
                dto?.copy(id = doc.id.toLongOrNull() ?: dto.id)?.toDreamAffirmation()
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) android.util.Log.w("SyncService", "Failed to parse remote affirmation", e)
                null
            }
        }
        
        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Loaded ${remoteAffirmations.size} remote affirmations")
        
        val allAffirmations = (localAffirmations + remoteAffirmations).distinctBy { it.id }
        
        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Merged ${allAffirmations.size} total affirmations")
        
        allAffirmations.forEach { affirmation ->
            try {
                // Используем метод репозитория для синхронизации аффирмации
                repository.insertAffirmationFromSync(affirmation)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Affirmation sync error", e)
            }
        }
        
        localAffirmations.forEach { affirmation ->
            try {
                val dto = DreamAffirmationDTO.fromDreamAffirmation(affirmation)
                remoteRef.document(affirmation.id.toString()).set(dto).await()
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) android.util.Log.w("SyncService", "Failed to upload affirmation", e)
            }
        }
        
        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Dream affirmations sync completed")
    }

    suspend fun syncDown(): Result<Unit> {
        if (!isFirebaseAvailable()) {
            return Result.failure(Exception("Firebase is not initialized"))
        }
        
        if (!isNetworkAvailable()) {
            return Result.failure(Exception("No internet connection"))
        }

        val user = auth?.currentUser
        if (user == null) {
            return Result.failure(Exception("User not authenticated"))
        }

        return try {
            syncSpheres(user.uid)
            syncTasks(user.uid)
            syncGoals(user.uid)
            syncIdeaFolders(user.uid)
            syncIdeaNotes(user.uid)
            syncDreamAffirmations(user.uid)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncUp(): Result<Unit> {
        if (!isFirebaseAvailable()) {
            return Result.failure(Exception("Firebase is not initialized"))
        }
        
        if (!isNetworkAvailable()) {
            return Result.failure(Exception("No internet connection"))
        }

        val user = auth?.currentUser
        if (user == null) {
            return Result.failure(Exception("User not authenticated"))
        }

        return try {
            val firestoreInstance = firestore ?: return Result.failure(Exception("Firestore is not available"))
            val localSpheres = repository.getAllSpheres().first()
            val localTasks = repository.getAllTasks().first()
            val localGoals = repository.getAllGoals().first()
            val localFolders = repository.getAllFolders().first()
            val localAffirmations = repository.getAllAffirmations().first()
            
            if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Uploading spheres, tasks, goals, folders, affirmations")
            
            val remoteRef = firestoreInstance.collection("users").document(user.uid)
            
            // Отправляем все данные на сервер
            localSpheres.forEach { sphere ->
                try {
                    val dto = LifeSphereDTO.fromLifeSphere(sphere)
                    remoteRef.collection("spheres").document(sphere.id.toString()).set(dto).await()
                    if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Uploaded sphere")
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) android.util.Log.e("SyncService", "Failed to upload sphere", e)
                }
            }
            
            localTasks.forEach { task ->
                try {
                    val dto = TaskDTO.fromTask(task)
                    remoteRef.collection("tasks").document(task.id.toString()).set(dto).await()
                    if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Uploaded task")
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) android.util.Log.e("SyncService", "Failed to upload task", e)
                }
            }
            
            localGoals.forEach { goal ->
                try {
                    val dto = GoalDTO.fromGoal(goal)
                    remoteRef.collection("goals").document(goal.id.toString()).set(dto).await()
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) android.util.Log.w("SyncService", "Failed to sync goal", e)
                }
            }
            
            localFolders.forEach { folder ->
                try {
                    val dto = IdeaFolderDTO.fromIdeaFolder(folder)
                    remoteRef.collection("ideaFolders").document(folder.id.toString()).set(dto).await()
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) android.util.Log.w("SyncService", "Failed to sync folder", e)
                }
            }
            
            val allLocalNotes = mutableListOf<IdeaNote>()
            allLocalNotes.addAll(repository.getNotesWithoutFolder().first())
            localFolders.forEach { folder ->
                allLocalNotes.addAll(repository.getNotesByFolderId(folder.id).first())
            }
            
            if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Uploading notes")
            
            allLocalNotes.forEach { note ->
                try {
                    val dto = IdeaNoteDTO.fromIdeaNote(note)
                    remoteRef.collection("ideaNotes").document(note.id.toString()).set(dto).await()
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) android.util.Log.w("SyncService", "Failed to sync note", e)
                }
            }
            
            // Синхронизируем аффирмации
            if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Uploading affirmations")
            localAffirmations.forEach { affirmation ->
                try {
                    val dto = DreamAffirmationDTO.fromDreamAffirmation(affirmation)
                    remoteRef.collection("dreamAffirmations").document(affirmation.id.toString()).set(dto).await()
                    if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Uploaded affirmation")
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) android.util.Log.w("SyncService", "Failed to sync affirmation", e)
                }
            }
            
            // Удаляем из Firebase элементы, которые были удалены локально
            val localSphereIds = localSpheres.map { it.id }.toSet()
            val localTaskIds = localTasks.map { it.id }.toSet()
            val localGoalIds = localGoals.map { it.id }.toSet()
            val localFolderIds = localFolders.map { it.id }.toSet()
            val localNoteIds = allLocalNotes.map { it.id }.toSet()
            val localAffirmationIds = localAffirmations.map { it.id }.toSet()
            
            // Удаляем сферы
            val remoteSpheresSnapshot = remoteRef.collection("spheres").get().await()
            remoteSpheresSnapshot.documents.forEach { doc ->
                val sphereId = doc.id.toLongOrNull()
                if (sphereId != null && sphereId !in localSphereIds) {
                    try {
                        doc.reference.delete().await()
                        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Deleted sphere from Firebase (was deleted locally)")
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) android.util.Log.w("SyncService", "Error deleting sphere from Firebase", e)
                    }
                }
            }
            
            // Удаляем задачи
            val remoteTasksSnapshot = remoteRef.collection("tasks").get().await()
            remoteTasksSnapshot.documents.forEach { doc ->
                val taskId = doc.id.toLongOrNull()
                if (taskId != null && taskId !in localTaskIds) {
                    try {
                        doc.reference.delete().await()
                        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Deleted task from Firebase (was deleted locally)")
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) android.util.Log.w("SyncService", "Error deleting task from Firebase", e)
                    }
                }
            }
            
            // Удаляем цели
            val remoteGoalsSnapshot = remoteRef.collection("goals").get().await()
            remoteGoalsSnapshot.documents.forEach { doc ->
                val goalId = doc.id.toIntOrNull()
                if (goalId != null && goalId !in localGoalIds) {
                    try {
                        doc.reference.delete().await()
                        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Deleted goal from Firebase (was deleted locally)")
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) android.util.Log.w("SyncService", "Error deleting goal from Firebase", e)
                    }
                }
            }
            
            // Удаляем папки
            val remoteFoldersSnapshot = remoteRef.collection("ideaFolders").get().await()
            remoteFoldersSnapshot.documents.forEach { doc ->
                val folderId = doc.id.toLongOrNull()
                if (folderId != null && folderId !in localFolderIds) {
                    try {
                        doc.reference.delete().await()
                        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Deleted folder from Firebase (was deleted locally)")
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) android.util.Log.w("SyncService", "Error deleting folder from Firebase", e)
                    }
                }
            }
            
            // Удаляем заметки
            val remoteNotesSnapshot = remoteRef.collection("ideaNotes").get().await()
            remoteNotesSnapshot.documents.forEach { doc ->
                val noteId = doc.id.toLongOrNull()
                if (noteId != null && noteId !in localNoteIds) {
                    try {
                        doc.reference.delete().await()
                        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Deleted note from Firebase (was deleted locally)")
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) android.util.Log.w("SyncService", "Error deleting note from Firebase", e)
                    }
                }
            }
            
            // Удаляем аффирмации
            val remoteAffirmationsSnapshot = remoteRef.collection("dreamAffirmations").get().await()
            remoteAffirmationsSnapshot.documents.forEach { doc ->
                val affirmationId = doc.id.toLongOrNull()
                if (affirmationId != null && affirmationId !in localAffirmationIds) {
                    try {
                        doc.reference.delete().await()
                        if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Deleted affirmation from Firebase (was deleted locally)")
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) android.util.Log.w("SyncService", "Error deleting affirmation from Firebase", e)
                    }
                }
            }
            
            if (BuildConfig.DEBUG) android.util.Log.d("SyncService", "Upload completed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.e("SyncService", "Upload failed", e)
            Result.failure(e)
        }
    }
}

