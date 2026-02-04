package com.example.ticketbookingapp.appUi.events

sealed class EventDetailEvent {
    data class Load(val eventId: Int) : EventDetailEvent()
    data class SeatNumberChanged(val value: String) : EventDetailEvent()
    data class BookTicket(val eventId: Int) : EventDetailEvent()
}