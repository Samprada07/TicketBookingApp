package com.example.ticketbookingapp.appUi.login

data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false   // triggers navigation on success
)