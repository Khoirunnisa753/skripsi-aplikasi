package com.skripsi.nisuk.backend.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.skripsi.nisuk.backend.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _registrationStatus = MutableLiveData<Boolean>()
    val registrationStatus: LiveData<Boolean> get() = _registrationStatus

    private val _loginStatus = MutableLiveData<Boolean>()
    val loginStatus: LiveData<Boolean> get() = _loginStatus

    val resetPasswordStatus = MutableLiveData<String>()

    fun registerWithEmailPassword(email: String, password: String, username: String, isDisability: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            val isRegistered = authRepository.registerWithEmailPassword(email, password, username,isDisability)
            _registrationStatus.postValue(isRegistered)
        }
    }

    fun registerWithGoogle(email: String, username: String,isDisability: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            val isRegistered = authRepository.registerWithGoogle(email, username,isDisability)
            _registrationStatus.postValue(isRegistered)
        }
    }
    fun loginWithEmailPassword(email: String, password: String, context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val isLoggedIn = authRepository.loginWithEmailPassword(email, password, context)
            _loginStatus.postValue(isLoggedIn)
        }
    }

    fun loginWithGoogle(idToken: String, context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val isLoggedIn = authRepository.loginWithGoogle(idToken, context)
            _loginStatus.postValue(isLoggedIn)
        }
    }

    fun sendResetPasswordEmail(email: String) {
        authRepository.sendPasswordResetEmail(email,
            onSuccess = {
                resetPasswordStatus.postValue("Link reset password telah dikirim ke email Anda.")
            },
            onFailure = { errorMessage ->
                resetPasswordStatus.postValue(errorMessage)
            }
        )
    }
}