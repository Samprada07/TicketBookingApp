package com.example.ticketbookingapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ticketbookingapp.appUi.tickets.MyTicketsEvent
import com.example.ticketbookingapp.appUi.tickets.MyTicketsState
import com.example.ticketbookingapp.network.ApiClient
import com.example.ticketbookingapp.network.ApiService
import com.example.ticketbookingapp.network.AuthManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class MyTicketsViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(MyTicketsState())
    val state = _state.asStateFlow()

    private val authManager = AuthManager(application)

    fun onEvent(event: MyTicketsEvent) {
        when (event) {
            MyTicketsEvent.Load, MyTicketsEvent.Retry -> fetchMyTickets()
            is MyTicketsEvent.CancelTicket -> cancelTicket(event.ticketId)
        }
    }

    private fun fetchMyTickets() {
        val token = authManager.getToken()
        if (token == null) {
            _state.value = _state.value.copy(error = "Not logged in")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val api = ApiClient.retrofit.create(ApiService::class.java)
                val response = api.getMyTickets(token = "Bearer $token")

                if (response.isSuccessful) {
                    val tickets = response.body()?.tickets ?: emptyList()
                    _state.value = _state.value.copy(isLoading = false, tickets = tickets)
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Failed to load tickets (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                Log.e("API", "Fetch tickets failed", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    private fun cancelTicket(ticketId: Int) {
        val token = authManager.getToken()
        if (token == null) {
            _state.value = _state.value.copy(error = "Not logged in")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(error = null, successMessage = null)
            try {
                val api = ApiClient.retrofit.create(ApiService::class.java)
                val response = api.cancelTicket(token = "Bearer $token", id = ticketId)

                if (response.isSuccessful) {
                    val message = response.body()?.message ?: "Ticket cancelled"
                    _state.value = _state.value.copy(successMessage = message)

                    delay(500)
                    fetchMyTickets()

                    delay(3000)
                    _state.value = _state.value.copy(successMessage = null)
                } else {
                    val errorMessage = try {
                        val json = JSONObject(response.errorBody()?.string() ?: "{}")
                        json.getString("error")
                    } catch (e: Exception) {
                        "Failed to cancel (${response.code()})"
                    }
                    _state.value = _state.value.copy(error = errorMessage)
                }
            } catch (e: Exception) {
                Log.e("API", "Cancel failed", e)
                _state.value = _state.value.copy(error = e.message ?: "Unknown error")
            }
        }
    }
}