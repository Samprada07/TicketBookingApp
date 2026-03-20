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
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set

private fun canCancelTicket(ticket: MyTicket): Boolean {
    return try {
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
            text = {
                Text(
                    "Cancel this ticket? Refund of ₹${
                        String.format(
                            Locale.US,
                            "%.2f",
                            ticket.price
                        )
                    } in 5-7 days."
                )
            },
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

    selectedTicket?.let { ticket ->
        Dialog(onDismissRequest = { selectedTicket = null }) {
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
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Ticket QR Code", style = MaterialTheme.typography.titleLarge)
                        IconButton(onClick = { selectedTicket = null }) {
                            Icon(Icons.Default.Close, "Close")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    val qr = generateQRCode("TICKET-${ticket.id}", 400)
                    qr?.let { Image(it.asImageBitmap(), "QR", Modifier.size(300.dp)) }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(ticket.eventName, style = MaterialTheme.typography.titleMedium)
                    Text("ID: ${ticket.id}", style = MaterialTheme.typography.bodySmall)
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
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { pad ->

        if (state.error != null && state.tickets.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                IconButton({ viewModel.onEvent(MyTicketsEvent.Retry) }) {
                    Icon(Icons.Default.Refresh, "Retry")
                }
            }
            return@Scaffold
        }

        if (state.tickets.isEmpty() && !state.isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("No tickets yet 🎟️", style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }

        PullToRefreshBox(
            state.isLoading,
            { viewModel.onEvent(MyTicketsEvent.Retry) },
            Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = pad.calculateTopPadding() + 16.dp,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = pad.calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.tickets.filter { ticket ->
                    ticket.paymentStatus == "succeeded" && (
                            // Hide cancelled tickets older than 7 days
                            @Suppress("CascadeIf")
                            if (ticket.status == "cancelled") {
                                try {
                                    val bookedDate = SimpleDateFormat(
                                        "yyyy-MM-dd'T'HH:mm:ss",
                                        Locale.getDefault()
                                    )
                                        .parse(ticket.bookedAt) ?: return@filter true
                                    val daysSinceCancelled =
                                        (Date().time - bookedDate.time) / (1000.0 * 60 * 60 * 24)
                                    daysSinceCancelled <= 7
                                } catch (_: Exception) {
                                    true
                                }
                            } else if (ticket.status == "expired") {
                                try {
                                    val dateFormat = SimpleDateFormat(
                                        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                                        Locale.getDefault()
                                    ).apply {
                                        timeZone = TimeZone.getTimeZone("UTC")
                                    }
                                    val eventStart =
                                        dateFormat.parse(ticket.startTime) ?: return@filter true
                                    val daysSinceEvent =
                                        (Date().time - eventStart.time) / (1000.0 * 60 * 60 * 24)
                                    daysSinceEvent <= 7
                                } catch (_: Exception) {
                                    true
                                }
                            } else {
                                true // Show active tickets
                            })
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
                                        "📅 Booked: ${ticket.bookedAt.take(10)}",  // Shows just the date
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

                                    if (ticket.status == "expired") {
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "⏰ EXPIRED",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (ticket.status == "active" && ticket.paymentStatus == "succeeded") {
                                    Spacer(Modifier.width(16.dp))
                                    generateQRCode("TICKET-${ticket.id}", 150)?.let {
                                        Image(
                                            it.asImageBitmap(),
                                            "QR",
                                            Modifier
                                                .size(80.dp)
                                                .clickable { selectedTicket = ticket }
                                        )
                                    }
                                }
                            }

                            if (ticket.status == "active" && ticket.paymentStatus == "succeeded") {
                                Spacer(Modifier.height(12.dp))
                                if (canCancelTicket(ticket)) {
                                    Button(
                                        onClick = { ticketToCancel = ticket },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !state.isRefunding,  // Disable during refund
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    ) {
                                        if (state.isRefunding) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text("Processing Refund...")
                                        } else {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "Cancel & Get ₹${
                                                    String.format(
                                                        Locale.US,
                                                        "%.2f",
                                                        ticket.price
                                                    )
                                                } Refund"
                                            )
                                        }
                                    }
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

private fun generateQRCode(text: String, size: Int): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val matrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size)
        val bmp = createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp[x, y] =
                    if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
        }
        bmp
    } catch (_: Exception) {
        null
    }
}