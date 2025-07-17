package com.skripsi.nisuk.backend.preference

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object SharedPreferencesHelper {

    private const val PREF_NAME = "user_prefs1"
    private const val KEY_USERNAME = "username1"
    private const val KEY_EMAIL = "email1"
    private const val IS_DISABILITY = "is_disability1"

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveUsername(username: String, context: Context) {
        val sharedPreferences = getSharedPreferences(context)
        sharedPreferences.edit().putString(KEY_USERNAME, username).apply()
    }
    fun saveEmail(email: String, context: Context) {
        val sharedPreferences = getSharedPreferences(context)
        sharedPreferences.edit().putString(KEY_EMAIL, email).apply()
    }
    fun getEmail(context: Context): String? {
        val sharedPreferences = getSharedPreferences(context)
        return sharedPreferences.getString(KEY_EMAIL, "")?: ""
    }
    fun getUsername(context: Context): String? {
        val sharedPreferences = getSharedPreferences(context)
        return sharedPreferences.getString(KEY_USERNAME, "")?: ""
    }
    fun saveDisabilityStatus(isDisability: Boolean, context: Context) {
        val sharedPreferences = getSharedPreferences(context)
        sharedPreferences.edit { putBoolean(IS_DISABILITY, isDisability) }
    }
    fun getDisabilityStatus(context: Context): Boolean {
        val sharedPreferences = getSharedPreferences(context)
        return sharedPreferences.getBoolean(IS_DISABILITY, false)
    }
    fun clearSession(context: Context) {
        val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            clear()
            apply()
        }
    }

    fun deleteUsername(context: Context) {
        val sharedPreferences = getSharedPreferences(context)
        sharedPreferences.edit().remove(KEY_USERNAME).apply()
    }
    fun deleteEmail(context: Context) {
        val sharedPreferences = getSharedPreferences(context)
        sharedPreferences.edit().remove(KEY_EMAIL).apply()
    }

}