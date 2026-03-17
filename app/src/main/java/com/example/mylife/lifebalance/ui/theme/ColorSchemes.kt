package com.example.mylife.lifebalance.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Error colors (red) - объявлены в начале, так как используются во всех схемах
val ErrorRed = Color(0xFFBA1A1A) // красный для light темы
val ErrorRedDark = Color(0xFFCF6679) // красный для dark темы

// PURPLE СВЕТЛАЯ ТЕМА
val PurpleBase = Color(0xFFf5eef6)
val PurpleBackground = Color(0xFFffffff)
val PurpleSurface = Color(0xFFf5eef6)
val PurpleSurfaceSoft = Color(0xFFebddee)
val PurplePrimary = Color(0xFFa98eaf)
val PurplePrimaryDark = Color(0xFF7E6AA0)
val PurpleSecondary = Color(0xFFc2a8c7)
val PurpleSecondaryDark = Color(0xFFCCC1E6)
val PurpleOutline = Color(0xFFd6cad8)
val TextMainPurple = Color(0xFF2F2C3A)
val TextSecondaryPurple = Color(0xFF6f5f72)

val PurpleLightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    onPrimary = TextMainPurple,
    primaryContainer = PurpleSecondary,
    onPrimaryContainer = TextMainPurple,
    secondary = PurpleSurfaceSoft,
    onSecondary = TextSecondaryPurple,
    secondaryContainer = PurpleOutline,
    onSecondaryContainer = TextMainPurple,
    background = PurpleBackground,
    onBackground = TextMainPurple,
    surface = PurpleSurface,
    onSurface = TextMainPurple,
    outline = PurpleOutline,
    error = ErrorRed,
    onError = Color(0xFFFFFFFF),
    tertiaryContainer=PurpleBase,
    onTertiary = TextMainPurple,
    tertiary = PurpleBackground
)
// PURPLE ТЕМНАЯ ТЕМА
val PurpleDarkBase = Color(0xFF1B1620)
val PurpleDarkBackground = Color(0xFF121014)
val PurpleDarkSurface = Color(0xFF1F1A24)
val PurpleDarkSurfaceSoft = Color(0xFF2A2330)
val PurpleDarkPrimary = Color(0xFF975eab) //FFB8A1C0
val PurpleDarkPrimaryDark = Color(0xFF8F7AAE)
val PurpleDarkSecondary = Color(0xFF9E87A6)
val PurpleDarkOutline = Color(0xFF3B3342)
val TextMainPurpleDark = Color(0xFFEAE6F0)
val TextSecondaryPurpleDark = Color(0xFFB8AEC2)
val PurpleDarkColorScheme = darkColorScheme(
    primary = PurpleDarkPrimary,
    onPrimary = TextMainPurpleDark,
    primaryContainer = PurpleDarkSecondary,
    onPrimaryContainer = PurpleDarkBase,
    secondary = PurpleDarkSurfaceSoft,
    onSecondary = TextSecondaryPurpleDark,
    secondaryContainer = PurpleDarkOutline,
    onSecondaryContainer = TextMainPurpleDark,
    background = PurpleDarkBackground,
    onBackground = TextMainPurpleDark,
    surface = PurpleDarkSurface,
    onSurface = TextMainPurpleDark,
    outline = PurpleDarkOutline,
    error = ErrorRed,
    onError = Color(0xFF000000),
    tertiaryContainer = PurpleDarkBase,
    onTertiary = PurpleDarkPrimaryDark,
    tertiary = PurpleDarkOutline
)

// BLUE СВЕТЛАЯ ТЕМА
val BlueBase= Color(0xFFf2f5ff)
val BlueBackground = Color(0xFFffffff)
val BlueSurface = Color(0xFFf2f5ff)
val BlueSurfaceSoft = Color(0xFFdde1ee)
val BluePrimary = Color(0xFF8f9cc4)
val BluePrimaryDark = Color(0xFFc2dded)
val BlueSecondary = Color(0xFFa4b3db)
val BlueSecondaryDark = Color(0xFFBFD9E8)
val BlueOutline = Color(0xFFc5cce6)
val TextMainBlue = Color(0xFF2F3E4A)
val TextSecondaryBlue = Color(0xFF5f6472)

val BlueLightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = TextMainBlue,
    primaryContainer = BlueSecondary,
    onPrimaryContainer = TextMainBlue,
    secondary = BlueSurfaceSoft,
    onSecondary = TextSecondaryBlue,
    secondaryContainer = BlueOutline,
    onSecondaryContainer = TextMainBlue,
    background = BlueBackground,
    onBackground = TextMainBlue,
    surface = BlueSurface,
    onSurface = TextMainBlue,
    outline = BlueOutline,
    error = ErrorRed,
    onError = Color(0xFFFFFFFF),
    tertiaryContainer=BlueBase,
    onTertiary = TextMainBlue,
    tertiary =BlueBackground
)
// BLUE ТЕМНАЯ ТЕМА
val BlueDarkBase = Color(0xFF161A24)
val BlueDarkBackground = Color(0xFF0F121A)
val BlueDarkSurface = Color(0xFF1A1F2B)
val BlueDarkSurfaceSoft = Color(0xFF222838)
val BlueDarkPrimary = Color(0xFF566aa8 ) //FFA8B8E8
val BlueDarkPrimaryDark = Color(0xFF7F93C8)
val BlueDarkSecondary = Color(0xFF8FA4D6)
val BlueDarkSecondaryDark = Color(0xFF6F84B5)
val BlueDarkOutline = Color(0xFF31384A)
val TextMainBlueDark = Color(0xFFE6EBFF)
val TextSecondaryBlueDark = Color(0xFFB4BDD6)

val BlueDarkColorScheme = darkColorScheme(
    primary = BlueDarkPrimary,
    onPrimary = TextMainBlueDark,
    primaryContainer = BlueDarkSecondary,
    onPrimaryContainer = BlueDarkBase,
    secondary = BlueDarkSurfaceSoft,
    onSecondary = TextSecondaryBlueDark,
    secondaryContainer = BlueDarkOutline,
    onSecondaryContainer = TextMainBlueDark,
    background = BlueDarkBackground,
    onBackground = TextMainBlueDark,
    surface = BlueDarkSurface,
    onSurface = TextMainBlueDark,
    outline = BlueDarkOutline,
    error = ErrorRed,
    onError = Color(0xFF000000),
    tertiaryContainer = BlueDarkBase,
    onTertiary = BlueDarkPrimaryDark,
    tertiary = BlueDarkOutline
)

// Green Theme СВЕТЛАЯ ТЕМА
val GreenBase=Color(0xFFEFF7F3)
val Lime43 = Color(0xFFffffff)//white color
val Mint = Color(0xFFA9C8B6)
val SagePrimary = Color(0xFF8FAF9F)//подходит для топбара, кнопок, иконок
val SageDark = Color(0xFF6F8F80)//для выделений, активных элементов
val MintSoft = Color(0xFFDDEEE6)//для акцентных кнопок, чипов, селекторов
val MintGray = Color(0xFFCBD9D2)//для границ, контейнеров
val BackgroundLight = Color(0xFFF2F5F3)//для основного фона приложения
val SurfaceSoft = Color(0xFFEFF7F3)//приятные контейнеры для списков, карточек задач
val TextMain = Color(0xFF2E3D39)//Тёмный текст
val TextSecondary = Color(0xFF5F726C)//Второстепенный текст (описания)
val ButtonPrimary = Color(0xFF6F8F80)//тёмно-шалфейный, контрастный но мягкий
val ButtonSecondary = Color(0xFFE2F0EA)//нежно-мятная

val GreenLightColorScheme = lightColorScheme(
    primary = SagePrimary,
    onPrimary = TextMain,
    primaryContainer = Mint,
    onPrimaryContainer = TextMain,
    secondary = MintSoft,
    onSecondary = TextSecondary,
    secondaryContainer = MintGray,
    onSecondaryContainer =TextMain,
    background = Lime43,
    onBackground = TextMain,
    surface = SurfaceSoft,
    onSurface = TextMain,
    outline = MintGray,
    error = ErrorRed,
    onError = Color(0xFFFFFFFF),
    tertiaryContainer=GreenBase,
    onTertiary = TextMain,
    tertiary = Lime43
)

// GREEN DARK THEME COLORS
val GreenDarkBase = Color(0xFF121917)
val GreenDarkBackground = Color(0xFF0E1412)
val GreenDarkSurface = Color(0xFF18201D)
val GreenDarkSurfaceSoft = Color(0xFF1F2A26)
val GreenDarkPrimary = Color(0xFF509675)  //FF8FB5A3
val GreenDarkPrimaryDark = Color(0xFF6F8F80)
val GreenDarkSecondary = Color(0xFFA9C8B6)
val GreenDarkOutline = Color(0xFF2F3D37)
val TextMainGreenDark = Color(0xFFE7F1ED)
val TextSecondaryGreenDark = Color(0xFFB7C9C1)

val GreenDarkColorScheme = darkColorScheme(
    primary = GreenDarkPrimary,
    onPrimary = TextMainGreenDark,
    primaryContainer = GreenDarkSecondary,
    onPrimaryContainer = GreenDarkBase,
    secondary = GreenDarkSurfaceSoft,
    onSecondary = TextSecondaryGreenDark,
    secondaryContainer = GreenDarkOutline,
    onSecondaryContainer = TextMainGreenDark,
    background = GreenDarkBackground,
    onBackground = TextMainGreenDark,
    surface = GreenDarkSurface,
    onSurface = TextMainGreenDark,
    outline = GreenDarkOutline,
    error = ErrorRed,
    onError = Color(0xFF000000),
    tertiaryContainer = GreenDarkBase,
    tertiary = GreenDarkOutline,
    onTertiary = GreenDarkPrimaryDark
)

