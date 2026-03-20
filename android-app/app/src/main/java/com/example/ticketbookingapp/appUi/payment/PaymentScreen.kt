package com.example.ticketbookingapp.appUi.payment

import android.view.ContextThemeWrapper
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ticketbookingapp.viewmodel.PaymentViewModel
import com.stripe.android.view.CardInputWidget
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    eventId: Int,
    eventName: String,
    eventPrice: Double,
    seatNumber: Int?,
    onNavigateBack: () -> Unit,
    onPaymentSuccess: () -> Unit,
    viewModel: PaymentViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var cardInputWidget: CardInputWidget? by remember { mutableStateOf(null) }

    LaunchedEffect(eventId) {
        viewModel.createPaymentIntent(eventId, seatNumber)
    }

    LaunchedEffect(state.paymentSuccess) {
        if (state.paymentSuccess) {
            snackbarHostState.showSnackbar("Payment successful!")
            kotlinx.coroutines.delay(1500)
            onPaymentSuccess()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Payment") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { pad ->

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Event details card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = eventName,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (seatNumber != null) {
                        Text(
                            text = "Seat: $seatNumber",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Total: ₹${String.format(Locale.US, "%.2f", eventPrice)}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Card input section
            Text(
                text = "Enter Card Details",
                style = MaterialTheme.typography.titleMedium
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                AndroidView(
                    factory = { ctx ->
                        val themedContext = ContextThemeWrapper(
                            ctx,
                            com.google.android.material.R.style.Theme_Material3_Light
                        )
                        CardInputWidget(themedContext).also { widget ->
                            cardInputWidget = widget
                            widget.setPadding(16, 16, 16, 16)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(80.dp)
                )
            }

            Text(
                text = "Test: 4242 4242 4242 4242, any future date, any CVC",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Error display with retry option
            state.error?.let { errorMessage ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Payment Failed",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )

                        if (state.canRetry) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { viewModel.retryPayment() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Try Again")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Pay button
            Button(
                onClick = {
                    cardInputWidget?.cardParams?.let { cardParams ->
                        viewModel.processPayment(context, cardParams)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isProcessing && state.error == null
            ) {
                if (state.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Processing...")
                } else {
                    Text("Pay ₹${String.format(Locale.US, "%.2f", eventPrice)}")
                }
            }
        }
    }
}