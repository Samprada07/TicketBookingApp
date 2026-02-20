package com.example.ticketbookingapp.appUi.admin

data class CreateEventState(
    val name: String = "",
    val description: String = "",
    val venue: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val totalSeats: String = "",
    val imageUrl: String = "",  // Stores uploaded image URL
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,  // separate flag for image upload
    val error: String? = null,
    val successMessage: String? = null,
    val isSuccess: Boolean = false
)