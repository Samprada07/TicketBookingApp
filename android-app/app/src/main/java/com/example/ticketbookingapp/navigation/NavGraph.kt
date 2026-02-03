package com.example.ticketbookingapp.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ticketbookingapp.appUi.login.LoginScreen
import com.example.ticketbookingapp.appUi.register.RegisterScreen

// ─── Route constants ─────────────────────────────────────────────
object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN          // app opens on Login
    ) {

        // ── Login ─────────────────────────────────────────────────
        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER)
                },
                onNavigateToHome = {
                    navController.navigate(Routes.HOME) {
                        // Clear the entire auth stack so back button doesn't return to Login
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // ── Register ──────────────────────────────────────────────
        composable(Routes.REGISTER) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        // Remove Register from back stack
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // ── Home (placeholder — replace with your actual Home screen) ─
        composable(Routes.HOME) {
            Text(text = "Welcome! You are logged in. 🎉")
        }
    }
}