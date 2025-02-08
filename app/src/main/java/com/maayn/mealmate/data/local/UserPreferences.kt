package com.maayn.mealmate.data.local

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {

    private val sharedPref: SharedPreferences =
        context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)

    // ✅ Save Login State
    fun setUserLoggedIn(loggedIn: Boolean) {
        sharedPref.edit().putBoolean("is_logged_in", loggedIn).apply()
    }

    // ✅ Check Login State
    fun isUserLoggedIn(): Boolean {
        return sharedPref.getBoolean("is_logged_in", false)
    }

    // ✅ Save User ID
    fun setUserId(userId: String) {
        sharedPref.edit().putString("user_id", userId).apply()
    }

    // ✅ Get User ID
    fun getUserId(): String? {
        return sharedPref.getString("user_id", null)
    }

    // ✅ Save Username
    fun setUsername(username: String) {
        sharedPref.edit().putString("username", username).apply()
    }

    // ✅ Get Username
    fun getUsername(): String? {
        return sharedPref.getString("username", null)
    }

    // ✅ Save Email
    fun setEmail(email: String) {
        sharedPref.edit().putString("email", email).apply()
    }

    // ✅ Get Email
    fun getEmail(): String? {
        return sharedPref.getString("email", null)
    }

    // ✅ Save Sync Status (Has user backed up data to Firebase?)
    fun setSyncStatus(isSynced: Boolean) {
        sharedPref.edit().putBoolean("is_synced", isSynced).apply()
    }

    // ✅ Get Sync Status
    fun isDataSynced(): Boolean {
        return sharedPref.getBoolean("is_synced", false)
    }

    // ✅ Clear Data on Logout
    fun clearUserData() {
        sharedPref.edit().clear().apply()
    }
}
