package com.example.ticketbookingapp.network

data class UserResponse(
    val token: String,
    val user: User
)

data class User(
    val id: Int,
    val name: String,
    val email: String
)
