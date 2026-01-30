package com.example.ticketbookingapp.network

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)