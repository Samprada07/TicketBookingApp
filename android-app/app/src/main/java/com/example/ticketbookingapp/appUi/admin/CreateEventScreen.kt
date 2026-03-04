package com.example.ticketbookingapp.appUi.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ticketbookingapp.viewmodel.CreateEventViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    eventId: Int? = null,
    onNavigateBack: () -> Unit,
    viewModel: CreateEventViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(eventId) {
        eventId?.let { viewModel.loadEventForEdit(it) }
    }

    // Show success message in snackbar
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
        }
    }

    // Show error message in snackbar
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long
            )
        }
    }

    // Navigate back on success
    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            kotlinx.coroutines.delay(1500) // Show success message first
            onNavigateBack()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            viewModel.onEvent(CreateEventEvent.ImageSelected(it))
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (eventId == null) "Create Event" else "Edit Event") },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                label = { Text("Event Name *") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading && !state.isUploading
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.onEvent(CreateEventEvent.DescriptionChanged(it)) },
                label = { Text("Description *") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                enabled = !state.isLoading && !state.isUploading
            )

            OutlinedTextField(
                value = state.venue,
                onValueChange = { viewModel.onEvent(CreateEventEvent.VenueChanged(it)) },
                label = { Text("Venue *") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading && !state.isUploading
            )

            OutlinedTextField(
                value = state.startTime,
                onValueChange = { viewModel.onEvent(CreateEventEvent.StartTimeChanged(it)) },
                label = { Text("Start Time *") },
                placeholder = { Text("2026-03-01 18:00:00") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading && !state.isUploading
            )

            OutlinedTextField(
                value = state.endTime,
                onValueChange = { viewModel.onEvent(CreateEventEvent.EndTimeChanged(it)) },
                label = { Text("End Time *") },
                placeholder = { Text("2026-03-01 21:00:00") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading && !state.isUploading
            )

            OutlinedTextField(
                value = state.totalSeats,
                onValueChange = { viewModel.onEvent(CreateEventEvent.TotalSeatsChanged(it)) },
                label = { Text("Total Seats *") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = !state.isLoading && !state.isUploading
            )

            OutlinedTextField(
                value = state.price,
                onValueChange = { viewModel.onEvent(CreateEventEvent.PriceChanged(it)) },
                label = { Text("Price (Rs.) *") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                enabled = !state.isLoading && !state.isUploading,
                prefix = { Text("Rs.") }
            )

            Text(
                "Event Image (optional)",
                style = MaterialTheme.typography.labelLarge
            )

            OutlinedButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading && !state.isUploading
            ) {
                Icon(Icons.Default.AddCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (selectedImageUri != null || state.imageUrl.isNotEmpty())
                        "Change Image"
                    else
                        "Select Image"
                )
            }

            // Image preview
            if (selectedImageUri != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            } else if (state.imageUrl.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    AsyncImage(
                        model = state.imageUrl,
                        contentDescription = "Current image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            if (state.isUploading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Uploading image...")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (eventId == null) {
                        viewModel.onEvent(CreateEventEvent.Submit)
                    } else {
                        viewModel.onEvent(CreateEventEvent.Update(eventId))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading && !state.isUploading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (eventId == null) "Creating..." else "Updating...")
                } else {
                    Text(if (eventId == null) "Create Event" else "Update Event")
                }
            }
        }
    }
}