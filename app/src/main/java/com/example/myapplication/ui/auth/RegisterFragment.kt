package com.example.myapplication.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.databinding.FragmentRegisterBinding
import com.example.myapplication.model.UserAccount
import com.example.myapplication.utils.HashUtils
import com.example.myapplication.utils.UserStorage

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnRegister.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                binding.tvError.text = "請填寫所有欄位"
                return@setOnClickListener
            }

            if (password.length < 6) {
                binding.tvError.text = "密碼長度至少需 6 碼"
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                binding.tvError.text = "密碼與確認密碼不一致"
                return@setOnClickListener
            }

            binding.tvError.text = ""

            val success = UserStorage.saveUser(
                requireContext(),
                UserAccount(username, HashUtils.sha256(password))
            )

            if (success) {
                Toast.makeText(requireContext(), "註冊成功，請登入", Toast.LENGTH_SHORT).show()
                val action = RegisterFragmentDirections.actionRegisterToLogin(username)
                findNavController().navigate(action)
            } else {
                binding.tvError.text = "名字已存在"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
