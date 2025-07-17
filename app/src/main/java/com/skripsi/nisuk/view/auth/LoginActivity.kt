package com.skripsi.nisuk.view.auth

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.skripsi.nisuk.R
import com.skripsi.nisuk.databinding.ActivityLoginBinding
import android.util.Pair
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.skripsi.nisuk.backend.preference.SharedPreferencesHelper
import com.skripsi.nisuk.backend.repository.AuthRepository
import com.skripsi.nisuk.backend.viewmodel.AuthViewModel
import com.skripsi.nisuk.backend.viewmodel.AuthViewModelFactory
import com.skripsi.nisuk.view.main.DashboardActivity
import com.skripsi.nisuk.view.main.tunanetra.PredictActivity
import java.util.Random

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(AuthRepository(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance()))
    }
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var database: DatabaseReference
    private val handler = Handler()
    private val userId = "user_id_123" // Ganti dengan user id sesuai kebutuhan

    private val RC_SIGN_IN = 9001
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnToTunanetra.setOnClickListener {
            val intent = Intent(this@LoginActivity, PredictActivity::class.java)
            startActivity(intent)
            finish()
        }
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        authViewModel.loginStatus.observe(this) { isLoggedIn ->
            if (isLoggedIn) {
                Toast.makeText(this, "Login berhasil!", Toast.LENGTH_SHORT).show()
                val isDisabilityStatus = SharedPreferencesHelper.getDisabilityStatus(this)
                if (isDisabilityStatus) {
                    startActivity(Intent(this, PredictActivity::class.java))
                } else {
                    startActivity(Intent(this, DashboardActivity::class.java))
                }
                finish()
            } else {
                performLogout()
                Toast.makeText(this, "Login gagal! Periksa email atau password Anda.", Toast.LENGTH_SHORT).show()
            }
        }
        database = FirebaseDatabase.getInstance().reference

        binding.apply {

            lottieAnimation.setOnClickListener {
                startSendingData()
            }
            tvRegister.setOnClickListener {
                val intent = Intent(this@LoginActivity, RegisterActivity::class.java)

                val options = ActivityOptions.makeSceneTransitionAnimation(
                    this@LoginActivity,
                    Pair.create(cardViewLogin as View, "card_transition")
                )
                startActivity(intent, options.toBundle())
            }
            btnLogin.setOnClickListener {
                val email = binding.emailInput.text.toString().trim()
                val password = binding.passwordInput.text.toString().trim()

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this@LoginActivity, "Email dan password harus diisi", Toast.LENGTH_SHORT).show()
                } else {
                    authViewModel.loginWithEmailPassword(email, password, this@LoginActivity)
                }
            }
            btnLoginGoogle.setOnClickListener {
                val googleSignInClient = GoogleSignIn.getClient(this@LoginActivity, gso)  // gso is GoogleSignInOptions

                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }
            tvForgotPassword.setOnClickListener {
                showForgotPasswordDialog()
            }
        }
    }
    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Lupa Password")
        builder.setMessage("Masukkan email Anda untuk reset password:")

        // Menggunakan layout XML yang telah dibuat
        val dialogView = layoutInflater.inflate(R.layout.dialog_forgot_password, null)
        val emailEditText = dialogView.findViewById<EditText>(R.id.email_input_forgot)

        builder.setView(dialogView)

        builder.setPositiveButton("Kirim") { dialog, _ ->
            val email = emailEditText.text.toString().trim()
            if (email.isNotEmpty()) {

                authViewModel.sendResetPasswordEmail(email)
            } else {
                Toast.makeText(this, "Email tidak boleh kosong!", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Batal") { dialog, _ -> dialog.cancel() }
        builder.show()
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                val errorMessage = "Google sign-in failed"
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()

                Log.w("GoogleSignIn", "signInResult:failed code=" + e.statusCode)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {

        val credential = GoogleAuthProvider.getCredential(idToken, null)
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = FirebaseAuth.getInstance().currentUser
                        user?.let {
                            authViewModel.loginWithGoogle(idToken, this)
                        }
                    } else {
                        Log.w("GoogleSignIn", "signInWithCredential:failure", task.exception)
                        Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                    }
                }

    }
    override fun onBackPressed() {
        val builder = AlertDialog.Builder(this)
        builder.apply {
            setTitle("Konfirmasi Keluar")
            setMessage("Apakah Anda yakin ingin keluar dari aplikasi?")
            setPositiveButton("Ya") { _, _ ->
                finish()
            }
            setNegativeButton("Batal", null)
        }
        builder.create().show()
    }
    private fun performLogout() {
        // Hapus sesi pengguna dari SharedPreferences
        SharedPreferencesHelper.clearSession(this)
        SharedPreferencesHelper.deleteUsername(this)
        googleSignInClient.signOut().addOnCompleteListener {
            FirebaseAuth.getInstance().signOut()
        }
    }
    private fun startSendingData() {
        val interval: Long = 5000 // 5 detik

        handler.postDelayed(object : Runnable {
            override fun run() {
                val currentTimestamp = System.currentTimeMillis() // Mendapatkan waktu saat ini dalam milidetik
                val latitude = -7.12345 + Random().nextDouble() * 0.001 // Menghasilkan latitude acak
                val longitude = 112.56789 + Random().nextDouble() * 0.001 // Menghasilkan longitude acak

                // Buat data yang akan dikirimkan
                val trackingData = TrackingData(latitude, longitude)

                // Kirim data ke Firebase Realtime Database
                database.child("tracking_history")
                    .child(userId)
                    .child("2025-02-18") // Ganti dengan tanggal yang diinginkan
                    .child(currentTimestamp.toString())
                    .setValue(trackingData)
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            Log.d("Firebase", "Data berhasil dikirim: $trackingData")
                        } else {
                            Log.e("Firebase", "Gagal mengirim data", it.exception)
                        }
                    }

                // Lanjutkan pengiriman data setiap interval
                handler.postDelayed(this, interval)
            }
        }, interval)
    }
    data class TrackingData(val latitude: Double, val longitude: Double)

}