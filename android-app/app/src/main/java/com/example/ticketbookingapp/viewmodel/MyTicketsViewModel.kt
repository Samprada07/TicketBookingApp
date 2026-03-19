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
import com.example.ticketbookingapp.network.RefundRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale

class MyTicketsViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(MyTicketsState())
    val state = _state.asStateFlow()

    private val authManager = AuthManager(application)

    fun onEvent(event: MyTicketsEvent) {
        when (event) {
            MyTicketsEvent.Load, MyTicketsEvent.Retry -> fetchMyTickets()
            is MyTicketsEvent.CancelTicket -> cancelTicketWithRefund(event.ticketId)
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

    private fun cancelTicketWithRefund(ticketId: Int) {
        val token = authManager.getToken()
        if (token == null) {
            _state.value = _state.value.copy(error = "Not logged in")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(error = null, successMessage = null, isRefunding = true)
            try {
                val api = ApiClient.retrofit.create(ApiService::class.java)
                val response = api.processRefund(
                    token = "Bearer $token",
                    request = RefundRequest(ticketId)
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    val refundData = response.body()!!
                    val message = "Refund of ₹${String.format(Locale.US, "%.2f", refundData.amount)} processed successfully. Will reflect in 5-7 days."
                    _state.value = _state.value.copy(
                        isRefunding = false,
                        successMessage = message
                    )

                    // Reload tickets to show updated status
                    delay(500)
                    fetchMyTickets()

                    // Clear success message after 4 seconds
                    delay(4000)
                    _state.value = _state.value.copy(successMessage = null)
                } else {
                    val errorMessage = try {
                        val json = JSONObject(response.errorBody()?.string() ?: "{}")
                        json.getString("error")
                    } catch (e: Exception) {
                        "Failed to process refund (${response.code()})"
                    }
                    _state.value = _state.value.copy(isRefunding = false, error = errorMessage)
                }
            } catch (e: Exception) {
                Log.e("API", "Refund failed", e)
                _state.value = _state.value.copy(
                    isRefunding = false,
                    error = "Refund failed: ${e.message}"
                )
            }
        }
    }
}