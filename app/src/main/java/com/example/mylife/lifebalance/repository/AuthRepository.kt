package com.example.mylife.lifebalance.repository

import android.content.Intent
import android.content.Context
import com.example.lifebalance.BuildConfig
import com.example.mylife.lifebalance.data.AppSettingsDataStore
import com.example.mylife.lifebalance.data.User
import com.example.mylife.lifebalance.data.UserDao
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val context: Context,
    private val userDao: UserDao
) {
    private val settingsDataStore = AppSettingsDataStore(context)
    private val auth: FirebaseAuth? by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseAuth.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.w("AuthRepository", "Firebase not initialized: ${e.message}")
            null
        }
    }

    private val _googleSignInClient: GoogleSignInClient? by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                val webClientId = context.getString(com.example.lifebalance.R.string.default_web_client_id)
                // Если Web Client ID не настроен, возвращаем null
                if (webClientId == "YOUR_WEB_CLIENT_ID_HERE" || webClientId.isEmpty()) {
                    if (BuildConfig.DEBUG) android.util.Log.w("AuthRepository", "Google Sign-In Web Client ID not configured")
                    return@lazy null
                }
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build()
                GoogleSignIn.getClient(context, gso)
            } else {
                null
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.w("AuthRepository", "Google Sign-In not available: ${e.message}")
            null
        }
    }

    private fun isFirebaseAvailable(): Boolean {
        return try {
            auth != null && FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
    
    fun getGoogleSignInClient(): GoogleSignInClient? {
        return _googleSignInClient
    }
    
    /**
     * Получает Intent для Google Sign-In с принудительным показом экрана выбора аккаунта
     * после переустановки приложения.
     * 
     * Если флаг hasCompletedFirstSignIn = false (первая установка/переустановка),
     * то перед созданием Intent вызывается signOut() для очистки кэша Google Sign-In,
     * что заставляет показать экран выбора аккаунта.
     */
    suspend fun getGoogleSignInIntent(): Intent? {
        val client = _googleSignInClient ?: return null
        
        // Проверяем, была ли выполнена первая авторизация
        val hasCompletedFirstSignIn = settingsDataStore.getHasCompletedFirstSignInSync()
        
        // Если это первая установка/переустановка (флаг = false), 
        // вызываем signOut() чтобы очистить кэш и показать экран выбора аккаунта
        if (!hasCompletedFirstSignIn) {
            try {
                if (BuildConfig.DEBUG) android.util.Log.d("AuthRepository", "First sign-in detected, clearing Google Sign-In cache to show account picker")
                client.signOut().await()
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) android.util.Log.w("AuthRepository", "Error clearing Google Sign-In cache: ${e.message}")
                // Продолжаем выполнение даже если signOut() не удался
            }
        }
        
        return client.signInIntent
    }

    val currentUser: FirebaseUser?
        get() = auth?.currentUser

    private val _currentUserFlow = MutableStateFlow<FirebaseUser?>(null)
    val currentUserFlow: Flow<FirebaseUser?> = _currentUserFlow.asStateFlow()

    init {
        // Инициализируем текущего пользователя, если Firebase доступен
        _currentUserFlow.value = auth?.currentUser
        
        // Добавляем слушатель только если Firebase доступен
        auth?.addAuthStateListener { firebaseAuth ->
            _currentUserFlow.value = firebaseAuth.currentUser
        }
    }

    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
        if (!isFirebaseAvailable() || auth == null) {
            return Result.failure(Exception("Firebase is not initialized. Please configure Firebase to enable authentication."))
        }
        return try {
            val result = auth!!.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                saveUserToLocal(user)
                // Устанавливаем флаг, что первая авторизация выполнена
                settingsDataStore.setHasCompletedFirstSignIn(true)
                Result.success(user)
            } else {
                Result.failure(Exception("Sign in failed: user is null"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String,
        displayName: String? = null
    ): Result<FirebaseUser> {
        if (!isFirebaseAvailable() || auth == null) {
            return Result.failure(Exception("Firebase is not initialized. Please configure Firebase to enable authentication."))
        }
        return try {
            val result = auth!!.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                // Обновляем профиль с именем, если оно указано
                if (!displayName.isNullOrBlank()) {
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()
                    user.updateProfile(profileUpdates).await()
                }
                saveUserToLocal(user)
                // Устанавливаем флаг, что первая авторизация выполнена
                settingsDataStore.setHasCompletedFirstSignIn(true)
                Result.success(user)
            } else {
                Result.failure(Exception("Registration failed: user is null"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        auth?.signOut()
        userDao.deleteAllUsers()
    }

    suspend fun updateUserProfile(displayName: String?, photoUrl: String?): Result<Unit> {
        if (!isFirebaseAvailable() || auth == null) {
            return Result.failure(Exception("Firebase is not initialized"))
        }
        return try {
            val user = auth!!.currentUser
            if (user != null) {
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .apply {
                        displayName?.let { setDisplayName(it) }
                        photoUrl?.let { setPhotoUri(android.net.Uri.parse(it)) }
                    }
                    .build()
                user.updateProfile(profileUpdates).await()
                saveUserToLocal(user)
                Result.success(Unit)
            } else {
                Result.failure(Exception("No user signed in"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        if (!isFirebaseAvailable() || auth == null) {
            return Result.failure(Exception("Firebase is not initialized"))
        }
        return try {
            auth!!.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun saveUserToLocal(firebaseUser: FirebaseUser) {
        val user = User(
            uid = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            displayName = firebaseUser.displayName,
            photoUrl = firebaseUser.photoUrl?.toString(),
            lastSyncTimestamp = System.currentTimeMillis(),
            isOnline = true
        )
        userDao.insertUser(user)
    }

    suspend fun getLocalUser(): User? {
        return userDao.getCurrentUser()
    }

    fun getLocalUserFlow(): Flow<User?> {
        return userDao.getCurrentUserFlow()
    }

    suspend fun updateLocalUser(user: User) {
        userDao.updateUser(user)
    }
    
    suspend fun signInWithGoogle(account: GoogleSignInAccount): Result<FirebaseUser> {
        if (!isFirebaseAvailable() || auth == null) {
            if (BuildConfig.DEBUG) android.util.Log.e("AuthRepository", "Firebase not available for Google sign-in")
            return Result.failure(Exception("Firebase is not initialized. Please configure Firebase to enable authentication."))
        }
        return try {
            val idToken = account.idToken
            if (idToken == null) {
                if (BuildConfig.DEBUG) android.util.Log.e("AuthRepository", "Google account idToken is null")
                return Result.failure(Exception("Не удалось получить токен от Google. Попробуйте еще раз."))
            }
            
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth!!.signInWithCredential(credential).await()
            val user = result.user
            if (user != null) {
                saveUserToLocal(user)
                // Устанавливаем флаг, что первая авторизация выполнена
                settingsDataStore.setHasCompletedFirstSignIn(true)
                if (BuildConfig.DEBUG) android.util.Log.d("AuthRepository", "Google sign-in successful")
                Result.success(user)
            } else {
                if (BuildConfig.DEBUG) android.util.Log.e("AuthRepository", "Google sign-in result user is null")
                Result.failure(Exception("Google sign in failed: user is null"))
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.e("AuthRepository", "Google sign-in error", e)
            val errorMessage = when {
                e.message?.contains("network", ignoreCase = true) == true -> 
                    "Ошибка сети. Проверьте подключение к интернету"
                e.message?.contains("invalid", ignoreCase = true) == true -> 
                    "Неверный токен. Попробуйте еще раз"
                else -> e.message ?: "Ошибка входа через Google"
            }
            Result.failure(Exception(errorMessage))
        }
    }
}

