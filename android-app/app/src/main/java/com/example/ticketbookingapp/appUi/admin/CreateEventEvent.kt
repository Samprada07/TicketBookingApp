package com.example.ticketbookingapp.appUi.admin

sealed class CreateEventEvent {
    data class NameChanged(val value: String) : CreateEventEvent()
    data class DescriptionChanged(val value: String) : CreateEventEvent()
    data class VenueChanged(val value: String) : CreateEventEvent()
    data class StartTimeChanged(val value: String) : CreateEventEvent()
    data class EndTimeChanged(val value: String) : CreateEventEvent()
    data class TotalSeatsChanged(val value: String) : CreateEventEvent()
    data class ImageUrlChanged(val value: String) : CreateEventEvent()
    object Submit : CreateEventEvent()
}