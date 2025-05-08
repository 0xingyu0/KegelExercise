package com.example.myapplication.poselandmarker.fragment

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.poselandmarker.PERMISSIONS_REQUIRED
import kotlinx.coroutines.launch

class PermissionsFragment : Fragment() {

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions.all { it.value }) {
                Toast.makeText(
                    context,
                    "All required permissions granted",
                    Toast.LENGTH_LONG
                ).show()
                navigateToCamera()
            } else {
                Toast.makeText(
                    context,
                    "需要相機權限才能開始訓練，已返回上一頁。",
                    Toast.LENGTH_LONG
                ).show()

                // 自動導回
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (hasPermissions(requireContext())) {
            navigateToCamera()
        } else {
            requestPermissionLauncher.launch(PERMISSIONS_REQUIRED)
        }
    }

    private fun navigateToCamera() {
        lifecycleScope.launch {
            if (isAdded) { // 避免 Fragment 被銷毀時導致錯誤
                findNavController().navigate(R.id.action_permissions_to_camera)
            }
        }
    }


    companion object {

        /** Convenience method used to check if all permissions required by this app are granted */
        fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
            ContextCompat.checkSelfPermission(
                context,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}