package com.example.ticketbookingapp.appUi.payment

import android.view.ContextThemeWrapper
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

@Suppress("DEPRECATION")
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

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Payment") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(24.dp),
            Arrangement.spacedBy(16.dp)
        ) {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(eventName, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    if (seatNumber != null) {
                        Text("Seat: $seatNumber")
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Total: ₹${String.format(Locale.US, "%.2f", eventPrice)}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text("Enter Card Details", style = MaterialTheme.typography.titleMedium)

            Card(Modifier.fillMaxWidth()) {
                AndroidView(
                    factory = { ctx ->
                        // Wrap in AppCompat theme context
                        val themedContext =
                            ContextThemeWrapper(ctx, androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar)
                        CardInputWidget(themedContext).also {
                            cardInputWidget = it
                            it.setPadding(16, 16, 16, 16)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(80.dp)
                )
            }

            Text(
                "Test: 4242 4242 4242 4242, any future date, any CVC",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    cardInputWidget?.cardParams?.let {
                        viewModel.processPayment(context, it)
                    }
                },
                Modifier.fillMaxWidth(),
                enabled = !state.isProcessing
            ) {
                if (state.isProcessing) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Processing...")
                } else {
                    Text("Pay ₹${String.format(Locale.US, "%.2f", eventPrice)}")
                }
            }
        }
    }
}
