package com.example.ticketbookingapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ticketbookingapp.appUi.events.EventDetailEvent
import com.example.ticketbookingapp.appUi.events.EventDetailState
import com.example.ticketbookingapp.network.ApiClient
import com.example.ticketbookingapp.network.ApiService
import com.example.ticketbookingapp.network.AuthManager
import com.example.ticketbookingapp.network.TicketRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class EventDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(EventDetailState())
    val state = _state.asStateFlow()

    private val authManager = AuthManager(application)

    fun onEvent(event: EventDetailEvent) {
        when (event) {
            is EventDetailEvent.Load -> fetchEvent(event.eventId)
            is EventDetailEvent.SeatNumberChanged -> {
                _state.value = _state.value.copy(seatNumber = event.value)
            }
            is EventDetailEvent.BookTicket -> bookTicket(event.eventId)
        }
    }

    private fun fetchEvent(eventId: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val api = ApiClient.retrofit.create(ApiService::class.java)
                val response = api.getEvent(eventId)

                if (response.isSuccessful) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        event = response.body()?.event
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Event not found"
                    )
                }
            } catch (e: Exception) {
                Log.e("API", "Fetch event failed", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    private fun bookTicket(eventId: Int) {
        val token = authManager.getToken()
        if (token == null) {
            _state.value = _state.value.copy(error = "Not logged in")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isBooking = true, error = null)
            try {
                val api = ApiClient.retrofit.create(ApiService::class.java)
                val seatNumber = _state.value.seatNumber.trim().toIntOrNull()

                val response = api.bookTicket(
                    token = "Bearer $token",
                    request = TicketRequest(
                        eventId = eventId,
                        seatNumber = seatNumber
                    )
                )

                if (response.isSuccessful) {
                    _state.value = _state.value.copy(
                        isBooking = false,
                        bookingSuccess = true
                    )
                } else {
                    val errorMessage = try {
                        val json = JSONObject(response.errorBody()?.string() ?: "{}")
                        json.getString("error")
                    } catch (e: Exception) {
                        "Booking failed (${response.code()})"
                    }
                    _state.value = _state.value.copy(
                        isBooking = false,
                        error = errorMessage
                    )
                }
            } catch (e: Exception) {
                Log.e("API", "Book ticket failed", e)
                _state.value = _state.value.copy(
                    isBooking = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }
}