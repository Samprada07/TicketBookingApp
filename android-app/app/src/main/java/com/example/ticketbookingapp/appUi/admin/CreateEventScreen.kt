package com.example.ticketbookingapp.appUi.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ticketbookingapp.viewmodel.CreateEventViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    onNavigateBack: () -> Unit,
    viewModel: CreateEventViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    // Navigate back on success
    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Event") },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.onEvent(CreateEventEvent.NameChanged(it)) },
                label = { Text("Event Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.onEvent(CreateEventEvent.DescriptionChanged(it)) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = state.venue,
                onValueChange = { viewModel.onEvent(CreateEventEvent.VenueChanged(it)) },
                label = { Text("Venue") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.startTime,
                onValueChange = { viewModel.onEvent(CreateEventEvent.StartTimeChanged(it)) },
                label = { Text("Start Time") },
                placeholder = { Text("e.g. 2026-03-01 18:00:00") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.endTime,
                onValueChange = { viewModel.onEvent(CreateEventEvent.EndTimeChanged(it)) },
                label = { Text("End Time") },
                placeholder = { Text("e.g. 2026-03-01 21:00:00") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.totalSeats,
                onValueChange = { viewModel.onEvent(CreateEventEvent.TotalSeatsChanged(it)) },
                label = { Text("Total Seats") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = state.imageUrl,
                onValueChange = { viewModel.onEvent(CreateEventEvent.ImageUrlChanged(it)) },
                label = { Text("Image URL (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            // Error message
            state.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.onEvent(CreateEventEvent.Submit) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Create Event")
                }
            }
        }
    }
}