package com.example.ticketbookingapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ticketbookingapp.appUi.events.EventDetailScreen
import com.example.ticketbookingapp.appUi.events.EventListScreen
import com.example.ticketbookingapp.appUi.login.LoginScreen
import com.example.ticketbookingapp.appUi.register.RegisterScreen
import com.example.ticketbookingapp.appUi.tickets.MyTicketsScreen

// ─── Route constants ─────────────────────────────────────────────
object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val EVENT_LIST = "event_list"
    const val EVENT_DETAIL = "event_detail/{eventId}"
    const val MY_TICKETS = "my_tickets"
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {

        // ── Login ─────────────────────────────────────────────────
        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER)
                },
                onNavigateToHome = {
                    navController.navigate(Routes.EVENT_LIST) {
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
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // ── Event List (Home) ─────────────────────────────────────
        composable(Routes.EVENT_LIST) {
            EventListScreen(
                onEventClick = { eventId ->
                    navController.navigate("event_detail/$eventId")
                },
                onNavigateToMyTickets = {
                    navController.navigate(Routes.MY_TICKETS)
                }
            )
        }

        // ── Event Detail + Book Ticket ────────────────────────────
        composable(
            route = Routes.EVENT_DETAIL,
            arguments = listOf(
                navArgument("eventId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getInt("eventId") ?: return@composable

            EventDetailScreen(
                eventId = eventId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ── My Tickets ────────────────────────────────────────────
        composable(Routes.MY_TICKETS) {
            MyTicketsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}