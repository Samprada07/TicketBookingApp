package com.example.ticketbookingapp.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ticketbookingapp.appUi.admin.CreateEventEvent
import com.example.ticketbookingapp.appUi.admin.CreateEventState
import com.example.ticketbookingapp.network.ApiClient
import com.example.ticketbookingapp.network.ApiService
import com.example.ticketbookingapp.network.AuthManager
import com.example.ticketbookingapp.network.CreateEventRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class CreateEventViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(CreateEventState())
    val state = _state.asStateFlow()

    private val authManager = AuthManager(application)
    private val context = application.applicationContext

    fun onEvent(event: CreateEventEvent) {
        when (event) {
            is CreateEventEvent.NameChanged -> _state.value = _state.value.copy(name = event.value)
            is CreateEventEvent.DescriptionChanged -> _state.value = _state.value.copy(description = event.value)
            is CreateEventEvent.VenueChanged -> _state.value = _state.value.copy(venue = event.value)
            is CreateEventEvent.StartTimeChanged -> _state.value = _state.value.copy(startTime = event.value)
            is CreateEventEvent.EndTimeChanged -> _state.value = _state.value.copy(endTime = event.value)
            is CreateEventEvent.TotalSeatsChanged -> _state.value = _state.value.copy(totalSeats = event.value)
            is CreateEventEvent.ImageSelected -> uploadImage(event.uri)
            CreateEventEvent.Submit -> createEvent()
            is CreateEventEvent.Update -> updateEvent(event.eventId)
            is CreateEventEvent.PriceChanged -> _state.value = _state.value.copy(price = event.value)
        }
    }

    fun loadEventForEdit(eventId: Int) {
        viewModelScope.launch {
            try {
                val api = ApiClient.retrofit.create(ApiService::class.java)
                val response = api.getEvent(eventId)
                if (response.isSuccessful) {
                    response.body()?.event?.let { event ->
                        _state.value = _state.value.copy(
                            name = event.name,
                            description = event.description,
                            venue = event.venue,
                            startTime = event.startTime,
                            endTime = event.endTime,
                            totalSeats = event.totalSeats.toString(),
                            imageUrl = event.imageUrl ?: "",
                            price = event.price.toString()
                        )
                    }
                } else {
                    _state.value = _state.value.copy(error = "Failed to load event details")
                }
            } catch (e: Exception) {
                Log.e("API", "Load event for edit failed", e)
                _state.value = _state.value.copy(error = getErrorMessage(e))
            }
        }
    }

    private fun uploadImage(uri: Uri) {
        val token = authManager.getToken()
        if (token == null) {
            _state.value = _state.value.copy(error = "Authentication required")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isUploading = true, error = null, successMessage = null)
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _state.value = _state.value.copy(isUploading = false, error = "Failed to read image file")
                    return@launch
                }

                val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
                inputStream.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }

                val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("image", file.name, requestBody)

                val api = ApiClient.retrofit.create(ApiService::class.java)
                val response = api.uploadImage(token = "Bearer $token", image = part)

                if (response.isSuccessful) {
                    val imageUrl = response.body()?.imageUrl ?: ""
                    _state.value = _state.value.copy(
                        isUploading = false,
                        imageUrl = imageUrl,
                        successMessage = "Image uploaded successfully! ✓"
                    )
                    // Clear success message after 2 seconds
                    delay(2000)
                    _state.value = _state.value.copy(successMessage = null)
                } else {
                    val errorMsg = try {
                        val json = JSONObject(response.errorBody()?.string() ?: "{}")
                        json.getString("error")
                    } catch (e: Exception) {
                        "Image upload failed (${response.code()})"
                    }
                    _state.value = _state.value.copy(isUploading = false, error = errorMsg)
                }

                file.delete()
            } catch (e: Exception) {
                Log.e("API", "Upload image failed", e)
                _state.value = _state.value.copy(
                    isUploading = false,
                    error = "Upload failed: ${getErrorMessage(e)}"
                )
            }
        }
    }

    private fun createEvent() {
        val state = _state.value
        val price = state.price.toDoubleOrNull()

        // Validation
        if (state.name.isBlank()) { _state.value = _state.value.copy(error = "Event name is required"); return }
        if (state.description.isBlank()) { _state.value = _state.value.copy(error = "Description is required"); return }
        if (state.venue.isBlank()) { _state.value = _state.value.copy(error = "Venue is required"); return }
        if (state.startTime.isBlank()) { _state.value = _state.value.copy(error = "Start time is required"); return }
        if (state.endTime.isBlank()) { _state.value = _state.value.copy(error = "End time is required"); return }
        if (price == null || price <= 0) {
            _state.value = _state.value.copy(error = "Price must be a valid positive number")
            return
        }

        val totalSeats = state.totalSeats.toIntOrNull()
        if (totalSeats == null || totalSeats <= 0) {
            _state.value = _state.value.copy(error = "Total seats must be a positive number")
            return
        }

        val token = authManager.getToken()
        if (token == null) {
            _state.value = _state.value.copy(error = "Authentication required")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, successMessage = null)
            try {
                val api = ApiClient.retrofit.create(ApiService::class.java)
                val response = api.createEvent(
                    token = "Bearer $token",
                    request = CreateEventRequest(
                        name = state.name.trim(),
                        description = state.description.trim(),
                        venue = state.venue.trim(),
                        startTime = state.startTime.trim(),
                        endTime = state.endTime.trim(),
                        totalSeats = totalSeats,
                        imageUrl = state.imageUrl.ifBlank { null },
                        price = price
                    )
                )

                if (response.isSuccessful) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        successMessage = "Event created successfully! 🎉"
                    )
                } else {
                    val errorMessage = try {
                        val json = JSONObject(response.errorBody()?.string() ?: "{}")
                        json.getString("error")
                    } catch (e: Exception) {
                        "Failed to create event (${response.code()})"
                    }
                    _state.value = _state.value.copy(isLoading = false, error = errorMessage)
                }
            } catch (e: Exception) {
                Log.e("API", "Create event failed", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = getErrorMessage(e)
                )
            }
        }
    }

    private fun updateEvent(eventId: Int) {
        val state = _state.value
        val price = state.price.toDoubleOrNull()

        // Validation
        if (state.name.isBlank()) { _state.value = _state.value.copy(error = "Event name is required"); return }
        if (state.description.isBlank()) { _state.value = _state.value.copy(error = "Description is required"); return }
        if (state.venue.isBlank()) { _state.value = _state.value.copy(error = "Venue is required"); return }
        if (state.startTime.isBlank()) { _state.value = _state.value.copy(error = "Start time is required"); return }
        if (state.endTime.isBlank()) { _state.value = _state.value.copy(error = "End time is required"); return }
        if (price == null || price <= 0) {
            _state.value = _state.value.copy(error = "Price must be a valid positive number")
            return
        }

        val totalSeats = state.totalSeats.toIntOrNull()
        if (totalSeats == null || totalSeats <= 0) {
            _state.value = _state.value.copy(error = "Total seats must be a positive number")
            return
        }

        val token = authManager.getToken()
        if (token == null) {
            _state.value = _state.value.copy(error = "Authentication required")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, successMessage = null)
            try {
                val api = ApiClient.retrofit.create(ApiService::class.java)
                val response = api.updateEvent(
                    token = "Bearer $token",
                    id = eventId,
                    request = CreateEventRequest(
                        name = state.name.trim(),
                        description = state.description.trim(),
                        venue = state.venue.trim(),
                        startTime = state.startTime.trim(),
                        endTime = state.endTime.trim(),
                        totalSeats = totalSeats,
                        imageUrl = state.imageUrl.ifBlank { null },
                        price = price
                    )
                )

                if (response.isSuccessful) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        successMessage = "Event updated successfully! ✓"
                    )
                } else {
                    val errorMessage = try {
                        val json = JSONObject(response.errorBody()?.string() ?: "{}")
                        json.getString("error")
                    } catch (e: Exception) {
                        "Failed to update event (${response.code()})"
                    }
                    _state.value = _state.value.copy(isLoading = false, error = errorMessage)
                }
            } catch (e: Exception) {
                Log.e("API", "Update event failed", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = getErrorMessage(e)
                )
            }
        }
    }

    private fun getErrorMessage(e: Exception): String {
        return when (e) {
            is UnknownHostException -> "No internet connection. Please check your network."
            is SocketTimeoutException -> "Request timed out. Please try again."
            is java.io.IOException -> "Network error. Please check your connection."
            else -> e.message ?: "An unexpected error occurred"
        }
    }
}