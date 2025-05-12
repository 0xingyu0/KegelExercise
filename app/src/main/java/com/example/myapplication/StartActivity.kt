// StartActivity.kt
package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityStartBinding
import com.example.myapplication.utils.UserSession

class StartActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 若已登入則直接跳轉 MainActivity
        if (UserSession.isLoggedIn(this)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}