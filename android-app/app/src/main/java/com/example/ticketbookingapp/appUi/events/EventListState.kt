package com.example.ticketbookingapp.appUi.events

import com.example.ticketbookingapp.network.Event

data class EventListState(
    val events: List<Event> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)