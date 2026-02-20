package com.example.ticketbookingapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ticketbookingapp.appUi.admin.CreateEventEvent
import com.example.ticketbookingapp.appUi.admin.CreateEventState
import com.example.ticketbookingapp.network.ApiClient
import com.example.ticketbookingapp.network.ApiService
import com.example.ticketbookingapp.network.AuthManager
import com.example.ticketbookingapp.network.CreateEventRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class CreateEventViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(CreateEventState())
    val state = _state.asStateFlow()

    private val authManager = AuthManager(application)

    fun onEvent(event: CreateEventEvent) {
        when (event) {
            is CreateEventEvent.NameChanged -> _state.value = _state.value.copy(name = event.value)
            is CreateEventEvent.DescriptionChanged -> _state.value = _state.value.copy(description = event.value)
            is CreateEventEvent.VenueChanged -> _state.value = _state.value.copy(venue = event.value)
            is CreateEventEvent.StartTimeChanged -> _state.value = _state.value.copy(startTime = event.value)
            is CreateEventEvent.EndTimeChanged -> _state.value = _state.value.copy(endTime = event.value)
            is CreateEventEvent.TotalSeatsChanged -> _state.value = _state.value.copy(totalSeats = event.value)
            is CreateEventEvent.ImageUrlChanged -> _state.value = _state.value.copy(imageUrl = event.value)
            CreateEventEvent.Submit -> createEvent()
        }
    }

    private fun createEvent() {
        val state = _state.value

        // Validation
        if (state.name.isBlank()) { _state.value = _state.value.copy(error = "Name is required"); return }
        if (state.description.isBlank()) { _state.value = _state.value.copy(error = "Description is required"); return }
        if (state.venue.isBlank()) { _state.value = _state.value.copy(error = "Venue is required"); return }
        if (state.startTime.isBlank()) { _state.value = _state.value.copy(error = "Start time is required"); return }
        if (state.endTime.isBlank()) { _state.value = _state.value.copy(error = "End time is required"); return }
        if (state.totalSeats.toIntOrNull() == null) { _state.value = _state.value.copy(error = "Total seats must be a number"); return }

        val token = authManager.getToken() ?: return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val api = ApiClient.retrofit.create(ApiService::class.java)
                val response = api.createEvent(
                    token = "Bearer $token",
                    request = CreateEventRequest(
                        name = state.name.trim(),
                        description = state.description.trim(),
                        venue = state.venue.trim(),
                        startTime = state.startTime.trim(),
                        endTime = state.endTime.trim(),
                        totalSeats = state.totalSeats.toInt(),
                        imageUrl = state.imageUrl.trim().ifBlank { null }
                    )
                )

                if (response.isSuccessful) {
                    _state.value = _state.value.copy(isLoading = false, isSuccess = true)
                } else {
                    val errorMessage = try {
                        val json = JSONObject(response.errorBody()?.string() ?: "{}")
                        json.getString("error")
                    } catch (e: Exception) {
                        "Failed to create event (${response.code()})"
                    }
                    _state.value = _state.value.copy(isLoading = false, error = errorMessage)
                }
            } catch (e: Exception) {
                Log.e("API", "Create event failed", e)
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Unknown error")
            }
        }
    }
}