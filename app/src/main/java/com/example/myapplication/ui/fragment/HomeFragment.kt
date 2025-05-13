package com.example.myapplication.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.databinding.FragmentHomeBinding
import com.example.myapplication.model.CheckInRecord
import com.example.myapplication.utils.CalendarUtils
import com.example.myapplication.utils.CheckInStorage
import com.example.myapplication.utils.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val cachedDots = mutableSetOf<LocalDate>()
    private lateinit var username: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        username = UserSession.getUsername(requireContext()) ?: "guest"

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val records = CheckInStorage.load(requireContext(), username)
            val dates = records.map { LocalDate.parse(it.date) }

            withContext(Dispatchers.Main) {
                cachedDots.addAll(dates)

                binding.calendarView.post {
                    CalendarUtils.setupCalendar(
                        calendarView = binding.calendarView,
                        onMonthScroll = { yearMonth ->
                            val formatter = DateTimeFormatter.ofPattern("yyyy年M月", Locale.getDefault())
                            binding.textCurrentMonth.text = yearMonth.format(formatter)
                        },
                        onDateClick = { /* 不處理點擊日期 */ },
                        shouldShowDot = { date -> cachedDots.contains(date) }
                    )

                    updateCalendarDots(YearMonth.now())
                }

                binding.btnCheckIn.setOnClickListener {
                    val today = LocalDate.now()
                    if (cachedDots.contains(today)) {
                        AlertDialog.Builder(requireContext())
                            .setMessage("今天已經簽到過了！")
                            .setPositiveButton("確定", null)
                            .show()
                    } else {
                        cachedDots.add(today)

                        val updatedRecords = cachedDots.map {
                            CheckInRecord(it.toString())
                        }

                        CheckInStorage.save(requireContext(), username, updatedRecords)
                        binding.calendarView.notifyDateChanged(today)

                        AlertDialog.Builder(requireContext())
                            .setMessage("簽到成功！")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            }
        }
    }

    private fun updateCalendarDots(month: YearMonth) {
        cachedDots.filter { it.year == month.year && it.month == month.month }
            .forEach { binding.calendarView.notifyDateChanged(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
