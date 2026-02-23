package com.example.ticketbookingapp.appUi.events

sealed class EventListEvent {
    object Load : EventListEvent()
    object Retry : EventListEvent()
    data class SearchQueryChanged(val query: String) : EventListEvent()
    data class SortByChanged(val sortBy: SortOption) : EventListEvent()
}

enum class SortOption {
    DATE,      // Sort by start_time
    NAME,      // Sort alphabetically
    SEATS      // Sort by available_seats (most to least)
}