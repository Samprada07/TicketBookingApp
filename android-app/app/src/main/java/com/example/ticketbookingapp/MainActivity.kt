package com.example.ticketbookingapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ticketbookingapp.appUi.register.RegisterScreen
import com.example.ticketbookingapp.ui.theme.TicketBookingAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TicketBookingAppTheme {
                RegisterScreen()
            }
        }
    }
}