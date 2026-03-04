package com.example.ticketbookingapp.appUi.tickets

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ticketbookingapp.network.MyTicket
import com.example.ticketbookingapp.viewmodel.MyTicketsViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import androidx.core.graphics.set
import androidx.core.graphics.createBitmap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private fun canCancelTicket(ticket: MyTicket): Boolean {
    return try {
        // Handle both formats: with T and Z
        val dateFormat = if (ticket.startTime.contains("T")) {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        } else {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        }

        val eventStart = dateFormat.parse(ticket.startTime) ?: return false
        val now = Date()
        val daysUntilEvent = (eventStart.time - now.time) / (1000.0 * 60 * 60 * 24)

        daysUntilEvent >= 2
    } catch (e: Exception) {
        android.util.Log.e("CancelTicket", "Date parse error: ${e.message}")
        false
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTicketsScreen(
    onNavigateBack: () -> Unit,
    viewModel: MyTicketsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedTicket by remember { mutableStateOf<MyTicket?>(null) }
    var ticketToCancel by remember { mutableStateOf<MyTicket?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Load data when screen appears
    LaunchedEffect(Unit) {
        viewModel.onEvent(MyTicketsEvent.Load)
    }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    ticketToCancel?.let { ticket ->
        AlertDialog(
            onDismissRequest = { ticketToCancel = null },
            title = { Text("Cancel Ticket") },
            text = { Text("Cancel this ticket? Refund of ₹${String.format(Locale.US, "%.2f", ticket.price)} in 5-7 days.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onEvent(MyTicketsEvent.CancelTicket(ticket.id))
                    ticketToCancel = null
                }) {
                    Text("Yes, Cancel", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { ticketToCancel = null }) {
                    Text("Keep Ticket")
                }
            }
        )
    }

    // Full-screen QR code dialog
    selectedTicket?.let { ticket ->
        Dialog(onDismissRequest = { }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ticket QR Code",
                            style = MaterialTheme.typography.titleLarge
                        )
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Large QR Code
                    val qrBitmap = generateQRCode("TICKET-${ticket.id}", 400)
                    qrBitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.size(300.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = ticket.eventName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Ticket ID: ${ticket.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("My Tickets") },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->

        // ── Error + Retry ─────────────────────────────────────────
        if (state.error != null && state.tickets.isEmpty()) {
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
                IconButton(onClick = { viewModel.onEvent(MyTicketsEvent.Retry) }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Retry")
                }
            }
            return@Scaffold
        }

        // ── Empty State ───────────────────────────────────────────
        if (state.tickets.isEmpty() && !state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tickets booked yet 🎟️",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        // ── Pull-to-Refresh + Tickets List ────────────────────────
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.onEvent(MyTicketsEvent.Retry) },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding() + 16.dp,
                    bottom = paddingValues.calculateBottomPadding() + 16.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.tickets.filter { ticket ->
                    // Hide cancelled tickets older than 3 days
                    if (ticket.status == "cancelled") {
                        try {
                            val cancelledDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                .parse(ticket.bookedAt) ?: return@filter true
                            val daysSinceCancelled = (Date().time - cancelledDate.time) / (1000.0 * 60 * 60 * 24)
                            daysSinceCancelled <= 3
                        } catch (e: Exception) {
                            true // Show if parse fails
                        }
                    } else {
                        true // Show active/expired tickets
                    }
                }) { ticket ->
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = if (ticket.status == "cancelled") {
                            CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer)
                        } else CardDefaults.cardColors()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        ticket.eventName,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "📍 ${ticket.venue}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "🕐 ${ticket.startTime}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        if (ticket.seatNumber != null) "🪑 Seat: ${ticket.seatNumber}" else "🪑 Any",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "💰 Rs.${String.format(Locale.US, "%.2f", ticket.price)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    if (ticket.status == "cancelled") {
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "❌ CANCELLED",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                if (ticket.status == "active") {
                                    Spacer(Modifier.width(16.dp))
                                    generateQRCode("TICKET-${ticket.id}", 150)?.let {
                                        Image(
                                            it.asImageBitmap(),
                                            "QR",
                                            Modifier.size(80.dp)
                                                .clickable { selectedTicket = ticket }
                                        )
                                    }
                                }
                            }

                            if (ticket.status == "active") {
                                Spacer(Modifier.height(12.dp))
                                if (canCancelTicket(ticket)) {
                                    Button(
                                        onClick = { ticketToCancel = ticket },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Cancel & Get ₹${String.format(Locale.US, "%.2f", ticket.price)} Refund")                                    }
                                } else {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Text(
                                            text = "⚠️ Cannot cancel (event is less than 2 days away)",
                                            modifier = Modifier.padding(12.dp),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
}

// Generate QR Code bitmap
fun generateQRCode(text: String, size: Int): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = createBitmap(width, height, Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap[x, y] =
                    if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
        }
        bitmap
    } catch (_: Exception) {
        null
    }
}