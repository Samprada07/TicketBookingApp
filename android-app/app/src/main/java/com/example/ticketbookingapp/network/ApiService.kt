package com.example.ticketbookingapp.network

interface ApiService {
    suspend fun register(request: RegisterRequest): RegisterResponse
}