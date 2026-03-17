package com.example.mylife.lifebalance.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.mylife.lifebalance.data.getSyncPrefsForAttachment
import java.util.Locale

// Вспомогательная функция для получения локали приложения (Composable)
@Composable
fun getAppLocale(): Locale {
    val context = LocalContext.current
    val language = remember {
        // Двухступенчатая логика локализации (читаем из того же хранилища, куда пишет saveLanguage)
        val prefs = getSyncPrefsForAttachment(context)
        val savedLanguage = prefs.getString("language", null)
        if (savedLanguage != null && savedLanguage.isNotEmpty()) {
            savedLanguage
        } else {
            val systemLanguage = context.resources.configuration.locales[0].language
            val supportedLanguages = listOf("ru", "uk", "en", "de", "fr", "es")
            if (systemLanguage in supportedLanguages) systemLanguage else "en"
        }
    }
    return Locale(language)
}

// Вспомогательная функция для получения локали приложения (не-Composable, для использования в onClick и т.д.)
fun getAppLocale(context: android.content.Context): Locale {
    val prefs = getSyncPrefsForAttachment(context)
    val savedLanguage = prefs.getString("language", null)
    val language = if (savedLanguage != null && savedLanguage.isNotEmpty()) {
        savedLanguage
    } else {
        val systemLanguage = context.resources.configuration.locales[0].language
        val supportedLanguages = listOf("ru", "uk", "en", "de", "fr", "es")
        if (systemLanguage in supportedLanguages) systemLanguage else "en"
    }
    return Locale(language)
}

