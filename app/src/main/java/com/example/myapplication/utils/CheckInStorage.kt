package com.example.myapplication.utils

import android.content.Context
import com.example.myapplication.model.CheckInRecord
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object CheckInStorage {

    private fun getFileName(username: String): String {
        return "checkin_${username}.json"
    }

    fun save(context: Context, username: String, records: List<CheckInRecord>) {
        val json = Gson().toJson(records)
        val file = File(context.filesDir, getFileName(username))
        file.writeText(json)
    }

    fun load(context: Context, username: String): List<CheckInRecord> {
        return try {
            val file = File(context.filesDir, getFileName(username))
            if (!file.exists()) return emptyList()
            val json = file.readText()
            val type = object : TypeToken<List<CheckInRecord>>() {}.type
            Gson().fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
