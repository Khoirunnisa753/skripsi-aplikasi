package com.skripsi.nisuk.view.adapter

import com.google.firebase.Timestamp

data class HistoryPredict (
    val tanggal: String,
    val nominal: String,
    val img64: String,
    val lat: Double,
    val long: Double
)