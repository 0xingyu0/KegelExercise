package com.example.myapplication.ui.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.StartActivity
import com.example.myapplication.databinding.FragmentAccountBinding
import com.example.myapplication.utils.UserSession
import java.io.File
import java.io.FileOutputStream

class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!

    // 啟動圖片選擇器，並處理格式與大小驗證後進行壓縮與儲存
    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri: Uri? = result.data?.data
                imageUri?.let {
                    val mimeType = requireContext().contentResolver.getType(it)
                    if (!isSupportedImageFormat(mimeType ?: "")) {
                        Toast.makeText(requireContext(), "不支援的圖片格式，請使用 JPG 或 PNG", Toast.LENGTH_SHORT).show()
                        return@let
                    }
                    if (!isFileSizeAcceptable(it)) {
                        Toast.makeText(requireContext(), "圖片過大，請選擇 5MB 以下的圖片", Toast.LENGTH_SHORT).show()
                        return@let
                    }
                    compressAndSaveAvatar(it)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val username = UserSession.getUsername(requireContext())!!
        binding.tvAccountName.text = username

        val versionName = requireContext().packageManager
            .getPackageInfo(requireContext().packageName, 0).versionName
        binding.tvVersion.text = "版本 $versionName"

        // 嘗試讀取使用者的頭像 URI，若為 null 則載入預設圖
        val avatarUri = getAvatarUri(username)
        if (avatarUri != null) {
            loadAvatar(avatarUri)
        } else {
            binding.imgAvatar.setImageResource(R.drawable.default_avatar)
        }

        binding.imgAvatar.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerLauncher.launch(intent)
        }

        binding.btnLogout.setOnClickListener {
            UserSession.logout(requireContext())
            startActivity(Intent(requireContext(), StartActivity::class.java))
            requireActivity().finish()
        }
    }

    // 壓縮圖片為 256x256，依大小調整品質，儲存為使用者專屬頭像
    private fun compressAndSaveAvatar(uri: Uri) {
        try {
            val username = UserSession.getUsername(requireContext())!!
            val source = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.createSource(requireContext().contentResolver, uri)
            } else return

            val originalBitmap = ImageDecoder.decodeBitmap(source)
            val resized = Bitmap.createScaledBitmap(originalBitmap, 256, 256, true)

            val inputSize = requireContext().contentResolver.openInputStream(uri)?.available() ?: 0
            val quality = if (inputSize > 1024 * 1024) 70 else 85

            val file = File(requireContext().cacheDir, "avatar_$username.jpg")
            val outputStream = FileOutputStream(file)
            resized.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputStream.flush()
            outputStream.close()

            val compressedUri = file.toUri()
            saveAvatarUri(username, compressedUri)
            loadAvatar(compressedUri)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 將使用者帳號對應的頭像 URI 儲存進 SharedPreferences
    private fun saveAvatarUri(username: String, uri: Uri) {
        requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE)
            .edit().putString("avatar_uri_$username", uri.toString()).apply()
    }

    // 取得該帳號儲存的頭像 URI（若尚未儲存則回傳 null）
    private fun getAvatarUri(username: String): Uri? {
        val saved = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE)
            .getString("avatar_uri_$username", null)
        return saved?.toUri()
    }

    private fun loadAvatar(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .circleCrop()
            .signature(com.bumptech.glide.signature.ObjectKey(System.currentTimeMillis())) // 強制 Glide 使用新的識別避免 cache
            .into(binding.imgAvatar)
    }

    private fun isSupportedImageFormat(mimeType: String): Boolean {
        return mimeType == "image/jpeg" || mimeType == "image/png"
    }

    // 確認檔案大小是否小於指定最大值（預設為 5MB）
    private fun isFileSizeAcceptable(uri: Uri, maxBytes: Int = 5 * 1024 * 1024): Boolean {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val size = inputStream?.available() ?: 0
            inputStream?.close()
            size <= maxBytes
        } catch (e: Exception) {
            false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
