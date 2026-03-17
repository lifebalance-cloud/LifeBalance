import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("org.jetbrains.kotlin.plugin.compose") // плагин Compose Compiler
}

// Контакты поддержки из local.properties (плейсхолдеры по умолчанию)
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.reader())
}
fun contactProp(key: String, placeholder: String): String =
    (localProperties.getProperty(key) ?: placeholder).replace("\\", "\\\\").replace("\"", "\\\"")

android {
    namespace = "com.example.lifebalance"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.mylife.lifebalance"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        buildConfigField("String", "CONTACT_EMAIL", "\"${contactProp("contact_email", "contact_email")}\"")
        buildConfigField("String", "CONTACT_TELEGRAM", "\"${contactProp("contact_telegram", "contact_telegram")}\"")
        buildConfigField("String", "CONTACT_WHATSAPP", "\"${contactProp("contact_whatsapp", "contact_whatsapp")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.6.0" // совместимо с Kotlin 2.0+
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")


    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.compose.material:material-icons-extended")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Для поддержки java.time на старых версиях Android
    implementation("com.jakewharton.threetenabp:threetenabp:1.4.6")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Шифрование настроек и ключа БД
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    // SQLCipher для шифрования Room
    implementation("net.zetetic:android-database-sqlcipher:4.5.2")
    implementation("androidx.sqlite:sqlite-ktx:2.4.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.android.gms:play-services-auth:20.7.0")

   //  Для работы с HTTP
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Для асинхронных вызовов (Kotlin Coroutines)
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    // Для JSON можно использовать org.json или kotlinx.serialization
}


// Условное применение Google Services плагина
val googleServicesFile = file("google-services.json")
if (googleServicesFile.exists()) {
    apply(plugin = "com.google.gms.google-services")
    println("Google Services plugin applied - google-services.json found")
} else {
    println("WARNING: google-services.json not found. Firebase features will not work.")
    println("Please download google-services.json from Firebase Console and place it in the app/ directory.")
}

