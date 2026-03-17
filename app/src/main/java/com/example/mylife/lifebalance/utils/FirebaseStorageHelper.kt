package com.example.mylife.lifebalance.utils

import android.content.Context
import android.net.Uri
import com.example.lifebalance.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.InputStream

object FirebaseStorageHelper {
    private fun getStorage(context: Context): FirebaseStorage? {
        return try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseStorage.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.w("FirebaseStorageHelper", "Firebase Storage not available: ${e.message}")
            null
        }
    }
    
    private fun getAuth(context: Context): FirebaseAuth? {
        return try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseAuth.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Загружает фото в Firebase Storage
     * @param context Контекст приложения
     * @param localUri Локальный URI файла
     * @param storagePath Путь в Firebase Storage (например, "dream_photos/sector_0/photo_1.jpg")
     * @return Путь в Firebase Storage или null в случае ошибки
     */
    suspend fun uploadPhoto(
        context: Context,
        localUri: Uri,
        storagePath: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val storage = getStorage(context) ?: return@withContext null
            val auth = getAuth(context)
            
            // Если пользователь не авторизован, не загружаем в Firebase
            if (auth?.currentUser == null) {
                if (BuildConfig.DEBUG) android.util.Log.d("FirebaseStorageHelper", "User not authenticated, skipping Firebase upload")
                return@withContext null
            }
            
            val storageRef = storage.reference.child(storagePath)
            
            // Читаем файл из локального хранилища
            val inputStream = context.contentResolver.openInputStream(localUri)
                ?: return@withContext null
            
            // Загружаем в Firebase Storage
            val uploadTask = storageRef.putStream(inputStream).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await()
            
            if (BuildConfig.DEBUG) android.util.Log.d("FirebaseStorageHelper", "Photo uploaded successfully: $storagePath")
            downloadUrl.toString()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.e("FirebaseStorageHelper", "Error uploading photo: ${e.message}", e)
            null
        }
    }
    
    /**
     * Удаляет фото из Firebase Storage
     * @param storagePath Путь в Firebase Storage
     */
    suspend fun deletePhoto(context: Context, storagePath: String) = withContext(Dispatchers.IO) {
        try {
            val storage = getStorage(context) ?: return@withContext
            val auth = getAuth(context)
            
            if (auth?.currentUser == null) {
                return@withContext
            }
            
            val storageRef = storage.reference.child(storagePath)
            storageRef.delete().await()
            if (BuildConfig.DEBUG) android.util.Log.d("FirebaseStorageHelper", "Photo deleted from Firebase: $storagePath")
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.w("FirebaseStorageHelper", "Error deleting photo from Firebase: ${e.message}")
        }
    }
    
    /**
     * Генерирует путь в Firebase Storage для фото сектора
     * @param userId ID пользователя
     * @param sectorId ID сектора
     * @param photoOrder Порядок фото (0 или 1)
     * @return Путь в Firebase Storage
     */
    fun generateStoragePath(userId: String, sectorId: Int, photoOrder: Int): String {
        return "dream_photos/$userId/sector_$sectorId/photo_$photoOrder.jpg"
    }
}

