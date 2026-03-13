package com.example.ticketbookingapp.appUi.payment

import com.stripe.android.model.CardParams

sealed class PaymentEvent {
    data class CreateIntent(val eventId: Int, val seatNumber: Int?) : PaymentEvent()
    data class ProcessPayment(val cardParams: CardParams) : PaymentEvent()
    object RetryPayment : PaymentEvent()
}