package com.example.ticketbookingapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ticketbookingapp.network.ApiClient
import com.example.ticketbookingapp.network.ApiService
import com.example.ticketbookingapp.network.AuthManager
import com.example.ticketbookingapp.network.MyTicket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FailedPaymentsState(
    val failedPayments: List<MyTicket> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class FailedPaymentsViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(FailedPaymentsState())
    val state = _state.asStateFlow()

    private val authManager = AuthManager(application)

    fun loadFailedPayments() {
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
                    // Filter only failed and pending payments
                    val failedPayments = tickets.filter { ticket ->
                        ticket.eventId > 0 &&
                                (ticket.paymentStatus == "failed" || ticket.paymentStatus == "pending") &&
                                ticket.status != "expired" && ticket.status != "cancelled"
                    }
                    _state.value = _state.value.copy(
                        isLoading = false,
                        failedPayments = failedPayments
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Failed to load payments"
                    )
                }
            } catch (e: Exception) {
                Log.e("FailedPayments", "Load failed", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
}