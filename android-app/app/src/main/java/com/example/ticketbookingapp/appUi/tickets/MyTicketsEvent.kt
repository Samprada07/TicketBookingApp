package com.example.ticketbookingapp.appUi.tickets

sealed class MyTicketsEvent {
    object Load : MyTicketsEvent()
    object Retry : MyTicketsEvent()
}