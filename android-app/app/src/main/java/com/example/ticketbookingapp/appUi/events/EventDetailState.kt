package com.example.ticketbookingapp.appUi.events

import com.example.ticketbookingapp.network.Event

data class EventDetailState(
    val event: Event? = null,
    val seatNumber: String = "",
    val isLoading: Boolean = false,
    val isBooking: Boolean = false,
    val error: String? = null,
    val bookingSuccess: Boolean = false
)