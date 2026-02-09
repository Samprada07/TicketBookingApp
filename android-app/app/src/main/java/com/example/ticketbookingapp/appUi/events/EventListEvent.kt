package com.example.ticketbookingapp.appUi.events

sealed class EventListEvent {
    object Load : EventListEvent()
    object Retry : EventListEvent()
    data class SearchQueryChanged(val query: String) : EventListEvent()
}