package com.skripsi.nisuk.view.auth

import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.Pair
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.skripsi.nisuk.R
import com.skripsi.nisuk.backend.preference.SharedPreferencesHelper
import com.skripsi.nisuk.backend.viewmodel.AuthViewModelFactory
import com.skripsi.nisuk.databinding.ActivityRegisterBinding
import com.skripsi.nisuk.backend.repository.AuthRepository
import com.skripsi.nisuk.backend.viewmodel.AuthViewModel

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val authRepository by lazy { AuthRepository(auth, firestore) }
    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(authRepository)
    }
    private val RC_SIGN_IN = 9001

    var isDisability = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cardViewRegister.translationY = 1000f
        binding.cardViewRegister.animate().translationY(0f).setDuration(500).start()

        binding.checkboxDisabilitas.setOnCheckedChangeListener { _, isChecked ->
            isDisability = isChecked
            if (isChecked) {
                Log.d("disabilitas", "Checked: $isDisability")
                Toast.makeText(this, "disabilitas", Toast.LENGTH_SHORT).show()
            } else {
                Log.d("disabilitas", "Checked: $isDisability")
                Toast.makeText(this, "non disabilitas", Toast.LENGTH_SHORT).show()
            }
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        viewModel.registrationStatus.observe(this) { isSuccess ->
            if (isSuccess) {
                performLogout()
                Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            } else {
                performLogout()
                Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show()
            }
        }
        binding.tvLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)

            val options = ActivityOptions.makeSceneTransitionAnimation(
                this,
                Pair.create(binding.cardViewRegister as View, "card_transition")
            )
            startActivity(intent, options.toBundle())
        }
        binding.btnRegister.setOnClickListener {
            val email = binding.emailInput.text.toString()
            val password = binding.passwordInput.text.toString()
            val username = binding.usernameInput.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty() && username.isNotEmpty()) {

                viewModel.registerWithEmailPassword(email, password, username, isDisability)
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnRegisterGoogle.setOnClickListener {
            val googleSignInClient = GoogleSignIn.getClient(this, gso)  // gso is GoogleSignInOptions

            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                performLogout()
                val errorMessage = "Google sign-in failed: ${e.statusCode}"
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()

                Log.w("GoogleSignIn", "signInResult:failed code=" + e.statusCode)
            }
        }
    }
    private fun performLogout() {
        // Hapus sesi pengguna dari SharedPreferences
        SharedPreferencesHelper.clearSession(this)
        SharedPreferencesHelper.deleteUsername(this)
        googleSignInClient.signOut().addOnCompleteListener {
            FirebaseAuth.getInstance().signOut()
        }
    }
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        Handler().postDelayed({
            auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val username = binding.usernameInput.text.toString()
                        if (user != null && username.isNotEmpty()) {
                            Log.d("GoogleSignIn", "signInWithCredential:success")
                            viewModel.registerWithGoogle(user.email!!, username, isDisability)
                            //performLogout()
                        } else {
                            performLogout()
                            showUsernameWarningDialog()
                        }
                    } else {
                        performLogout()
                        Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                    }
                }
        }, 100L)

    }
    private fun showUsernameWarningDialog() {
        // Membuat dialog peringatan
        AlertDialog.Builder(this)
            .setTitle("Peringatan")
            .setMessage("Silakan isi kolom username untuk mendaftar dengan akun Google.")
            .setPositiveButton("OK") { _, _ ->
                // Fokus pada kolom username dan buka keyboard
                binding.usernameInput.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(binding.usernameInput, InputMethodManager.SHOW_IMPLICIT)
            }
            .setCancelable(false) // Agar dialog tidak bisa ditutup selain dengan OK
            .show()
    }
}