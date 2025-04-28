package com.example.myapplication.userdata

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.ActivityCheckHistoryBinding
import com.example.myapplication.userdata.module.HistoryItem

class CheckHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckHistoryBinding
    private lateinit var jsonFileHelper: JsonFileHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize View Binding
        binding = ActivityCheckHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize JsonFileHelper
        jsonFileHelper = JsonFileHelper(this)

        // Load history data
        val userData = jsonFileHelper.readData()

        // Combine weight and exercise history into a single list
        val weightMap = userData.weightHistory.associateBy { it.date }
        val exerciseMap = userData.exerciseHistory.associateBy { it.date }
        val allDates = (weightMap.keys + exerciseMap.keys).toSet()

        val historyList = allDates.map { date ->
            val weight = weightMap[date]?.weight ?: 0.0
            val minutes = exerciseMap[date]?.minutes ?: 0
            HistoryItem(date, weight, minutes)
        }.sortedByDescending { it.date }


        // Set up RecyclerView
        val adapter = HistoryAdapter(historyList)
        binding.recyclerViewHistory.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewHistory.adapter = adapter
    }
}