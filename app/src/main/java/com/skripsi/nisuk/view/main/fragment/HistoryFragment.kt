package com.skripsi.nisuk.view.main.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.skripsi.nisuk.backend.preference.SharedPreferencesHelper
import com.skripsi.nisuk.databinding.FragmentHistoryBinding
import com.skripsi.nisuk.view.adapter.HistoryPredict
import com.skripsi.nisuk.view.adapter.HistoryPredictAdapter


class HistoryFragment : Fragment() {

    private lateinit var _binding: FragmentHistoryBinding
    private val binding get() = _binding!!
    private lateinit var database: DatabaseReference
    private lateinit var historyAdapter: HistoryPredictAdapter
    private val historyList = mutableListOf<HistoryPredict>()
    var usernameAkunAnak: String? = ""
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)

        database = FirebaseDatabase.getInstance().reference

        historyAdapter = HistoryPredictAdapter(historyList)
        binding.recyclerViewHistory.apply {
            adapter = historyAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        usernameAkunAnak = SharedPreferencesHelper.getUsername(requireContext())
        if (usernameAkunAnak != ""){
            binding.recyclerViewHistory.visibility = View.VISIBLE
            binding.tvNoData.visibility = View.GONE

            fetchHistoryData()
        }else{
            binding.recyclerViewHistory.visibility = View.GONE
            binding.tvNoData.visibility = View.VISIBLE
        }
        return binding.root
    }
    private fun fetchHistoryData() {
        val hasilPrediksiRef = database.child("akun_tunanetra").child(usernameAkunAnak.toString()).child("hasil_prediksi")

        hasilPrediksiRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Kosongkan list lama
                historyList.clear()

                if (snapshot.exists()) {
                    for (childSnapshot in snapshot.children) {
                        val dataMap = childSnapshot.value as? Map<*, *> ?: continue

                        val nominal = dataMap["nominal"] as? String ?: ""
                        val tanggal = (dataMap["tanggal"] as? String)?: ""
                        val img64 = dataMap["imageBase64"] as? String ?: ""
                        val lat = dataMap["latitude"] as? Double ?: 0.0
                        val long = dataMap["longitude"] as? Double ?: 0.0

                        // Tambahkan ke daftar
                        historyList.add(HistoryPredict(tanggal, nominal, img64, lat, long))
                    }

                    if (historyList.isEmpty()) {
                        binding.recyclerViewHistory.visibility = View.GONE
                        binding.tvNoData.visibility = View.VISIBLE
                    } else {
                        binding.recyclerViewHistory.visibility = View.VISIBLE
                        binding.tvNoData.visibility = View.GONE
                        historyAdapter.notifyDataSetChanged()
                    }
                } else {
                    // Tidak ada data
                    binding.recyclerViewHistory.visibility = View.GONE
                    binding.tvNoData.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Gagal mengambil data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


}