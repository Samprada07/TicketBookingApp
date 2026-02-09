package com.example.ticketbookingapp.appUi.events

import com.example.ticketbookingapp.network.Event

data class EventListState(
    val events: List<Event> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
) {
    // Computed property: filter events based on search query
    val filteredEvents: List<Event>
        get() = if (searchQuery.isBlank()) {
            events
        } else {
            events.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.venue.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true)
            }
        }
}