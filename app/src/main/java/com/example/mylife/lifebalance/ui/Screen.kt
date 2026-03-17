package com.example.mylife.lifebalance.ui

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Home : Screen("home")
    object Goals : Screen("goals")
    object Balance : Screen("balance")
    object Ideas : Screen("ideas")
    object Dreams : Screen("dreams")
    object Calendar : Screen("calendar")
    object Settings : Screen("settings")

    object GoalsDetails : Screen("goals_details/{sphereId}") {
        fun createRoute(sphereId: Int) = "goals_details/$sphereId"
    }

}
