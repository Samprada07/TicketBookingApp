package com.example.ticketbookingapp.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ticketbookingapp.appUi.payment.PaymentState
import com.example.ticketbookingapp.network.*
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.model.CardParams
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethodCreateParams
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class PaymentViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(PaymentState())
    val state = _state.asStateFlow()
    private val authManager = AuthManager(application)

    // Store for retry
    private var currentEventId: Int? = null
    private var currentSeatNumber: Int? = null
    private var currentCardParams: CardParams? = null
    private var currentContext: Context? = null

    fun createPaymentIntent(eventId: Int, seatNumber: Int?) {
        currentEventId = eventId
        currentSeatNumber = seatNumber

        val token = authManager.getToken() ?: run {
            _state.value = _state.value.copy(error = "Please log in to continue")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val api = ApiClient.retrofit.create(ApiService::class.java)
                val response = api.createPaymentIntent(
                    "Bearer $token",
                    CreatePaymentIntentRequest(eventId, seatNumber)
                )

                if (response.isSuccessful) {
                    response.body()?.let {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            clientSecret = it.clientSecret,
                            ticketId = it.ticketId,
                            amount = it.amount,
                            canRetry = false
                        )
                    }
                } else {
                    val error = parseApiError(response.errorBody()?.string(), response.code())
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error,
                        canRetry = isRetryableError(response.code())
                    )
                }
            } catch (e: Exception) {
                Log.e("Payment", "Intent failed", e)
                val errorMessage = getNetworkErrorMessage(e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = errorMessage,
                    canRetry = true
                )
            }
        }
    }

    fun processPayment(context: Context, cardParams: CardParams) {
        currentContext = context
        currentCardParams = cardParams

        val clientSecret = _state.value.clientSecret ?: run {
            _state.value = _state.value.copy(error = "Payment session expired. Please try again.")
            return
        }

        val token = authManager.getToken() ?: run {
            _state.value = _state.value.copy(error = "Please log in to continue")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isProcessing = true, error = null)
            try {
                val stripe =
                    Stripe(context, PaymentConfiguration.getInstance(context).publishableKey)
                val confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                    PaymentMethodCreateParams.createCard(cardParams),
                    clientSecret
                )

                // Confirm with Stripe
                stripe.confirmPayment(context as androidx.activity.ComponentActivity, confirmParams)
                delay(2000) // Wait for Stripe processing

                // Verify with backend
                val api = ApiClient.retrofit.create(ApiService::class.java)
                val paymentIntentId = clientSecret.split("_secret_")[0]
                val response =
                    api.confirmPayment("Bearer $token", ConfirmPaymentRequest(paymentIntentId))

                if (response.isSuccessful && response.body()?.success == true) {
                    _state.value = _state.value.copy(
                        isProcessing = false,
                        paymentSuccess = true,
                        canRetry = false
                    )
                } else {
                    val error = response.body()?.message ?: "Payment verification failed"
                    _state.value = _state.value.copy(
                        isProcessing = false,
                        error = error,
                        canRetry = true,
                        errorType = "verification_failed"
                    )
                }
            } catch (e: Exception) {
                Log.e("Payment", "Failed", e)
                val errorMessage = when {
                    e.message?.contains(
                        "card",
                        ignoreCase = true
                    ) == true -> "Card declined: ${e.message}"

                    e.message?.contains(
                        "invalid",
                        ignoreCase = true
                    ) == true -> "Invalid card details"

                    e is SocketTimeoutException -> "Payment timed out"
                    e is UnknownHostException -> "No internet connection"
                    else -> "Payment failed: ${e.message ?: "Unknown error"}"
                }
                _state.value = _state.value.copy(
                    isProcessing = false,
                    error = errorMessage,
                    canRetry = true
                )
            }
        }
    }

    fun retryPayment() {
        val context = currentContext
        val cardParams = currentCardParams
        val eventId = currentEventId
        val seatNumber = currentSeatNumber

        when {
            _state.value.clientSecret == null && eventId != null -> {
                // Retry creating payment intent
                createPaymentIntent(eventId, seatNumber)
            }

            context != null && cardParams != null -> {
                // Retry processing payment
                processPayment(context, cardParams)
            }

            else -> {
                _state.value = _state.value.copy(error = "Cannot retry. Please start over.")
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun parseApiError(errorBody: String?, code: Int): String {
        return try {
            val json = JSONObject(errorBody ?: "{}")
            json.optString("error", "Payment failed (Error $code)")
        } catch (e: Exception) {
            when (code) {
                400 -> "Invalid payment details"
                401 -> "Session expired. Please log in again"
                404 -> "Event not found"
                409 -> "This ticket was already booked"
                500 -> "Server error. Please try again later"
                else -> "Payment failed (Error $code)"
            }
        }
    }

    private fun getNetworkErrorMessage(e: Exception): String {
        return when (e) {
            is UnknownHostException -> "No internet connection. Please check your network."
            is SocketTimeoutException -> "Request timed out. Please try again."
            else -> "Network error: ${e.message ?: "Please check your connection"}"
        }
    }

    private fun isRetryableError(code: Int): Boolean {
        return code in listOf(408, 500, 502, 503, 504) // Timeout and server errors
    }
}