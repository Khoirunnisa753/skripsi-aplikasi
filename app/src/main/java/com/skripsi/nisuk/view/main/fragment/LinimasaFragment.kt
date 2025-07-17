package com.skripsi.nisuk.view.main.fragment

import android.app.DatePickerDialog
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.animation.AccelerateDecelerateInterpolator
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.skripsi.nisuk.R
import com.skripsi.nisuk.backend.preference.SharedPreferencesHelper
import com.skripsi.nisuk.databinding.FragmentHomeBinding
import com.skripsi.nisuk.databinding.FragmentLinimasaBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class LinimasaFragment : Fragment(), OnMapReadyCallback {
    private lateinit var _binding: FragmentLinimasaBinding
    private val binding get() = _binding
    private lateinit var mMap: GoogleMap
    private lateinit var mapView: MapView
    private val database = FirebaseDatabase.getInstance()

    private val locations = mutableListOf<LatLng>() // Menyimpan titik yang diambil dari Firebase
    private val timestamps = mutableMapOf<LatLng, String>() // Menyimpan timestamp untuk setiap lokasi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLinimasaBinding.inflate(layoutInflater, container, false)

        val usernameMonitoring = SharedPreferencesHelper.getUsername(requireContext())
        if (usernameMonitoring != ""){
            binding.map.visibility = View.VISIBLE
            binding.infoLayout.visibility = View.VISIBLE

        }
        binding.apply {
            btnPickDate.setOnClickListener {
                val calendar = Calendar.getInstance()
                DatePickerDialog(requireContext(), { _, year, month, day ->
                    val selectedDate = "$day/${month + 1}/$year"
                    tvSelectedDate.text = selectedDate
                    if (usernameMonitoring != ""){
                        fetchLocationsFromFirebase(usernameMonitoring.toString(), selectedDate)
                    }

                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
            }
        }

        mapView = binding.root.findViewById(R.id.map)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        return binding.root
    }

    private fun fetchLocationsFromFirebase(username: String, selectedDate: String) {
        val locale = Locale("id", "ID")
        val localeEng = Locale.ENGLISH

        // Format ulang tanggal ke "dd MMM yyyy"
        val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date: Date = inputFormat.parse(selectedDate) ?: Date()
        val indoFormat = SimpleDateFormat("dd MMM yyyy", locale)
        val engFormat = SimpleDateFormat("dd MMM yyyy", localeEng)
        val indoDate = indoFormat.format(date)
        val engDate = engFormat.format(date)

        val trackingRef = database.getReference("akun_tunanetra").child(username).child("data/$indoDate")
        Toast.makeText(requireContext(), "username $username", Toast.LENGTH_SHORT).show()
        trackingRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(requireContext(), "ada data", Toast.LENGTH_SHORT).show()
                    handleLocationSnapshot(snapshot)
                } else {
                    val fallbackRef = database.getReference("akun_tunanetra").child(username).child("data/$engDate")
                    fallbackRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(fallbackSnapshot: DataSnapshot) {
                            if (fallbackSnapshot.exists()) {
                                handleLocationSnapshot(fallbackSnapshot)
                            } else {
                                Toast.makeText(requireContext(), "Tidak ada data lokasi", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(requireContext(), "Gagal ambil data: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeFragment", "Gagal ambil data: ${error.message}")
            }
        })
    }

    private fun handleLocationSnapshot(snapshot: DataSnapshot) {
        locations.clear()
        timestamps.clear()
        mMap.clear()

        for (locationSnapshot in snapshot.children) {
            val latitudeRaw = locationSnapshot.child("latitude").value
            val longitudeRaw = locationSnapshot.child("longitude").value
            val timestamp = locationSnapshot.child("timestamp").getValue(String::class.java)

            if (latitudeRaw is Double && longitudeRaw is Double && timestamp != null) {
                val latLng = LatLng(latitudeRaw, longitudeRaw)
                locations.add(latLng)
                timestamps[latLng] = timestamp
            }
        }

        updateMapWithLocations()
    }
    private fun updateMapWithLocations() {
        mMap.clear()

        if (locations.isEmpty()) return

        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        mMap.isBuildingsEnabled = false

        val polylineOptions = PolylineOptions()
            .width(6f)
            .color(Color.BLUE)
            .geodesic(true)
            .jointType(JointType.ROUND)

        locations.forEachIndexed { index, location ->
            val timestamp = timestamps[location] ?: "Waktu tidak tersedia"

            val hue = when (index) {
                0 -> BitmapDescriptorFactory.HUE_GREEN
                locations.lastIndex -> BitmapDescriptorFactory.HUE_RED
                else -> BitmapDescriptorFactory.HUE_BLUE
            }

            mMap.addMarker(
                MarkerOptions()
                    .position(location)
                    .title("Lokasi $index")
                    .snippet(timestamp)
                    .icon(BitmapDescriptorFactory.defaultMarker(hue))
            )

            polylineOptions.add(location)
        }

        mMap.addPolyline(polylineOptions)

        // Klik info window → buka lokasi di Google Maps
        mMap.setOnInfoWindowClickListener { marker ->
            val pos = marker.position
            val uri = Uri.parse("geo:${pos.latitude},${pos.longitude}?q=${pos.latitude},${pos.longitude}(Lokasi)")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
            }

            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "Google Maps tidak tersedia", Toast.LENGTH_SHORT).show()
            }
        }

        // Kamera fokus ke semua titik
        val bounds = LatLngBounds.builder().apply {
            locations.forEach { include(it) }
        }.build()
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
    }
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Menambahkan style.json
        try {
            val success = mMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.gmaps_style)
            )
            if (!success) {
                Log.e("HomeFragment", "Gagal memuat style.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e("HomeFragment", "Tidak dapat menemukan file style: ", e)
        }

        // Update peta dengan lokasi setelah peta siap
        if (locations.isNotEmpty()) {
            updateMapWithLocations()
        }
    }
    private fun animateMarker(marker: Marker?) {
        if (marker == null) return
        val handler = Handler(Looper.getMainLooper())
        val start = SystemClock.uptimeMillis()
        val duration = 1000L // Animasi 1 detik

        val interpolator = AccelerateDecelerateInterpolator()

        handler.post(object : Runnable {
            override fun run() {
                val elapsed = SystemClock.uptimeMillis() - start
                val t = interpolator.getInterpolation(elapsed / duration.toFloat())

                marker.alpha = t // Efek fade-in
                marker.setAnchor(0.5f, 1f - (t / 2)) // Efek naik turun

                if (t < 1.0) {
                    handler.postDelayed(this, 16)
                }
            }
        })
    }
    private fun addPulsatingCircle(latLng: LatLng, color: Int) {
        val circle = mMap.addCircle(
            CircleOptions()
                .center(latLng)
                .radius(1.0)
                .strokeWidth(2f)
                .strokeColor(color)
                .fillColor(Color.argb(70, Color.red(color), Color.green(color), Color.blue(color)))
        )

        val handler = Handler(Looper.getMainLooper())
        val start = SystemClock.uptimeMillis()
        val duration = 2000L // 2 detik

        handler.post(object : Runnable {
            override fun run() {
                val elapsed = SystemClock.uptimeMillis() - start
                val t = elapsed / duration.toFloat()

                circle.radius = 50.0 * t // Membesarkan lingkaran

                if (t < 1.0) {
                    handler.postDelayed(this, 16)
                } else {
                    circle.remove()
                }
            }
        })
    }
}