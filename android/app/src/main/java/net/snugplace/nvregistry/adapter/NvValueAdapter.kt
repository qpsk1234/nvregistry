package net.snugplace.nvregistry.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.snugplace.nvregistry.R
import net.snugplace.nvregistry.util.PayloadParser

class NvValueAdapter(
    private var elements: List<PayloadParser.ParsedElement>,
    private val jsonElements: List<PayloadParser.ParsedElement>, // JSON初期値（リセット用）
    private var isDarkTheme: Boolean = true,
    private val onSetClick: (index: Int, hexInput: String) -> Unit
) : RecyclerView.Adapter<NvValueAdapter.ViewHolder>() {

    fun updateElements(newElements: List<PayloadParser.ParsedElement>) {
        elements = newElements
        notifyDataSetChanged()
    }

    fun setTheme(dark: Boolean) {
        isDarkTheme = dark
        notifyDataSetChanged()
    }

    /** 全行のEditTextをJSON初期値に戻す */
    fun resetAllToJson() {
        notifyDataSetChanged() // rebind後にjsonElements参照を使う
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.nv_value_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(elements[position], jsonElements.getOrNull(position), isDarkTheme)
    }

    override fun getItemCount() = elements.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val indexLabel: TextView = view.findViewById(R.id.nvIndexLabel)
        private val hexLabel: TextView = view.findViewById(R.id.nvHexLabel)
        private val decLabel: TextView = view.findViewById(R.id.nvDecLabel)
        private val editText: EditText = view.findViewById(R.id.nvEditText)
        private val setButton: Button = view.findViewById(R.id.nvSetButton)
        private val resetButton: Button = view.findViewById(R.id.nvResetButton)

        fun bind(
            element: PayloadParser.ParsedElement,
            jsonElement: PayloadParser.ParsedElement?,
            isDark: Boolean
        ) {
            indexLabel.text = "[${element.index}]"
            hexLabel.text = element.hexDisplay
            decLabel.text = "(${element.decValue})"
            editText.setText(element.hexDisplay.removePrefix("0x"))

            // JSON値と現在値が異なる場合はhexLabelを橙色で強調
            val hasChanged = jsonElement != null && jsonElement.hexDisplay != element.hexDisplay
            hexLabel.setTextColor(
                if (hasChanged) 0xFFFF8C00.toInt()
                else if (isDark) 0xFFBB86FC.toInt() else 0xFF6200EE.toInt()
            )

            if (isDark) {
                editText.setBackgroundColor(0xFFE0E0E0.toInt())
                editText.setTextColor(Color.BLACK)
                editText.setHintTextColor(0xFF666666.toInt())
                itemView.setBackgroundColor(0xFF2A2A2A.toInt())
                indexLabel.setTextColor(0xFF888888.toInt())
                decLabel.setTextColor(0xFF9E9E9E.toInt())
            } else {
                editText.setBackgroundColor(0xFF323232.toInt())
                editText.setTextColor(Color.WHITE)
                editText.setHintTextColor(0xFFAAAAAA.toInt())
                itemView.setBackgroundColor(0xFFF5F5F5.toInt())
                indexLabel.setTextColor(0xFF555555.toInt())
                decLabel.setTextColor(0xFF555555.toInt())
            }

            // ↺ボタン：EditTextをJSON値でプリフィル
            if (jsonElement != null) {
                resetButton.visibility = View.VISIBLE
                resetButton.setOnClickListener {
                    editText.setText(jsonElement.hexDisplay.removePrefix("0x"))
                }
            } else {
                resetButton.visibility = View.GONE
            }

            setButton.setOnClickListener {
                onSetClick(element.index, editText.text.toString().trim())
            }
        }
    }
}