//ОРАНЖЕВАЯ ТЕМА

val PeachBase = Color(0xFFFFf5e7)
val PeachBackground = Color(0xFFffffff)
val PeachSurface = Color(0xFFf4f4f4)  //light grey
val PeachSurfaceSoft = Color(0xFFFFEED0)
val OrangePrimary = Color(0xFFffb959)
val OrangePrimaryDark = Color(0xFFE6B05C)
val PeachSecondary = Color(0xFFfece97)
val PeachSecondaryDark = Color(0xFFEFCF95)
val PeachOutline = Color(0xFFdcdcdc) //grey
val TextMainWarm = Color(0xFF4A3A24)
val TextSecondaryWarm = Color(0xFF7A6545)

val OrangeLightColorScheme = lightColorScheme(
    primary = OrangePrimary,
    onPrimary = TextMainWarm,
    primaryContainer = PeachSecondary,
    onPrimaryContainer = TextMainWarm,
    tertiaryContainer=PeachBase,
    secondary = PeachSurfaceSoft,
    onSecondary = TextSecondaryWarm,
    secondaryContainer = PeachOutline,
    onSecondaryContainer = TextMainWarm,
    background = PeachBackground,
    onBackground = TextMainWarm,
    surface = PeachSurface,
    onSurface = TextMainWarm,
    outline = PeachOutline,
    error = ErrorRed,
    onError = Color.White,
    onTertiary = TextMainWarm,
    tertiary = PeachBackground
)
// ORANGE + GREY DARK THEME COLORS
val PeachDarkBase = Color(0xFF1A1612)
val PeachDarkBackground = Color(0xFF12100E)
val PeachDarkSurface = Color(0xFF1F1C18)
val PeachDarkSurfaceSoft = Color(0xFF2A241D)
val OrangeDarkPrimary = Color(0xFFf09f26)
val OrangeDarkPrimaryDark = Color(0xFFE6B05C)
val PeachDarkSecondary = Color(0xFFf0b360)
val PeachDarkSecondaryDark = Color(0xFFEFCF95)
val PeachDarkOutline = Color(0xFF3A342D)
val TextMainWarmDark = Color(0xFFFFF3E0)
val TextSecondaryWarmDark = Color(0xFFCDBA9E)

val OrangeDarkColorScheme = darkColorScheme(
    primary = OrangeDarkPrimary,
    onPrimary = TextMainWarmDark,
    primaryContainer = PeachDarkSecondary,
    onPrimaryContainer = PeachDarkBase,
    secondary = PeachDarkSurfaceSoft,
    onSecondary = TextSecondaryWarmDark,
    secondaryContainer = PeachDarkOutline,
    onSecondaryContainer = TextMainWarmDark,
    background = PeachDarkBackground,
    onBackground = TextMainWarmDark,
    surface = PeachDarkSurface,
    onSurface = TextMainWarmDark,
    outline = PeachDarkOutline,
    error = ErrorRed,
    onError = Color(0xFF000000),
    tertiaryContainer = PeachDarkBase,
    tertiary = PeachDarkOutline,
    onTertiary = OrangeDarkPrimaryDark
)

//ТЕМНАЯ
//  ТЕМНАЯ ТЕМА
val DarkBase = Color(0xFF212120)
val DarkBackground = Color(0xFF121014)
val DarkSurface = Color(0xFF363636)
val DarkSurfaceSoft = Color(0xFF192230)
val DarkPrimary = Color(0xFF83AF3B)
val DarkPrimaryDark = Color(0xFF7B8A51)
val DarkSecondary = Color(0xFFD7FE03)
val DarkOutline = Color(0xFF4D4D4D)
val TextMainDark = Color(0xFFFEFFFC)
val TextSecondaryDark = Color(0xFFFEFFFC)
val DarkGreyColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = TextMainDark,
    primaryContainer = DarkSecondary,
    onPrimaryContainer = DarkBase,
    secondary = DarkSurfaceSoft,
    onSecondary = TextSecondaryDark,
    secondaryContainer = DarkOutline,
    onSecondaryContainer = TextMainDark,
    background = DarkBackground,
    onBackground = TextMainDark,
    surface = DarkSurface,
    onSurface = TextMainDark,
    outline = DarkOutline,
    error = ErrorRed,
    onError = Color(0xFF000000),
    tertiaryContainer = DarkBase,
    onTertiary = DarkPrimaryDark,
    tertiary = DarkOutline
)


