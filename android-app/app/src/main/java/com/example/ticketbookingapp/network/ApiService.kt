package com.example.ticketbookingapp.network

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

data class LoginRequest(
    val email: String,
    val password: String
)

data class UploadImageResponse(
    val imageUrl: String
)

interface ApiService {

    // ── Auth ────────────────────────────────────────────────────
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<UserResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<UserResponse>

    // ── Events (public) ──────────────────────────────────────────
    @GET("api/events")
    suspend fun getEvents(): Response<EventListResponse>

    @GET("api/events/{id}")
    suspend fun getEvent(@Path("id") id: Int): Response<EventDetailResponse>

    // ── Events (admin only) ──────────────────────────────────────
    @POST("api/events")
    suspend fun createEvent(
        @Header("Authorization") token: String,
        @Body request: CreateEventRequest
    ): Response<EventDetailResponse>

    @PUT("api/events/{id}")  // NEW
    suspend fun updateEvent(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: CreateEventRequest
    ): Response<EventDetailResponse>

    @DELETE("api/events/{id}")
    suspend fun deleteEvent(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Unit>

    @GET("api/events/{id}/bookings")
    suspend fun getEventBookings(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<BookingsResponse>

    // ── Upload (admin only) ──────────────────────────────────────
    @Multipart  // NEW
    @POST("api/upload")
    suspend fun uploadImage(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part
    ): Response<UploadImageResponse>

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