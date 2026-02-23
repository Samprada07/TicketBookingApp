package com.example.ticketbookingapp.network

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class AuthManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit { putString("jwt_token", token) }
    }

    fun getToken(): String? = prefs.getString("jwt_token", null)

    fun saveUserRole(role: String) {
        prefs.edit { putString("user_role", role) }
    }

    fun getUserRole(): String? = prefs.getString("user_role", "user")

    fun isAdmin(): Boolean = getUserRole() == "admin"

    fun saveUserInfo(name: String, email: String) {
        prefs.edit {
            putString("user_name", name)
            putString("user_email", email)
        }
    }

    fun getUserName(): String? = prefs.getString("user_name", null)

    fun getUserEmail(): String? = prefs.getString("user_email", null)

    fun clearToken() {
        prefs.edit { clear() }
    }
}