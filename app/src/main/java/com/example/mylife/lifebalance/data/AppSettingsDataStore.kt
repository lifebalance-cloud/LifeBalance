package com.example.mylife.lifebalance.data

import android.content.Context
import android.content.SharedPreferences
import com.example.lifebalance.BuildConfig
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Locale

val Context.appSettingsDataStore by preferencesDataStore(name = "app_settings")

private const val MIGRATION_SYNC_DONE = "migration_plain_sync_done"

/** Однократная миграция из незашифрованных SharedPreferences в EncryptedSharedPreferences. */
private fun migrateFromPlainSyncPrefsIfNeeded(context: Context, encrypted: SharedPreferences) {
    if (encrypted.getBoolean(MIGRATION_SYNC_DONE, false)) return
    val plain = context.getSharedPreferences("app_settings_sync", Context.MODE_PRIVATE)
    val edit = encrypted.edit()
    plain.getString("color_scheme", null)?.let { edit.putString("color_scheme", it) }
    plain.getString("language", null)?.let { edit.putString("language", it) }
    edit.putBoolean("language_explicitly_set", plain.getBoolean("language_explicitly_set", false))
    edit.putBoolean(MIGRATION_SYNC_DONE, true)
    edit.apply()
}

/**
 * Возвращает SharedPreferences для синхронного чтения языка/темы (с миграцией из plain в encrypted).
 * Используется в MainActivity.attachBaseContext, LocaleUtils и LifeBalanceRepository,
 * чтобы читать из того же хранилища, куда saveLanguage/saveTheme пишут (app_settings_sync_enc).
 */
fun getSyncPrefsForAttachment(context: Context): SharedPreferences {
    val encrypted = SecureStorageHelper.getEncryptedSyncPrefs(context)
    migrateFromPlainSyncPrefsIfNeeded(context, encrypted)
    return encrypted
}

class AppSettingsDataStore(private val context: Context) {
    
    private val THEME_KEY = stringPreferencesKey("color_scheme")
    private val LANGUAGE_KEY = stringPreferencesKey("language")
    private val HAS_COMPLETED_FIRST_SIGN_IN_KEY = booleanPreferencesKey("has_completed_first_sign_in")
    private val AI_REQUESTS_DATE_KEY = stringPreferencesKey("ai_requests_date")
    private val AI_REQUESTS_COUNT_KEY = intPreferencesKey("ai_requests_count")
    private val AI_NOTES_REQUESTS_DATE_KEY = stringPreferencesKey("ai_notes_requests_date")
    private val AI_NOTES_REQUESTS_COUNT_KEY = intPreferencesKey("ai_notes_requests_count")
    private val AI_DATA_PROCESSING_CONSENT_KEY = booleanPreferencesKey("ai_data_processing_consent_granted")
    
    // EncryptedSharedPreferences для синхронного чтения языка/темы (защита от чтения с диска)
    private val prefs: SharedPreferences by lazy {
        val encrypted = SecureStorageHelper.getEncryptedSyncPrefs(context)
        migrateFromPlainSyncPrefsIfNeeded(context, encrypted)
        encrypted
    }
    
    val selectedTheme: Flow<String> = context.appSettingsDataStore.data
        .map { preferences ->
            preferences[THEME_KEY] ?: "purple"
        }
    
    val selectedLanguage: Flow<String> = context.appSettingsDataStore.data
        .map { preferences ->
            // Двухступенчатая логика: сначала сохраненный язык, потом системный
            preferences[LANGUAGE_KEY] ?: getSystemLanguage()
        }
    
    /**
     * Получает язык системы телефона из конфигурации ресурсов
     */
    fun getSystemLanguage(): String {
        // Используем конфигурацию ресурсов для получения языка системы
        // Это более надежно, чем Locale.getDefault(), который может быть изменен
        val systemLanguage = context.resources.configuration.locales[0].language
        
        // Проверяем, поддерживается ли язык системы в приложении
        // Если нет - используем английский как fallback
        val supportedLanguages = listOf("ru", "uk", "en", "de", "fr", "es")
        return if (systemLanguage in supportedLanguages) {
            systemLanguage
        } else {
            "en" // Fallback на английский, если язык системы не поддерживается
        }
    }
    
