// TrainingRecord.kt
package com.example.myapplication.model

data class TrainingRecord(
    val date: String,       // 格式為 "yyyy-MM-dd"
    val details: String     // 可擴展成次數、角度等資訊
)
