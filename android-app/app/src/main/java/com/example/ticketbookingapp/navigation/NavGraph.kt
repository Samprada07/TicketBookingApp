package com.example.ticketbookingapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ticketbookingapp.appUi.admin.AdminPanelScreen
import com.example.ticketbookingapp.appUi.admin.CreateEventScreen
import com.example.ticketbookingapp.appUi.admin.EventBookingsScreen
import com.example.ticketbookingapp.appUi.events.EventDetailScreen
import com.example.ticketbookingapp.appUi.events.EventListScreen
import com.example.ticketbookingapp.appUi.login.LoginScreen
import com.example.ticketbookingapp.appUi.payment.PaymentHistoryScreen
import com.example.ticketbookingapp.appUi.payment.PaymentScreen
import com.example.ticketbookingapp.appUi.profile.ProfileScreen
import com.example.ticketbookingapp.appUi.register.RegisterScreen
import com.example.ticketbookingapp.appUi.splash.SplashScreen
import com.example.ticketbookingapp.appUi.tickets.MyTicketsScreen
import com.example.ticketbookingapp.navigation.Routes.EVENT_LIST
import com.example.ticketbookingapp.navigation.Routes.MY_TICKETS
import com.example.ticketbookingapp.navigation.Routes.PAYMENT
import com.example.ticketbookingapp.navigation.Routes.PAYMENT_HISTORY
import com.example.ticketbookingapp.network.AuthManager

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val EVENT_LIST = "event_list"
    const val EVENT_DETAIL = "event_detail/{eventId}"
    const val MY_TICKETS = "my_tickets"
    const val ADMIN_PANEL = "admin_panel"
    const val CREATE_EVENT = "create_event?eventId={eventId}"
    const val EVENT_BOOKINGS = "event_bookings/{eventId}/{eventName}"
    const val PROFILE = "profile"
    const val PAYMENT = "payment/{eventId}/{eventName}/{eventPrice}/{seatNumber}"
    const val PAYMENT_HISTORY = "payment_history"
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val authManager = AuthManager(context.applicationContext)

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {

        // ── Splash ────────────────────────────────────────────────
        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToEventList = {
                    navController.navigate(Routes.EVENT_LIST) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

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
                },
                onNavigateToAdminPanel = {
                    navController.navigate(Routes.ADMIN_PANEL)
                },
                onNavigateToProfile = {
                    navController.navigate(Routes.PROFILE)
                },
                onLogout = {
                    authManager.clearToken()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
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
                onNavigateBack = { navController.popBackStack() },
                navController = navController
            )
        }

        // ── My Tickets ────────────────────────────────────────────
        composable(Routes.MY_TICKETS) {
            MyTicketsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Admin Panel ───────────────────────────────────────────
        composable(Routes.ADMIN_PANEL) {
            AdminPanelScreen(
                onNavigateBack = { navController.popBackStack() },
                onCreateEvent = { navController.navigate("create_event") },
                onEditEvent = { eventId ->
                    navController.navigate("create_event?eventId=$eventId")
                },
                onViewBookings = { eventId, eventName ->
                    navController.navigate("event_bookings/$eventId/$eventName")
                }
            )
        }

        // ── Create Event ──────────────────────────────────────────
        composable(
            route = Routes.CREATE_EVENT,
            arguments = listOf(
                navArgument("eventId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val eventIdString = backStackEntry.arguments?.getString("eventId")
            val eventId = eventIdString?.toIntOrNull()

            CreateEventScreen(
                eventId = eventId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Event Bookings ────────────────────────────────────────
        composable(
            route = Routes.EVENT_BOOKINGS,
            arguments = listOf(
                navArgument("eventId") { type = NavType.IntType },
                navArgument("eventName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getInt("eventId") ?: return@composable
            val eventName = backStackEntry.arguments?.getString("eventName") ?: ""
            EventBookingsScreen(
                eventId = eventId,
                eventName = eventName,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        // ── User Profile ────────────────────────────────────────
        composable(Routes.PROFILE) {
            ProfileScreen(
                onNavigateBack = { navController.navigateUp() },
                navController = navController
            )
        }

        // ── Payment ────────────────────────────────────────
        composable(
            route = PAYMENT,
            arguments = listOf(
                navArgument("eventId") { type = NavType.IntType },
                navArgument("eventName") { type = NavType.StringType },
                navArgument("eventPrice") { type = NavType.FloatType },
                navArgument("seatNumber") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getInt("eventId") ?: 0
            val eventName = backStackEntry.arguments?.getString("eventName") ?: ""
            val eventPrice = backStackEntry.arguments?.getFloat("eventPrice")?.toDouble() ?: 0.0
            val seatNumber = backStackEntry.arguments?.getInt("seatNumber")?.takeIf { it != -1 }

            PaymentScreen(
                eventId = eventId,
                eventName = eventName,
                eventPrice = eventPrice,
                seatNumber = seatNumber,
                onNavigateBack = { navController.navigateUp() },
                onPaymentSuccess = {
                    navController.navigate(MY_TICKETS) {
                        popUpTo(EVENT_LIST) { inclusive = false }
                    }
                }
            )
        }

        // ── Payment History ────────────────────────────────────────
        composable(PAYMENT_HISTORY) {
            PaymentHistoryScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}