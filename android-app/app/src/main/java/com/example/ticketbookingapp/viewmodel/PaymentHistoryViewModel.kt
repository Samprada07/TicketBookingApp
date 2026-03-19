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

data class PaymentHistoryState(
    val payments: List<MyTicket> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class PaymentHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(PaymentHistoryState())
    val state = _state.asStateFlow()

    private val authManager = AuthManager(application)

    fun loadPaymentHistory() {
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
                    // Filter only tickets with payment info (exclude pending/failed)
                    val payments = tickets.filter {
                        it.paymentStatus == "succeeded" || it.paymentStatus == "refunded"
                    }
                    _state.value = _state.value.copy(
                        isLoading = false,
                        payments = payments
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Failed to load payment history"
                    )
                }
            } catch (e: Exception) {
                Log.e("PaymentHistory", "Load failed", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
}