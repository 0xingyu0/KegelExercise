// TrainingStorage.kt
package com.example.myapplication.utils

import android.content.Context
import com.example.myapplication.model.TrainingRecord
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object TrainingStorage {

    private const val FILE_NAME = "training_records.json"

    fun save(context: Context, records: List<TrainingRecord>) {
        val json = Gson().toJson(records)
        context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).use {
            it.write(json.toByteArray())
        }
    }

    fun load(context: Context): List<TrainingRecord> {
        return try {
            val json = context.openFileInput(FILE_NAME).bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<TrainingRecord>>() {}.type
            Gson().fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
