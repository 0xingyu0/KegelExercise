package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.ui.fragment.HomeFragment
import com.example.myapplication.ui.fragment.TrainFragment
import com.example.myapplication.ui.fragment.AccountFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // 預設顯示首頁 Fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, HomeFragment())
            .commit()

        // 底部導航點擊切換 Fragment
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, HomeFragment())
                        .commit()
                    true
                }
                R.id.navigation_train -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, TrainFragment())
                        .commit()
                    true
                }
                R.id.navigation_account -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, AccountFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }

        /*val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        val navController = navHostFragment.navController
        activityMainBinding.navigation.setupWithNavController(navController)
        activityMainBinding.navigation.setOnNavigationItemReselectedListener {
            // ignore the reselection
        }*/
    }

    override fun onBackPressed() {
        finish()
    }
}