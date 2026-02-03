package com.example.ticketbookingapp.network

import com.google.gson.annotations.SerializedName

data class Event(
    val id: Int,
    val name: String,
    val description: String,
    val venue: String,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    @SerializedName("total_seats") val totalSeats: Int,
    @SerializedName("available_seats") val availableSeats: Int
)

data class EventListResponse(
    val events: List<Event>
)

data class EventDetailResponse(
    val event: Event
)