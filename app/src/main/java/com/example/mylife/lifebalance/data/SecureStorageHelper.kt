package com.example.mylife.lifebalance.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.Base64

/**
 * Хелпер для шифрованного хранения: MasterKey в Android Keystore,
 * ключ БД и чувствительные настройки — в EncryptedSharedPreferences.
 */
object SecureStorageHelper {

    private const val ENCRYPTED_PREFS_SYNC = "app_settings_sync_enc"
    private const val ENCRYPTED_PREFS_DB_KEY = "secure_db_key"
    private const val KEY_DB_PASSPHRASE = "db_passphrase_b64"

    /**
     * MasterKey для EncryptedSharedPreferences (хранится в Android Keystore).
     */
    fun getMasterKey(context: Context): MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    /**
     * EncryptedSharedPreferences для синхронных настроек (язык, тема).
     * Используется в attachBaseContext и при сохранении из DataStore.
     */
    fun getEncryptedSyncPrefs(context: Context): SharedPreferences {
        return EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_SYNC,
            getMasterKey(context),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Есть ли сохранённый пароль для SQLCipher (уже мигрировали или новый пользователь).
     */
    fun hasDbPassphrase(context: Context): Boolean {
        return try {
            getDbKeyPrefs(context).contains(KEY_DB_PASSPHRASE)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Получить или создать пароль для SQLCipher. Хранится в EncryptedSharedPreferences.
     * Ключ не хранится в открытом виде в коде.
     */
    fun getOrCreateDbPassphrase(context: Context): ByteArray {
        val prefs = getDbKeyPrefs(context)
        val existing = prefs.getString(KEY_DB_PASSPHRASE, null)
        if (!existing.isNullOrEmpty()) {
            return Base64.getDecoder().decode(existing)
        }
        val passphrase = ByteArray(32).apply { java.security.SecureRandom().nextBytes(this) }
        prefs.edit().putString(KEY_DB_PASSPHRASE, Base64.getEncoder().encodeToString(passphrase)).apply()
        return passphrase
    }

    private fun getDbKeyPrefs(context: Context): SharedPreferences {
        return EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_DB_KEY,
            getMasterKey(context),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
