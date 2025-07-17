package com.skripsi.nisuk.view.main.tunanetra

import android.Manifest
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.skripsi.nisuk.R
import com.skripsi.nisuk.backend.preference.SharedPreferencesHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startLocationUpdates()
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 60000 // 10 detik
            fastestInterval = 30000
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    sendLocationToFirebase(it.latitude, it.longitude)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun sendLocationToFirebase(lat: Double, lng: Double) {
        // Ambil waktu sekarang
        val currentTime = System.currentTimeMillis()

        // Format tanggal (14 Mei 2025) dan waktu (03:00)
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) // Format tanggal (contoh: 14 Mei 2025)
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault()) // Format waktu (contoh: 03:00)

        // Format tanggal dan waktu
        val date = dateFormat.format(Date(currentTime)) // Format tanggal, contoh: 14 Mei 2025
        val time = timeFormat.format(Date(currentTime)) // Format waktu, contoh: 03:00

        // Gabungkan tanggal dan waktu
        val formattedDateTime = "$date - $time"

        // Membuat path berdasarkan format yang diinginkan
        val username = SharedPreferencesHelper.getUsername(context = this)
        val lokasiRef = FirebaseDatabase.getInstance().getReference("akun_tunanetra").child(username.toString()).child ("data/$date/$time")

        // Data yang akan disimpan
        val lokasi = mapOf(
            "latitude" to lat,
            "longitude" to lng,
            "timestamp" to formattedDateTime // Tanggal dan waktu dalam format yang diinginkan
        )

        // Simpan data ke Firebase
        lokasiRef.setValue(lokasi)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notif = NotificationCompat.Builder(this, "lokasi_channel")
            .setContentTitle("Tracking Lokasi")
            .setContentText("Mengirim lokasi ke Firebase Realtime...")
            .setSmallIcon(R.drawable.baseline_add_location_24)
            .build()

        startForeground(1, notif)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
