package com.example.myapplication.userdata.module

data class UserData(
    val weightHistory: MutableList<WeightEntry> = mutableListOf(),
    val exerciseHistory: MutableList<ExerciseEntry> = mutableListOf()
)
