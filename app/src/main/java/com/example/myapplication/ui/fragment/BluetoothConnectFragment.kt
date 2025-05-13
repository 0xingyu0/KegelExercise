package com.example.myapplication.ui.fragment

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.*
import android.view.*
import android.widget.Toast
import android.content.Intent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.FragmentBluetoothConnectBinding
import java.io.IOException
import java.util.*

class BluetoothConnectFragment : Fragment() {

    private var _binding: FragmentBluetoothConnectBinding? = null
    private val bluetoothAdapter: BluetoothAdapter? by lazy { BluetoothAdapter.getDefaultAdapter() }
    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 2000L

    private val targetDeviceName = "ESP32_Pressure" // ← 替換為你的 Arduino 名稱
    private val targetUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // SPP UUID

    private var isReceiving = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBluetoothConnectBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding?.loadingText?.text = "Loading..."
        checkPermissionsAndStart()
    }

    private fun checkPermissionsAndStart() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startBluetoothConnectionLoop()
        } else {
            Toast.makeText(requireContext(), "未授權藍牙權限", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startBluetoothConnectionLoop() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!isAdded) return  // fragment 已移除不再執行

                val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                } else {
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                }

                if (!hasPermission) {
                    _binding?.loadingText?.text = "權限不足，無法存取藍牙"
                    return
                }

                val pairedDevices = bluetoothAdapter?.bondedDevices
                val targetDevice = pairedDevices?.find { it.name == targetDeviceName }

                if (targetDevice != null) {
                    _binding?.loadingText?.text = "已找到裝置，嘗試連接..."
                    try {
                        val socket = targetDevice.createRfcommSocketToServiceRecord(targetUUID)
                        socket.connect()
                        bluetoothAdapter?.cancelDiscovery()
                        _binding?.loadingText?.text = "連接成功！"
                        // 連線成功後顯示按鈕
                        _binding?.startGameButton?.visibility = View.VISIBLE

                        // 設定按鈕點擊事件
                        _binding?.startGameButton?.setOnClickListener {
                            val intent = Intent(requireContext(), com.example.myapplication.ui.activity.AnimalSelectionActivity::class.java)
                            startActivity(intent)
                            activity?.finish()
                        }
                        startReceiveThread(socket)
                        return
                    } catch (e: SecurityException) {
                        _binding?.loadingText?.text = "權限不足：${e.message}"
                    } catch (e: Exception) {
                        _binding?.loadingText?.text = "連接失敗，重新嘗試中..."
                    }
                } else {
                    _binding?.loadingText?.text = "尚未找到配對裝置，請確認藍牙已開啟並配對"
                }

                handler.postDelayed(this, checkInterval)
            }
        }, checkInterval)
    }

    private fun startReceiveThread(socket: BluetoothSocket) {
        isReceiving = true
        Thread {
            try {
                val inputStream = socket.inputStream
                val buffer = ByteArray(1024)

                while (isReceiving) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead > 0) {
                        val message = String(buffer, 0, bytesRead)
                        activity?.runOnUiThread {
                            _binding?.receivedText?.text = "接收資料：$message"
                        }
                    }
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    _binding?.receivedText?.text = "資料接收失敗：${e.message}"
                }
                try {
                    socket.close()
                } catch (_: IOException) { }
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isReceiving = false
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }
}
