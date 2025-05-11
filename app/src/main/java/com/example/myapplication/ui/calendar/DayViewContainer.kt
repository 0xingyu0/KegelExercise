// DayViewContainer.kt
package com.example.myapplication.ui.calendar

import android.view.View
import android.widget.TextView
import com.kizitonwose.calendar.view.ViewContainer
import com.example.myapplication.R

class DayViewContainer(view: View) : ViewContainer(view) {
    lateinit var day: com.kizitonwose.calendar.core.CalendarDay
    val textView: TextView = view.findViewById(R.id.dayText)
    val dotView: View = view.findViewById(R.id.dotView)
}
