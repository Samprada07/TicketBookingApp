package com.example.ticketbookingapp.appUi.admin

import android.net.Uri

sealed class CreateEventEvent {
    data class NameChanged(val value: String) : CreateEventEvent()
    data class DescriptionChanged(val value: String) : CreateEventEvent()
    data class VenueChanged(val value: String) : CreateEventEvent()
    data class StartTimeChanged(val value: String) : CreateEventEvent()
    data class EndTimeChanged(val value: String) : CreateEventEvent()
    data class TotalSeatsChanged(val value: String) : CreateEventEvent()
    data class PriceChanged(val value: String) : CreateEventEvent()
    data class ImageSelected(val uri: Uri) : CreateEventEvent()
    object Submit : CreateEventEvent()
    data class Update(val eventId: Int) : CreateEventEvent()
}