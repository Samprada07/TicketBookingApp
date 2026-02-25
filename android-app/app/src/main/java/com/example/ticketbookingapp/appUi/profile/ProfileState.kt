package com.example.ticketbookingapp.appUi.profile

data class ProfileState(
    val name: String = "",
    val email: String = "",
    val role: String = "user",
    val totalTickets: Int = 0,
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)