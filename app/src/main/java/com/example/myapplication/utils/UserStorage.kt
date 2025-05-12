package com.example.myapplication.utils

import android.content.Context
import com.example.myapplication.model.UserAccount
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object UserStorage {
    private const val FILE_NAME = "users.json"

    fun saveUser(context: Context, user: UserAccount): Boolean {
        val users = loadUsers(context).toMutableList()

        if (users.any { it.username == user.username }) return false // 帳號重複

        users.add(user)
        val json = Gson().toJson(users)
        context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).use {
            it.write(json.toByteArray())
        }
        return true
    }

    fun loadUsers(context: Context): List<UserAccount> {
        return try {
            val json = context.openFileInput(FILE_NAME).bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<UserAccount>>() {}.type
            Gson().fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun verifyUser(context: Context, username: String, password: String): Boolean {
        val hash = HashUtils.sha256(password)
        return loadUsers(context).any { it.username == username && it.passwordHash == hash }
    }
}
