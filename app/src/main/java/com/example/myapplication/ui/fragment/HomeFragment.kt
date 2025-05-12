// HomeFragment.kt
package com.example.myapplication.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentHomeBinding
import com.example.myapplication.model.TrainingRecord
import com.example.myapplication.utils.CalendarUtils
import com.example.myapplication.utils.TrainingStorage
import com.kizitonwose.calendar.core.yearMonth
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val trainingRecords = mutableListOf<TrainingRecord>()
    private val trainedDates = mutableSetOf<LocalDate>()
    private val cachedDots = mutableSetOf<LocalDate>()
    private lateinit var recordMap: Map<YearMonth, List<TrainingRecord>>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val startLoadTime = System.currentTimeMillis()
            val records = TrainingStorage.load(requireContext())
            val dates = records.map { LocalDate.parse(it.date) }.toSet()
            val endLoadTime = System.currentTimeMillis()

            Log.d("HomeFragment", "資料載入耗時: ${endLoadTime - startLoadTime}ms")

            withContext(Dispatchers.Main) {
                trainingRecords.clear()
                trainingRecords.addAll(records)
                trainedDates.clear()
                trainedDates.addAll(dates)

                // 延後到畫面繪製完成後再設定 CalendarView
                binding.calendarView.post {
                    // 依月份分組
                    recordMap = trainingRecords.groupBy { LocalDate.parse(it.date).yearMonth }

                    CalendarUtils.setupCalendar(
                        calendarView = binding.calendarView,
                        onMonthScroll = { yearMonth ->
                            val formatter = DateTimeFormatter.ofPattern("yyyy年M月", Locale.getDefault())
                            binding.textCurrentMonth.text = yearMonth.format(formatter)

                            val thisMonthRecords = recordMap[yearMonth] ?: emptyList()
                            updateCalendarDots(yearMonth, thisMonthRecords)
                        },
                        onDateClick = { selectedDate ->
                            val record = trainingRecords.find { it.date == selectedDate.toString() }
                            record?.let {
                                AlertDialog.Builder(requireContext())
                                    .setTitle("訓練紀錄：${it.date}")
                                    .setMessage(it.details)
                                    .setPositiveButton("關閉", null)
                                    .setNegativeButton("刪除") { _, _ ->
                                        trainingRecords.remove(it)
                                        cachedDots.remove(LocalDate.parse(it.date))
                                        TrainingStorage.save(requireContext(), trainingRecords)
                                        binding.calendarView.notifyDateChanged(LocalDate.parse(it.date))

                                        AlertDialog.Builder(requireContext())
                                            .setMessage("已刪除 ${it.date} 的訓練紀錄")
                                            .setPositiveButton("OK", null)
                                            .show()
                                    }
                                    .show()
                            }
                        },
                        shouldShowDot = { date -> cachedDots.contains(date) }
                    )

                    // 初始月份資料
                    val todayMonth = LocalDate.now().yearMonth
                    val todayRecords = recordMap[todayMonth] ?: emptyList()
                    updateCalendarDots(todayMonth, todayRecords)
                }

            }

            // 設定新增訓練按鈕
            binding.btnAddTodayTraining.setOnClickListener {
                val today = LocalDate.now()
                if (trainingRecords.any { it.date == today.toString() }) {
                    AlertDialog.Builder(requireContext())
                        .setMessage("今天已經有訓練紀錄了！")
                        .setPositiveButton("確定", null)
                        .show()
                    return@setOnClickListener
                }

                val dialogView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.dialog_add_training, null)
                val numberPicker = dialogView.findViewById<NumberPicker>(R.id.numberPicker)
                val spinner = dialogView.findViewById<Spinner>(R.id.spinnerFeel)

                numberPicker.minValue = 1
                numberPicker.maxValue = 10

                val feelings = listOf("很好", "普通", "有點困難")
                val adapter =
                    ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, feelings)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = adapter

                AlertDialog.Builder(requireContext())
                    .setTitle("新增訓練紀錄")
                    .setView(dialogView)
                    .setPositiveButton("儲存") { _, _ ->
                        val count = numberPicker.value
                        val feeling = spinner.selectedItem.toString()
                        val details = "完成訓練 $count 次，感受：$feeling"

                        val newRecord = TrainingRecord(today.toString(), details)
                        trainingRecords.add(newRecord)
                        trainedDates.add(today)
                        TrainingStorage.save(requireContext(), trainingRecords)
                        binding.calendarView.notifyDateChanged(today)

                        AlertDialog.Builder(requireContext())
                            .setMessage("已新增今天的訓練紀錄！")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        }
    }

    private fun updateCalendarDots(month: YearMonth, records: List<TrainingRecord>) {
        val dates = records.map { LocalDate.parse(it.date) }
        cachedDots.clear()
        cachedDots.addAll(dates)
        dates.forEach {
            binding.calendarView.notifyDateChanged(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
