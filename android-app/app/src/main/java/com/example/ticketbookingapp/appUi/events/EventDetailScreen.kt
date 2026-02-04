package com.example.ticketbookingapp.appUi.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ticketbookingapp.viewmodel.EventDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: Int,
    onNavigateBack: () -> Unit,
    viewModel: EventDetailViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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

        // ── Event Detail + Book Form ──────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
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

            // Venue & Time
            Text(text = "📍 ${event.venue}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "🕐 ${event.startTime}  –  ${event.endTime}", style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(16.dp))

            // Seats info
            Text(
                text = "Seats: ${event.availableSeats} available / ${event.totalSeats} total",
                style = MaterialTheme.typography.bodyMedium,
                color = if (event.availableSeats > 0)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Seat number input (optional) ──────────────────────
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

            // ── Book Button ───────────────────────────────────────
            Button(
                onClick = {
                    viewModel.onEvent(EventDetailEvent.BookTicket(eventId))
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isBooking && event.availableSeats > 0
            ) {
                if (state.isBooking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else if (event.availableSeats > 0) {
                    Text("Book Ticket")
                } else {
                    Text("No Seats Available")
                }
            }
        }
    }
}