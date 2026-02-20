package com.example.ticketbookingapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ticketbookingapp.network.ApiClient
import com.example.ticketbookingapp.network.ApiService
import com.example.ticketbookingapp.network.AuthManager
import com.example.ticketbookingapp.network.Booking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EventBookingsState(
    val bookings: List<Booking> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class EventBookingsViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(EventBookingsState())
    val state = _state.asStateFlow()

    private val authManager = AuthManager(application)

    fun loadBookings(eventId: Int) {
        val token = authManager.getToken() ?: return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val api = ApiClient.retrofit.create(ApiService::class.java)
                val response = api.getEventBookings(token = "Bearer $token", id = eventId)

                if (response.isSuccessful) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        bookings = response.body()?.bookings ?: emptyList()
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Failed to load bookings (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                Log.e("API", "Fetch bookings failed", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }
}