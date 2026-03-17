package com.example.mylife.lifebalance.data

import androidx.room.Entity
import androidx.room.PrimaryKey
// Вместо: import java.time.LocalDate
import org.threeten.bp.LocalDate

@Entity()
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sphereId: Int,
    val text: String,
    val deadline: LocalDate,
    val photoUri: String? = null, // Для обратной совместимости
    val photoUris: String? = null, // Список URI через разделитель "|||"
    val link: String? = null,
    val checked: Boolean = false // Состояние зачеркнутости цели
)

// Функции-хелперы для работы со списком фото
private const val PHOTO_SEPARATOR = "|||"
private const val MAX_PHOTOS = 3

fun Goal.getPhotoUrisList(): List<String> {
    val uris = mutableListOf<String>()
    // Поддержка старого формата (photoUri)
    photoUri?.takeIf { it.isNotBlank() }?.let { uris.add(it) }
    // Новый формат (photoUris)
    photoUris?.takeIf { it.isNotBlank() }?.let { 
        uris.addAll(it.split(PHOTO_SEPARATOR).filter { uri -> uri.isNotBlank() })
    }
    return uris.distinct().take(MAX_PHOTOS)
}

fun List<String>.toPhotoUrisString(): String? {
    return if (isEmpty()) null else take(MAX_PHOTOS).joinToString(PHOTO_SEPARATOR)
}
