package com.skripsi.nisuk.backend.repository

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.skripsi.nisuk.backend.preference.SharedPreferencesHelper
import kotlinx.coroutines.tasks.await

class AuthRepository(private val auth: FirebaseAuth, private val firestore: FirebaseFirestore) {

    suspend fun registerWithEmailPassword(email: String, password: String, username: String, isDisability: Boolean = false): Boolean {
        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()

            val user = result.user ?: return false

            val userMap = hashMapOf("email" to email, "username" to username, "isDisability" to isDisability)
            firestore.collection("users").document(user.uid).set(userMap).await()

            return true
        } catch (e: Exception) {
            return false
        }
    }

    suspend fun registerWithGoogle(email: String, username: String,isDisability: Boolean = false): Boolean {
        try {
            val user = auth.currentUser
            if (user == null) {
                Log.e("RegisterWithGoogle", "User is not logged in.")
                return false
            }

            val userMap = hashMapOf("email" to email, "username" to username, "isDisability" to isDisability)
            firestore.collection("users").document(user.uid).set(userMap).await()

            return true
        } catch (e: Exception) {
            Log.e("RegisterWithGoogle", "Error: ${e.message}")
            return false
        }
    }


    suspend fun loginWithEmailPassword(email: String, password: String, context: Context): Boolean {
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            if (result.user != null) {
                SharedPreferencesHelper.saveEmail(email, context)
                val uid = result.user!!.uid
                val userDoc = firestore.collection("users").document(uid).get().await()
                val username = userDoc.getString("username") ?: return false
                val isDisability = userDoc.getBoolean("isDisability") ?: false

                SharedPreferencesHelper.saveDisabilityStatus(isDisability, context)
                return true
            }
            return false
        } catch (e: Exception) {
            return false
        }
    }

    suspend fun loginWithGoogle(idToken: String, context: Context): Boolean {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)

            val result = auth.signInWithCredential(credential).await()

            val user = result.user
            if (user != null) {
                val email = user.email ?: return false
                SharedPreferencesHelper.saveEmail(email, context)
                val userDoc = firestore.collection("users").whereEqualTo("email", email).get().await()

                if (!userDoc.isEmpty) {
                    val username = userDoc.documents[0].getString("username")
                    val isDisability = userDoc.documents[0].getBoolean("isDisability") ?: false
                    SharedPreferencesHelper.saveDisabilityStatus(isDisability, context)
                    return true

                }
            }
            return false
        } catch (e: Exception) {
            return false
        }
    }
    fun sendPasswordResetEmail(email: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    val exception = task.exception
                    if (exception is FirebaseAuthInvalidUserException) {
                        onFailure("Email tidak terdaftar!")
                    } else {
                        onFailure("Gagal mengirim email. Coba lagi nanti.")
                    }
                }
            }
    }
}
