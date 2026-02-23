package com.example.ticketbookingapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ticketbookingapp.appUi.register.RegisterEvent
import com.example.ticketbookingapp.appUi.register.RegisterState
import com.example.ticketbookingapp.network.ApiClient
import com.example.ticketbookingapp.network.ApiService
import com.example.ticketbookingapp.network.AuthManager
import com.example.ticketbookingapp.network.RegisterRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class RegisterViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(RegisterState())
    val state = _state.asStateFlow()

    private val authManager = AuthManager(application)

    fun onEvent(event: RegisterEvent) {
        when (event) {
            is RegisterEvent.NameChanged -> {
                _state.value = _state.value.copy(name = event.value)
            }
            is RegisterEvent.EmailChanged -> {
                _state.value = _state.value.copy(email = event.value)
            }
            is RegisterEvent.PasswordChanged -> {
                _state.value = _state.value.copy(password = event.value)
            }
            RegisterEvent.Submit -> {
                register()
            }
        }
    }

    private fun register() {
        val name = _state.value.name.trim()
        val email = _state.value.email.trim()
        val password = _state.value.password

        if (name.isEmpty()) {
            _state.value = _state.value.copy(error = "Name is required")
            return
        }
        if (email.isEmpty()) {
            _state.value = _state.value.copy(error = "Email is required")
            return
        }
        if (password.length < 6) {
            _state.value = _state.value.copy(error = "Password must be at least 6 characters")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            Log.d("API", "Starting register API call")
            try {
                val api = ApiClient.retrofit.create(ApiService::class.java)
                val response = api.register(
                    RegisterRequest(
                        name = name,
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

                    body?.user?.role?.let { role ->
                        authManager.saveUserRole(role)
                        Log.d("AUTH", "User role: $role")
                    }

                    body?.user?.let { user ->
                        authManager.saveUserInfo(user.name, user.email)
                        Log.d("AUTH", "Saved user info: ${user.name}, ${user.email}")
                    }

                    _state.value = _state.value.copy(isLoading = false, isSuccess = true)

                } else {
                    val errorMessage = try {
                        val json = JSONObject(response.errorBody()?.string() ?: "{}")
                        json.getString("error")
                    } catch (e: Exception) {
                        "Registration failed (${response.code()})"
                    }

                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = errorMessage
                    )
                }

            } catch (e: Exception) {
                Log.e("API", "Register failed", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }
}