package com.example.mylife.lifebalance

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mylife.lifebalance.data.AppDatabase
import com.example.mylife.lifebalance.data.AppSettingsDataStore
import com.example.mylife.lifebalance.data.getSyncPrefsForAttachment
import com.example.mylife.lifebalance.repository.AuthRepository
import com.example.mylife.lifebalance.repository.LifeBalanceRepository
import com.example.mylife.lifebalance.repository.SyncService
import com.example.mylife.lifebalance.ui.AppNavHost
import com.example.mylife.lifebalance.ui.theme.LifeBalanceTheme
import com.example.mylife.lifebalance.viewmodel.LifeBalanceViewModel
import com.example.mylife.lifebalance.viewmodel.LifeBalanceViewModelFactory
import com.example.lifebalance.BuildConfig
import com.google.firebase.FirebaseApp
import com.jakewharton.threetenabp.AndroidThreeTen
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context?) {
        // Двухступенчатая логика локализации:
        // 1. Если пользователь уже выбирал язык - используем сохраненный
        // 2. Если это первый запуск - используем язык системы
        val prefs = newBase?.let { getSyncPrefsForAttachment(it) }
        val savedLanguage = prefs?.getString("language", null)
        
        // Получаем язык системы из конфигурации ресурсов
        // Важно: используем конфигурацию ресурсов, а не Locale.getDefault(),
        // так как Locale.getDefault() может быть изменен предыдущими вызовами
        val systemLanguage = newBase?.resources?.configuration?.locales?.get(0)?.language
            ?: Locale.getDefault().language
        
        if (BuildConfig.DEBUG) {
            android.util.Log.e("MainActivity", "=== LANGUAGE DEBUG: System language detected: $systemLanguage ===")
            android.util.Log.e("MainActivity", "=== LANGUAGE DEBUG: System locale full: ${newBase?.resources?.configuration?.locales?.get(0)} ===")
            android.util.Log.e("MainActivity", "=== LANGUAGE DEBUG: Saved language was: $savedLanguage ===")
        }
        
        val supportedLanguages = listOf("ru", "uk", "en", "de", "fr", "es")
        
        // Проверяем, был ли язык явно выбран пользователем
        val wasLanguageExplicitlySet = prefs?.getBoolean("language_explicitly_set", false) ?: false
        
        // Проверяем, является ли это первым запуском после установки/переустановки
        // Используем версию кода приложения для определения первого запуска
        val currentVersionCode = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                newBase?.packageManager?.getPackageInfo(newBase.packageName, 0)?.longVersionCode ?: 0L
            } else {
                @Suppress("DEPRECATION")
                newBase?.packageManager?.getPackageInfo(newBase.packageName, 0)?.versionCode?.toLong() ?: 0L
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.w("MainActivity", "Failed to get version code: ${e.message}")
            0L
        }
        val lastVersionCode = prefs?.getLong("last_version_code", 0L) ?: 0L
        val isFirstLaunchAfterInstall = currentVersionCode != lastVersionCode && currentVersionCode > 0L
        
        // Если это первый запуск после установки/переустановки, очищаем настройки языка
        if (isFirstLaunchAfterInstall) {
            if (lastVersionCode > 0L) {
                // Это переустановка - сбрасываем язык на системный
                if (BuildConfig.DEBUG) android.util.Log.w("MainActivity", "=== LANGUAGE DEBUG: First launch after reinstall detected (current=$currentVersionCode, last=$lastVersionCode), resetting language to system ===")
                prefs?.edit()?.remove("language")?.remove("language_explicitly_set")?.putLong("last_version_code", currentVersionCode)?.apply()
            } else {
                // Первая установка - сохраняем версию
                if (BuildConfig.DEBUG) android.util.Log.d("MainActivity", "=== LANGUAGE DEBUG: First install detected, saving version code: $currentVersionCode ===")
                prefs?.edit()?.putLong("last_version_code", currentVersionCode)?.apply()
            }
        }
        
        // Определяем язык для использования
        val language = when {
            // Если язык был явно выбран пользователем и он валидный И это не первый запуск после переустановки - используем его
            savedLanguage != null && 
            savedLanguage.isNotEmpty() && 
            savedLanguage in supportedLanguages && 
            wasLanguageExplicitlySet &&
            !isFirstLaunchAfterInstall -> {
                if (BuildConfig.DEBUG) android.util.Log.e("MainActivity", "=== LANGUAGE DEBUG: Using explicitly saved language: $savedLanguage ===")
                savedLanguage
            }
            // Во всех остальных случаях (первый запуск, переустановка, невалидный язык) - используем язык системы
            else -> {
                // Очищаем невалидный или неявно установленный язык
                if (savedLanguage != null && savedLanguage.isNotEmpty()) {
                    if (savedLanguage !in supportedLanguages || !wasLanguageExplicitlySet || isFirstLaunchAfterInstall) {
                        if (BuildConfig.DEBUG) android.util.Log.w("MainActivity", "=== LANGUAGE DEBUG: Clearing invalid/auto-saved language or first launch: '$savedLanguage' ===")
                        prefs?.edit()?.remove("language")?.remove("language_explicitly_set")?.apply()
                    }
                }
                
                // Используем язык системы, проверяя поддержку
                val finalLanguage = if (systemLanguage in supportedLanguages) {
                    systemLanguage
                } else {
                    if (BuildConfig.DEBUG) android.util.Log.w("MainActivity", "=== LANGUAGE DEBUG: System language $systemLanguage not supported, using fallback: en ===")
                    "en" // Fallback на английский, если язык системы не поддерживается
                }
                if (BuildConfig.DEBUG) android.util.Log.e("MainActivity", "=== LANGUAGE DEBUG: Using system language: $finalLanguage (wasLanguageExplicitlySet=$wasLanguageExplicitlySet, savedLanguage=$savedLanguage, isFirstLaunch=$isFirstLaunchAfterInstall) ===")
                finalLanguage
            }
        }
        
        val locale = Locale(language)
        Locale.setDefault(locale)
        
        val config = Configuration(newBase?.resources?.configuration)
        config.setLocale(locale)
        
        if (BuildConfig.DEBUG) android.util.Log.e("MainActivity", "=== LANGUAGE DEBUG: Final locale set: ${locale.language} ===")
        
        super.attachBaseContext(newBase?.createConfigurationContext(config))
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (BuildConfig.DEBUG) {
            android.util.Log.e("MainActivity", "=== LANGUAGE DEBUG: onCreate called ===")
            android.util.Log.e("MainActivity", "=== LANGUAGE DEBUG: Current locale: ${resources.configuration.locales[0]} ===")
        }

        // Инициализация Firebase (безопасно, если google-services.json отсутствует)
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                android.util.Log.w("MainActivity", "Firebase initialization failed: ${e.message}")
                android.util.Log.w("MainActivity", "App will work in offline mode. Please configure Firebase to enable authentication and sync.")
            }
        }

        // Инициализация threetenabp
        AndroidThreeTen.init(this)

        // Инициализация базы данных
        val database = AppDatabase.getDatabase(applicationContext)

        // === LifeBalanceRepository с контекстом ===
        val repository = LifeBalanceRepository(
            context = this,                       // <-- Context передаём первым
            lifeSphereDao = database.lifeSphereDao(),
            taskDao = database.taskDao(),
            goalDao = database.goalDao(),
            ideaFolderDao = database.ideaFolderDao(),
            ideaNoteDao = database.ideaNoteDao(),
            dreamSectorPhotoDao = database.dreamSectorPhotoDao(),
            dreamAffirmationDao = database.dreamAffirmationDao()
        )

        // === AuthRepository ===
        val authRepository = AuthRepository(
            context = this,
            userDao = database.userDao()
        )

        // === SyncService ===
        val syncService = SyncService(
            context = this,
            repository = repository,
            authRepository = authRepository
        )

        // Фабрика для ViewModel
        val settingsDataStore = AppSettingsDataStore(this)
        val viewModelFactory = LifeBalanceViewModelFactory(repository, syncService, settingsDataStore)

        // Загружаем сохраненную тему синхронно до рендеринга (то же хранилище, что и язык)
        val syncPrefs = getSyncPrefsForAttachment(this)
        
        // Инициализация языка при первом запуске - используем язык системы
        // Не устанавливаем принудительно, чтобы сохранить двухступенчатую логику
        // (язык будет определен в attachBaseContext)
        
        // Инициализация темы по умолчанию при первом запуске
        if (!syncPrefs.contains("color_scheme")) {
            syncPrefs.edit().putString("color_scheme", "purple").apply()
        }
        
        // Загружаем тему синхронно из SharedPreferences
        val initialTheme = settingsDataStore.getThemeSync()

        setContent {
            val context = LocalContext.current
            val settingsDataStoreCompose = remember { AppSettingsDataStore(context) }
            
            // Используем синхронно загруженную тему как начальное значение
            val selectedTheme by settingsDataStoreCompose.selectedTheme.collectAsState(initial = initialTheme)
            val darkTheme = isSystemInDarkTheme()
            
            LifeBalanceTheme(
                darkTheme = darkTheme,
                colorSchemeName = selectedTheme,
                dynamicColor = selectedTheme == "system"
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: LifeBalanceViewModel = viewModel(factory = viewModelFactory)
                    AppNavHost(
                        viewModel = viewModel,
                        authRepository = authRepository,
                        syncService = syncService
                    )
                }
            }
        }
    }
}
