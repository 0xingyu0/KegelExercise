// CalendarUtils.kt
package com.example.myapplication.utils

import android.view.View
import android.widget.TextView
import com.example.myapplication.R
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.yearMonth
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import java.time.LocalDate

object CalendarUtils {

    fun setupCalendar(
        calendarView: CalendarView,
        trainedDates: MutableSet<LocalDate>,
        today: LocalDate = LocalDate.now()
    ) {
        val startMonth = today.minusMonths(12).yearMonth
        val endMonth = today.plusMonths(12).yearMonth
        val daysOfWeek = daysOfWeek()

        calendarView.setup(startMonth, endMonth, daysOfWeek.first())
        calendarView.scrollToMonth(today.yearMonth)

        calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)

            override fun bind(container: DayViewContainer, day: CalendarDay) {
                container.day = day
                container.textView.text = day.date.dayOfMonth.toString()
                container.textView.alpha = if (day.position == DayPosition.MonthDate) 1f else 0.3f

                if (trainedDates.contains(day.date)) {
                    container.dotView.visibility = View.VISIBLE
                } else {
                    container.dotView.visibility = View.GONE
                }
            }
        }

        // 模擬今天完成訓練（可刪除或改為按鈕事件）
        trainedDates.add(today)
        calendarView.notifyDateChanged(today)
    }

    class DayViewContainer(view: View) : ViewContainer(view) {
        lateinit var day: CalendarDay
        val textView: TextView = view.findViewById(R.id.dayText)
        val dotView: View = view.findViewById(R.id.dotView)
    }
} 
