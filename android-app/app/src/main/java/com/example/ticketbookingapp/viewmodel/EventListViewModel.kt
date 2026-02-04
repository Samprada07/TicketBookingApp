package com.example.ticketbookingapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ticketbookingapp.appUi.events.EventListEvent
import com.example.ticketbookingapp.appUi.events.EventListState
import com.example.ticketbookingapp.network.ApiClient
import com.example.ticketbookingapp.network.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EventListViewModel : ViewModel() {
    private val _state = MutableStateFlow(EventListState())
    val state = _state.asStateFlow()

    init {
        onEvent(EventListEvent.Load)
    }

    fun onEvent(event: EventListEvent) {
        when (event) {
            EventListEvent.Load, EventListEvent.Retry -> fetchEvents()
        }
    }

    private fun fetchEvents() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val api = ApiClient.retrofit.create(ApiService::class.java)
                val response = api.getEvents()

                if (response.isSuccessful) {
                    val events = response.body()?.events ?: emptyList()
                    _state.value = _state.value.copy(isLoading = false, events = events)
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Failed to load events (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                Log.e("API", "Fetch events failed", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }
}