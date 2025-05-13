package com.example.myapplication.model

import java.time.LocalDate

data class TrainingRecord(
    val date: LocalDate,
    val content: String,
    val symptoms: String,
    val feeling: String
)
