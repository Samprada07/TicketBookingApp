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

class PaymentViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(PaymentState())
    val state = _state.asStateFlow()
    private val authManager = AuthManager(application)

    fun createPaymentIntent(eventId: Int, seatNumber: Int?) {
        val token = authManager.getToken() ?: run {
            _state.value = _state.value.copy(error = "Not logged in")
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
                            amount = it.amount
                        )
                    }
                } else {
                    val error = try {
                        JSONObject(response.errorBody()?.string() ?: "{}").getString("error")
                    } catch (e: Exception) {
                        "Failed (${response.code()})"
                    }
                    _state.value = _state.value.copy(isLoading = false, error = error)
                }
            } catch (e: Exception) {
                Log.e("Payment", "Intent failed", e)
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun processPayment(context: Context, cardParams: CardParams) {
        val clientSecret = _state.value.clientSecret ?: run {
            _state.value = _state.value.copy(error = "Payment not initialized")
            return
        }

        val token = authManager.getToken() ?: run {
            _state.value = _state.value.copy(error = "Not logged in")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isProcessing = true, error = null)
            try {
                val stripe = Stripe(context, PaymentConfiguration.getInstance(context).publishableKey)
                val confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                    PaymentMethodCreateParams.createCard(cardParams),
                    clientSecret
                )

                stripe.confirmPayment(context as androidx.activity.ComponentActivity, confirmParams)
                delay(2000)

                val api = ApiClient.retrofit.create(ApiService::class.java)
                val paymentIntentId = clientSecret.split("_secret_")[0]
                val response = api.confirmPayment("Bearer $token", ConfirmPaymentRequest(paymentIntentId))

                if (response.isSuccessful && response.body()?.success == true) {
                    _state.value = _state.value.copy(isProcessing = false, paymentSuccess = true)
                } else {
                    _state.value = _state.value.copy(
                        isProcessing = false,
                        error = response.body()?.message ?: "Confirmation failed"
                    )
                }
            } catch (e: Exception) {
                Log.e("Payment", "Failed", e)
                _state.value = _state.value.copy(isProcessing = false, error = "Payment failed: ${e.message}")
            }
        }
    }
}