package com.example.ticketbookingapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ticketbookingapp.appUi.profile.ProfileEvent
import com.example.ticketbookingapp.appUi.profile.ProfileState
import com.example.ticketbookingapp.network.ApiClient
import com.example.ticketbookingapp.network.ApiService
import com.example.ticketbookingapp.network.AuthManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

data class UpdateProfileRequest(
    val name: String,
    val email: String
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(ProfileState())
    val state = _state.asStateFlow()

    private val authManager = AuthManager(application)

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val name = authManager.getUserName() ?: "Unknown"
            val email = authManager.getUserEmail() ?: "Unknown"
            val role = authManager.getUserRole() ?: "user"

            // Load ticket count
            val token = authManager.getToken()
            var ticketCount = 0

            if (token != null) {
                try {
                    val api = ApiClient.retrofit.create(ApiService::class.java)
                    val response = api.getMyTickets(token = "Bearer $token")
                    if (response.isSuccessful) {
                        ticketCount = response.body()?.tickets?.size ?: 0
                    }
                } catch (e: Exception) {
                    Log.e("API", "Failed to load tickets", e)
                }
            }

            _state.value = _state.value.copy(
                name = name,
                email = email,
                role = role,
                totalTickets = ticketCount,
                isLoading = false
            )
        }
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.NameChanged -> {
                _state.value = _state.value.copy(name = event.value)
            }
            is ProfileEvent.EmailChanged -> {
                _state.value = _state.value.copy(email = event.value)
            }
            ProfileEvent.ToggleEditMode -> {
                _state.value = _state.value.copy(
                    isEditMode = !_state.value.isEditMode,
                    error = null
                )
            }
            ProfileEvent.Save -> saveProfile()
        }
    }

    private fun saveProfile() {
        val state = _state.value

        // Validation
        if (state.name.isBlank()) {
            _state.value = _state.value.copy(error = "Name cannot be empty")
            return
        }
        if (state.email.isBlank()) {
            _state.value = _state.value.copy(error = "Email cannot be empty")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _state.value = _state.value.copy(error = "Invalid email format")
            return
        }

        val token = authManager.getToken()
        if (token == null) {
            _state.value = _state.value.copy(error = "Authentication required")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null, successMessage = null)

            try {
                val api = ApiClient.retrofit.create(ApiService::class.java)
                val response = api.updateProfile(
                    token = "Bearer $token",
                    request = UpdateProfileRequest(
                        name = state.name.trim(),
                        email = state.email.trim()
                    )
                )

                if (response.isSuccessful) {
                    val updatedUser = response.body()?.user
                    if (updatedUser != null) {
                        // Save to AuthManager
                        authManager.saveUserInfo(updatedUser.name, updatedUser.email)

                        _state.value = _state.value.copy(
                            isSaving = false,
                            isEditMode = false,
                            successMessage = "Profile updated successfully! ✓"
                        )

                        // Clear success message after 2 seconds
                        delay(2000)
                        _state.value = _state.value.copy(successMessage = null)
                    }
                } else {
                    val errorMessage = try {
                        val json = JSONObject(response.errorBody()?.string() ?: "{}")
                        json.getString("error")
                    } catch (e: Exception) {
                        "Failed to update profile (${response.code()})"
                    }
                    _state.value = _state.value.copy(isSaving = false, error = errorMessage)
                }
            } catch (e: Exception) {
                Log.e("API", "Update profile failed", e)
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }
}