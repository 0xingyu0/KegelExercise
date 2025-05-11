// TrainFragment.kt
package com.example.myapplication.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.FragmentTrainBinding
import com.example.myapplication.ui.activity.PoseLandmarkerActivity
import com.example.myapplication.R
class TrainFragment : Fragment() {

    private var _binding: FragmentTrainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonHaveSensor.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, BluetoothConnectFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.buttonNoSensor.setOnClickListener {
            val intent = Intent(requireContext(), PoseLandmarkerActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
