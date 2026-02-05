package com.example.ticketbookingapp.appUi.tickets

import com.example.ticketbookingapp.network.MyTicket

data class MyTicketsState(
    val tickets: List<MyTicket> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)