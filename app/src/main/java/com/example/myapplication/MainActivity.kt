package com.example.myapplication

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.activity.OnBackPressedCallback
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.ui.fragment.AccountFragment
import com.example.myapplication.ui.fragment.HomeFragment
import com.example.myapplication.ui.fragment.TrainFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 預設顯示首頁 Fragment
        Handler(Looper.getMainLooper()).post {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, HomeFragment())
                .commit()
        }

        // 底部導航點擊切換 Fragment
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, HomeFragment())
                        .commit()
                    true
                }
                R.id.navigation_train -> {
                    supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, TrainFragment())
                        .commit()
                    true
                }
                R.id.navigation_account -> {
                    supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, AccountFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            private var doubleBackToExitPressedOnce = false
            private val exitHandler = Handler(Looper.getMainLooper())

            override fun handleOnBackPressed() {
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)

                when (currentFragment) {
                    is TrainFragment, is AccountFragment -> {
                        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragmentContainer, HomeFragment())
                            .commit()
                        binding.bottomNavigation.selectedItemId = R.id.navigation_home
                    }

                    is HomeFragment -> {
                        if (doubleBackToExitPressedOnce) {
                            finish()
                        } else {
                            doubleBackToExitPressedOnce = true
                            Toast.makeText(this@MainActivity, "再按一次返回鍵離開應用程式", Toast.LENGTH_SHORT).show()
                            exitHandler.postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
                        }
                    }

                    else -> {
                        // 調用預設返回（處理 back stack）
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })
    }
}
