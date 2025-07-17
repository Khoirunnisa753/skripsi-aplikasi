package com.skripsi.nisuk.view.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.skripsi.nisuk.R
import com.skripsi.nisuk.backend.preference.SharedPreferencesHelper
import com.skripsi.nisuk.view.auth.LoginActivity
import com.skripsi.nisuk.view.main.DashboardActivity
import com.skripsi.nisuk.view.main.tunanetra.PredictActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val username = SharedPreferencesHelper.getUsername(this)
        val email = SharedPreferencesHelper.getEmail(this)

        Handler(Looper.getMainLooper()).postDelayed({
            if (email != "") {
                Toast.makeText(this, "Login sebagai: $email", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, DashboardActivity::class.java)

                startActivity(intent)
                finish()
            } else if (email == "" && username != "") {
                val intent = Intent(this, PredictActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
        }, 2500)
    }
}
