package com.example.ticketbookingapp.appUi.events

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ticketbookingapp.network.AuthManager
import com.example.ticketbookingapp.viewmodel.EventListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(
    onEventClick: (Int) -> Unit,
    onNavigateToMyTickets: () -> Unit,
    onNavigateToAdminPanel: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit,
    viewModel: EventListViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val authManager = remember { AuthManager(context.applicationContext) }
    val isAdmin = authManager.isAdmin()
    val userName = authManager.getUserName() ?: "User"

    // Auto-refresh when screen becomes visible (after returning from admin panel)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onEvent(EventListEvent.Retry)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = {
                Log.d("LOGOUT", "Dialog dismissed")
                showLogoutDialog = false
            },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        Log.d("LOGOUT", "Confirmed - calling onLogout()")
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    Log.d("LOGOUT", "Cancelled")
                    showLogoutDialog = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hi, $userName") },
                actions = {
                    // Profile button (only for non-admins)
                    if (!isAdmin) {
                        IconButton(onClick = { onNavigateToProfile() }) {
                            Icon(Icons.Default.Person, contentDescription = "Profile")
                        }
                    }
                    if (isAdmin) {
                        IconButton(onClick = { onNavigateToAdminPanel() }) {
                            Icon(Icons.Default.AccountBox, contentDescription = "Admin Panel")
                        }
                    }
                    if (!isAdmin) {
                        TextButton(onClick = { onNavigateToMyTickets() }) {
                            Text("My Tickets")
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = {
                        Log.d("LOGOUT", "Logout button clicked")
                        showLogoutDialog = true
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Logout",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            // Sort chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.sortBy == SortOption.DATE,
                    onClick = { viewModel.onEvent(EventListEvent.SortByChanged(SortOption.DATE)) },
                    label = { Text("Date") }
                )
                FilterChip(
                    selected = state.sortBy == SortOption.NAME,
                    onClick = { viewModel.onEvent(EventListEvent.SortByChanged(SortOption.NAME)) },
                    label = { Text("Name") }
                )
                FilterChip(
                    selected = state.sortBy == SortOption.SEATS,
                    onClick = { viewModel.onEvent(EventListEvent.SortByChanged(SortOption.SEATS)) },
                    label = { Text("Seats") }
                )
            }

            // Search Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onEvent(EventListEvent.SearchQueryChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search events by name, venue...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            viewModel.onEvent(EventListEvent.SearchQueryChanged(""))
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )

            // Error + Retry
            if (state.error != null && state.events.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.error!!,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    IconButton(onClick = { viewModel.onEvent(EventListEvent.Retry) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retry")
                    }
                }
                return@Column
            }

            // No Results
            if (state.filteredEvents.isEmpty() && state.searchQuery.isNotEmpty() && !state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No events found for \"${state.searchQuery}\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                return@Column
            }

            // Pull-to-Refresh + Event List
            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = { viewModel.onEvent(EventListEvent.Retry) },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = 8.dp,
                        bottom = paddingValues.calculateBottomPadding() + 16.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.filteredEvents) { event ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEventClick(event.id) }
                        ) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                if (event.imageUrl != null) {
                                    AsyncImage(
                                        model = event.imageUrl,
                                        contentDescription = event.name,
                                        modifier = Modifier
                                            .width(120.dp)
                                            .height(120.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Surface(
                                        modifier = Modifier
                                            .width(120.dp)
                                            .height(120.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {}
                                }

                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .weight(1f)
                                ) {
                                    Text(
                                        text = event.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = event.venue,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Seats available: ${event.availableSeats} / ${event.totalSeats}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (event.availableSeats > 0)
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        else
                                            MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}