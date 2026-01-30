package com.example.ticketbookingapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ticketbookingapp.appUi.register.RegisterEvent
import com.example.ticketbookingapp.appUi.register.RegisterState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {

    private val _state = MutableStateFlow(RegisterState())
    val state = _state.asStateFlow()

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
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            // Fake API call
            delay(1500)

            _state.value = _state.value.copy(
                isLoading = false
            )
        }
    }
}
