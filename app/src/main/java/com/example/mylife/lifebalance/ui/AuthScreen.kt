package com.example.mylife.lifebalance.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.example.lifebalance.BuildConfig
import com.example.lifebalance.R
import com.example.mylife.lifebalance.repository.AuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    authRepository: AuthRepository,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Launcher для Google Sign-In
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                scope.launch {
                    try {
                        isLoading = true
                        errorMessage = null
                        successMessage = null
                        
                        // Используем await() для асинхронного получения результата
                        val account = task.await()
                        
                        if (account != null) {
                            if (BuildConfig.DEBUG) {
                                android.util.Log.d("AuthScreen", "Google account received")
                            }
                            val signInResult = authRepository.signInWithGoogle(account)
                            signInResult.fold(
                                onSuccess = { user ->
                                    if (BuildConfig.DEBUG) {
                                        android.util.Log.d("AuthScreen", "Google sign-in successful")
                                    }
                                    successMessage = context.getString(R.string.auth_google_success)
                                    kotlinx.coroutines.delay(1000)
                                    onAuthSuccess()
                                },
                                onFailure = { e ->
                                    if (BuildConfig.DEBUG) {
                                        android.util.Log.e("AuthScreen", "Google sign-in failed", e)
                                    }
                                    errorMessage = e.message ?: context.getString(R.string.auth_google_error)
                                }
                            )
                        } else {
                            if (BuildConfig.DEBUG) {
                                android.util.Log.e("AuthScreen", "Google account is null")
                            }
                            errorMessage = context.getString(R.string.auth_google_account_null)
                        }
                    } catch (e: ApiException) {
                        val errorMsg = when (e.statusCode) {
                            com.google.android.gms.common.ConnectionResult.NETWORK_ERROR -> context.getString(R.string.auth_network_error)
                            12501 -> context.getString(R.string.auth_sign_in_cancelled) // SIGN_IN_CANCELLED
                            4 -> context.getString(R.string.auth_sign_in_required) // SIGN_IN_REQUIRED
                            else -> {
                                val message = e.message ?: context.getString(R.string.auth_unknown_error)
                                when {
                                    message.contains("cancel", ignoreCase = true) -> context.getString(R.string.auth_sign_in_cancelled)
                                    message.contains("network", ignoreCase = true) -> context.getString(R.string.auth_network_error)
                                    else -> context.getString(R.string.auth_google_error_message, message)
                                }
                            }
                        }
                        errorMessage = errorMsg
                        if (BuildConfig.DEBUG) {
                            android.util.Log.e("AuthScreen", "Google sign-in ApiException", e)
                        }
                    } catch (e: Exception) {
                        errorMessage = context.getString(R.string.auth_unexpected_error, e.message ?: "")
                        if (BuildConfig.DEBUG) {
                            android.util.Log.e("AuthScreen", "Google sign-in exception", e)
                        }
                    } finally {
                        isLoading = false
                    }
                }
            }
            Activity.RESULT_CANCELED -> {
                isLoading = false
                // Пользователь отменил вход - не показываем ошибку
            }
            else -> {
                isLoading = false
                errorMessage = context.getString(R.string.auth_unknown_sign_in_error)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Заголовок
            Text(
                text = if (isLoginMode) stringResource(R.string.auth_title_login) else stringResource(R.string.auth_title_register),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Поле Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.auth_email)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )

            // Поле Display Name (только для регистрации)
            if (!isLoginMode) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text(stringResource(R.string.auth_display_name_optional)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    singleLine = true
                )
            }

            // Поле Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.auth_password)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true
            )

            // Сообщение об ошибке
            errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }

            // Сообщение об успехе
            successMessage?.let { success ->
                Text(
                    text = success,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }

            // Кнопка входа/регистрации
            Button(
                onClick = {
                    if (isLoading) return@Button
                    isLoading = true
                    errorMessage = null
                    successMessage = null

                    scope.launch {
                        val result = if (isLoginMode) {
                            authRepository.signInWithEmailAndPassword(email, password)
                        } else {
                            authRepository.createUserWithEmailAndPassword(
                                email,
                                password,
                                displayName.takeIf { it.isNotBlank() }
                            )
                        }

                        isLoading = false
                        result.fold(
                            onSuccess = {
                                successMessage = if (isLoginMode) context.getString(R.string.auth_login_success) else context.getString(R.string.auth_register_success)
                                kotlinx.coroutines.delay(1000)
                                onAuthSuccess()
                            },
                            onFailure = {
                                errorMessage = it.message ?: context.getString(R.string.auth_generic_error)
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (isLoginMode) stringResource(R.string.auth_btn_login) else stringResource(R.string.auth_btn_register))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Разделитель
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.auth_or),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Divider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопка входа через Google
            val googleSignInClient = authRepository.getGoogleSignInClient()
            if (googleSignInClient != null) {
                OutlinedButton(
                    onClick = {
                        if (isLoading) return@OutlinedButton
                        scope.launch {
                            try {
                                isLoading = true
                                errorMessage = null
                                successMessage = null
                                // Используем новый метод, который принудительно показывает экран выбора аккаунта после переустановки
                                val signInIntent = authRepository.getGoogleSignInIntent()
                                if (signInIntent != null) {
                                    googleSignInLauncher.launch(signInIntent)
                                } else {
                                    isLoading = false
                                    errorMessage = context.getString(R.string.auth_google_launch_failed)
                                }
                            } catch (e: Exception) {
                                isLoading = false
                                errorMessage = context.getString(R.string.auth_google_launch_error, e.message ?: "")
                                if (BuildConfig.DEBUG) {
                                    android.util.Log.e("AuthScreen", "Failed to launch Google sign-in", e)
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(stringResource(R.string.auth_google_sign_in))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Переключение между входом и регистрацией
            TextButton(
                onClick = {
                    isLoginMode = !isLoginMode
                    errorMessage = null
                    successMessage = null
                }
            ) {
                Text(
                    text = if (isLoginMode) {
                        stringResource(R.string.auth_switch_to_register)
                    } else {
                        stringResource(R.string.auth_switch_to_login)
                    }
                )
            }

            // Забыли пароль (только для входа)
            if (isLoginMode) {
                TextButton(
                    onClick = {
                        if (email.isBlank()) {
                            errorMessage = context.getString(R.string.auth_enter_email_for_reset)
                            return@TextButton
                        }
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            val result = authRepository.sendPasswordResetEmail(email)
                            isLoading = false
                            result.fold(
                                onSuccess = {
                                    successMessage = context.getString(R.string.auth_reset_email_sent, email)
                                },
                                onFailure = {
                                    errorMessage = it.message ?: context.getString(R.string.auth_reset_email_error)
                                }
                            )
                        }
                    },
                    enabled = !isLoading
                ) {
                    Text(stringResource(R.string.auth_forgot_password))
                }
            }
        }
    }
}

