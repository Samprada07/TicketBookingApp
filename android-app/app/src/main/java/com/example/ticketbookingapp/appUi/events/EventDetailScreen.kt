package com.example.ticketbookingapp.appUi.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.ticketbookingapp.network.AuthManager
import com.example.ticketbookingapp.viewmodel.EventDetailViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: Int,
    onNavigateBack: () -> Unit,
    navController: NavController,
    viewModel: EventDetailViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current
    val authManager = remember { AuthManager(context.applicationContext) }
    val isAdmin = authManager.isAdmin()

    // Load event on first composition
    LaunchedEffect(eventId) {
        viewModel.onEvent(EventDetailEvent.Load(eventId))
    }

    // Show success snackbar and go back
    LaunchedEffect(state.bookingSuccess) {
        if (state.bookingSuccess) {
            snackbarHostState.showSnackbar("Ticket booked successfully! 🎉")
            onNavigateBack()
        }
    }

    // Show error in snackbar
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Event Details") },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->

        // ── Loading ───────────────────────────────────────────────
        if (state.isLoading || state.event == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val event = state.event!!

        // ── Event Detail ──────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Event Image
            if (event.imageUrl != null) {
                AsyncImage(
                    model = event.imageUrl,
                    contentDescription = event.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {}
            }

            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .padding(bottom = paddingValues.calculateBottomPadding())
            ) {
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "📍 ${event.venue}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "🕐 ${event.startTime}  –  ${event.endTime}", style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Seats: ${event.availableSeats} available / ${event.totalSeats} total",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (event.availableSeats > 0)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "💰 Rs.${String.format(Locale.US, "%.2f", event.price)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ── Admin: show info message instead of booking ───
                if (isAdmin) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "👮 Admin View — Booking is disabled for admins.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                } else {
                    // ── User: show seat input and book button ─────
                    OutlinedTextField(
                        value = state.seatNumber,
                        onValueChange = {
                            viewModel.onEvent(EventDetailEvent.SeatNumberChanged(it))
                        },
                        label = { Text("Seat Number (optional)") },
                        placeholder = { Text("Leave blank for any seat") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            navController.navigate(
                                "payment/${event.id}/${event.name}/${event.price}/-1"
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = event.availableSeats > 0
                    ) {
                        if (event.availableSeats > 0) {
                            Text("Proceed to Payment - ₹${String.format(Locale.US, "%.2f", event.price)}")
                        } else {
                            Text("No Seats Available")
                        }
                    }
                }
            }
        }
    }
}