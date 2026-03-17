package com.example.mylife.lifebalance.utils

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.example.lifebalance.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

object ImageStorageHelper {
    private const val IMAGES_DIR = "goal_images"
    
    /**
     * Копирует фото из исходного URI в приватное хранилище приложения
     * и возвращает новый URI, который будет доступен после перезапуска
     */
    suspend fun copyImageToPrivateStorage(
        context: Context,
        sourceUri: Uri
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            // Создаем директорию для изображений, если её нет
            val imagesDir = File(context.filesDir, IMAGES_DIR)
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }
            
            // Создаем уникальное имя файла
            val fileName = "goal_${System.currentTimeMillis()}_${(0..1000).random()}.jpg"
            val destFile = File(imagesDir, fileName)
            
            // Копируем файл
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            // Возвращаем FileProvider URI для Android 7.0+ или file:// для старых версий
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    destFile
                )
            } else {
                Uri.fromFile(destFile)
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e("ImageStorageHelper", "copyImageToPrivateStorage failed", e)
            }
            null
        }
    }
    
    /**
     * Получает InputStream для чтения изображения из URI
     */
    suspend fun getImageInputStream(
        context: Context,
        uriString: String
    ): InputStream? = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e("ImageStorageHelper", "getImageInputStream failed", e)
            }
            null
        }
    }
    
    /**
     * Удаляет изображение из приватного хранилища
     */
    suspend fun deleteImageFromStorage(
        context: Context,
        uriString: String
    ) = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriString)
            // Если это FileProvider URI, извлекаем путь к файлу
            if (uri.scheme == "content" && uri.authority?.contains(context.packageName) == true) {
                // Пытаемся извлечь путь из URI
                val path = uri.path ?: return@withContext
                val fileName = path.substringAfterLast("/")
                val file = File(context.filesDir, "$IMAGES_DIR/$fileName")
                if (file.exists()) {
                    file.delete()
                }
            } else if (uri.scheme == "file") {
                val file = File(uri.path ?: return@withContext)
                if (file.exists()) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e("ImageStorageHelper", "deleteImageFromStorage failed", e)
            }
        }
    }
    
    /**
     * Проверяет, является ли URI файлом в приватном хранилище приложения
     */
    fun isPrivateStorageUri(context: Context, uriString: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            uri.authority?.contains(context.packageName) == true ||
            (uri.scheme == "file" && uri.path?.contains(context.filesDir.path) == true)
        } catch (e: Exception) {
            false
        }
    }
}














