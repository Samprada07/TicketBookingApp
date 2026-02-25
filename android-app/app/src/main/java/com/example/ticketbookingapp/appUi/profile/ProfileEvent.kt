package com.example.ticketbookingapp.appUi.profile

sealed class ProfileEvent {
    data class NameChanged(val value: String) : ProfileEvent()
    data class EmailChanged(val value: String) : ProfileEvent()
    object Save : ProfileEvent()
    object ToggleEditMode : ProfileEvent()
}