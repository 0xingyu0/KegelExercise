package com.example.myapplication.ui.fragment

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.*
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.FragmentBluetoothConnectBinding
import java.util.*

class BluetoothConnectFragment : Fragment() {

    private var _binding: FragmentBluetoothConnectBinding? = null
    private val binding get() = _binding!!

    private val bluetoothAdapter: BluetoothAdapter? by lazy { BluetoothAdapter.getDefaultAdapter() }
    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 2000L

    private val targetDeviceName = "你的感測器名稱" // ← 替換為你的 Arduino 名稱
    private val targetUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // SPP UUID

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBluetoothConnectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.loadingText.text = "Loding..."
        checkPermissionsAndStart()
    }

    private fun checkPermissionsAndStart() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(requireActivity(), notGranted.toTypedArray(), 100)
        } else {
            startBluetoothConnectionLoop()
        }
    }

    private fun startBluetoothConnectionLoop() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                // 確保已取得 BLUETOOTH_CONNECT 權限
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                            Manifest.permission.BLUETOOTH_CONNECT
                        else
                            Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    binding.loadingText.text = "權限不足，無法存取藍牙"
                    return
                }

                val pairedDevices = bluetoothAdapter?.bondedDevices
                val targetDevice = pairedDevices?.find { it.name == targetDeviceName }

                if (targetDevice != null) {
                    binding.loadingText.text = "已找到裝置，嘗試連接..."
                    try {
                        val socket = targetDevice.createRfcommSocketToServiceRecord(targetUUID)
                        socket.connect()
                        bluetoothAdapter?.cancelDiscovery()
                        binding.loadingText.text = "連接成功！"

                        // 接收 Arduino 傳來資料
                        startReceiveThread(socket)

                        // 若連線後不需要換畫面可註解以下
                        // parentFragmentManager.beginTransaction()
                        //     .replace(R.id.fragmentContainer, NextTrainingFragment())
                        //     .commit()
                        return
                    } catch (e: SecurityException) {
                        binding.loadingText.text = "權限不足：${e.message}"
                    } catch (e: Exception) {
                        binding.loadingText.text = "連接失敗，重新嘗試中..."
                    }
                } else {
                    binding.loadingText.text = "尚未找到配對裝置，請確認藍牙已開啟並配對"
                }

                handler.postDelayed(this, checkInterval)
            }
        }, checkInterval)
    }

    private fun startReceiveThread(socket: BluetoothSocket) {
        Thread {
            try {
                val inputStream = socket.inputStream
                val buffer = ByteArray(1024)

                while (true) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead > 0) {
                        val message = String(buffer, 0, bytesRead)
                        requireActivity().runOnUiThread {
                            binding.receivedText.text = "接收資料：$message"
                        }
                    }
                }
            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    binding.receivedText.text = "資料接收失敗：${e.message}"
                }
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        handler.removeCallbacksAndMessages(null)
    }
}
