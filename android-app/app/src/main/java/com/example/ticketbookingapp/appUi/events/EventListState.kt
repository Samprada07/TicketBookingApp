package com.example.ticketbookingapp.appUi.events

import com.example.ticketbookingapp.network.Event

data class EventListState(
    val events: List<Event> = emptyList(),
    val searchQuery: String = "",
    val sortBy: SortOption = SortOption.DATE,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val filteredEvents: List<Event>
        get() {
            // First filter by search
            val filtered = if (searchQuery.isBlank()) {
                events
            } else {
                events.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                            it.venue.contains(searchQuery, ignoreCase = true) ||
                            it.description.contains(searchQuery, ignoreCase = true)
                }
            }

            // Then sort
            return when (sortBy) {
                SortOption.DATE -> filtered.sortedBy { it.startTime }
                SortOption.NAME -> filtered.sortedBy { it.name }
                SortOption.SEATS -> filtered.sortedByDescending { it.availableSeats }
            }
        }
}