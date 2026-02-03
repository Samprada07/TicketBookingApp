package com.example.ticketbookingapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ticketbookingapp.appUi.login.LoginEvent
import com.example.ticketbookingapp.appUi.login.LoginState
import com.example.ticketbookingapp.network.ApiClient
import com.example.ticketbookingapp.network.ApiService
import com.example.ticketbookingapp.network.AuthManager
import com.example.ticketbookingapp.network.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(LoginState())
    val state = _state.asStateFlow()

    private val authManager = AuthManager(application)

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged -> {
                _state.value = _state.value.copy(email = event.value)
            }
            is LoginEvent.PasswordChanged -> {
                _state.value = _state.value.copy(password = event.value)
            }
            LoginEvent.Submit -> {
                login()
            }
        }
    }

    private fun login() {
        val email = _state.value.email.trim()
        val password = _state.value.password

        // Basic client-side validation
        if (email.isEmpty()) {
            _state.value = _state.value.copy(error = "Email is required")
            return
        }
        if (password.isEmpty()) {
            _state.value = _state.value.copy(error = "Password is required")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            Log.d("API", "Starting login API call")
            try {
                val api = ApiClient.retrofit.create(ApiService::class.java)
                val response = api.login(
                    LoginRequest(
                        email = email,
                        password = password
                    )
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    body?.token?.let { token ->
                        authManager.saveToken(token)
                        Log.d("JWT", "Saved token: $token")
                    } ?: Log.d("JWT", "No token in response")

                    // ✅ Signal success → NavGraph will navigate to Home
                    _state.value = _state.value.copy(isLoading = false, isSuccess = true)

                } else {
                    // Parse actual error message from backend JSON: { "error": "..." }
                    val errorMessage = try {
                        val json = JSONObject(response.errorBody()?.string() ?: "{}")
                        json.getString("error")
                    } catch (e: Exception) {
                        "Login failed (${response.code()})"
                    }

                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = errorMessage
                    )
                }

            } catch (e: Exception) {
                Log.e("API", "Login failed", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }
}