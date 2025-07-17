package com.skripsi.nisuk.view.main

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.skripsi.nisuk.R
import com.skripsi.nisuk.backend.preference.SharedPreferencesHelper
import com.skripsi.nisuk.databinding.ActivityDashboardBinding
import com.skripsi.nisuk.view.auth.LoginActivity
import com.skripsi.nisuk.view.main.fragment.HistoryFragment
import com.skripsi.nisuk.view.main.fragment.HomeFragment
import com.skripsi.nisuk.view.main.fragment.LinimasaFragment

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding : ActivityDashboardBinding
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        replaceFragment(HomeFragment())

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.apply {
            buttonLogout.setOnClickListener {
                showLogoutConfirmationDialog()
            }
        }
        binding.bottomNavigationView.setOnItemSelectedListener { item ->

            when (item.itemId) {
                R.id.home_user -> {
                    replaceFragment(HomeFragment())
                    true

                }

                R.id.riwayat_maps -> {
                    replaceFragment(HistoryFragment())
                    true
                }
                R.id.linimasa -> {
                    replaceFragment(LinimasaFragment())
                    true
                }
                else -> false
            }
        }
    }
    private fun performLogout() {

            SharedPreferencesHelper.clearSession(this)
            SharedPreferencesHelper.deleteUsername(this)
        SharedPreferencesHelper.deleteEmail(this)
            googleSignInClient.signOut().addOnCompleteListener {
                FirebaseAuth.getInstance().signOut()

                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
    }
    private fun replaceFragment(fragment: Fragment) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        // Cek apakah fragment baru sudah ada di back stack
        if (currentFragment == null || currentFragment::class.java != fragment::class.java) {
            fragmentTransaction.replace(R.id.fragment_container, fragment)
            fragmentTransaction.addToBackStack(null)
            fragmentTransaction.commit()
        }
    }
    private fun showLogoutConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.apply {
            setTitle("Konfirmasi Logout")
            setMessage("Apakah Anda yakin ingin logout?")
            setPositiveButton("Ya") { _, _ ->
                performLogout()
            }
            setNegativeButton("Batal", null)
        }
        builder.create().show()
    }
}