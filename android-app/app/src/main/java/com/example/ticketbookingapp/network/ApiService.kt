package com.example.ticketbookingapp.network

import retrofit2.Response
import retrofit2.http.*

// LoginRequest lives here (you don't have a standalone file for it)
data class LoginRequest(
    val email: String,
    val password: String
)

interface ApiService {

    // ── Auth ────────────────────────────────────────────────────
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<UserResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<UserResponse>

    // ── Events ──────────────────────────────────────────────────
    @GET("api/events")
    suspend fun getEvents(): Response<EventListResponse>

    @GET("api/events/{id}")
    suspend fun getEvent(@Path("id") id: Int): Response<EventDetailResponse>

    // ── Tickets ─────────────────────────────────────────────────
    @POST("api/tickets/book")
    suspend fun bookTicket(
        @Header("Authorization") token: String,
        @Body request: TicketRequest
    ): Response<TicketResponse>

    @GET("api/tickets/my")
    suspend fun getMyTickets(
        @Header("Authorization") token: String
    ): Response<MyTicketsResponse>
}