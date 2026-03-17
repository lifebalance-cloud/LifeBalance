package com.example.mylife.lifebalance.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun LifeBalanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorSchemeName: String = "purple",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = getColorScheme(
        darkTheme = darkTheme,
        colorSchemeName = colorSchemeName,
        dynamicColor = dynamicColor
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun getColorScheme(
    darkTheme: Boolean,
    colorSchemeName: String,
    dynamicColor: Boolean
): ColorScheme {
    val context = LocalContext.current
    
    // Если включён динамический цвет (системная тема)
    if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    
    // Выбор цветовой схемы
    return when (colorSchemeName) {
        "blue" -> if (darkTheme) BlueDarkColorScheme else BlueLightColorScheme
        "green" -> if (darkTheme) GreenDarkColorScheme else GreenLightColorScheme
        "purple" -> if (darkTheme) PurpleDarkColorScheme else PurpleLightColorScheme
        "dark" -> if (darkTheme) DarkGreyColorScheme else DarkGreyColorScheme
        "orange", "system" -> if (darkTheme) OrangeDarkColorScheme else OrangeLightColorScheme
        else -> if (darkTheme) OrangeDarkColorScheme else OrangeLightColorScheme
    }
}