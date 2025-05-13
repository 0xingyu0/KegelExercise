// LoginFragment.kt
package com.example.myapplication.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.MainActivity
import com.example.myapplication.databinding.FragmentLoginBinding
import com.example.myapplication.utils.UserStorage
import com.example.myapplication.utils.UserSession

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val args = arguments?.let { LoginFragmentArgs.fromBundle(it) }
        args?.username?.let {
            binding.etUsername.setText(it)
        }

        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString()
            val password = binding.etPassword.text.toString()
            val valid = UserStorage.verifyUser(requireContext(), username, password)

            if (valid) {
                UserSession.setLoggedIn(requireContext(), true)
                UserSession.setUsername(requireContext(), username)
                startActivity(Intent(requireContext(), MainActivity::class.java))
                requireActivity().finish()
            } else {
                Toast.makeText(requireContext(), "帳號或密碼錯誤", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnToRegister.setOnClickListener {
            findNavController().navigate(LoginFragmentDirections.actionLoginToRegister())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
