package com.example.ticketbookingapp.appUi.payment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ticketbookingapp.viewmodel.PaymentHistoryViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: PaymentHistoryViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadPaymentHistory()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment History") },
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

        if (state.payments.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(
                    "No payment history yet 💳",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            return@Scaffold
        }

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
            items(state.payments) { payment ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = when (payment.paymentStatus ?: "pending") {
                        "succeeded" -> CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                        "refunded" -> CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                        "failed" -> CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        else -> CardDefaults.cardColors()
                    }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    payment.eventName,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    payment.bookedAt,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                "Rs.${String.format(Locale.US, "%.2f", payment.price)}",
                                style = MaterialTheme.typography.titleLarge,
                                color = when (payment.paymentStatus) {
                                    "succeeded" -> MaterialTheme.colorScheme.primary
                                    "refunded" -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        // Status badge
                        Surface(
                            color = when (payment.paymentStatus ?: "pending") {
                                "succeeded" -> MaterialTheme.colorScheme.primary
                                "refunded" -> MaterialTheme.colorScheme.error
                                "failed" -> MaterialTheme.colorScheme.outline
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = when (payment.paymentStatus ?: "pending") {
                                    "succeeded" -> "✓ Paid"
                                    "refunded" -> "↩ Refunded"
                                    "failed" -> "✗ Failed"
                                    "pending" -> "⏳ Pending"
                                    else -> payment.paymentStatus
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        // Show refund ID if refunded
                        if (payment.refundId != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Refund ID: ${payment.refundId}",
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