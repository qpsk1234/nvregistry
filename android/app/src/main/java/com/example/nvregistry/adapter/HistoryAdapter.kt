package com.example.nvregistry.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.nvregistry.R
import com.example.nvregistry.model.ChangeRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val records: List<ChangeRecord>,
    private val onNameClick: (ChangeRecord) -> Unit  // RegistryNameクリックコールバック
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPAN)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.history_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(records[position])
    }

    override fun getItemCount() = records.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val timestamp: TextView = view.findViewById(R.id.historyTimestamp)
        private val name: TextView = view.findViewById(R.id.historyName)
        private val jsonValue: TextView = view.findViewById(R.id.historyJsonValue)
        private val beforeValue: TextView = view.findViewById(R.id.historyBeforeValue)
        private val newValue: TextView = view.findViewById(R.id.historyNewValue)
        private val afterGetValue: TextView = view.findViewById(R.id.historyAfterGetValue)
        private val status: TextView = view.findViewById(R.id.historyStatus)

        fun bind(record: ChangeRecord) {
            timestamp.text = dateFormat.format(Date(record.timestamp))
            name.text = record.registryName

            // RegistryNameをタップするとGET/SET画面へ遷移
            name.setTextColor(0xFF4FC3F7.toInt()) // リンク風の水色
            name.paintFlags = name.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
            name.setOnClickListener { onNameClick(record) }

            jsonValue.text = itemView.context.getString(
                R.string.history_json_value, record.jsonPayload.take(40) + if (record.jsonPayload.length > 40) "..." else "")
            beforeValue.text = itemView.context.getString(R.string.history_before_value, record.valueBeforeChange)
            newValue.text = itemView.context.getString(R.string.history_new_value, record.newValue)
            afterGetValue.text = itemView.context.getString(R.string.history_after_get, record.postSetGetResult.take(120))

            val successText = if (record.success)
                itemView.context.getString(R.string.history_success)
            else
                itemView.context.getString(R.string.history_failure)
            status.text = successText
            status.setTextColor(
                if (record.success)
                    itemView.context.getColor(android.R.color.holo_green_light)
                else
                    itemView.context.getColor(android.R.color.holo_red_light)
            )
        }
    }
}
