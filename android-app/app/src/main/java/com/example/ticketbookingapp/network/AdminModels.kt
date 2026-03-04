package com.example.ticketbookingapp.network

import com.google.gson.annotations.SerializedName

// Request body for POST /api/events (admin only)
data class CreateEventRequest(
    val name: String,
    val description: String,
    val venue: String,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    @SerializedName("total_seats") val totalSeats: Int,
    @SerializedName("image_url") val imageUrl: String? = null,
    val price: Double
)

// Response from GET /api/events/:id/bookings (admin only)
data class Booking(
    val id: Int,
    @SerializedName("seat_number") val seatNumber: Int?,
    @SerializedName("booked_at") val bookedAt: String,
    @SerializedName("user_name") val userName: String,
    @SerializedName("user_email") val userEmail: String
)

data class BookingsResponse(
    val bookings: List<Booking>
)