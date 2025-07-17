package com.skripsi.nisuk.view.main.tunanetra

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.skripsi.nisuk.ml.MoneyClassifier
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator


import com.skripsi.nisuk.R
import com.skripsi.nisuk.view.auth.LoginActivity
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors
import androidx.core.content.edit
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.skripsi.nisuk.backend.preference.SharedPreferencesHelper
import com.skripsi.nisuk.databinding.ActivityPredictBinding
import java.io.ByteArrayOutputStream

class PredictActivity : AppCompatActivity(), TextToSpeech.OnInitListener{
    private lateinit var captureButton: Button
    private lateinit var logout: ImageView
    private lateinit var imagePreview: ImageView
    private lateinit var predictionResult: TextView
    private lateinit var confidenceText: TextView
    private lateinit var viewFinder: PreviewView
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognizerIntent: Intent
    private val LOCATION_PERMISSION_REQUEST_CODE = 100
    private lateinit var googleSignInClient: GoogleSignInClient
    private val database = FirebaseDatabase.getInstance()
    private var selectedImageUri: Uri? = null
    private var isListeningEnabled = true
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    var currentLocation: Location? = null

    // CameraX components
    private var imageCapture: ImageCapture? = null
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    // TTS component
    private lateinit var textToSpeech: TextToSpeech
    private var ttsInitialized = false
    // Constants
    private companion object {
        private const val INPUT_SIZE = 224
        private val REQUEST_CODE_MICROPHONE = 1001
        private const val TAG = "MoneyClassifier"
    }
    private val permissions = mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            add(Manifest.permission.FOREGROUND_SERVICE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
        }
    }.toTypedArray()

    private lateinit var binding: ActivityPredictBinding
    var usernameAkun: String? = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPredictBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestAllPermissions()


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getLastLocation()

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        viewFinder = findViewById(R.id.viewFinder)
        captureButton = findViewById( R.id.captureButton)
        imagePreview = findViewById( R.id.imagePreview)
        predictionResult = findViewById( R.id.predictionResult)
        confidenceText = findViewById( R.id.confidenceText)
        logout = findViewById(R.id.buttonLogout)
        textToSpeech = TextToSpeech(this, this)
        createNotificationChannel()
        usernameAkun = SharedPreferencesHelper.getUsername(this)
        if (usernameAkun != ""){
            startLocationService()
            binding.etUsername.setText(usernameAkun)
        }

        binding.apply {
            addUsername.setOnClickListener {
                etUsername.visibility = View.VISIBLE
                sendUsername.visibility = View.VISIBLE
                addUsername.visibility = View.GONE
                etUsername.isEnabled = true
                etUsername.isFocusableInTouchMode = true
                etUsername.requestFocus()
            }
            sendUsername.setOnClickListener {
                etUsername.visibility = View.GONE
                sendUsername.visibility = View.GONE
                addUsername.visibility = View.VISIBLE
                val username = etUsername.text.toString()

                val refUsername = database.getReference("akun_tunanetra").child(username)
                refUsername.child("Nama").setValue(username)
                etUsername.isEnabled = false
                etUsername.isFocusable = false
                etUsername.isFocusableInTouchMode = false
                usernameAkun = etUsername.text.toString()
                SharedPreferencesHelper.saveUsername( etUsername.text.toString(), this@PredictActivity)
                startLocationService()
            }
        }
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        logout.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Konfirmasi Logout")
            builder.setMessage("Apakah Anda yakin ingin logout dan menghentikan pelacakan lokasi?")
            builder.setPositiveButton("Ya") { dialog, _ ->


                SharedPreferencesHelper.clearSession(this)
                SharedPreferencesHelper.deleteUsername(this)
                val serviceIntent = Intent(this, LocationService::class.java)
                stopService(serviceIntent)
                textToSpeech.shutdown()
                textToSpeech.stop()
                isListeningEnabled = false
                stopListening()

                googleSignInClient.signOut().addOnCompleteListener {
                    FirebaseAuth.getInstance().signOut()
                    dialog.dismiss()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }

            }
            builder.setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            builder.create().show()
        }

        captureButton.setOnClickListener { takePhoto() }
    }

    private fun speak(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale("id", "ID") // Bahasa Indonesia
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return
        imageCapture.flashMode = ImageCapture.FLASH_MODE_ON

        // Create temporary file
        val photoFile = File(
            externalMediaDirs.first(),
            "${System.currentTimeMillis()}.jpg"
        )

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    runOnUiThread {
                        Toast.makeText(baseContext, "Gagal mengambil gambar", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    selectedImageUri = output.savedUri ?: Uri.fromFile(photoFile)
                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)

                    runOnUiThread {
                        imagePreview.setImageBitmap(bitmap)
                        imagePreview.visibility = ImageView.VISIBLE
                        predictImage(bitmap)
                    }

                    // 🔁 Konversi ke Base64 dan kirim ke Firebase
                    val base64String = encodeToBase64(bitmap)


                    // 🚮 Hapus file sementara
                    photoFile.delete()
                }
            }
        )
    }
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
    private fun encodeToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 40, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun predictImage(bitmap: Bitmap) {
        // Run prediction in background thread
        cameraExecutor.execute {
            try {
                // Convert Bitmap to TensorImage
                val tensorImage = TensorImage(DataType.FLOAT32)
                tensorImage.load(bitmap)

                // Create ImageProcessor
                val imageProcessor = ImageProcessor.Builder()
                    .add(ResizeOp(
                        INPUT_SIZE,
                        INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                    .add(NormalizeOp(0f, 255f))
                    .build()

                // Preprocess the image
                val processedImage = imageProcessor.process(tensorImage)

                // Load model and run inference
                val model = MoneyClassifier.newInstance(this@PredictActivity)

                // Create input tensor
                val inputFeature0 = TensorBuffer.createFixedSize(
                    intArrayOf(1,
                        INPUT_SIZE,
                        INPUT_SIZE, 3), DataType.FLOAT32)
                inputFeature0.loadBuffer(processedImage.buffer)

                // Run inference
                val outputs = model.process(inputFeature0)
                val outputFeature0 = outputs.outputFeature0AsTensorBuffer

                // Process results
                val confidences = outputFeature0.floatArray
                val maxPos = confidences.indices.maxByOrNull { confidences[it] } ?: 0
                val confidence = confidences[maxPos]
                val classes = arrayOf("10 k", "100 RB", "1k", "20 K", "2k", "50 K", "5k")
                val predictedClass = classes[maxPos]

                if (confidence >= 0.55) {
                    // Update UI and speak result
                    runOnUiThread {
                        val (displayText, speechText) = when (predictedClass) {
                            "10 k" -> Pair("Rp 10.000", "sepuluh ribu rupiah")
                            "20 K" -> Pair("Rp 20.000", "dua puluh ribu rupiah")
                            "50 K" -> Pair("Rp 50.000", "lima puluh ribu rupiah")
                            "100 RB" -> Pair("Rp 100.000", "seratus ribu rupiah")
                            "1k" -> Pair("Rp 1.000", "seribu rupiah")
                            "2k" -> Pair("Rp 2.000", "dua ribu rupiah")
                            "5k" -> Pair("Rp 5.000", "lima ribu rupiah")
                            else -> Pair(predictedClass, predictedClass)
                        }

                        predictionResult.text = "Nominal: $displayText"
                        confidenceText.text = "Akurasi: ${"%.2f".format(confidence * 100)}%"
                        Log.e("kepercayaan berhasil", "Confidence: ${"%.2f".format(confidence * 100)}%")
                        Log.e("kepercayaan berhasil", "Predicted class: $predictedClass")

                        // Kirim ke Firebase Realtime Database pakai timestamp sebagai key
                        val timestamp = System.currentTimeMillis().toString()

                        if (usernameAkun != "") {
                            val ref = database.getReference("akun_tunanetra")
                                .child(usernameAkun.toString())
                                .child("hasil_prediksi")
                                .child(timestamp)

                            val base64Image = encodeToBase64(bitmap)

                            // Ambil lokasi (pastikan sudah punya permission dan LocationManager/FusedLocationClient diatur)
                            val latitude = currentLocation?.latitude ?: 0.0
                            val longitude = currentLocation?.longitude ?: 0.0

                            val data = mapOf(
                                "prediksi" to predictedClass,
                                "nominal" to displayText,
                                "akurasi" to confidence,
                                "imageBase64" to base64Image,
                                "tanggal" to timestamp,
                                "latitude" to latitude,
                                "longitude" to longitude
                            )

                            ref.setValue(data)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Data berhasil dikirim", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { error ->
                                    Toast.makeText(this, "Gagal mengirim: ${error.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                        confidenceText.setTextColor(
                            if (confidence < 0.85) {
                                ContextCompat.getColor(this@PredictActivity, android.R.color.holo_red_dark)
                            } else {
                                ContextCompat.getColor(this@PredictActivity, android.R.color.holo_green_dark)
                            }
                        )
                        // Speak the result
                        speak("Nominal yang terbaca adalah $speechText")
                    }
                }else {
                    confidenceText.setTextColor(
                        ContextCompat.getColor(this@PredictActivity, android.R.color.holo_red_dark)
                    )
                    confidenceText.text = "Akurasi: ${"%.2f".format(confidence * 100)}%"
                    predictionResult.text = "Nominal: Tidak Terdeteksi"

                    Log.e("kepercayaan", "Confidence: ${"%.2f".format(confidence * 100)}%")
                    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        // Pola: [start delay, durasi getar 1, jeda, durasi getar 2]
                        val timings = longArrayOf(0, 300, 150, 300) // 2x getar
                        val amplitudes = intArrayOf(0, 255, 0, 255) // 255 = amplitudo maksimum
                        vibrator.vibrate(VibrationEffect.createWaveform(timings, -1)) // -1 = no repeat
                    } else {
                        // Untuk Android < API 26, pakai pola tanpa amplitudo
                        val pattern = longArrayOf(0, 300, 150, 300)
                        vibrator.vibrate(pattern, -1)
                    }

                    speak("Uang tidak Terdeteksi, silahkan coba lagi ya")
                }
                // Clean up
                model.close()

            } catch (e: Exception) {
                Log.e(TAG, "Prediction error", e)
                runOnUiThread {
                    confidenceText.text = ""
                    predictionResult.text = "Error dalam prediksi"
                    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        // Pola: [start delay, durasi getar 1, jeda, durasi getar 2]
                        val timings = longArrayOf(0, 300, 150, 300) // 2x getar
                        val amplitudes = intArrayOf(0, 255, 0, 255) // 255 = amplitudo maksimum
                        vibrator.vibrate(VibrationEffect.createWaveform(timings, -1)) // -1 = no repeat
                    } else {
                        // Untuk Android < API 26, pakai pola tanpa amplitudo
                        val pattern = longArrayOf(0, 300, 150, 300)
                        vibrator.vibrate(pattern, -1)
                    }
                    speak("Silahkan coba lagi")

                    Toast.makeText(this@PredictActivity, "Error dalam prediksi", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startListening() {
        // Tidak usah cek isRecognitionAvailable, karena bisa false walaupun perangkat support
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Silakan ucapkan perintah")
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                //Toast.makeText(this@PredictActivity, "Siap mendengarkan...", Toast.LENGTH_SHORT).show()
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                //Toast.makeText(this@PredictActivity, "restart listening", Toast.LENGTH_SHORT).show()
                if (isListeningEnabled) {
                    restartListening()
                }
            }

            override fun onError(error: Int) {

                Handler(Looper.getMainLooper()).postDelayed({
                    speechRecognizer.startListening(recognizerIntent)
                }, 1500)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.let {
                    val spokenText = it[0].lowercase(Locale.getDefault())
                    Toast.makeText(this@PredictActivity, "Kamu mengatakan: $spokenText", Toast.LENGTH_SHORT).show()

                    if (spokenText.contains("ambil gambar")) {
                        Toast.makeText(applicationContext, "Perintah: Ambil Gambar diterima", Toast.LENGTH_SHORT).show()
                        takePhoto()
                    } else {
                        Toast.makeText(applicationContext, "Perintah tidak dikenali: $spokenText", Toast.LENGTH_SHORT).show()
                    }
                }
                if (isListeningEnabled) {
                    restartListening()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer.startListening(recognizerIntent)
    }

    private fun restartListening() {
        //Toast.makeText(this@PredictActivity, "restart listening", Toast.LENGTH_SHORT).show()
        Handler(Looper.getMainLooper()).postDelayed({
            speechRecognizer.startListening(recognizerIntent)
        }, 500)
    }





    private fun startLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    // 🔔 Ini adalah tempat untuk Notification Channel
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "lokasi_channel",
                "Layanan Lokasi",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        //speechRecognizer.destroy()
        textToSpeech.shutdown()
        textToSpeech.stop()
    }
    private fun stopListening() {
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.stopListening()
            speechRecognizer.cancel()
            speechRecognizer.destroy()
        }
    }
    override fun onPause() {
        super.onPause()
        isListeningEnabled = false
        stopListening()
    }
    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                currentLocation = location
            }
        }
    }
    private fun requestAllPermissions() {
        val notGrantedPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGrantedPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, notGrantedPermissions.toTypedArray(), 100)
        } else {
            onPermissionsGranted()
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                onPermissionsGranted()
            } else {
                Toast.makeText(this, "Beberapa izin ditolak", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun onPermissionsGranted() {
        Toast.makeText(this, "Semua izin diberikan", Toast.LENGTH_SHORT).show()
        startCamera()
        startListening()
        if (usernameAkun != ""){
            startLocationService()
            binding.etUsername.setText(usernameAkun)
        }
    }

}
