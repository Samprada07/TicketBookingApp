package com.example.ticketbookingapp.appUi.admin

data class CreateEventState(
    val name: String = "",
    val description: String = "",
    val venue: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val totalSeats: String = "",
    val price: String = "",
    val imageUrl: String = "",
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val isSuccess: Boolean = false
)