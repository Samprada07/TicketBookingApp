package com.example.ticketbookingapp.network

import com.google.gson.annotations.SerializedName

// Request body for POST /api/tickets/book
data class TicketRequest(
    @SerializedName("event_id") val eventId: Int,
    @SerializedName("seat_number") val seatNumber: Int? = null   // optional — backend allows null
)

// Response from POST /api/tickets/book  →  { ticket: {...} }
data class Ticket(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("event_id") val eventId: Int,
    @SerializedName("seat_number") val seatNumber: Int?
)

data class TicketResponse(
    val ticket: Ticket
)

// Response from GET /api/tickets/my  →  { tickets: [...] }
data class MyTicket(
    val id: Int,
    @SerializedName("seat_number") val seatNumber: Int?,
    @SerializedName("booked_at") val bookedAt: String,
    @SerializedName("event_name") val eventName: String,
    val venue: String,
    @SerializedName("start_time") val startTime: String
)

data class MyTicketsResponse(
    val tickets: List<MyTicket>
)