package com.example.myapplication.userdata

import android.content.Context
import com.google.gson.Gson
import java.io.File
import com.example.myapplication.userdata.module.UserData

class JsonFileHelper(private val context: Context) {

    companion object {
        private const val FILE_NAME = "test.json"
    }

    private val jsonFile = File(context.filesDir, FILE_NAME)
    private val gson = Gson()

    fun readData(): UserData {
        return try {
            if (jsonFile.exists()) {
                val jsonString = jsonFile.readText()
                println("JSON Data: $jsonString")
                gson.fromJson(jsonString, UserData::class.java)
            } else {
                println("JSON file does not exist, creating new data")
                UserData(mutableListOf(), mutableListOf())
            }
        } catch (e: Exception) {
            println("Error reading JSON file: ${e.message}")
            UserData(mutableListOf(), mutableListOf())
        }
    }

    fun writeData(userData: UserData) {
        try {
            val jsonString = gson.toJson(userData)
            println("Writing JSON Data: $jsonString")
            jsonFile.writeText(jsonString)
        } catch (e: Exception) {
            println("Error writing JSON file: ${e.message}")
        }
    }
}
