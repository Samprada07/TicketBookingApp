package com.example.ticketbookingapp.appUi.payment

data class PaymentState(
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val clientSecret: String? = null,
    val ticketId: Int? = null,
    val amount: Double = 0.0,
    val paymentSuccess: Boolean = false,
    val error: String? = null,
    val canRetry: Boolean = false,
    val errorType: String? = null
)