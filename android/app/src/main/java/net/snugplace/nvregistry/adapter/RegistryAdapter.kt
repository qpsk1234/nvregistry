package net.snugplace.nvregistry.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.snugplace.nvregistry.R
import net.snugplace.nvregistry.model.RegistryEntry
import net.snugplace.nvregistry.util.PayloadParser

class RegistryAdapter(private val onItemClick: (RegistryEntry) -> Unit) :
    RecyclerView.Adapter<RegistryAdapter.ViewHolder>() {

    private var items: List<RegistryEntry> = emptyList()
    private var isDarkTheme: Boolean = true
    private var changedNames: Set<String> = emptySet()
    private var mismatchedNames: Set<String> = emptySet()
    private var latestPayloads: Map<String, String> = emptyMap()

    fun submitList(newList: List<RegistryEntry>) {
        items = newList
        notifyDataSetChanged()
    }

    fun setDarkTheme(dark: Boolean) {
        isDarkTheme = dark
        notifyDataSetChanged()
    }

    /** 差分情報を更新する（JSON読込後にMainActivityから呼ぶ） */
    fun updateDiffState(
        changed: Set<String>,
        mismatched: Set<String>,
        latestMap: Map<String, String>
    ) {
        changedNames = changed
        mismatchedNames = mismatched
        latestPayloads = latestMap
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.registry_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, isDarkTheme, changedNames, mismatchedNames, latestPayloads[item.RegistryName])
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val nameTextView: TextView = view.findViewById(R.id.nameTextView)
        private val typeTextView: TextView = view.findViewById(R.id.typeTextView)
        private val hexTextView: TextView = view.findViewById(R.id.hexTextView)
        private val decTextView: TextView = view.findViewById(R.id.decTextView)
        private val changedIndicator: TextView = view.findViewById(R.id.changedIndicator)

        fun bind(
            item: RegistryEntry,
            isDark: Boolean,
            changedNames: Set<String>,
            mismatchedNames: Set<String>,
            latestPayload: String?
        ) {
            val isMismatched = item.RegistryName in mismatchedNames
            val isChanged = item.RegistryName in changedNames

            // 背景色：不一致 > 変更済み > デフォルト
            val bgColor = when {
                isMismatched -> if (isDark) 0xFF3D2B00.toInt() else 0xFFFFF3E0.toInt()
                isChanged    -> if (isDark) 0xFF002B1E.toInt() else 0xFFE8F5E9.toInt()
                else         -> if (isDark) 0xFF1E1E1E.toInt() else 0xFFFFFFFF.toInt()
            }
            itemView.setBackgroundColor(bgColor)

            // インジケーター
            when {
                isMismatched -> {
                    changedIndicator.visibility = View.VISIBLE
                    changedIndicator.text = itemView.context.getString(R.string.indicator_mismatched)
                    changedIndicator.setTextColor(0xFFFF8C00.toInt())
                }
                isChanged -> {
                    changedIndicator.visibility = View.VISIBLE
                    changedIndicator.text = itemView.context.getString(R.string.indicator_changed)
                    changedIndicator.setTextColor(0xFF03DAC5.toInt())
                }
                else -> changedIndicator.visibility = View.GONE
            }

            val textColor = if (isDark) 0xFFE0E0E0.toInt() else 0xFF212121.toInt()
            val subColor = if (isDark) 0xFF9E9E9E.toInt() else 0xFF555555.toInt()
            val hexColor = if (isDark) 0xFFBB86FC.toInt() else 0xFF6200EE.toInt()
            val decColor = if (isDark) 0xFFFFB74D.toInt() else 0xFFE65100.toInt()

            nameTextView.setTextColor(textColor)
            typeTextView.setTextColor(subColor)
            hexTextView.setTextColor(hexColor)
            decTextView.setTextColor(decColor)

            nameTextView.text = item.RegistryName
            typeTextView.text = "${item.TypeName}  size=${item.Size}  count=${item.Count}"

            val actualPayload = latestPayload ?: item.Payload
            val parsed = PayloadParser.parse(actualPayload, item.TypeName, item.Size, item.Count)
            hexTextView.text = PayloadParser.summarize(parsed, item.TypeName, item.Count)
            decTextView.text = PayloadParser.summarizeDec(parsed, item.Count)
        }
    }
}