    suspend fun saveTheme(theme: String) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[THEME_KEY] = theme
        }
        // Также сохраняем в SharedPreferences для синхронного чтения
        prefs.edit().putString("color_scheme", theme).apply()
    }
    
    suspend fun saveLanguage(language: String) {
        // Сохраняем в DataStore
        context.appSettingsDataStore.edit { prefs ->
            prefs[LANGUAGE_KEY] = language
        }
        // Также сохраняем в SharedPreferences для синхронного чтения
        // И помечаем, что язык был явно выбран пользователем
        prefs.edit()
            .putString("language", language)
            .putBoolean("language_explicitly_set", true)
            .apply()
    }
    
    /**
     * Получает язык синхронно с двухступенчатой логикой:
     * 1. Если пользователь уже выбирал язык - возвращаем сохраненный
     * 2. Если это первый запуск или переустановка - возвращаем язык системы
     */
    fun getLanguageSync(): String {
        // Проверяем, был ли язык уже выбран пользователем
        val savedLanguage = prefs.getString("language", null)
        val wasLanguageExplicitlySet = prefs.getBoolean("language_explicitly_set", false)
        val supportedLanguages = listOf("ru", "uk", "en", "de", "fr", "es")
        
        return if (savedLanguage != null && 
                   savedLanguage.isNotEmpty() && 
                   savedLanguage in supportedLanguages && 
                   wasLanguageExplicitlySet) {
            // Пользователь явно выбирал язык и он валидный - используем его выбор
            savedLanguage
        } else {
            // Первый запуск, переустановка или язык не был явно выбран - используем язык системы
            // Если сохраненный язык невалидный или не был явно выбран, очищаем его
            if (savedLanguage != null && savedLanguage.isNotEmpty()) {
                if (savedLanguage !in supportedLanguages || !wasLanguageExplicitlySet) {
                    if (BuildConfig.DEBUG) android.util.Log.w("AppSettingsDataStore", "Invalid or auto-saved language detected, clearing it")
                    prefs.edit().remove("language").remove("language_explicitly_set").apply()
                }
            }
            getSystemLanguage()
        }
    }
    
    fun getThemeSync(): String {
        return prefs.getString("color_scheme", "purple") ?: "purple"
    }
    
    val hasCompletedFirstSignIn: Flow<Boolean> = context.appSettingsDataStore.data
        .map { preferences ->
            preferences[HAS_COMPLETED_FIRST_SIGN_IN_KEY] ?: false
        }
    
    suspend fun setHasCompletedFirstSignIn(completed: Boolean) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[HAS_COMPLETED_FIRST_SIGN_IN_KEY] = completed
        }
    }
    
    suspend fun getHasCompletedFirstSignInSync(): Boolean {
        return context.appSettingsDataStore.data.map { preferences ->
            preferences[HAS_COMPLETED_FIRST_SIGN_IN_KEY] ?: false
        }.first()
    }
    
    // ===== AI data processing consent (EEA/UK / GDPR) =====
    val aiDataProcessingConsentGranted: Flow<Boolean> = context.appSettingsDataStore.data
        .map { preferences ->
            preferences[AI_DATA_PROCESSING_CONSENT_KEY] ?: false
        }
    
    suspend fun setAiDataProcessingConsent(granted: Boolean) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[AI_DATA_PROCESSING_CONSENT_KEY] = granted
        }
    }
    
    suspend fun getAiDataProcessingConsent(): Boolean {
        return context.appSettingsDataStore.data.map { preferences ->
            preferences[AI_DATA_PROCESSING_CONSENT_KEY] ?: false
        }.first()
    }
    
    // ===== AI Requests Limiting =====
    private val MAX_AI_REQUESTS_PER_DAY = 5
    
    /**
     * Проверяет, можно ли сделать AI запрос (не превышен ли лимит в 3 запроса в сутки)
     * @return true если можно сделать запрос, false если лимит превышен
     */
    suspend fun canMakeAiRequest(): Boolean {
        val today = org.threeten.bp.LocalDate.now().toString()
        val preferences = context.appSettingsDataStore.data.first()
        
        val lastRequestDate = preferences[AI_REQUESTS_DATE_KEY] ?: ""
        val requestCount = preferences[AI_REQUESTS_COUNT_KEY] ?: 0
        
        // Если последний запрос был не сегодня, сбрасываем счетчик
        if (lastRequestDate != today) {
            return true
        }
        
        // Если запросы были сегодня, проверяем количество
        return requestCount < MAX_AI_REQUESTS_PER_DAY
    }
    
    /**
     * Получает количество оставшихся AI запросов на сегодня
     */
    suspend fun getRemainingAiRequests(): Int {
        val today = org.threeten.bp.LocalDate.now().toString()
        val preferences = context.appSettingsDataStore.data.first()
        
        val lastRequestDate = preferences[AI_REQUESTS_DATE_KEY] ?: ""
        val requestCount = preferences[AI_REQUESTS_COUNT_KEY] ?: 0
        
        // Если последний запрос был не сегодня, возвращаем полный лимит
        if (lastRequestDate != today) {
            return MAX_AI_REQUESTS_PER_DAY
        }
        
        return (MAX_AI_REQUESTS_PER_DAY - requestCount).coerceAtLeast(0)
    }
    
    /**
     * Регистрирует выполнение AI запроса
     */
    suspend fun registerAiRequest() {
        val today = org.threeten.bp.LocalDate.now().toString()

        context.appSettingsDataStore.edit { prefs ->
            val lastRequestDate = prefs[AI_REQUESTS_DATE_KEY] ?: ""
            val currentCount = prefs[AI_REQUESTS_COUNT_KEY] ?: 0

            if (lastRequestDate != today) {
                // Если последний запрос был не сегодня, сбрасываем счетчик
                prefs[AI_REQUESTS_DATE_KEY] = today
                prefs[AI_REQUESTS_COUNT_KEY] = 1
            } else {
                // Увеличиваем счетчик запросов за сегодня
                prefs[AI_REQUESTS_COUNT_KEY] = currentCount + 1
            }
        }
    }

    // ===== AI Notes (Ideas page) — лимит 20 запросов в сутки =====
    private val MAX_AI_NOTES_REQUESTS_PER_DAY = 20

    suspend fun canMakeAiNotesRequest(): Boolean {
        val today = org.threeten.bp.LocalDate.now().toString()
        val preferences = context.appSettingsDataStore.data.first()
        val lastRequestDate = preferences[AI_NOTES_REQUESTS_DATE_KEY] ?: ""
        val requestCount = preferences[AI_NOTES_REQUESTS_COUNT_KEY] ?: 0
        if (lastRequestDate != today) return true
        return requestCount < MAX_AI_NOTES_REQUESTS_PER_DAY
    }

    suspend fun registerAiNotesRequest() {
        val today = org.threeten.bp.LocalDate.now().toString()
        context.appSettingsDataStore.edit { prefs ->
            val lastRequestDate = prefs[AI_NOTES_REQUESTS_DATE_KEY] ?: ""
            val currentCount = prefs[AI_NOTES_REQUESTS_COUNT_KEY] ?: 0
            if (lastRequestDate != today) {
                prefs[AI_NOTES_REQUESTS_DATE_KEY] = today
                prefs[AI_NOTES_REQUESTS_COUNT_KEY] = 1
            } else {
                prefs[AI_NOTES_REQUESTS_COUNT_KEY] = currentCount + 1
            }
        }
    }
}

