# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# --- Удаление логов в release (снижение риска утечки через logcat) ---
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# --- Gson / Retrofit: модели запросов и ответов API ---
-keep class com.example.mylife.lifebalance.data.AiAnalyzeRequest { *; }
-keep class com.example.mylife.lifebalance.data.AiAnalyzeResponse { *; }
-keep class com.example.mylife.lifebalance.data.AnalyzeRequest { *; }
-keep class com.example.mylife.lifebalance.data.AnalyzeResponse { *; }

# --- Firestore: DTO для сериализации при синхронизации ---
-keep class com.example.mylife.lifebalance.data.LifeSphereDTO { *; }
-keep class com.example.mylife.lifebalance.data.TaskDTO { *; }
-keep class com.example.mylife.lifebalance.data.GoalDTO { *; }
-keep class com.example.mylife.lifebalance.data.IdeaFolderDTO { *; }
-keep class com.example.mylife.lifebalance.data.IdeaNoteDTO { *; }
-keep class com.example.mylife.lifebalance.data.DreamAffirmationDTO { *; }

# --- Kotlin: сохраняем метаданные для data class и рефлексии ---
-keepattributes Signature
-keepattributes *Annotation*

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile