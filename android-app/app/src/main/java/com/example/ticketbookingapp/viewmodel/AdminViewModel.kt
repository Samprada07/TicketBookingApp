package com.example.ticketbookingapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ticketbookingapp.network.ApiClient
import com.example.ticketbookingapp.network.ApiService
import com.example.ticketbookingapp.network.AuthManager
import com.example.ticketbookingapp.network.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminState(
    val events: List<Event> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class AdminViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(AdminState())
    val state = _state.asStateFlow()

    private val authManager = AuthManager(application)

    fun loadEvents() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val api = ApiClient.retrofit.create(ApiService::class.java)
                val response = api.getEvents()
                if (response.isSuccessful) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        events = response.body()?.events ?: emptyList()
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Failed to load events (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                Log.e("API", "Admin fetch events failed", e)
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Unknown error")
            }
        }
    }

    fun deleteEvent(eventId: Int) {
        val token = authManager.getToken() ?: return
        viewModelScope.launch {
            try {
                val api = ApiClient.retrofit.create(ApiService::class.java)
                val response = api.deleteEvent(token = "Bearer $token", id = eventId)
                if (response.isSuccessful) {
                    // Remove deleted event from local list
                    _state.value = _state.value.copy(
                        events = _state.value.events.filter { it.id != eventId }
                    )
                } else {
                    _state.value = _state.value.copy(error = "Failed to delete event (${response.code()})")
                }
            } catch (e: Exception) {
                Log.e("API", "Delete event failed", e)
                _state.value = _state.value.copy(error = e.message ?: "Unknown error")
            }
        }
    }
}