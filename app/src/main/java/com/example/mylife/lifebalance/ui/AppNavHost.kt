package com.example.mylife.lifebalance.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.lifebalance.BuildConfig
import com.example.mylife.lifebalance.data.AppSettingsDataStore
import com.example.mylife.lifebalance.repository.AuthRepository
import com.example.mylife.lifebalance.repository.SyncService
import com.example.mylife.lifebalance.viewmodel.LifeBalanceViewModel
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AppNavHost(
    viewModel: LifeBalanceViewModel,
    authRepository: AuthRepository,
    syncService: SyncService
) {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsDataStore = remember { AppSettingsDataStore(context) }

    val isFirebaseAvailable = remember {
        try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    // null = ещё проверяем авторизацию
    var isAuthenticated by remember { mutableStateOf<Boolean?>(null) }
    var hasSyncedOnStart by remember { mutableStateOf(false) }

    // Проверяем авторизацию при старте
    LaunchedEffect(Unit) {
        if (isFirebaseAvailable) {
            val localUser = authRepository.getLocalUser()
            val hasCompletedFirstSignIn = settingsDataStore.getHasCompletedFirstSignInSync()

            // Пользователь считается авторизованным ТОЛЬКО если есть localUser
            // Если localUser = null, но hasCompletedFirstSignIn = true (например, после восстановления данных),
            // нужно сбросить флаг и показать экран авторизации
            if (localUser == null) {
                // Пользователь не авторизован - сбрасываем флаг и Firebase сессию
                if (hasCompletedFirstSignIn) {
                    // Сбрасываем флаг, так как пользователя нет в базе (например, после переустановки)
                    settingsDataStore.setHasCompletedFirstSignIn(false)
                }
                try { authRepository.signOut() } catch (_: Exception) {}
                isAuthenticated = false
            } else {
                // Пользователь есть в базе - он авторизован
                isAuthenticated = true
            }

            if (isAuthenticated == true && !hasSyncedOnStart) {
                hasSyncedOnStart = true
                withContext(Dispatchers.IO) {
                    try {
                        syncService.syncAllData()
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) {
                            android.util.Log.e("AppNavHost", "Sync error on app start", e)
                        }
                    }
                }
            }
        } else {
            isAuthenticated = true
        }
    }

    // Навигация после проверки авторизации
    LaunchedEffect(isAuthenticated) {
        isAuthenticated?.let { auth ->
            val targetRoute = if (auth) Screen.Home.route else Screen.Auth.route
            val currentRoute = navController.currentDestination?.route
            if (currentRoute != targetRoute) {
                navController.navigate(targetRoute) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    // Пока не проверили авторизацию, показываем SplashScreen (или простой индикатор)
    if (isAuthenticated == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    NavHost(
        navController = navController,
        startDestination = if (isAuthenticated == true) Screen.Home.route else Screen.Auth.route
    ) {
        composable(Screen.Auth.route) {
            val syncScope = remember { CoroutineScope(Dispatchers.IO + SupervisorJob()) }
            AuthScreen(
                authRepository = authRepository,
                onAuthSuccess = {
                    isAuthenticated = true
                    syncScope.launch {
                        try { syncService.syncAllData() } catch (_: Exception) {}
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(viewModel, onNavigate = { screen -> navController.navigate(screen.route) }, navController)
        }

        composable(Screen.Goals.route) {
            GoalsScreen(viewModel, onNavigate = { screen -> navController.navigate(screen.route) }, navController)
        }

        composable(Screen.Balance.route) {
            MainScreen(viewModel, navController)
        }

        composable(Screen.Ideas.route) {
            IdeasScreen(viewModel, onNavigate = { screen -> navController.navigate(screen.route) }, navController)
        }

        composable(Screen.Dreams.route) {
            DreamScreen(viewModel, onNavigate = { screen -> navController.navigate(screen.route) }, navController)
        }

        composable(Screen.Calendar.route) {
            CalendarScreen(viewModel, onNavigate = { screen -> navController.navigate(screen.route) }, navController)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onNavigate = { screen -> navController.navigate(screen.route) }, navController,
                authRepository = authRepository, syncService = syncService, viewModel = viewModel)
        }

        composable(
            route = Screen.GoalsDetails.route,
            arguments = listOf(navArgument("sphereId") { type = NavType.IntType })
        ) { backStackEntry ->
            val sphereId = backStackEntry.arguments?.getInt("sphereId") ?: 0
            GoalsDetailsScreen(sphereId, viewModel, navController)
        }
    }
}
