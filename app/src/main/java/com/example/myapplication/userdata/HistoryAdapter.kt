package com.example.myapplication.userdata

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.userdata.module.HistoryItem
import com.example.myapplication.R

class HistoryAdapter(private val historyList: List<HistoryItem>) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewDate: TextView = itemView.findViewById(R.id.textViewDate)
        val textViewWeight: TextView = itemView.findViewById(R.id.textViewWeight)
        val textViewMinutes: TextView = itemView.findViewById(R.id.textViewMinutes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = historyList[position]
        holder.textViewDate.text = item.date
        holder.textViewWeight.text = item.weight.toString()
        holder.textViewMinutes.text = item.minutes.toString()
    }

    override fun getItemCount(): Int {
        return historyList.size
    }
}