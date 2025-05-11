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
import java.time.YearMonth

object CalendarUtils {

    fun setupCalendar(
        calendarView: CalendarView,
        onMonthScroll: ((yearMonth: YearMonth) -> Unit),
        onDateClick: ((LocalDate) -> Unit),
        shouldShowDot: (LocalDate) -> Boolean
    ) {
        val today = LocalDate.now()
        val startMonth = today.minusMonths(1).yearMonth
        val endMonth = today.plusMonths(1).yearMonth
        val daysOfWeek = daysOfWeek()

        calendarView.setup(startMonth, endMonth, daysOfWeek.first())
        calendarView.scrollToMonth(today.yearMonth)

        calendarView.monthScrollListener = {
            onMonthScroll(it.yearMonth)
        }

        calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)

            override fun bind(container: DayViewContainer, day: CalendarDay) {
                container.day = day
                container.textView.text = day.date.dayOfMonth.toString()
                container.textView.alpha = if (day.position == DayPosition.MonthDate) 1f else 0.3f

                container.dotView.visibility =
                    if (day.position == DayPosition.MonthDate && shouldShowDot(day.date)) View.VISIBLE
                    else View.GONE

                container.view.setOnClickListener {
                    if (day.position == DayPosition.MonthDate) {
                        onDateClick(day.date)
                    }
                }
            }
        }
    }

    class DayViewContainer(view: View) : ViewContainer(view) {
        lateinit var day: CalendarDay
        val textView: TextView = view.findViewById(R.id.dayText)
        val dotView: View = view.findViewById(R.id.dotView)
    }
}
