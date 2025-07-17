package com.skripsi.nisuk.view.adapter

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.skripsi.nisuk.R
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.ImageView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class HistoryPredictAdapter (
    private val historyList: List<HistoryPredict>,
):
    RecyclerView.Adapter<HistoryPredictAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNominal: TextView = view.findViewById(R.id.tvNominal)
        val tvTanggal: TextView = view.findViewById(R.id.tvTanggal)
        val tvImg64: ImageView = view.findViewById(R.id.imgPrediksi)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history_prediksi, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val history = historyList[position]
        holder.tvNominal.text = "Nominal: ${history.nominal}"

        val timestampMillis = history.tanggal.toLong()
        val latitude = history.lat
        val longitude = history.long

        val date = Date(timestampMillis)
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        val formattedTime = formatter.format(date)

        holder.tvTanggal.text = "$formattedTime WIB"

        val bitmap = base64ToBitmap(history.img64)
        if (bitmap != null) {
            holder.tvImg64.setImageBitmap(bitmap)
        } else {
            holder.tvImg64.setImageResource(R.drawable.baseline_history_24)
        }

        // ✅ Set klik item
        holder.itemView.setOnClickListener {
            showImageDialog(holder.itemView, bitmap, latitude, longitude)
        }
    }


    fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            null
        }
    }
    private fun showImageDialog(view: View, bitmap: Bitmap?, latitude: Double, longitude: Double) {
        val dialogView = LayoutInflater.from(view.context).inflate(R.layout.dialog_history_detail, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.dialogImageView)
        val btnClose = dialogView.findViewById<TextView>(R.id.btnClose)
        val btnMap = dialogView.findViewById<TextView>(R.id.btnOpenMap)

        imageView.setImageBitmap(bitmap)

        val dialog = android.app.AlertDialog.Builder(view.context)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnClose.setOnClickListener { dialog.dismiss() }

        btnMap.setOnClickListener {
            val uri = android.net.Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(Prediksi Lokasi)")
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")
            view.context.startActivity(intent)
        }

        dialog.show()
    }

    override fun getItemCount(): Int = historyList.size
}